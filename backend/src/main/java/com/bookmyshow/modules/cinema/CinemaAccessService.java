package com.bookmyshow.modules.cinema;

import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.modules.cinema.CinemaTypes.CinemaAdminResponse;
import com.bookmyshow.modules.user.UserService;
import com.bookmyshow.security.CurrentUser;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Who may manage which cinema. Admins manage every cinema. Theatre admins manage the cinemas assigned to them. */
@Service
@Transactional(readOnly = true)
public class CinemaAccessService {

    private final CinemaAdminRepository cinemaAdminRepository;
    private final CinemaService cinemaService;
    private final UserService userService;
    private final Clock clock;

    public CinemaAccessService(
            CinemaAdminRepository cinemaAdminRepository,
            CinemaService cinemaService,
            UserService userService,
            Clock clock) {
        this.cinemaAdminRepository = cinemaAdminRepository;
        this.cinemaService = cinemaService;
        this.userService = userService;
        this.clock = clock;
    }

    /** Lets a user manage a cinema. A plain user becomes a theatre admin. */
    @Transactional
    public CinemaAdminResponse assignAdmin(long cinemaId, long userId) {
        cinemaService.ensureExists(cinemaId);
        userService.ensureExists(userId);
        if (cinemaAdminRepository.existsByUserIdAndCinemaId(userId, cinemaId)) {
            throw new ConflictException("User " + userId + " already manages cinema " + cinemaId);
        }
        userService.grantTheatreAdmin(userId);
        CinemaAdmin saved = cinemaAdminRepository.save(new CinemaAdmin(userId, cinemaId, clock.instant()));
        return CinemaAdminResponse.from(saved);
    }

    /** The cinemas this admin may see numbers for. Admins get all of them. Anyone else is refused. */
    public List<Long> managedCinemaIds(CurrentUser user) {
        if (user.isAdmin()) {
            return cinemaService.listAllIds();
        }
        if (user.isTheatreAdmin()) {
            return cinemaAdminRepository.findByUserId(user.id()).stream()
                    .map(CinemaAdmin::getCinemaId)
                    .toList();
        }
        throw new ForbiddenException("Admins only");
    }

    /** Used by other modules before changing anything in a cinema. 404 if the cinema is missing, 403 if not allowed. */
    public void ensureCanManage(CurrentUser user, long cinemaId) {
        cinemaService.ensureExists(cinemaId);
        if (user.isAdmin()) {
            return;
        }
        if (user.isTheatreAdmin() && cinemaAdminRepository.existsByUserIdAndCinemaId(user.id(), cinemaId)) {
            return;
        }
        throw new ForbiddenException("You cannot manage cinema " + cinemaId);
    }
}
