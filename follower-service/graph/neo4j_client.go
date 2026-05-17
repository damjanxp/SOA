package graph

import (
	"context"
	"fmt"
	"os"

	"github.com/neo4j/neo4j-go-driver/v5/neo4j"
)

// ConnectNeo4j reads connection details from environment variables and returns a Neo4j driver.
func ConnectNeo4j() (neo4j.DriverWithContext, error) {
	uri := os.Getenv("NEO4J_URI")
	user := os.Getenv("NEO4J_USER")
	password := os.Getenv("NEO4J_PASSWORD")

	if uri == "" || user == "" || password == "" {
		return nil, fmt.Errorf("missing Neo4j environment variables (NEO4J_URI, NEO4J_USER, NEO4J_PASSWORD)")
	}

	driver, err := neo4j.NewDriverWithContext(uri, neo4j.BasicAuth(user, password, ""))
	if err != nil {
		return nil, fmt.Errorf("failed to create Neo4j driver: %w", err)
	}

	return driver, nil
}

// VerifyConnectivity checks that the Neo4j driver can reach the database (health check).
func VerifyConnectivity(driver neo4j.DriverWithContext) error {
	ctx := context.Background()
	if err := driver.VerifyConnectivity(ctx); err != nil {
		return fmt.Errorf("Neo4j connectivity check failed: %w", err)
	}
	return nil
}

