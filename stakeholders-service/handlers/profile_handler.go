package handlers

import (
	"errors"
	"net/http"
	"stakeholders-service/repository"
	"time"

	"github.com/gin-gonic/gin"
	"gorm.io/gorm"
)

type ProfileHandler struct {
	UserRepo *repository.UserRepository
}

type UpdateProfileRequest struct {
	FirstName       *string `json:"firstName"`
	LastName        *string `json:"lastName"`
	ProfileImageUrl *string `json:"profileImageUrl"`
	Bio             *string `json:"bio"`
	Motto           *string `json:"motto"`
}

func NewProfileHandler(userRepo *repository.UserRepository) *ProfileHandler {
	return &ProfileHandler{UserRepo: userRepo}
}

func (h *ProfileHandler) GetProfile(c *gin.Context) {
	userID := c.Param("id")

	profile, err := h.UserRepo.GetProfileByUserID(userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Profile not found"})
		return
	}

	c.JSON(http.StatusOK, profile)
}

func (h *ProfileHandler) UpdateProfile(c *gin.Context) {
	userID := c.Param("id")
	requesterID := c.GetString("userId")

	if requesterID == "" || requesterID != userID {
		c.JSON(http.StatusForbidden, gin.H{"error": "Forbidden"})
		return
	}

	var req UpdateProfileRequest
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"error": "Invalid request"})
		return
	}

	updates := map[string]interface{}{
		"updated_at": time.Now(),
	}

	if req.FirstName != nil {
		updates["first_name"] = *req.FirstName
	}
	if req.LastName != nil {
		updates["last_name"] = *req.LastName
	}
	if req.ProfileImageUrl != nil {
		updates["profile_image_url"] = *req.ProfileImageUrl
	}
	if req.Bio != nil {
		updates["bio"] = *req.Bio
	}
	if req.Motto != nil {
		updates["motto"] = *req.Motto
	}

	err := h.UserRepo.UpdateProfileByUserID(userID, updates)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"error": "Profile not found"})
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"error": "Failed to update profile"})
		return
	}

	profile, err := h.UserRepo.GetProfileByUserID(userID)
	if err != nil {
		c.JSON(http.StatusNotFound, gin.H{"error": "Profile not found"})
		return
	}

	c.JSON(http.StatusOK, profile)
}
