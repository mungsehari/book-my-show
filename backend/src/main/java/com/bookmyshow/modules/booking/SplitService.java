package com.bookmyshow.modules.booking;

import com.bookmyshow.common.error.BadRequestException;
import com.bookmyshow.common.error.ConflictException;
import com.bookmyshow.common.error.ForbiddenException;
import com.bookmyshow.common.error.NotFoundException;
import com.bookmyshow.integrations.notify.Notifier;
import com.bookmyshow.integrations.payment.PaymentGateway;
import com.bookmyshow.integrations.payment.PaymentGateway.PaymentOrder;
import com.bookmyshow.modules.booking.BookingTypes.ConfirmPaymentRequest;
import com.bookmyshow.modules.booking.BookingTypes.ShareResponse;
import com.bookmyshow.modules.booking.BookingTypes.SplitResponse;
import com.bookmyshow.modules.friend.FriendService;
import com.bookmyshow.modules.show.ShowSeatBookingService;
import com.bookmyshow.modules.show.ShowSeatLockService;
import com.bookmyshow.modules.show.ShowTypes.ActiveLock;
import com.bookmyshow.security.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Split payment: friends each pay their share of one booking. The seats stay held for {@link #SPLIT_WINDOW}. When the
 * last share is paid the seats are sold. If time runs out, or the seats were lost, everyone who paid is refunded.
 */
@Service
@Transactional(readOnly = true)
public class SplitService {

    /** How long friends have to pay. The seat lock is extended to match. */
    static final Duration SPLIT_WINDOW = Duration.ofMinutes(15);

    private final BookingRepository bookingRepository;
    private final PaymentShareRepository shareRepository;
    private final BookingMemberRepository memberRepository;
    private final ShowSeatLockService seatLockService;
    private final ShowSeatBookingService seatBookingService;
    private final FriendService friendService;
    private final PaymentGateway paymentGateway;
    private final Notifier notifier;
    private final Clock clock;

    public SplitService(
            BookingRepository bookingRepository,
            PaymentShareRepository shareRepository,
            BookingMemberRepository memberRepository,
            ShowSeatLockService seatLockService,
            ShowSeatBookingService seatBookingService,
            FriendService friendService,
            PaymentGateway paymentGateway,
            Notifier notifier,
            Clock clock) {
        this.bookingRepository = bookingRepository;
        this.shareRepository = shareRepository;
        this.memberRepository = memberRepository;
        this.seatLockService = seatLockService;
        this.seatBookingService = seatBookingService;
        this.friendService = friendService;
        this.paymentGateway = paymentGateway;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Transactional
    public SplitResponse create(String lockId, List<Long> friendIds, CurrentUser organizer) {
        Set<Long> friends = new LinkedHashSet<>(friendIds);
        if (friends.size() != friendIds.size() || friends.contains(organizer.id())) {
            throw new BadRequestException("List each friend once, and not yourself");
        }
        Set<Long> myFriends = friendService.friendIds(organizer.id());
        for (Long friendId : friends) {
            if (!myFriends.contains(friendId)) {
                throw new BadRequestException("User " + friendId + " is not your friend");
            }
        }
        if (bookingRepository.findByLockId(lockId).isPresent()) {
            throw new ConflictException("These seats are already in a checkout");
        }
        ActiveLock lock = seatLockService.getActiveLock(lockId, organizer);
        seatLockService.extendLock(lockId, organizer, SPLIT_WINDOW);

        Instant now = clock.instant();
        Booking booking = new Booking(lock, BookingService.CURRENCY, now);
        booking.markSplit();
        booking = bookingRepository.save(booking);

        List<Long> payers = new ArrayList<>();
        payers.add(organizer.id());
        payers.addAll(friends);
        long[] paise = splitPaise(BookingService.toPaise(booking), payers.size());
        List<PaymentShare> shares = new ArrayList<>();
        for (int i = 0; i < payers.size(); i++) {
            PaymentShare share = shareRepository.save(new PaymentShare(
                    booking.getId(), payers.get(i), BigDecimal.valueOf(paise[i], 2), booking.getCurrency(), now));
            PaymentOrder order = paymentGateway.createOrder(paise[i], booking.getCurrency(), "share-" + share.getId());
            share.attachOrder(order.id());
            shares.add(share);
        }
        long bookingId = booking.getId();
        friends.forEach(friendId -> memberRepository.save(new BookingMember(bookingId, friendId)));
        friends.forEach(friendId -> notifier.notify(
                friendId,
                "Pay your share",
                "User " + organizer.id() + " split a booking with you. Pay within " + SPLIT_WINDOW.toMinutes()
                        + " minutes (booking " + bookingId + ")."));
        return toResponse(booking, shares);
    }

    /** Your unpaid shares, newest first. */
    public List<ShareResponse> myPendingShares(CurrentUser user) {
        return shareRepository.findByUserIdAndStatusOrderByCreatedAtDesc(user.id(), PaymentShare.PENDING).stream()
                .map(this::toShare)
                .toList();
    }

    /** Checks what Razorpay checkout returned for one share, then records it. */
    @Transactional
    public ShareResponse confirm(long shareId, ConfirmPaymentRequest request, CurrentUser user) {
        PaymentShare share = shareRepository
                .findByIdForUpdate(shareId)
                .orElseThrow(() -> new NotFoundException("Share " + shareId + " not found"));
        if (share.getUserId() != user.id()) {
            throw new ForbiddenException("This share belongs to another user");
        }
        if (!request.razorpayOrderId().equals(share.getRazorpayOrderId())) {
            throw new BadRequestException("Order does not match this share");
        }
        if (!paymentGateway.isValidPaymentSignature(
                request.razorpayOrderId(), request.razorpayPaymentId(), request.razorpaySignature())) {
            throw new BadRequestException("Payment signature is invalid");
        }
        applyPayment(share, request.razorpayPaymentId());
        return toShare(share);
    }

    /**
     * Records a verified payment for a share. Safe to call twice (confirm call and webhook). If the booking can no longer
     * complete, the money goes straight back. The last payment sells the seats.
     */
    @Transactional
    public void applyPayment(PaymentShare share, String paymentId) {
        if (!share.isPending()) {
            return;
        }
        Booking booking = bookingRepository
                .findByIdForUpdate(share.getBookingId())
                .orElseThrow(() -> new NotFoundException("Booking " + share.getBookingId() + " not found"));
        share.markPaid(paymentId, clock.instant());
        if (!BookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
            refund(share, "Split booking " + booking.getId() + " did not complete");
            return;
        }
        List<PaymentShare> shares = shareRepository.findByBookingIdOrderByIdAsc(booking.getId());
        if (!shares.stream().allMatch(PaymentShare::isPaid)) {
            return;
        }
        if (seatBookingService.sellSeats(booking.getShowId(), booking.getSeatIds(), booking.getId())) {
            booking.confirm("split", clock.instant());
            seatLockService.releaseAfterSale(booking.getLockId());
            shares.forEach(s -> notifier.notify(
                    s.getUserId(),
                    "Booking confirmed",
                    "Everyone paid. Booking " + booking.getId() + " is confirmed."));
        } else {
            booking.markNeedsRefund("split");
            shares.forEach(s -> refund(s, "Seats sold out, split booking " + booking.getId()));
            booking.markRefunded("split-refunds", clock.instant());
        }
    }

    /** Run by a job: split bookings past their window expire, and everyone who paid is refunded. */
    @Transactional
    public int expireStale() {
        List<Booking> stale = bookingRepository.findTop100ByStatusAndSplitTrueAndCreatedAtBefore(
                BookingStatus.PENDING_PAYMENT, clock.instant().minus(SPLIT_WINDOW));
        for (Booking booking : stale) {
            booking.markExpired();
            for (PaymentShare share : shareRepository.findByBookingIdOrderByIdAsc(booking.getId())) {
                if (share.isPaid()) {
                    refund(share, "Split booking " + booking.getId() + " expired");
                } else if (share.isPending()) {
                    share.markVoid();
                }
            }
            seatLockService.releaseAfterSale(booking.getLockId());
        }
        return stale.size();
    }

    /** Equal paise each; the first payer (the organizer) also takes the leftover. */
    static long[] splitPaise(long totalPaise, int people) {
        long[] parts = new long[people];
        long base = totalPaise / people;
        Arrays.fill(parts, base);
        parts[0] += totalPaise - base * people;
        return parts;
    }

    private void refund(PaymentShare share, String note) {
        if (!share.isPaid()) {
            return;
        }
        String refundId = paymentGateway.refund(
                share.getRazorpayPaymentId(),
                share.getAmount().movePointRight(2).longValueExact(),
                note);
        share.markRefunded(refundId, clock.instant());
        notifier.notify(
                share.getUserId(),
                "Refund sent",
                note + ". " + share.getAmount() + " " + share.getCurrency() + " is on its way back to you.");
    }

    private SplitResponse toResponse(Booking booking, List<PaymentShare> shares) {
        return new SplitResponse(
                booking.getId(),
                booking.getStatus(),
                booking.getAmount(),
                booking.getCreatedAt().plus(SPLIT_WINDOW),
                shares.stream().map(this::toShare).toList());
    }

    private ShareResponse toShare(PaymentShare share) {
        return new ShareResponse(
                share.getId(),
                share.getBookingId(),
                share.getUserId(),
                share.getAmount(),
                share.getAmount().movePointRight(2).longValueExact(),
                share.getCurrency(),
                share.getStatus(),
                share.getRazorpayOrderId(),
                paymentGateway.publicKey());
    }
}
