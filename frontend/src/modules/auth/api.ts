import { ApiError, appGet, appPost } from "@/lib/api";
import type {
  LoginInput,
  OtpSent,
  OtpVerifyInput,
  RegisterInput,
  User,
} from "@/modules/auth/types";

/** Gives the signed-in user, or null when nobody is signed in. */
export async function fetchMe(): Promise<User | null> {
  try {
    return await appGet<User>("/api/me");
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) return null;
    throw error;
  }
}

export const login = (input: LoginInput) => appPost<User>("/api/auth/login", input);
export const register = (input: RegisterInput) =>
  appPost<User>("/api/auth/register", input);
export const requestOtp = (phone: string) =>
  appPost<OtpSent>("/api/auth/otp-request", { phone });
export const verifyOtp = (input: OtpVerifyInput) =>
  appPost<User>("/api/auth/otp-verify", input);
export const googleLogin = (idToken: string) =>
  appPost<User>("/api/auth/google", { idToken });
export const logout = () => appPost<void>("/api/auth/logout");
