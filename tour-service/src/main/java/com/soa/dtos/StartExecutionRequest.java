// ─── StartExecutionRequest.java ───────────────────────────────────────────────
package com.soa.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartExecutionRequest {

    @NotNull
    private Long tourId;

    @NotNull
    private Double startLat;

    @NotNull
    private Double startLong;
}