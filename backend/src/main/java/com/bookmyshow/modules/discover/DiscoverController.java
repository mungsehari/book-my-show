package com.bookmyshow.modules.discover;

import com.bookmyshow.modules.discover.DiscoverTypes.MoodSearchResponse;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanRequest;
import com.bookmyshow.modules.discover.DiscoverTypes.PlanResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Discover")
public class DiscoverController {

    private final MoodSearchService moodSearchService;
    private final PlannerService plannerService;

    public DiscoverController(MoodSearchService moodSearchService, PlannerService plannerService) {
        this.moodSearchService = moodSearchService;
        this.plannerService = plannerService;
    }

    @GetMapping("/api/v1/movies/search")
    @Operation(summary = "Mood search: describe what you feel like watching, in English or Hinglish")
    public MoodSearchResponse search(
            @Parameter(description = "For example: kuch halka funny, 2 ghante se kam")
                    @RequestParam
                    @NotBlank
                    @Size(max = 200)
                    String q,
            @Parameter(description = "Only movies playing in this city in the next 7 days")
                    @RequestParam(required = false)
                    @Positive
                    Long cityId,
            HttpServletRequest request) {
        return moodSearchService.search(q, cityId, request.getRemoteAddr());
    }

    @PostMapping("/api/v1/planner")
    @SecurityRequirement(name = "bearer")
    @Operation(summary = "Weekend planner: up to 3 bookable plans with a show, food, and parking")
    public PlanResponse plan(@Valid @RequestBody PlanRequest request, HttpServletRequest servletRequest) {
        return plannerService.plan(request, servletRequest.getRemoteAddr());
    }
}
