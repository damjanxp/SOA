package handlers

import (
	"fmt"
	"net/http"
	"os"

	"github.com/gin-gonic/gin"
)

type NotifyFollowersRequest struct {
	AuthorID string `json:"authorId" binding:"required"`
	TourID   string `json:"tourId" binding:"required"`
	TourName string `json:"tourName" binding:"required"`
	Type     string `json:"type" binding:"required"`
}

func NotifyFollowers(c *gin.Context) {
	// 1. Provjeri X-Internal-Secret header
	secret := os.Getenv("INTERNAL_SECRET")
	if c.GetHeader("X-Internal-Secret") != secret {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Invalid internal secret"})
		return
	}

	// 2. Parsiraj body
	var req NotifyFollowersRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	// 3. Loguj notifikaciju
	fmt.Printf("NOTIFICATION: Tour %s published by author %s, notifying followers\n",
		req.TourName, req.AuthorID)

	// 4. Vrati odgovor
	c.JSON(http.StatusOK, gin.H{
		"notified": true,
		"authorId": req.AuthorID,
		"tourId":   req.TourID,
	})
}

