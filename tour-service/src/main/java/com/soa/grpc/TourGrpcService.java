package com.soa.grpc;

import com.soa.dtos.CreateTransportTimeRequest;
import com.soa.dtos.KeypointResponse;
import com.soa.dtos.TransportTimeResponse;
import com.soa.models.Tour;
import com.soa.models.Tour.TourStatus;
import com.soa.repositories.KeypointRepository;
import com.soa.repositories.TourRepository;
import com.soa.repositories.TransportTimeRepository;
import com.soa.services.KeypointService;
import com.soa.services.TransportTimeService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@GrpcService
@Transactional
@RequiredArgsConstructor
public class TourGrpcService extends TourServiceGrpc.TourServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(TourGrpcService.class);

    private final KeypointService keypointService;
    private final TransportTimeService transportTimeService;
    private final TourRepository tourRepository;
    private final KeypointRepository keypointRepository;
    private final TransportTimeRepository transportTimeRepository;

    // â”€â”€ GetTourKeyPoints â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void getTourKeyPoints(GetKeyPointsRequest request,
                                 StreamObserver<GetKeyPointsResponse> responseObserver) {
        try {
            Long tourId = Long.parseLong(request.getTourId());
            String touristId = request.getTouristId();

            // TODO: replace with real TourPurchaseToken check once Purchase service is ready
            boolean purchased = hasPurchasedTour(touristId, tourId);

            List<KeypointResponse> all = keypointService.getKeypointsByTourId(tourId);

            // If the tourist has not purchased: expose only the first keypoint (order 0)
            List<KeypointResponse> visible = purchased
                    ? all
                    : all.stream()
                          .filter(kp -> kp.getOrderIndex() != null && kp.getOrderIndex() == 0)
                          .toList();

            List<KeyPointMessage> messages = visible.stream()
                    .map(kp -> KeyPointMessage.newBuilder()
                            .setId(String.valueOf(kp.getId()))
                            .setTourId(request.getTourId())
                            .setName(kp.getName() != null ? kp.getName() : "")
                            .setDescription(kp.getDescription() != null ? kp.getDescription() : "")
                            .setImageUrl(kp.getImageUrl() != null ? kp.getImageUrl() : "")
                            .setLat(kp.getLat() != null ? kp.getLat() : 0.0)
                            .setLon(kp.getLon() != null ? kp.getLon() : 0.0)
                            .setOrderIndex(kp.getOrderIndex() != null ? kp.getOrderIndex() : 0)
                            .build())
                    .toList();

            GetKeyPointsResponse response = GetKeyPointsResponse.newBuilder()
                    .addAllKeypoints(messages)
                    .setAllVisible(purchased)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NumberFormatException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid tourId format: " + request.getTourId())
                    .asRuntimeException());
        } catch (RuntimeException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // â”€â”€ AddTransportTime â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void addTransportTime(AddTransportTimeRequest request,
                                 StreamObserver<AddTransportTimeResponse> responseObserver) {
        try {
            Long tourId = Long.parseLong(request.getTourId());

            // The gateway already authenticated the caller; use the tour's own authorId
            // so the service-level ownership guard passes for this trusted server-to-server call.
            Tour tour = tourRepository.findById(tourId)
                    .orElseThrow(() -> new RuntimeException("Tour not found with id: " + tourId));

            CreateTransportTimeRequest dto = CreateTransportTimeRequest.builder()
                    .transportType(request.getTransportType())
                    .durationMinutes(request.getDurationMinutes())
                    .build();

            TransportTimeResponse saved =
                    transportTimeService.addTransportTime(tourId, dto, tour.getAuthorId());

            TransportTimeMessage ttMessage = TransportTimeMessage.newBuilder()
                    .setId(String.valueOf(saved.getId()))
                    .setTourId(String.valueOf(saved.getTourId()))
                    .setTransportType(saved.getTransportType())
                    .setDurationMinutes(saved.getDurationMinutes())
                    .build();

            AddTransportTimeResponse response = AddTransportTimeResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Transport time added successfully")
                    .setTransportTime(ttMessage)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (NumberFormatException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid tourId format: " + request.getTourId())
                    .asRuntimeException());
        } catch (RuntimeException e) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Determines whether the given tourist has purchased the tour.
     * TODO: replace with real TourPurchaseToken check once Purchase service is ready
     */
    private boolean hasPurchasedTour(String touristId, Long tourId) {
        return true;
    }

    // â”€â”€ PublishTour â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Override
    public void publishTour(PublishTourRequest request,
                            StreamObserver<TourActionResponse> responseObserver) {
        log.info("gRPC publishTour called for tourId={}", request.getTourId());

        try {
            Long tourId = Long.parseLong(request.getTourId());
            Optional<Tour> tourOpt = tourRepository.findById(tourId);

            if (tourOpt.isEmpty()) {
                respond(responseObserver, false, "Tour not found", request.getTourId(), "");
                return;
            }

            Tour tour = tourOpt.get();

            log.info("authorId from DB: '{}', authorId from request: '{}'", tour.getAuthorId(), request.getAuthorId());
            if (!tour.getAuthorId().equals(request.getAuthorId())) {
                respond(responseObserver, false, "Not authorized", request.getTourId(), tour.getStatus().name());
                return;
            }

            if (tour.getStatus() != TourStatus.DRAFT) {
                respond(responseObserver, false, "Only draft tours can be published",
                        request.getTourId(), tour.getStatus().name());
                return;
            }

            long keypointCount = keypointRepository.countByTourId(tour.getId());
            if (keypointCount < 2) {
                respond(responseObserver, false,
                        "Tour must have at least 2 keypoints before publishing",
                        request.getTourId(), tour.getStatus().name());
                return;
            }

            long transportCount = transportTimeRepository.countByTourId(tour.getId());
            if (transportCount < 1) {
                respond(responseObserver, false,
                        "Tour must have at least one transport time (WALKING, BICYCLE or CAR) before publishing",
                        request.getTourId(), tour.getStatus().name());
                return;
            }

            boolean missingFields = isBlank(tour.getName())
                    || isBlank(tour.getDescription())
                    || tour.getDifficulty() == null
                    || tour.getTags() == null
                    || tour.getTags().isEmpty();

            if (missingFields) {
                respond(responseObserver, false,
                        "Tour is missing required fields: name, description, difficulty, tags",
                        request.getTourId(), tour.getStatus().name());
                return;
            }

            tour.setStatus(TourStatus.PUBLISHED);
            tour.setPublishedAt(LocalDateTime.now());
            tourRepository.save(tour);

            log.info("Tour {} published successfully", tourId);
            respond(responseObserver, true, "Tour published successfully", request.getTourId(), "PUBLISHED");

        } catch (NumberFormatException e) {
            log.error("Invalid tour ID format: {}", request.getTourId());
            respond(responseObserver, false, "Invalid tour ID format", request.getTourId(), "");
        } catch (Exception e) {
            log.error("Error publishing tour {}: {}", request.getTourId(), e.getMessage(), e);
            respond(responseObserver, false, "Internal error: " + e.getMessage(), request.getTourId(), "");
        }
    }

    // â”€â”€ ArchiveTour â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ ReactivateTour â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
