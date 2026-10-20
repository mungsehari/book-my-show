const SCRIPT_URL = "https://accounts.google.com/gsi/client";

type CredentialResponse = { credential: string };

type GoogleId = {
  initialize: (config: {
    client_id: string;
    callback: (response: CredentialResponse) => void;
  }) => void;
  renderButton: (
    element: HTMLElement,
    options: {
      theme: "outline" | "filled_blue" | "filled_black";
      size: "large" | "medium" | "small";
      text: "signin_with" | "continue_with";
      shape: "rectangular" | "pill";
      width: number;
    },
  ) => void;
};

declare global {
  interface Window {
    google?: { accounts: { id: GoogleId } };
  }
}

export const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID ?? "";

let loading: Promise<void> | null = null;

function loadScript(): Promise<void> {
  if (window.google) return Promise.resolve();
  loading ??= new Promise<void>((resolve, reject) => {
    const script = document.createElement("script");
    script.src = SCRIPT_URL;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => {
      loading = null;
      reject(new Error("Could not load Google sign-in. Check your internet."));
    };
    document.body.appendChild(script);
  });
  return loading;
}

/** Draws the Google button in the element. onToken gets the Google ID token. */
export async function renderGoogleButton(
  element: HTMLElement,
  onToken: (idToken: string) => void,
): Promise<void> {
  await loadScript();
  const id = window.google?.accounts.id;
  if (!id) throw new Error("Google sign-in is not available.");
  id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: (response) => onToken(response.credential),
  });
  id.renderButton(element, {
    theme: "outline",
    size: "large",
    text: "continue_with",
    shape: "pill",
    width: Math.min(element.clientWidth || 320, 400),
  });
}
