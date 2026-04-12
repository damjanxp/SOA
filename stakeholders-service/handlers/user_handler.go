package handlers

import (
	"net/http"
	"stakeholders-service/repository"

	"github.com/gin-gonic/gin"
)

type UserHandler struct {
	UserRepo *repository.UserRepository
}

func NewUserHandler(userRepo *repository.UserRepository) *UserHandler {
	return &UserHandler{UserRepo: userRepo}
}

func (h *UserHandler) GetAllUsers(c *gin.Context) {
	role := c.GetString("role")
	if role != "admin" {
		c.JSON(http.StatusForbidden, gin.H{"error": "Pristup zabranjen"})
		return
	}

	users, err := h.UserRepo.GetAllUsers()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Greska pri dohvatanju korisnika"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"data": users})
}
