export type Booking = {
  id: number;
  showId: number;
  movieName: string | null;
  cinemaId: number;
  screenName: string;
  showDate: string;
  startTime: string;
  seatIds: number[];
  seatLabels: string[];
  status: string;
  amount: number;
  currency: string;
  createdAt: string;
  confirmedAt: string | null;
};

export type BookingWhen = "upcoming" | "past";

/** One person's share of a split booking. Pay it with the Razorpay order it carries. */
export type Share = {
  id: number;
  bookingId: number;
  userId: number;
  amount: number;
  amountInPaise: number;
  currency: string;
  status: string;
  razorpayOrderId: string;
  razorpayKeyId: string;
};

export type Split = {
  bookingId: number;
  status: string;
  total: number;
  expiresAt: string;
  shares: Share[];
};

/** A ticket offered for resale. price is the only price it can sell for. */
export type ResaleListing = {
  bookingId: number;
  showId: number;
  seats: string[];
  price: number;
  currency: string;
  onHold: boolean;
};

export type Checkout = {
  bookingId: number;
  status: string;
  amount: number;
  amountInPaise: number;
  currency: string;
  razorpayOrderId: string;
  razorpayKeyId: string;
};

export type PaymentProof = {
  razorpayOrderId: string;
  razorpayPaymentId: string;
  razorpaySignature: string;
};

export type Ticket = {
  bookingId: number;
  ticketCode: string;
  movieName: string | null;
  cinemaName: string;
  screenName: string;
  showDate: string;
  startTime: string;
  seats: string[];
  amount: number;
  currency: string;
  checkedInAt: string | null;
};
