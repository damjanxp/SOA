package models

import "time"

type Profile struct {
    ID              string    `json:"id" gorm:"primaryKey;type:uuid;default:gen_random_uuid()"`
    UserID          string    `json:"userId" gorm:"not null"`
    FirstName       string    `json:"firstName"`
    LastName        string    `json:"lastName"`
    ProfileImageUrl string    `json:"profileImageUrl"`
    Bio             string    `json:"bio"`
    Motto           string    `json:"motto"`
    UpdatedAt       time.Time `json:"updatedAt"`
}