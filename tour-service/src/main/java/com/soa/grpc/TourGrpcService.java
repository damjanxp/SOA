package com.soa.grpc;

import com.soa.grpc.proto.PublishTourRequest;
import com.soa.grpc.proto.TourActionResponse;
import com.soa.grpc.proto.TourIdRequest;
import com.soa.grpc.proto.TourServiceGrpc;
import com.soa.models.Tour;
import com.soa.models.Tour.TourStatus;
import com.soa.repositories.KeypointRepository;
import com.soa.repositories.TourRepository;
import com.soa.repositories.TransportTimeRepository;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@GrpcService
@Transactional
public class TourGrpcService extends TourServiceGrpc.TourServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TourGrpcService.class);

    @Autowired
    private TourRepository tourRepository;

    @Autowired
    private KeypointRepository keypointRepository;

    @Autowired
    private TransportTimeRepository transportTimeRepository;

    // ─── PublishTour ─────────────────────────────────────────────────────────────

    @Override
    public void publishTour(PublishTourRequest request,
                            StreamObserver<TourActionResponse> responseObserver) {
        log.info("gRPC publishTour called for tourId={}", request.getTourId());

        try {
            // 1. Pronadji turu po tourId
            Long tourId = Long.parseLong(request.getTourId());
            Optional<Tour> tourOpt = tourRepository.findById(tourId);

            if (tourOpt.isEmpty()) {
                responseObserver.onNext(TourActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Tour not found")
                        .setTourId(request.getTourId())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            Tour tour = tourOpt.get();

            // 2. Provjeri authorId
            log.info("authorId from DB: '{}', authorId from request: '{}'", tour.getAuthorId(), request.getAuthorId());
            if (!tour.getAuthorId().equals(request.getAuthorId())) {
                responseObserver.onNext(TourActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Not authorized")
                        .setTourId(request.getTourId())
                        .setStatus(tour.getStatus().name())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // 3. Provjeri status == DRAFT
            if (tour.getStatus() != TourStatus.DRAFT) {
                responseObserver.onNext(TourActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Only draft tours can be published")
                        .setTourId(request.getTourId())
                        .setStatus(tour.getStatus().name())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // 4. Provjeri broj kljucnih tacaka (min 2)
            long keypointCount = keypointRepository.countByTourId(tour.getId());
            if (keypointCount < 2) {
                responseObserver.onNext(TourActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Tour must have at least 2 keypoints before publishing")
                        .setTourId(request.getTourId())
                        .setStatus(tour.getStatus().name())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // 5. Provjeri transport vremena (min 1)
            long transportCount = transportTimeRepository.countByTourId(tour.getId());
            if (transportCount < 1) {
                responseObserver.onNext(TourActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Tour must have at least one transport time (WALK, BIKE or CAR) before publishing")
                        .setTourId(request.getTourId())
                        .setStatus(tour.getStatus().name())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // 6. Provjeri obavezna polja: name, description, difficulty, tags
            boolean missingFields = isBlank(tour.getName())
                    || isBlank(tour.getDescription())
                    || tour.getDifficulty() == null
                    || tour.getTags() == null
                    || tour.getTags().isEmpty();

            if (missingFields) {
                responseObserver.onNext(TourActionResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Tour is missing required fields: name, description, difficulty, tags")
                        .setTourId(request.getTourId())
                        .setStatus(tour.getStatus().name())
                        .build());
                responseObserver.onCompleted();
                return;
            }

            // 7. Sve validacije prosle — objavi turu
            tour.setStatus(TourStatus.PUBLISHED);
            tour.setPublishedAt(LocalDateTime.now());
            tourRepository.save(tour);

            log.info("Tour {} published successfully", tourId);
            responseObserver.onNext(TourActionResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Tour published successfully")
                    .setTourId(request.getTourId())
                    .setStatus("PUBLISHED")
                    .build());
            responseObserver.onCompleted();

        } catch (NumberFormatException e) {
            log.error("Invalid tour ID format: {}", request.getTourId());
            responseObserver.onNext(TourActionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Invalid tour ID format")
                    .setTourId(request.getTourId())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error publishing tour {}: {}", request.getTourId(), e.getMessage(), e);
            responseObserver.onNext(TourActionResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Internal error: " + e.getMessage())
                    .setTourId(request.getTourId())
                    .build());
            responseObserver.onCompleted();
        }
    }

    // ─── ArchiveTour ─────────────────────────────────────────────────────────────

    @Override
    public void archiveTour(TourIdRequest request,
                            StreamObserver<TourActionResponse> responseObserver) {
        log.info("gRPC archiveTour called for tourId={}", request.getTourId());

        try {
            Long tourId = Long.parseLong(request.getTourId());
            Optional<Tour> optTour = tourRepository.findById(tourId);

            if (optTour.isEmpty()) {
                respond(responseObserver, false, "Tour not found", request.getTourId(), "");
                return;
            }

            Tour tour = optTour.get();

            if (tour.getStatus() != TourStatus.PUBLISHED) {
                respond(responseObserver, false,
                        "Only published tours can be archived (current status: " + tour.getStatus() + ")",
                        request.getTourId(), tour.getStatus().name());
                return;
            }

            tour.setStatus(TourStatus.ARCHIVED);
            tour.setArchivedAt(LocalDateTime.now());
            tourRepository.save(tour);

            log.info("Tour {} archived successfully", tourId);
            respond(responseObserver, true, "Tour archived successfully", request.getTourId(), "ARCHIVED");

        } catch (NumberFormatException e) {
            respond(responseObserver, false, "Invalid tour ID format", request.getTourId(), "");
        } catch (Exception e) {
            log.error("Error archiving tour {}: {}", request.getTourId(), e.getMessage(), e);
            respond(responseObserver, false, "Internal error: " + e.getMessage(), request.getTourId(), "");
        }
    }

    // ─── ReactivateTour ──────────────────────────────────────────────────────────

    @Override
    public void reactivateTour(TourIdRequest request,
                               StreamObserver<TourActionResponse> responseObserver) {
        log.info("gRPC reactivateTour called for tourId={}", request.getTourId());

        try {
            Long tourId = Long.parseLong(request.getTourId());
            Optional<Tour> optTour = tourRepository.findById(tourId);

            if (optTour.isEmpty()) {
                respond(responseObserver, false, "Tour not found", request.getTourId(), "");
                return;
            }

            Tour tour = optTour.get();

            if (tour.getStatus() != TourStatus.ARCHIVED) {
                respond(responseObserver, false,
                        "Only archived tours can be reactivated (current status: " + tour.getStatus() + ")",
                        request.getTourId(), tour.getStatus().name());
                return;
            }

            tour.setStatus(TourStatus.PUBLISHED);
            tour.setArchivedAt(null);
            tourRepository.save(tour);

            log.info("Tour {} reactivated successfully", tourId);
            respond(responseObserver, true, "Tour reactivated successfully", request.getTourId(), "PUBLISHED");

        } catch (NumberFormatException e) {
            respond(responseObserver, false, "Invalid tour ID format", request.getTourId(), "");
        } catch (Exception e) {
            log.error("Error reactivating tour {}: {}", request.getTourId(), e.getMessage(), e);
            respond(responseObserver, false, "Internal error: " + e.getMessage(), request.getTourId(), "");
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────────

    private void respond(StreamObserver<TourActionResponse> observer,
                         boolean success, String message, String tourId, String status) {
        TourActionResponse response = TourActionResponse.newBuilder()
                .setSuccess(success)
                .setMessage(message)
                .setTourId(tourId)
                .setStatus(status)
                .build();
        observer.onNext(response);
        observer.onCompleted();
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}

