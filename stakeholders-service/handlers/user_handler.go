package handlers

import (
	"net/http"
	"stakeholders-service/models"
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

func (h *UserHandler) BlockUser(c *gin.Context) {
	role := c.GetString("role")

	if role != "admin" {
		c.JSON(http.StatusForbidden, gin.H{"error": "Samo admin moze blokirati korisnike"})
		return
	}

	userID := c.Param("id")

	user, err := h.UserRepo.FindById(userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Korisnik ne postoji"})
		return
	}

	if user.Role == models.RoleAdmin {
		c.JSON(http.StatusForbidden, gin.H{"error": "Ne mozete blokirati admin korisnika"})
		return
	}

	if err := h.UserRepo.BlockUser(userID); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Greska pri blokiranju korisnika"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Korisnik blokiran"})
}