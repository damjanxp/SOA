package saga

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/damjanxp/gateway/clients"
)

type PublishTourSAGA struct {
	tourGrpcClient  *clients.TourGrpcClient
	stakeholdersURL string
	internalSecret  string
}

func NewPublishTourSAGA(tourClient *clients.TourGrpcClient) *PublishTourSAGA {
	return &PublishTourSAGA{
		tourGrpcClient:  tourClient,
		stakeholdersURL: os.Getenv("STAKEHOLDERS_SERVICE_URL"),
		internalSecret:  os.Getenv("INTERNAL_SECRET"),
	}
}

func (s *PublishTourSAGA) Execute(ctx context.Context, tourId, authorId, tourName string) error {
	// ─── KORAK 1: Objavi turu preko gRPC ────────────────────────────────────────
	log.Printf("[SAGA][PublishTour] Korak 1: Objavljujem turu %s", tourId)

	resp, err := s.tourGrpcClient.PublishTour(ctx, tourId, authorId)
	if err != nil {
		return fmt.Errorf("SAGA failed at step 1: gRPC error: %w", err)
	}
	if !resp.GetSuccess() {
		return fmt.Errorf("SAGA failed at step 1: %s", resp.GetMessage())
	}

	log.Printf("[SAGA][PublishTour] Korak 1 uspjesan: tura u statusu %s", resp.GetStatus())

	// ─── KORAK 2: Notifikacija pratilaca ────────────────────────────────────────
	log.Printf("[SAGA][PublishTour] Korak 2: Saljem notifikaciju pratiocima")

	notifyBody := map[string]string{
		"authorId": authorId,
		"tourId":   tourId,
		"tourName": tourName,
		"type":     "TOUR_PUBLISHED",
	}
	jsonBody, _ := json.Marshal(notifyBody)

	req, err := http.NewRequestWithContext(ctx, "POST",
		s.stakeholdersURL+"/api/internal/notify-followers",
		bytes.NewBuffer(jsonBody))
	if err != nil {
		return s.compensate(ctx, tourId, "failed to build notification request")
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Secret", s.internalSecret)

	httpClient := &http.Client{Timeout: 5 * time.Second}
	notifyResp, err := httpClient.Do(req)

	if err != nil || notifyResp.StatusCode != http.StatusOK {
		reason := "notification request failed"
		if err != nil {
			reason = err.Error()
		} else {
			reason = fmt.Sprintf("notification returned status %d", notifyResp.StatusCode)
		}
		return s.compensate(ctx, tourId, reason)
	}

	log.Printf("[SAGA][PublishTour] SAGA uspjesno zavrsena: tura %s objavljena", tourId)
	return nil
}

// compensate vraca turu na ARCHIVED pa je SAGA oznacena kao neuspjesna.
// ReactivateTour vraca ARCHIVED -> PUBLISHED, ArchiveTour vraca PUBLISHED -> ARCHIVED.
// Posto je tura tek objavljena (PUBLISHED), pozivamo ArchiveTour kao kompenzaciju.
func (s *PublishTourSAGA) compensate(ctx context.Context, tourId, reason string) error {
	log.Printf("[SAGA][PublishTour] Korak 2 FAIL (%s) - pokrecemo kompenzaciju", reason)

	compResp, compErr := s.tourGrpcClient.ArchiveTour(ctx, tourId)
	if compErr != nil {
		log.Printf("[SAGA][PublishTour] Kompenzacija FAIL: %v", compErr)
	} else {
		log.Printf("[SAGA][PublishTour] Kompenzacija: tura arhivirana, status=%s", compResp.GetStatus())
	}

	return fmt.Errorf("SAGA failed at step 2: %s, tour reverted", reason)
}

