package com.bookmyshow.modules.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.modules.show.ShowCrowdService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.modules.show.ShowTypes.CrowdResponse;
import com.bookmyshow.modules.waitlist.WaitlistTypes.WaitlistResponse;
import com.bookmyshow.security.CurrentUser;
import com.bookmyshow.security.Roles;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class WaitlistServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");
    private static final CurrentUser ASHA = new CurrentUser(7L, Roles.USER);

    private WaitlistRepository waitlistRepository;
    private ShowCrowdService crowdService;
    private ShowSeatLockService seatLockService;
    private Notifier notifier;
    private WaitlistService waitlistService;

    @BeforeEach
    void setUp() {
        waitlistRepository = mock(WaitlistRepository.class);
        crowdService = mock(ShowCrowdService.class);
        seatLockService = mock(ShowSeatLockService.class);
        notifier = mock(Notifier.class);
        waitlistService = new WaitlistService(
                waitlistRepository, crowdService, seatLockService, notifier, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void joinsFullShowWithPosition() {
        when(crowdService.getCrowd(50L)).thenReturn(crowd(1));
        when(waitlistRepository.save(any(WaitlistEntry.class))).thenAnswer(call -> withId(call.getArgument(0), 3L));
        when(waitlistRepository.countByShowIdAndStatusAndCreatedAtBefore(50L, WaitlistStatus.WAITING, NOW))
                .thenReturn(4L);

        WaitlistResponse response = waitlistService.join(50L, 2, ASHA);

        assertThat(response.status()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(response.position()).isEqualTo(5L);
    }

    @Test
    void cannotJoinWhenSeatsAreFree() {
        when(crowdService.getCrowd(50L)).thenReturn(crowd(5));

        assertThatThrownBy(() -> waitlistService.join(50L, 2, ASHA))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Book them directly");
    }

    @Test
    void joinsOnlyOnce() {
        when(crowdService.getCrowd(50L)).thenReturn(crowd(0));
        when(waitlistRepository.existsByShowIdAndUserIdAndStatusIn(50L, 7L, WaitlistStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> waitlistService.join(50L, 2, ASHA)).isInstanceOf(ConflictException.class);
    }

    @Test
    void freedSeatsGoToFirstPersonWhoFits() {
        WaitlistEntry wantsFour = withId(new WaitlistEntry(50L, 7L, 4, NOW.minusSeconds(300)), 1L);
        WaitlistEntry wantsTwo = withId(new WaitlistEntry(50L, 8L, 2, NOW.minusSeconds(200)), 2L);
        when(waitlistRepository.findByShowIdAndStatusOrderByCreatedAtAsc(50L, WaitlistStatus.WAITING))
                .thenReturn(List.of(wantsFour, wantsTwo));
        when(seatLockService.lockForUser(8L, 50L, List.of(11L, 12L), WaitlistService.OFFER_TTL))
                .thenReturn(Optional.of("lock-8"));

        waitlistService.offerSeats(50L, List.of(11L, 12L));

        assertThat(wantsFour.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(wantsTwo.getStatus()).isEqualTo(WaitlistStatus.OFFERED);
        assertThat(wantsTwo.getLockId()).isEqualTo("lock-8");
        assertThat(wantsTwo.getOfferedSeatIds()).containsExactly(11L, 12L);
        verify(notifier).notify(eq(8L), anyString(), anyString());
    }

    @Test
    void skipsPersonWhoseLockFails() {
        WaitlistEntry first = withId(new WaitlistEntry(50L, 7L, 1, NOW.minusSeconds(300)), 1L);
        WaitlistEntry second = withId(new WaitlistEntry(50L, 8L, 1, NOW.minusSeconds(200)), 2L);
        when(waitlistRepository.findByShowIdAndStatusOrderByCreatedAtAsc(50L, WaitlistStatus.WAITING))
                .thenReturn(List.of(first, second));
        when(seatLockService.lockForUser(7L, 50L, List.of(11L), WaitlistService.OFFER_TTL))
                .thenReturn(Optional.empty());
        when(seatLockService.lockForUser(8L, 50L, List.of(11L), WaitlistService.OFFER_TTL))
                .thenReturn(Optional.of("lock-8"));

        waitlistService.offerSeats(50L, List.of(11L));

        assertThat(first.getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(second.getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    }

    @Test
    void expiredOfferPassesSeatsOn() {
        WaitlistEntry offered = withId(new WaitlistEntry(50L, 7L, 1, NOW.minusSeconds(900)), 1L);
        offered.offer("lock-7", List.of(11L), NOW.minusSeconds(700), NOW.minusSeconds(100));
        WaitlistEntry next = withId(new WaitlistEntry(50L, 8L, 1, NOW.minusSeconds(800)), 2L);
        when(waitlistRepository.findTop100ByStatusAndOfferExpiresAtBefore(WaitlistStatus.OFFERED, NOW))
                .thenReturn(List.of(offered));
        when(waitlistRepository.findByShowIdAndStatusOrderByCreatedAtAsc(50L, WaitlistStatus.WAITING))
                .thenReturn(List.of(next));
        when(seatLockService.lockForUser(8L, 50L, List.of(11L), WaitlistService.OFFER_TTL))
                .thenReturn(Optional.of("lock-8"));

        assertThat(waitlistService.expireOffers()).isEqualTo(1);

        assertThat(offered.getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
        verify(seatLockService).releaseAfterSale("lock-7");
        assertThat(next.getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    }

    @Test
    void leavingWhileWaitingFreesNothing() {
        WaitlistEntry waiting = withId(new WaitlistEntry(50L, 7L, 1, NOW), 1L);
        when(waitlistRepository.findById(1L)).thenReturn(Optional.of(waiting));

        waitlistService.leave(1L, ASHA);

        assertThat(waiting.getStatus()).isEqualTo(WaitlistStatus.LEFT);
        verify(seatLockService, never()).lockForUser(anyLong(), anyLong(), any(), any());
    }

    @Test
    void markBookedClosesOffer() {
        WaitlistEntry offered = withId(new WaitlistEntry(50L, 7L, 1, NOW), 1L);
        offered.offer("lock-7", List.of(11L), NOW, NOW.plusSeconds(600));
        when(waitlistRepository.findByLockIdAndStatus("lock-7", WaitlistStatus.OFFERED))
                .thenReturn(Optional.of(offered));

        waitlistService.markBooked("lock-7");

        assertThat(offered.getStatus()).isEqualTo(WaitlistStatus.BOOKED);
    }

    private static CrowdResponse crowd(int available) {
        return new CrowdResponse(50L, 100, 100 - available, 0, available, 100 - available, "HIGH");
    }

    private static WaitlistEntry withId(WaitlistEntry entry, long id) {
        ReflectionTestUtils.setField(entry, "id", id);
        return entry;
    }
}
