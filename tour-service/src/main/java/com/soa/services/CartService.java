package com.soa.services;

import com.soa.dtos.CartItemRequest;
import com.soa.dtos.CartResponse;
import com.soa.dtos.OrderItemResponse;
import com.soa.models.OrderItem;
import com.soa.models.ShoppingCart;
import com.soa.models.Tour;
import com.soa.repositories.ShoppingCartRepository;
import com.soa.repositories.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CartService {

    private final ShoppingCartRepository shoppingCartRepository;
    private final TourRepository tourRepository;

    /**
     * Adds a tour to the tourist's cart.
     * Creates the cart if it does not exist yet.
     * Rejects the request if the tour is already in the cart (PENDING).
     */
    public CartResponse addItem(String touristId, CartItemRequest request) {
        Tour tour = tourRepository.findById(request.getTourId())
                .orElseThrow(() -> new RuntimeException("Tour not found with id: " + request.getTourId()));

        if (tour.getStatus() != Tour.TourStatus.PUBLISHED) {
            throw new RuntimeException("Only published tours can be added to cart");
        }

        ShoppingCart cart = shoppingCartRepository.findByTouristId(touristId)
                .orElseGet(() -> shoppingCartRepository.save(
                        ShoppingCart.builder().touristId(touristId).build()));

        boolean alreadyInCart = cart.getItems().stream()
                .anyMatch(i -> i.getTourId().equals(request.getTourId())
                               && i.getStatus() == OrderItem.ItemStatus.PENDING);

        if (alreadyInCart) {
            throw new RuntimeException("Tour is already in the cart");
        }

        OrderItem item = OrderItem.builder()
                .cart(cart)
                .tourId(tour.getId())
                .tourName(tour.getName())
                .price(tour.getPrice())
                .build();

        cart.getItems().add(item);
        cart.recalculateTotalPrice();
        return mapToResponse(shoppingCartRepository.save(cart));
    }

    /**
     * Removes a tour from the tourist's cart and recalculates the total.
     */
    public CartResponse removeItem(String touristId, Long tourId) {
        ShoppingCart cart = shoppingCartRepository.findByTouristId(touristId)
                .orElseThrow(() -> new RuntimeException("Cart not found for tourist: " + touristId));

        boolean removed = cart.getItems().removeIf(
                i -> i.getTourId().equals(tourId) && i.getStatus() == OrderItem.ItemStatus.PENDING);

        if (!removed) {
            throw new RuntimeException("Tour with id " + tourId + " is not in the cart");
        }

        cart.recalculateTotalPrice();
        return mapToResponse(shoppingCartRepository.save(cart));
    }

    /**
     * Returns the tourist's cart, or an empty cart response if none exists yet.
     */
    @Transactional(readOnly = true)
    public CartResponse getCart(String touristId) {
        return shoppingCartRepository.findByTouristId(touristId)
                .map(this::mapToResponse)
                .orElseGet(() -> CartResponse.builder()
                        .touristId(touristId)
                        .items(List.of())
                        .totalPrice(BigDecimal.ZERO)
                        .build());
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    public CartResponse mapToResponse(ShoppingCart cart) {
        List<OrderItemResponse> itemResponses = cart.getItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .cartId(cart.getId())
                        .tourId(item.getTourId())
                        .tourName(item.getTourName())
                        .price(item.getPrice())
                        .status(item.getStatus().name())
                        .build())
                .toList();

        return CartResponse.builder()
                .id(cart.getId())
                .touristId(cart.getTouristId())
                .items(itemResponses)
                .totalPrice(cart.getTotalPrice())
                .createdAt(cart.getCreatedAt())
                .build();
    }
}
