package com.bookmyshow.modules.screen;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.modules.cinema.CinemaAccessService;
import com.bookmyshow.modules.screen.ScreenTypes.CreateScreenViewRequest;
import com.bookmyshow.modules.screen.ScreenTypes.ScreenViewResponse;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seat view preview: theatres add a photo per block of rows, and the frontend shows the photo for the row a user picks.
 * Blocks on one screen may not overlap, so each row has at most one photo.
 */
@Service
@Transactional(readOnly = true)
public class ScreenViewService {

    private final ScreenViewRepository viewRepository;
    private final ScreenRepository screenRepository;
    private final CinemaAccessService cinemaAccessService;
    private final Clock clock;

    public ScreenViewService(
            ScreenViewRepository viewRepository,
            ScreenRepository screenRepository,
            CinemaAccessService cinemaAccessService,
            Clock clock) {
        this.viewRepository = viewRepository;
        this.screenRepository = screenRepository;
        this.cinemaAccessService = cinemaAccessService;
        this.clock = clock;
    }

    public List<ScreenViewResponse> list(long screenId) {
        ensureScreen(screenId);
        return viewRepository.findByScreenIdOrderByFromRowAsc(screenId).stream()
                .map(ScreenViewResponse::from)
                .toList();
    }

    /** The screen row is locked first, so two admins cannot add overlapping blocks at the same time. */
    @Transactional
    public ScreenViewResponse add(long screenId, CreateScreenViewRequest request, CurrentUser user) {
        Screen screen = screenRepository
                .findByIdForUpdate(screenId)
                .orElseThrow(() -> new NotFoundException("Screen " + screenId + " not found"));
        cinemaAccessService.ensureCanManage(user, screen.getCinemaId());
        if (request.fromRow().compareTo(request.toRow()) > 0) {
            throw new BadRequestException("fromRow must come before or equal toRow");
        }
        for (ScreenView existing : viewRepository.findByScreenIdOrderByFromRowAsc(screenId)) {
            if (existing.overlaps(request.fromRow(), request.toRow())) {
                throw new ConflictException("Rows " + request.fromRow() + "-" + request.toRow()
                        + " overlap the photo for rows " + existing.getFromRow() + "-" + existing.getToRow());
            }
        }
        String caption = request.caption() == null || request.caption().isBlank()
                ? null
                : request.caption().strip();
        ScreenView saved = viewRepository.save(new ScreenView(
                screenId, request.fromRow(), request.toRow(), request.imageUrl(), caption, clock.instant()));
        return ScreenViewResponse.from(saved);
    }

    private void ensureScreen(long screenId) {
        if (!screenRepository.existsById(screenId)) {
            throw new NotFoundException("Screen " + screenId + " not found");
        }
    }
}
