package main

import (
	"log"
	"os"
	"stakeholders-service/handlers"
	"stakeholders-service/middleware"
	"stakeholders-service/models"
	"stakeholders-service/repository"

	"github.com/gin-gonic/gin"
	"gorm.io/driver/postgres"
	"gorm.io/gorm"
)

func main() {
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
