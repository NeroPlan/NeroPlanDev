export type LifePattern =
  | "MORNING"
  | "EVENING";

export type PlanningStyle =
  | "PLANNED"
  | "IMPULSIVE";

export interface OnboardingData {
  lifePattern: LifePattern;

  wakeUpTime: string;
  sleepTime: string;

  planningStyle: PlanningStyle;

  interests: string[];
}