import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  addMovie,
  addPlace,
  addScreen,
  addShow,
  addView,
  assignTheatreAdmin,
  checkIn,
  fetchLayout,
  fetchScreens,
  refundBooking,
} from "@/modules/admin/api";
import type { ShowInput } from "@/modules/admin/types";
import { useMe } from "@/modules/auth/hooks";
import { ADMIN_ROLES } from "@/modules/admin/types";

/** Who is signed in, and what admin power they have. */
export function useAdminRole() {
  const { data: user, isLoading } = useMe();
  return {
    isLoading,
    isAdmin: user?.role === "ADMIN",
    canManage: !!user && ADMIN_ROLES.includes(user.role),
  };
}

export const useScreens = (cinemaId: number | null) =>
  useQuery({
    queryKey: ["screens", cinemaId],
    queryFn: () => fetchScreens(cinemaId as number),
    enabled: cinemaId !== null,
  });

export const useLayout = (screenId: number | null) =>
  useQuery({
    queryKey: ["layout", screenId],
    queryFn: () => fetchLayout(screenId as number),
    enabled: screenId !== null,
  });

export function useAddMovie() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: addMovie,
    onSuccess: () => client.invalidateQueries({ queryKey: ["movies"] }),
  });
}

export function useAddScreen() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: { cinemaId: number; name: string; rows: Parameters<typeof addScreen>[1]["rows"] }) =>
      addScreen(input.cinemaId, { name: input.name, rows: input.rows }),
    onSuccess: () => client.invalidateQueries({ queryKey: ["screens"] }),
  });
}

export function useAddShow() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (input: { screenId: number; show: ShowInput }) => addShow(input.screenId, input.show),
    onSuccess: () => client.invalidateQueries({ queryKey: ["shows"] }),
  });
}

export const useAddPlace = () =>
  useMutation({
    mutationFn: (input: { cinemaId: number; place: Parameters<typeof addPlace>[1] }) =>
      addPlace(input.cinemaId, input.place),
  });

export const useAddView = () =>
  useMutation({
    mutationFn: (input: { screenId: number; view: Parameters<typeof addView>[1] }) =>
      addView(input.screenId, input.view),
  });

export const useCheckIn = () => useMutation({ mutationFn: checkIn });

export const useAssignAdmin = () =>
  useMutation({
    mutationFn: (input: { cinemaId: number; userId: number }) =>
      assignTheatreAdmin(input.cinemaId, input.userId),
  });

export const useRefund = () => useMutation({ mutationFn: refundBooking });
