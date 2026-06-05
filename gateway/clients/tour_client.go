package clients

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"

	pb "github.com/damjanxp/gateway/pb/tour"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

type TourGrpcClient struct {
	conn       *grpc.ClientConn
	client     pb.TourServiceClient
	restURL    string
	httpClient *http.Client
}

func NewTourGrpcClient() *TourGrpcClient {
	addr := os.Getenv("TOUR_GRPC_URL")
	if addr == "" {
		addr = "tour-service:9093"
	}

	restURL := os.Getenv("TOUR_SERVICE_URL")
	if restURL == "" {
		restURL = "http://tour-service:8080"
	}

	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("Failed to connect to tour-service gRPC at %s: %v", addr, err)
	}

	log.Printf("TourGrpcClient connected to gRPC at %s and REST at %s", addr, restURL)
	return &TourGrpcClient{
		conn:       conn,
		client:     pb.NewTourServiceClient(conn),
		restURL:    restURL,
		httpClient: &http.Client{},
	}
}

func (t *TourGrpcClient) Close() {
	if t.conn != nil {
		t.conn.Close()
	}
}

func (t *TourGrpcClient) PublishTour(ctx context.Context, tourId, authorId string) (*pb.TourActionResponse, error) {
	return t.client.PublishTour(ctx, &pb.PublishTourRequest{
		TourId:   tourId,
		AuthorId: authorId,
	})
}

func (t *TourGrpcClient) ArchiveTour(ctx context.Context, tourId string) (*pb.TourActionResponse, error) {
	return t.client.ArchiveTour(ctx, &pb.TourIdRequest{
		TourId: tourId,
	})
}

func (t *TourGrpcClient) ReactivateTour(ctx context.Context, tourId string) (*pb.TourActionResponse, error) {
	return t.client.ReactivateTour(ctx, &pb.TourIdRequest{
		TourId: tourId,
	})
}

func (t *TourGrpcClient) StartTourExecution(ctx context.Context, touristId, tourId string, startLat, startLong float64) (*pb.StartTourExecutionResponse, error) {
	return t.client.StartTourExecution(ctx, &pb.StartTourExecutionRequest{
		TouristId: touristId,
		TourId:    tourId,
		StartLat:  startLat,
		StartLong: startLong,
	})
}

func (t *TourGrpcClient) CheckNearbyKeyPoint(ctx context.Context, executionId int64, lat, lon float64) (*pb.CheckNearbyKeyPointResponse, error) {
	return t.client.CheckNearbyKeyPoint(ctx, &pb.CheckNearbyKeyPointRequest{
		ExecutionId: executionId,
		Lat:         lat,
		Lon:         lon,
	})
}

func (t *TourGrpcClient) AddToCart(ctx context.Context, touristId, tourId string) (map[string]interface{}, error) {
	url := fmt.Sprintf("%s/api/cart/%s/items", t.restURL, touristId)

	payload := map[string]interface{}{
		"tourId": tourId,
	}

	body, err := json.Marshal(payload)
	if err != nil {
		return nil, err
	}

	req, err := http.NewRequestWithContext(ctx, "POST", url, bytes.NewReader(body))
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := t.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	if resp.StatusCode >= 400 {
		return nil, fmt.Errorf("REST API error: %s", string(respBody))
	}

	var cartResp map[string]interface{}
	if err := json.Unmarshal(respBody, &cartResp); err != nil {
		return nil, err
	}

	return cartResp, nil
}

func (t *TourGrpcClient) Checkout(ctx context.Context, touristId string) ([]map[string]interface{}, error) {
	url := fmt.Sprintf("%s/api/cart/%s/checkout", t.restURL, touristId)

	req, err := http.NewRequestWithContext(ctx, "POST", url, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := t.httpClient.Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	respBody, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	if resp.StatusCode >= 400 {
		return nil, fmt.Errorf("REST API error: %s", string(respBody))
	}

	var checkoutResp []map[string]interface{}
	if err := json.Unmarshal(respBody, &checkoutResp); err != nil {
		return nil, err
	}

	return checkoutResp, nil
}

func getStringValue(data map[string]interface{}, key string) string {
	if val, ok := data[key]; ok {
		if str, ok := val.(string); ok {
			return str
		}
	}
	return ""
}

func getFloatValue(data map[string]interface{}, key string) float64 {
	if val, ok := data[key]; ok {
		if num, ok := val.(float64); ok {
			return num
		}
	}
	return 0
}
