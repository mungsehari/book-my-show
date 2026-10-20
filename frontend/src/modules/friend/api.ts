import { appGet, appPost, type ListResponse } from "@/lib/api";
import type {
  Friend,
  FriendRequest,
  FriendsActivity,
  Privacy,
} from "@/modules/friend/types";

export const fetchFriends = () => appGet<ListResponse<Friend>>("/api/friends");

export const fetchRequests = () =>
  appGet<ListResponse<FriendRequest>>("/api/friends/requests");

export const sendRequest = (email: string) =>
  appPost<FriendRequest>("/api/friends/requests", { email });

export const answerRequest = (id: number, action: "accept" | "decline") =>
  appPost<FriendRequest>(`/api/friends/requests/${id}/${action}`);

export const removeFriend = (userId: number) =>
  appPost<void>(`/api/friends/${userId}/remove`);

export const fetchActivity = (movieId: number) =>
  appGet<FriendsActivity>("/api/friends/activity", { movieId });

export const fetchPrivacy = () => appGet<Privacy>("/api/me/privacy");

export const savePrivacy = (shareActivity: boolean) =>
  appPost<Privacy>("/api/me/privacy", { shareActivity });
