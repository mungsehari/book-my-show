package com.bookmyshow.modules.alert;

import com.bookmyshow.common.response.ListResponse;
import com.bookmyshow.modules.alert.PriceAlertTypes.CreatePriceAlertRequest;
import com.bookmyshow.modules.alert.PriceAlertTypes.PriceAlertResponse;
import com.bookmyshow.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/price-alerts")
@Tag(name = "Price alerts")
@SecurityRequirement(name = "bearer")
public class PriceAlertController {

    private final PriceAlertService alertService;

    public PriceAlertController(PriceAlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Get told once when this movie in this city costs maxPrice or less")
    public PriceAlertResponse create(
            @Valid @RequestBody CreatePriceAlertRequest request, @AuthenticationPrincipal Jwt jwt) {
        return alertService.create(request, CurrentUser.from(jwt));
    }

    @GetMapping("/me")
    @Operation(summary = "Your price alerts, newest first")
    public ListResponse<PriceAlertResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return ListResponse.of(alertService.mine(CurrentUser.from(jwt)));
    }

    @PostMapping("/{alertId}/cancel")
    @Operation(summary = "Stop a price alert")
    public PriceAlertResponse cancel(@PathVariable @Positive long alertId, @AuthenticationPrincipal Jwt jwt) {
        return alertService.cancel(alertId, CurrentUser.from(jwt));
    }
}
