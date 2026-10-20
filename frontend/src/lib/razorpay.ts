const SCRIPT_URL = "https://checkout.razorpay.com/v1/checkout.js";

export type RazorpaySuccess = {
  razorpay_order_id: string;
  razorpay_payment_id: string;
  razorpay_signature: string;
};

type RazorpayOptions = {
  key: string;
  amount: number;
  currency: string;
  order_id: string;
  name: string;
  description: string;
  prefill?: { name?: string; email?: string; contact?: string };
  theme?: { color: string };
  handler: (response: RazorpaySuccess) => void;
  modal: { ondismiss: () => void };
};

type RazorpayInstance = {
  open: () => void;
  on: (event: "payment.failed", callback: (response: { error: { description: string } }) => void) => void;
};

declare global {
  interface Window {
    Razorpay?: new (options: RazorpayOptions) => RazorpayInstance;
  }
}

let loading: Promise<void> | null = null;

function loadScript(): Promise<void> {
  if (window.Razorpay) return Promise.resolve();
  loading ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SCRIPT_URL;
    script.onload = () => resolve();
    script.onerror = () => {
      loading = null;
      reject(new Error("Could not load the payment window. Check your internet."));
    };
    document.body.appendChild(script);
  });
  return loading;
}

export class PaymentCancelled extends Error {
  constructor() {
    super("Payment window closed. Your seats are still on hold.");
    this.name = "PaymentCancelled";
  }
}

type PayInput = {
  key: string;
  orderId: string;
  amountInPaise: number;
  currency: string;
  description: string;
  prefill?: { name?: string; email?: string; contact?: string };
};

/** Opens Razorpay checkout. Resolves with the proof, rejects if closed or failed. */
export async function payWithRazorpay(input: PayInput): Promise<RazorpaySuccess> {
  await loadScript();
  const Razorpay = window.Razorpay;
  if (!Razorpay) throw new Error("Payment window is not available.");

  return new Promise<RazorpaySuccess>((resolve, reject) => {
    const checkout = new Razorpay({
      key: input.key,
      amount: input.amountInPaise,
      currency: input.currency,
      order_id: input.orderId,
      name: "Book My Show",
      description: input.description,
      prefill: input.prefill,
      theme: { color: "#e11d48" },
      handler: resolve,
      modal: { ondismiss: () => reject(new PaymentCancelled()) },
    });
    checkout.on("payment.failed", (response) =>
      reject(new Error(response.error.description || "Payment failed.")),
    );
    checkout.open();
  });
}
