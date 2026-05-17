package main

import (
	"log"
	"net/http"
	"os"

	"github.com/damjanxp/follower-service/graph"
	"github.com/damjanxp/follower-service/handlers"
	"github.com/damjanxp/follower-service/middleware"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
)

func main() {
	// Load environment variables from .env file
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, reading from environment")
	}

	// Connect to Neo4j
	driver, err := graph.ConnectNeo4j()
	if err != nil {
		log.Fatalf("Failed to connect to Neo4j: %v", err)
	}
	defer driver.Close(nil)

	// Verify Neo4j connectivity
	if err := graph.VerifyConnectivity(driver); err != nil {
		log.Fatalf("Neo4j health check failed: %v", err)
	}
	log.Println("Connected to Neo4j successfully")

	// Initialize Gin router
	router := gin.Default()

	// Health check endpoint (no auth required)
	router.GET("/health", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"status": "OK"})
	})

	// Initialize handler
	followerHandler := &handlers.FollowerHandler{Driver: driver}

	// Protected API routes
	api := router.Group("/api", middleware.AuthMiddleware())
	{
		api.POST("/followers/follow/:userId", followerHandler.Follow)
		api.DELETE("/followers/unfollow/:userId", followerHandler.Unfollow)
		api.GET("/followers/is-following/:userId", followerHandler.IsFollowing)
	}

	// Start server
	port := os.Getenv("PORT")
	if port == "" {
		port = "8084"
	}

	log.Printf("follower-service running on port %s", port)
	if err := router.Run(":" + port); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}