package repository

import (
	"stakeholders-service/models"

	"gorm.io/gorm"
)

type UserRepository struct {
	DB *gorm.DB
}

func NewUserRepository(db *gorm.DB) *UserRepository {
	return &UserRepository{DB: db}
}

func (r *UserRepository) CreateUser(user *models.User) error {
	return r.DB.Create(user).Error
}

func (r *UserRepository) FindByUsername(username string) (*models.User, error) {
	var user models.User
	result := r.DB.Where("username = ?", username).First(&user)
	return &user, result.Error
}

func (r *UserRepository) FindByEmail(email string) (*models.User, error) {
	var user models.User
	result := r.DB.Where("email = ?", email).First(&user)
	return &user, result.Error
}

func (r *UserRepository) GetAllUsers() ([]models.User, error) {
	var users []models.User
	result := r.DB.Find(&users)
	return users, result.Error
}

func (r *UserRepository) CreateProfile(profile *models.Profile) error {
	return r.DB.Create(profile).Error
}

func (r *UserRepository) FindById(id string) (*models.User, error) {
	var user models.User
	result := r.DB.Where("id = ?", id).First(&user)
	return &user, result.Error
}

func (r *UserRepository) BlockUser(userID string) error {
	return r.DB.Model(&models.User{}).
		Where("id = ?", userID).
		Update("is_blocked", true).Error
}

func (r *UserRepository) GetProfileByUserID(userID string) (*models.Profile, error) {
	var profile models.Profile
	result := r.DB.Where("user_id = ?", userID).First(&profile)
	return &profile, result.Error
}

func (r *UserRepository) UpdateProfileByUserID(userID string, updates map[string]interface{}) error {
	result := r.DB.Model(&models.Profile{}).Where("user_id = ?", userID).Updates(updates)
	if result.Error != nil {
		return result.Error
	}
	if result.RowsAffected == 0 {
		return gorm.ErrRecordNotFound
	}
	return nil
}