package clients

import (
	"context"
	"log"
	"os"

	pb "github.com/damjanxp/gateway/pb/tour"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials/insecure"
)

// TourGrpcClient wraps the generated gRPC client for tour-service.
type TourGrpcClient struct {
	conn   *grpc.ClientConn
	client pb.TourServiceClient
}

// NewTourGrpcClient dials tour-service gRPC server.
// Address is read from TOUR_GRPC_URL env var (default: tour-service:9093).
func NewTourGrpcClient() *TourGrpcClient {
	addr := os.Getenv("TOUR_GRPC_URL")
	if addr == "" {
		addr = "tour-service:9093"
	}

	conn, err := grpc.NewClient(addr, grpc.WithTransportCredentials(insecure.NewCredentials()))
	if err != nil {
		log.Fatalf("Failed to connect to tour-service gRPC at %s: %v", addr, err)
	}

	log.Printf("TourGrpcClient connected to %s", addr)
	return &TourGrpcClient{
		conn:   conn,
		client: pb.NewTourServiceClient(conn),
	}
}

// Close gracefully closes the gRPC connection.
func (t *TourGrpcClient) Close() {
	if t.conn != nil {
		t.conn.Close()
	}
}

// PublishTour calls TourService.PublishTour via gRPC.
func (t *TourGrpcClient) PublishTour(ctx context.Context, tourId, authorId string) (*pb.TourActionResponse, error) {
	return t.client.PublishTour(ctx, &pb.PublishTourRequest{
		TourId:   tourId,
		AuthorId: authorId,
	})
}

// ArchiveTour calls TourService.ArchiveTour via gRPC.
func (t *TourGrpcClient) ArchiveTour(ctx context.Context, tourId string) (*pb.TourActionResponse, error) {
	return t.client.ArchiveTour(ctx, &pb.TourIdRequest{
		TourId: tourId,
	})
}

// ReactivateTour calls TourService.ReactivateTour via gRPC.
func (t *TourGrpcClient) ReactivateTour(ctx context.Context, tourId string) (*pb.TourActionResponse, error) {
	return t.client.ReactivateTour(ctx, &pb.TourIdRequest{
		TourId: tourId,
	})
}

