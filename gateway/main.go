package main

import (
	"log"
	"os"

	"github.com/damjanxp/gateway/routes"
	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
)

func main() {
	// Load environment variables from .env file
	if err := godotenv.Load(); err != nil {
		log.Println("No .env file found, reading from environment")
	}

	// Initialize Gin router with default middleware (Logger + Recovery)
	r := gin.Default()

	// CORS — allow Angular dev server
	r.Use(cors.New(cors.Config{
		AllowOrigins:     []string{"http://localhost:4200"},
		AllowMethods:     []string{"GET", "POST", "PUT", "DELETE", "OPTIONS"},
		AllowHeaders:     []string{"Origin", "Content-Type", "Authorization", "X-User-Id", "X-User-Role"},
		ExposeHeaders:    []string{"Content-Length"},
		AllowCredentials: true,
	}))

	// Register all routes
	routes.SetupRouter(r)

	// Determine port
	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("API Gateway running on port %s", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatalf("Failed to start gateway: %v", err)
	}
}
