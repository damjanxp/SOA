package com.soa.controllers;

import com.soa.dtos.CartItemRequest;
import com.soa.dtos.CartResponse;
import com.soa.dtos.TourPurchaseTokenResponse;
import com.soa.models.TourPurchaseToken;
import com.soa.saga.CheckoutSaga;
import com.soa.services.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CheckoutSaga checkoutSaga;

    /**
     * Add a tour to the cart.
     * POST /api/cart/{touristId}/items   body: { "tourId": 1 }
     */
    @PostMapping("/{touristId}/items")
    public ResponseEntity<CartResponse> addItem(
            @PathVariable String touristId,
            @Valid @RequestBody CartItemRequest request,
            HttpServletRequest httpRequest) {

        verifyOwnership(touristId, httpRequest);
        CartResponse response = cartService.addItem(touristId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove a tour from the cart and recalculate the total.
     * DELETE /api/cart/{touristId}/items/{tourId}
     */
    @DeleteMapping("/{touristId}/items/{tourId}")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable String touristId,
            @PathVariable Long tourId,
            HttpServletRequest httpRequest) {

        verifyOwnership(touristId, httpRequest);
        CartResponse response = cartService.removeItem(touristId, tourId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get the current cart with all items and the calculated total.
     * GET /api/cart/{touristId}
     */
    @GetMapping("/{touristId}")
    public ResponseEntity<CartResponse> getCart(
            @PathVariable String touristId,
            HttpServletRequest httpRequest) {

        verifyOwnership(touristId, httpRequest);
        CartResponse response = cartService.getCart(touristId);
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger the CheckoutSaga for this tourist's cart.
     * POST /api/cart/{touristId}/checkout
     *
     * Returns a list of TourPurchaseToken objects on success.
     * Returns 400 if any tour in the cart is no longer available.
     */
    @PostMapping("/{touristId}/checkout")
    public ResponseEntity<?> checkout(
            @PathVariable String touristId,
            HttpServletRequest httpRequest) {

        verifyOwnership(touristId, httpRequest);

        try {
            List<TourPurchaseToken> tokens = checkoutSaga.execute(touristId);
            List<TourPurchaseTokenResponse> response = tokens.stream()
                    .map(this::mapToken)
                    .toList();
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Ensures the authenticated user is accessing their own cart.
     */
    private void verifyOwnership(String touristId, HttpServletRequest request) {
        String userId = getUserIdFromRequest(request);
        if (!userId.equals(touristId)) {
            throw new RuntimeException("Forbidden: you can only access your own cart");
        }
    }

    private String getUserIdFromRequest(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("Unauthorized: User ID not found");
        }
        return (String) userIdObj;
    }

    private TourPurchaseTokenResponse mapToken(TourPurchaseToken t) {
        return TourPurchaseTokenResponse.builder()
                .id(t.getId())
                .touristId(t.getTouristId())
                .tourId(t.getTourId())
                .token(t.getToken())
                .purchasedAt(t.getPurchasedAt())
                .build();
    }
}
