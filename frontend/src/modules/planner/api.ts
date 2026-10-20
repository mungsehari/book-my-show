import { appPost } from "@/lib/api";
import type { PlanRequest, PlanResult } from "@/modules/planner/types";

export const makePlan = (request: PlanRequest) =>
  appPost<PlanResult>("/api/planner", request);
