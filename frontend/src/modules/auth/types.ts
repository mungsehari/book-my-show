export type User = {
  id: number;
  name: string;
  email: string | null;
  phone: string | null;
  role: string;
};

export type OtpSent = { phone: string; expiresInSeconds: number };

export type LoginInput = { email: string; password: string };
export type RegisterInput = { name: string; email: string; password: string };
export type OtpVerifyInput = { phone: string; code: string; name?: string };
