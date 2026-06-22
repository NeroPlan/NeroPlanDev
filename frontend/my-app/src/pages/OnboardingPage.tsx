import { useState } from "react";

import OnboardingCard from "../components/onboarding/OnboardingCard";
import SelectButton from "../components/onboarding/SelectButton";
import TimeSelector from "../components/onboarding/TimeSelector";
import InterestSelector from "../components/onboarding/InterestSelector";

export default function OnboardingPage() {
  const [lifePattern, setLifePattern] =
    useState("MORNING");

  const [planningStyle, setPlanningStyle] =
    useState("PLANNED");

  const [wakeUpTime, setWakeUpTime] =
    useState("07:00");

  const [sleepTime, setSleepTime] =
    useState("23:00");

  const [interests, setInterests] =
    useState<string[]>([]);

  const handleSubmit = () => {
    console.log({
      lifePattern,
      planningStyle,
      wakeUpTime,
      sleepTime,
      interests,
    });
  };

  return (
    <div className="min-h-screen bg-slate-100 p-6">
      <div className="max-w-lg mx-auto space-y-6">

        <h1 className="text-3xl font-bold text-center">
          초기 정보 입력
        </h1>

        <OnboardingCard title="생활 패턴">
          <div className="grid grid-cols-2 gap-3">
            <SelectButton
              label="아침형 인간"
              selected={
                lifePattern ===
                "MORNING"
              }
              onClick={() =>
                setLifePattern(
                  "MORNING"
                )
              }
            />

            <SelectButton
              label="저녁형 인간"
              selected={
                lifePattern ===
                "EVENING"
              }
              onClick={() =>
                setLifePattern(
                  "EVENING"
                )
              }
            />
          </div>
        </OnboardingCard>

        <OnboardingCard title="기상 시간">
          <TimeSelector
            value={wakeUpTime}
            onChange={setWakeUpTime}
          />
        </OnboardingCard>

        <OnboardingCard title="취침 시간">
          <TimeSelector
            value={sleepTime}
            onChange={setSleepTime}
          />
        </OnboardingCard>

        <OnboardingCard title="계획 성향">
          <div className="grid grid-cols-2 gap-3">
            <SelectButton
              label="계획형"
              selected={
                planningStyle ===
                "PLANNED"
              }
              onClick={() =>
                setPlanningStyle(
                  "PLANNED"
                )
              }
            />

            <SelectButton
              label="즉흥형"
              selected={
                planningStyle ===
                "IMPULSIVE"
              }
              onClick={() =>
                setPlanningStyle(
                  "IMPULSIVE"
                )
              }
            />
          </div>
        </OnboardingCard>

        <OnboardingCard title="관심 분야">
          <InterestSelector
            selected={interests}
            onChange={setInterests}
          />
        </OnboardingCard>

        <button
          onClick={handleSubmit}
          className="
            w-full
            h-14
            rounded-2xl
            bg-black
            text-white
            font-semibold
          "
        >
          시작하기
        </button>

      </div>
    </div>
  );
}