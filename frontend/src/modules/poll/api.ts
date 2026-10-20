import { appGet, appPost, type ListResponse } from "@/lib/api";
import type { CreatePollInput, Poll } from "@/modules/poll/types";

export const fetchMyPolls = () => appGet<ListResponse<Poll>>("/api/polls");

export const fetchPoll = (id: number) => appGet<Poll>(`/api/polls/${id}`);

export const createPoll = (input: CreatePollInput) => appPost<Poll>("/api/polls", input);

export const votePoll = (id: number, showId: number) =>
  appPost<Poll>(`/api/polls/${id}/vote`, { showId });

export const closePoll = (id: number) => appPost<Poll>(`/api/polls/${id}/close`);
