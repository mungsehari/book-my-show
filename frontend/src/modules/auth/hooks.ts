import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  fetchMe,
  googleLogin,
  login,
  logout,
  register,
  requestOtp,
  verifyOtp,
} from "@/modules/auth/api";
import type { User } from "@/modules/auth/types";

const ME_KEY = ["me"];

export const useMe = () =>
  useQuery({ queryKey: ME_KEY, queryFn: fetchMe, staleTime: 60_000, retry: false });

/** Sign-in mutation that stores the user in the cache on success. */
function useSignIn<TInput>(call: (input: TInput) => Promise<User>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: call,
    onSuccess: (user) => client.setQueryData(ME_KEY, user),
  });
}

export const useLogin = () => useSignIn(login);
export const useRegister = () => useSignIn(register);
export const useVerifyOtp = () => useSignIn(verifyOtp);
export const useGoogleLogin = () => useSignIn(googleLogin);
export const useRequestOtp = () => useMutation({ mutationFn: requestOtp });

export function useLogout() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: logout,
    onSuccess: () => {
      client.setQueryData(ME_KEY, null);
      client.removeQueries({ queryKey: ["bookings"] });
    },
  });
}
