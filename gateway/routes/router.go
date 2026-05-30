package routes

import (
	"bytes"
	"io"
	"net/http"
	"net/url"
	"os"

	"github.com/damjanxp/gateway/clients"
	"github.com/damjanxp/gateway/middleware"
	"github.com/damjanxp/gateway/saga"
	"github.com/gin-gonic/gin"
)

// ReverseProxy forwards the incoming request to targetURL, appending the original
// path and query string. It injects X-User-Id and X-User-Role headers from the
// Gin context (populated by AuthMiddleware) and copies the response back to the client.
func ReverseProxy(targetURL string) gin.HandlerFunc {
	return func(c *gin.Context) {
		// Build the full destination URL
		target, err := url.Parse(targetURL)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Invalid target URL"})
			return
		}

		target.Path = c.Request.URL.Path
		target.RawQuery = c.Request.URL.RawQuery

		// Read body into buffer so it can be forwarded correctly.
		// Passing c.Request.Body directly can cause EOF if Gin middleware already touched the stream.
		bodyBytes, err := io.ReadAll(c.Request.Body)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to read request body"})
			return
		}

		// Create the outgoing request with the buffered body
		proxyReq, err := http.NewRequest(c.Request.Method, target.String(), bytes.NewReader(bodyBytes))
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to create proxy request"})
			return
		}

		// Copy all original headers
		for key, values := range c.Request.Header {
			for _, v := range values {
				proxyReq.Header.Add(key, v)
			}
		}

		// Set correct Content-Length so upstream doesn't get EOF
		proxyReq.ContentLength = int64(len(bodyBytes))

		// Inject user identity headers from JWT context (set by AuthMiddleware)
		if userId, exists := c.Get("userId"); exists {
			proxyReq.Header.Set("X-User-Id", userId.(string))
		}
		if role, exists := c.Get("role"); exists {
			proxyReq.Header.Set("X-User-Role", role.(string))
		}

		// Execute the request
		client := &http.Client{}
		resp, err := client.Do(proxyReq)
		if err != nil {
			c.JSON(http.StatusBadGateway, gin.H{"error": "Failed to reach upstream service"})
			return
		}
		defer resp.Body.Close()

		// Copy response headers
		for key, values := range resp.Header {
			for _, v := range values {
				c.Header(key, v)
			}
		}

		// Copy status code and body
		c.Status(resp.StatusCode)
		io.Copy(c.Writer, resp.Body)
	}
}

// corsMiddleware sets CORS headers and short-circuits OPTIONS preflight requests.
func corsMiddleware() gin.HandlerFunc {
	return func(c *gin.Context) {
		c.Header("Access-Control-Allow-Origin", "*")
		c.Header("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS")
		c.Header("Access-Control-Allow-Headers", "Origin, Content-Type, Authorization, X-User-Id, X-User-Role")

		if c.Request.Method == http.MethodOptions {
			c.AbortWithStatus(http.StatusNoContent)
			return
		}
		c.Next()
	}
}

// SetupRouter configures all gateway routes on the provided Gin engine.
func SetupRouter(r *gin.Engine, tourGrpc *clients.TourGrpcClient, publishSaga *saga.PublishTourSAGA) {
	// Disable automatic redirect for trailing slashes — prevents Authorization header being
	// dropped when e.g. /api/blogs is redirected to /api/blogs/ by Gin.
	r.RedirectTrailingSlash = false
	r.RedirectFixedPath = false

	// Apply CORS globally before any auth middleware so OPTIONS preflight passes
	r.Use(corsMiddleware())

	stakeholdersURL := os.Getenv("STAKEHOLDERS_SERVICE_URL")
	blogURL := os.Getenv("BLOG_SERVICE_URL")
	followerURL := os.Getenv("FOLLOWER_SERVICE_URL")
	tourURL := os.Getenv("TOUR_SERVICE_URL")

	// Health check — no auth required
	r.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "ok", "service": "gateway"})
	})

	// Unprotected auth routes (OPTIONS needed for CORS preflight)
	r.OPTIONS("/api/auth/register", func(c *gin.Context) { c.Status(http.StatusNoContent) })
	r.OPTIONS("/api/auth/login", func(c *gin.Context) { c.Status(http.StatusNoContent) })
	r.POST("/api/auth/register", ReverseProxy(stakeholdersURL))
	r.POST("/api/auth/login", ReverseProxy(stakeholdersURL))

	// Protected routes — require valid JWT
	protected := r.Group("/api", middleware.AuthMiddleware())
	{
		// Stakeholders service — match both /users and /users/...
		protected.Any("/users", ReverseProxy(stakeholdersURL))
		protected.Any("/users/*path", ReverseProxy(stakeholdersURL))
		protected.Any("/profile", ReverseProxy(stakeholdersURL))
		protected.Any("/profile/*path", ReverseProxy(stakeholdersURL))

		// Follower service
		protected.Any("/followers", ReverseProxy(followerURL))
		protected.Any("/followers/*path", ReverseProxy(followerURL))

		// Blog service
		protected.Any("/blogs", ReverseProxy(blogURL))
		protected.Any("/blogs/*path", ReverseProxy(blogURL))

		// Tour service — ture
		protected.GET("/tours/published", ReverseProxy(tourURL))
		protected.GET("/tours/my", ReverseProxy(tourURL))
		protected.POST("/tours", ReverseProxy(tourURL))
		protected.GET("/tours/:id", ReverseProxy(tourURL))
		protected.PUT("/tours/:id", ReverseProxy(tourURL))
		protected.DELETE("/tours/:id", ReverseProxy(tourURL))
		protected.POST("/tours/:id/publish", func(c *gin.Context) {
			tourId := c.Param("id")
			authorIdRaw, _ := c.Get("userId")
			authorId, _ := authorIdRaw.(string)

			// Pokusaj da procitas tourName iz body-ja
			var body struct {
				TourName string `json:"tourName"`
			}
			if err := c.ShouldBindJSON(&body); err != nil || body.TourName == "" {
				body.TourName = "Tour #" + tourId
			}

			if err := publishSaga.Execute(c.Request.Context(), tourId, authorId, body.TourName); err != nil {
				c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
				return
			}
			c.JSON(http.StatusOK, gin.H{
				"status":  "PUBLISHED",
				"message": "Tour published successfully",
			})
		})

		protected.POST("/tours/:id/archive", func(c *gin.Context) {
			tourId := c.Param("id")

			resp, err := tourGrpc.ArchiveTour(c.Request.Context(), tourId)
			if err != nil {
				c.JSON(http.StatusBadGateway, gin.H{"error": "gRPC error: " + err.Error()})
				return
			}
			if !resp.GetSuccess() {
				c.JSON(http.StatusBadRequest, gin.H{"error": resp.GetMessage()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"status": resp.GetStatus(), "tourId": resp.GetTourId()})
		})

		protected.POST("/tours/:id/reactivate", func(c *gin.Context) {
			tourId := c.Param("id")

			resp, err := tourGrpc.ReactivateTour(c.Request.Context(), tourId)
			if err != nil {
				c.JSON(http.StatusBadGateway, gin.H{"error": "gRPC error: " + err.Error()})
				return
			}
			if !resp.GetSuccess() {
				c.JSON(http.StatusBadRequest, gin.H{"error": resp.GetMessage()})
				return
			}
			c.JSON(http.StatusOK, gin.H{"status": resp.GetStatus(), "tourId": resp.GetTourId()})
		})

		// Tour service — kljucne tacke
		protected.POST("/tours/:id/keypoints", ReverseProxy(tourURL))
		protected.GET("/tours/:id/keypoints", ReverseProxy(tourURL))
		protected.PUT("/tours/:id/keypoints/:kpId", ReverseProxy(tourURL))
		protected.DELETE("/tours/:id/keypoints/:kpId", ReverseProxy(tourURL))

		// Transport times
		protected.POST("/tours/:id/transport-times", ReverseProxy(tourURL))
		protected.GET("/tours/:id/transport-times", ReverseProxy(tourURL))
		protected.DELETE("/tours/:id/transport-times/:ttId", ReverseProxy(tourURL))

		// Tour service — recenzije
		protected.POST("/tours/:id/reviews", ReverseProxy(tourURL))
		protected.GET("/tours/:id/reviews", ReverseProxy(tourURL))

		// Tour service — korpa i kupovina
		protected.POST("/tours/:id/cart", ReverseProxy(tourURL))
		protected.GET("/cart", ReverseProxy(tourURL))
		protected.DELETE("/cart/:itemId", ReverseProxy(tourURL))
		protected.POST("/purchases", ReverseProxy(tourURL))
		protected.GET("/purchases", ReverseProxy(tourURL))
	}
}
