package com.soa.saga;

import com.soa.models.OrderItem;
import com.soa.models.ShoppingCart;
import com.soa.models.Tour;
import com.soa.models.TourPurchaseToken;
import com.soa.repositories.OrderItemRepository;
import com.soa.repositories.ShoppingCartRepository;
import com.soa.repositories.TourPurchaseTokenRepository;
import com.soa.repositories.TourRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CheckoutSaga — Orchestration SAGA for tour purchase.
 *
 * Happy path (all within one @Transactional boundary):
 *   STEP 1  — Reserve: mark all PENDING cart items as RESERVED.
 *   STEP 2  — Validate: verify every reserved tour is still PUBLISHED.
 *   STEP 3  — Commit: generate TourPurchaseTokens and clear the cart.
 *
 * Compensation (triggered when STEP 2 fails):
 *   — Set all RESERVED items back to CANCELLED (explicit undo code).
 *   — Throw RuntimeException → Spring rolls back the transaction.
 *   — Net DB effect: all items remain PENDING (both the RESERVED and
 *     CANCELLED writes are undone by the rollback).
 */
@Service
@RequiredArgsConstructor
public class CheckoutSaga {

    private final ShoppingCartRepository shoppingCartRepository;
    private final OrderItemRepository orderItemRepository;
    private final TourRepository tourRepository;
    private final TourPurchaseTokenRepository tourPurchaseTokenRepository;

    @Transactional(rollbackFor = RuntimeException.class)
    public List<TourPurchaseToken> execute(String touristId) {

        // ── SAGA STEP 1: RESERVE ─────────────────────────────────────────────
        // Load the tourist's cart and collect all PENDING items.
        // Mark each one as RESERVED to signal that checkout is in progress.

        ShoppingCart cart = shoppingCartRepository.findByTouristId(touristId)
                .orElseThrow(() -> new RuntimeException("Cart not found for tourist: " + touristId));

        List<OrderItem> pendingItems = cart.getItems().stream()
                .filter(i -> i.getStatus() == OrderItem.ItemStatus.PENDING)
                .collect(Collectors.toList());

        if (pendingItems.isEmpty()) {
            throw new RuntimeException("Cart is empty — nothing to checkout");
        }

        pendingItems.forEach(item -> item.setStatus(OrderItem.ItemStatus.RESERVED));
        orderItemRepository.saveAll(pendingItems);

        // ── SAGA STEP 2: VALIDATE TOURS ──────────────────────────────────────
        // For every reserved item, confirm the corresponding tour is still
        // PUBLISHED.  An ARCHIVED, DRAFT, or missing tour is collected for
        // the error message and triggers compensation.

        List<String> unavailableTourNames = new ArrayList<>();

        for (OrderItem item : pendingItems) {
            Tour tour = tourRepository.findById(item.getTourId()).orElse(null);

            if (tour == null || tour.getStatus() != Tour.TourStatus.PUBLISHED) {
                String name = (tour != null) ? tour.getName() : item.getTourName();
                unavailableTourNames.add(name);
            }
        }

        if (!unavailableTourNames.isEmpty()) {
            // ── COMPENSATION ─────────────────────────────────────────────────
            // Explicitly revert every RESERVED item to CANCELLED.
            // The subsequent throw causes Spring to roll back the transaction,
            // so neither the RESERVED nor the CANCELLED writes reach the DB —
            // items end up in their original PENDING state.

            pendingItems.forEach(item -> item.setStatus(OrderItem.ItemStatus.CANCELLED));
            orderItemRepository.saveAll(pendingItems);

            String names = unavailableTourNames.stream()
                    .map(n -> "'" + n + "'")
                    .collect(Collectors.joining(", "));

            throw new RuntimeException("Tour " + names + " is no longer available.");
        }

        // ── SAGA STEP 3: COMMIT — generate tokens and clear cart ─────────────
        // For each successfully reserved item, create a TourPurchaseToken
        // (token UUID is generated in @PrePersist).
        // Then wipe the cart so it is ready for the next session.

        List<TourPurchaseToken> tokens = new ArrayList<>();

        for (OrderItem item : pendingItems) {
            TourPurchaseToken purchaseToken = TourPurchaseToken.builder()
                    .touristId(touristId)
                    .tourId(item.getTourId())
                    // token + purchasedAt are set by @PrePersist on TourPurchaseToken
                    .build();

            tokens.add(tourPurchaseTokenRepository.save(purchaseToken));
        }

        // Clear the cart: orphanRemoval on ShoppingCart.items deletes all items.
        cart.getItems().clear();
        cart.setTotalPrice(BigDecimal.ZERO);
        shoppingCartRepository.save(cart);

        return tokens;
    }
}
