package models

import (
    "time"
)

type Role string

const (
    RoleTourist Role = "tourist"
    RoleGuide   Role = "guide"
    RoleAdmin   Role = "admin"
)

type User struct {
    ID        string    `json:"id" gorm:"primaryKey;type:uuid;default:gen_random_uuid()"`
    Username  string    `json:"username" gorm:"unique;not null"`
    Password  string    `json:"-" gorm:"not null"`
    Email     string    `json:"email" gorm:"unique;not null"`
    Role      Role      `json:"role" gorm:"not null"`
    IsBlocked bool      `json:"isBlocked" gorm:"default:false"`
    CreatedAt time.Time `json:"createdAt"`
}