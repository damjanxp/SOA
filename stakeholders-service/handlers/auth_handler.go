package handlers

import (
	"net/http"
	"stakeholders-service/middleware"
	"stakeholders-service/models"
	"stakeholders-service/repository"

	"github.com/gin-gonic/gin"
	"golang.org/x/crypto/bcrypt"
)

type AuthHandler struct {
	UserRepo *repository.UserRepository
}

func NewAuthHandler(userRepo *repository.UserRepository) *AuthHandler {
	return &AuthHandler{UserRepo: userRepo}
}

func (h *AuthHandler) Register(c *gin.Context) {
	var input struct {
		Username string      `json:"username" binding:"required"`
		Password string      `json:"password" binding:"required,min=8"`
		Email    string      `json:"email" binding:"required,email"`
		Role     models.Role `json:"role" binding:"required"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	if input.Role == models.RoleAdmin {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Ne mozete se registrovati kao admin"})
		return
	}

	if _, err := h.UserRepo.FindByUsername(input.Username); err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "Korisnicko ime vec postoji"})
		return
	}

	if _, err := h.UserRepo.FindByEmail(input.Email); err == nil {
		c.JSON(http.StatusConflict, gin.H{"error": "Email vec postoji"})
		return
	}

	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(input.Password), bcrypt.DefaultCost)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Greska pri registraciji"})
		return
	}

	user := &models.User{
		Username: input.Username,
		Password: string(hashedPassword),
		Email:    input.Email,
		Role:     input.Role,
	}

	if err := h.UserRepo.CreateUser(user); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Greska pri kreiranju korisnika"})
		return
	}

	profile := &models.Profile{UserID: user.ID}
	h.UserRepo.CreateProfile(profile)

	c.JSON(http.StatusCreated, gin.H{"data": user})
}

func (h *AuthHandler) Login(c *gin.Context) {
	var input struct {
		Username string `json:"username" binding:"required"`
		Password string `json:"password" binding:"required"`
	}

	if err := c.ShouldBindJSON(&input); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": err.Error()})
		return
	}

	user, err := h.UserRepo.FindByUsername(input.Username)
	if err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Pogresni kredencijali"})
		return
	}

	if user.IsBlocked {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Nalog je blokiran"})
		return
	}

	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(input.Password)); err != nil {
		c.JSON(http.StatusUnauthorized, gin.H{"error": "Pogresni kredencijali"})
		return
	}

	token, err := middleware.GenerateToken(user.ID, user.Username, string(user.Role))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Greska pri generisanju tokena"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"token": token})
}
