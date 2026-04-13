package main

import (
	"log"
	"os"
	"stakeholders-service/handlers"
	"stakeholders-service/middleware"
	"stakeholders-service/models"
	"stakeholders-service/repository"

	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func main() {
	// Loaduj .env fajl
	godotenv.Load()
	dsn := "host=" + os.Getenv("DB_HOST") +
		" user=" + os.Getenv("DB_USER") +
		" password=" + os.Getenv("DB_PASSWORD") +
		" dbname=" + os.Getenv("DB_NAME") +
		" port=" + os.Getenv("DB_PORT") +
		" sslmode=disable"

	db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{})
	if err != nil {
		log.Fatal("Greska pri konekciji na bazu: ", err)
	}

	db.AutoMigrate(&models.User{}, &models.Profile{})

	userRepo := repository.NewUserRepository(db)
	authHandler := handlers.NewAuthHandler(userRepo)
	userHandler := handlers.NewUserHandler(userRepo)

	r := gin.Default()

	// CORS konfiguracija
	r.Use(func(c *gin.Context) {
		c.Writer.Header().Set("Access-Control-Allow-Origin", c.Request.Header.Get("Origin"))
		if c.Request.Header.Get("Origin") == "" {
			c.Writer.Header().Set("Access-Control-Allow-Origin", "*")
		}
		c.Writer.Header().Set("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, OPTIONS")
		c.Writer.Header().Set("Access-Control-Allow-Headers", "Content-Type, Accept, Authorization, X-CSRF-Token, X-Requested-With")
		c.Writer.Header().Set("Access-Control-Allow-Credentials", "true")

		if c.Request.Method == "OPTIONS" {
			c.Writer.WriteHeader(204)
			return
		}

		c.Next()
	})

	r.POST("/api/auth/register", authHandler.Register)
	r.POST("/api/auth/login", authHandler.Login)

	protected := r.Group("/api")
	protected.Use(middleware.AuthMiddleware())
	{
		protected.GET("/users", userHandler.GetAllUsers)
	}

	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	log.Println("Servis pokrenut na portu: " + port)
	r.Run(":" + port)
}
