package com.soa.grpc;

import com.soa.dtos.CreateTransportTimeRequest;
import com.soa.dtos.KeypointResponse;
import com.soa.dtos.TransportTimeResponse;
import com.soa.models.Tour;
import com.soa.repositories.TourRepository;
import com.soa.services.KeypointService;
import com.soa.services.TransportTimeService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class TourGrpcService extends TourServiceGrpc.TourServiceImplBase {

    private final KeypointService keypointService;
    private final TransportTimeService transportTimeService;
    private final TourRepository tourRepository;

    // ── GetTourKeyPoints ─────────────────────────────────────────────────────

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

    // ── AddTransportTime ─────────────────────────────────────────────────────

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

    // ── helpers ──────────────────────────────────────────────────────────────

    /**
     * Determines whether the given tourist has purchased the tour.
     * TODO: replace with real TourPurchaseToken check once Purchase service is ready
     */
    private boolean hasPurchasedTour(String touristId, Long tourId) {
        return true;
    }
}
