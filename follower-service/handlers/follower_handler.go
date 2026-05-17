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
			SET a.username = $followerUsername
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

// GetRecommendations handles GET /api/followers/:userId/recommendations
// Returns up to 10 users that the given user's friends follow but the user doesn't, ordered by mutual count.
func (h *FollowerHandler) GetRecommendations(c *gin.Context) {
	myId := c.Param("userId")

	ctx := context.Background()
	session := h.Driver.NewSession(ctx, neo4j.SessionConfig{AccessMode: neo4j.AccessModeRead})
	defer session.Close(ctx)

	type Recommendation struct {
		UserID      string `json:"userId"`
		Username    string `json:"username"`
		MutualCount int64  `json:"mutualCount"`
	}

	result, err := session.ExecuteRead(ctx, func(tx neo4j.ManagedTransaction) (any, error) {
		query := `
			MATCH (me:User {userId: $myId})-[:FOLLOWS]->(friend:User)-[:FOLLOWS]->(rec:User)
			WHERE NOT (me)-[:FOLLOWS]->(rec) AND rec.userId <> $myId
			RETURN rec, count(*) as mutual ORDER BY mutual DESC LIMIT 10
		`
		res, err := tx.Run(ctx, query, map[string]any{"myId": myId})
		if err != nil {
			return nil, err
		}

		var recommendations []Recommendation
		for res.Next(ctx) {
			record := res.Record()

			recNode, _ := record.Get("rec")
			mutual, _ := record.Get("mutual")

			node, ok := recNode.(neo4j.Node)
			if !ok {
				continue
			}

			userId, _ := node.Props["userId"].(string)
			username, _ := node.Props["username"].(string)
			mutualCount, _ := mutual.(int64)

			recommendations = append(recommendations, Recommendation{
				UserID:      userId,
				Username:    username,
				MutualCount: mutualCount,
			})
		}
		if err := res.Err(); err != nil {
			return nil, err
		}
		return recommendations, nil
	})

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to fetch recommendations"})
		return
	}

	recs, _ := result.([]Recommendation)
	if recs == nil {
		recs = []Recommendation{}
	}
	c.JSON(http.StatusOK, recs)
}

// GetFollowing handles GET /api/followers/:userId/following
// Returns the list of users that the given user follows.
func (h *FollowerHandler) GetFollowing(c *gin.Context) {
	userId := c.Param("userId")

	ctx := context.Background()
	session := h.Driver.NewSession(ctx, neo4j.SessionConfig{AccessMode: neo4j.AccessModeRead})
	defer session.Close(ctx)

	type UserEntry struct {
		UserID   string `json:"userId"`
		Username string `json:"username"`
	}

	result, err := session.ExecuteRead(ctx, func(tx neo4j.ManagedTransaction) (any, error) {
		query := `
			MATCH (me:User {userId: $userId})-[:FOLLOWS]->(u:User)
			RETURN u
		`
		res, err := tx.Run(ctx, query, map[string]any{"userId": userId})
		if err != nil {
			return nil, err
		}

		var users []UserEntry
		for res.Next(ctx) {
			record := res.Record()
			node, ok := record.Values[0].(neo4j.Node)
			if !ok {
				continue
			}
			uid, _ := node.Props["userId"].(string)
			uname, _ := node.Props["username"].(string)
			users = append(users, UserEntry{UserID: uid, Username: uname})
		}
		if err := res.Err(); err != nil {
			return nil, err
		}
		return users, nil
	})

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to fetch following"})
		return
	}

	list, _ := result.([]UserEntry)
	if list == nil {
		list = []UserEntry{}
	}
	c.JSON(http.StatusOK, list)
}

// GetFollowers handles GET /api/followers/:userId/followers
// Returns the list of users that follow the given user.
func (h *FollowerHandler) GetFollowers(c *gin.Context) {
	userId := c.Param("userId")

	ctx := context.Background()
	session := h.Driver.NewSession(ctx, neo4j.SessionConfig{AccessMode: neo4j.AccessModeRead})
	defer session.Close(ctx)

	type UserEntry struct {
		UserID   string `json:"userId"`
		Username string `json:"username"`
	}

	result, err := session.ExecuteRead(ctx, func(tx neo4j.ManagedTransaction) (any, error) {
		query := `
			MATCH (u:User)-[:FOLLOWS]->(me:User {userId: $userId})
			RETURN u
		`
		res, err := tx.Run(ctx, query, map[string]any{"userId": userId})
		if err != nil {
			return nil, err
		}

		var users []UserEntry
		for res.Next(ctx) {
			record := res.Record()
			node, ok := record.Values[0].(neo4j.Node)
			if !ok {
				continue
			}
			uid, _ := node.Props["userId"].(string)
			uname, _ := node.Props["username"].(string)
			users = append(users, UserEntry{UserID: uid, Username: uname})
		}
		if err := res.Err(); err != nil {
			return nil, err
		}
		return users, nil
	})

	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"error": "failed to fetch followers"})
		return
	}

	list, _ := result.([]UserEntry)
	if list == nil {
		list = []UserEntry{}
	}
	c.JSON(http.StatusOK, list)
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
