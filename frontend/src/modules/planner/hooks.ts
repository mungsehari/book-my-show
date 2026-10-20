import { useMutation } from "@tanstack/react-query";
import { makePlan } from "@/modules/planner/api";

export const usePlanner = () => useMutation({ mutationFn: makePlan });
