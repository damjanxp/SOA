package handlers

import (
	"context"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/neo4j/neo4j-go-driver/v5/neo4j"
)

// FollowerHandler holds the Neo4j driver used by all follower-related handlers.
type FollowerHandler struct {
	Driver neo4j.DriverWithContext
}

// Follow handles POST /api/followers/follow/:userId
// Creates a FOLLOWS relationship from the authenticated user to the target user.
func (h *FollowerHandler) Follow(c *gin.Context) {
	followerId := c.GetString("userId")
	followerUsername := c.GetString("username")
	targetUserId := c.Param("userId")

	if followerId == targetUserId {
		c.JSON(http.StatusBadRequest, gin.H{"error": "you cannot follow yourself"})
		return
	}

	ctx := context.Background()
	session := h.Driver.NewSession(ctx, neo4j.SessionConfig{AccessMode: neo4j.AccessModeWrite})
	defer session.Close(ctx)

	_, err := session.ExecuteWrite(ctx, func(tx neo4j.ManagedTransaction) (any, error) {
		query := `
			MERGE (a:User {userId: $followerId})
			ON CREATE SET a.username = $followerUsername
			MERGE (b:User {userId: $targetUserId})
			MERGE (a)-[:FOLLOWS {since: datetime()}]->(b)
		`
		_, err := tx.Run(ctx, query, map[string]any{
			"followerId":       followerId,
			"followerUsername": followerUsername,
			"targetUserId":     targetUserId,
		})
		return nil, err
	})

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to follow user"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Following"})
}

// Unfollow handles DELETE /api/followers/unfollow/:userId
// Removes the FOLLOWS relationship from the authenticated user to the target user.
func (h *FollowerHandler) Unfollow(c *gin.Context) {
	followerId := c.GetString("userId")
	targetUserId := c.Param("userId")

	ctx := context.Background()
	session := h.Driver.NewSession(ctx, neo4j.SessionConfig{AccessMode: neo4j.AccessModeWrite})
	defer session.Close(ctx)

	_, err := session.ExecuteWrite(ctx, func(tx neo4j.ManagedTransaction) (any, error) {
		query := `
			MATCH (a:User {userId: $followerId})-[r:FOLLOWS]->(b:User {userId: $targetUserId})
			DELETE r
		`
		_, err := tx.Run(ctx, query, map[string]any{
			"followerId":   followerId,
			"targetUserId": targetUserId,
		})
		return nil, err
	})

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to unfollow user"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"message": "Unfollowed"})
}

// IsFollowing handles GET /api/followers/is-following/:userId
// Returns whether the authenticated user follows the target user.
func (h *FollowerHandler) IsFollowing(c *gin.Context) {
	followerId := c.GetString("userId")
	targetUserId := c.Param("userId")

	ctx := context.Background()
	session := h.Driver.NewSession(ctx, neo4j.SessionConfig{AccessMode: neo4j.AccessModeRead})
	defer session.Close(ctx)

	result, err := session.ExecuteRead(ctx, func(tx neo4j.ManagedTransaction) (any, error) {
		query := `
			MATCH (a:User {userId: $followerId})-[:FOLLOWS]->(b:User {userId: $targetUserId})
			RETURN count(*) > 0 AS isFollowing
		`
		res, err := tx.Run(ctx, query, map[string]any{
			"followerId":   followerId,
			"targetUserId": targetUserId,
		})
		if err != nil {
			return false, err
		}
		if res.Next(ctx) {
			val, _ := res.Record().Get("isFollowing")
			return val, nil
		}
		return false, nil
	})

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to check follow status"})
		return
	}

	c.JSON(http.StatusOK, gin.H{"isFollowing": result})
}

