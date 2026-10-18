package com.bookmyshow.modules.alert;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.alert.PriceAlertTypes.CreatePriceAlertRequest;
import com.bookmyshow.modules.alert.PriceAlertTypes.PriceAlertResponse;
import com.bookmyshow.modules.city.CityService;
import com.bookmyshow.modules.movie.MovieService;
import com.bookmyshow.modules.show.ShowSeatMapService;
import com.bookmyshow.modules.show.ShowService;
import com.bookmyshow.modules.show.ShowTypes.ShowResponse;
import com.bookmyshow.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Price alerts. A job checks active alerts against live prices (last-minute deals included) and tells the user once,
 * about the soonest show that is cheap enough.
 */
@Service
@Transactional(readOnly = true)
public class PriceAlertService {

    static final int MAX_ACTIVE_PER_USER = 10;
    private static final int BATCH = 200;

    private final PriceAlertRepository alertRepository;
    private final MovieService movieService;
    private final CityService cityService;
    private final ShowService showService;
    private final ShowSeatMapService seatMapService;
    private final Notifier notifier;
    private final Clock clock;

    public PriceAlertService(
            PriceAlertRepository alertRepository,
            MovieService movieService,
            CityService cityService,
            ShowService showService,
            ShowSeatMapService seatMapService,
            Notifier notifier,
            Clock clock) {
        this.alertRepository = alertRepository;
        this.movieService = movieService;
        this.cityService = cityService;
        this.showService = showService;
        this.seatMapService = seatMapService;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional
    public PriceAlertResponse create(CreatePriceAlertRequest request, CurrentUser user) {
        movieService.ensureExists(request.movieId());
        cityService.ensureExists(request.cityId());
        if (alertRepository.existsByUserIdAndMovieIdAndCityIdAndStatus(
                user.id(), request.movieId(), request.cityId(), PriceAlertStatus.ACTIVE)) {
            throw new ConflictException("You already have an alert for this movie in this city");
        }
        if (alertRepository.countByUserIdAndStatus(user.id(), PriceAlertStatus.ACTIVE) >= MAX_ACTIVE_PER_USER) {
            throw new BadRequestException("You can have at most " + MAX_ACTIVE_PER_USER + " active price alerts");
        }
        PriceAlert alert = alertRepository.save(
                new PriceAlert(user.id(), request.movieId(), request.cityId(), request.maxPrice(), clock.instant()));
        return PriceAlertResponse.from(alert);
    }

    public List<PriceAlertResponse> mine(CurrentUser user) {
        return alertRepository.findByUserIdOrderByCreatedAtDesc(user.id()).stream()
                .map(PriceAlertResponse::from)
                .toList();
    }

    @Transactional
    public PriceAlertResponse cancel(long alertId, CurrentUser user) {
        PriceAlert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() -> new NotFoundException("Price alert " + alertId + " not found"));
        if (alert.getUserId() != user.id()) {
            throw new ForbiddenException("This alert belongs to another user");
        }
        if (PriceAlertStatus.ACTIVE.equals(alert.getStatus())) {
            alert.cancel();
        }
        return PriceAlertResponse.from(alert);
    }

    /**
     * Run by a job. Checks every active alert, a batch at a time. Shows and prices are looked up once per run, so many
     * alerts for the same movie and city cost little. Returns how many alerts fired.
     */
    @Transactional
    public int runChecks() {
        Map<String, List<ShowResponse>> showsByMovieCity = new HashMap<>();
        Map<Long, Optional<BigDecimal>> lowestByShow = new HashMap<>();
        int fired = 0;
        long afterId = 0;
        List<PriceAlert> batch;
        do {
            batch = alertRepository.findByStatusAndIdGreaterThanOrderByIdAsc(
                    PriceAlertStatus.ACTIVE, afterId, PageRequest.of(0, BATCH));
            for (PriceAlert alert : batch) {
                afterId = alert.getId();
                List<ShowResponse> shows = showsByMovieCity.computeIfAbsent(
                        alert.getMovieId() + ":" + alert.getCityId(),
                        key -> showService.listUpcomingForMovieInCity(alert.getMovieId(), alert.getCityId()));
                for (ShowResponse show : shows) {
                    Optional<BigDecimal> lowest =
                            lowestByShow.computeIfAbsent(show.id(), seatMapService::lowestAvailablePrice);
                    if (lowest.isPresent() && lowest.get().compareTo(alert.getMaxPrice()) <= 0) {
                        alert.trigger(show.id(), lowest.get(), clock.instant());
                        notifier.notify(
                                alert.getUserId(),
                                "Price alert: " + show.movieName(),
                                show.movieName() + " is available from " + lowest.get() + " INR on " + show.showDate()
                                        + " at " + show.startTime() + " (show " + show.id() + ").");
                        fired++;
                        break;
                    }
                }
            }
        } while (batch.size() == BATCH);
        return fired;
    }
}
