// pages/PlanPage.tsx

import PlanCard from "../components/common/PlanCard";
import PlanInput from "../components/common/PlanInput";
import AnalyzeButton from "../components/PlanPage/button";

export default function PlanPage() {
    return (
            <div className="space-y-8">

                <PlanCard
                    title="출근 전 계획"
                    plans={[
                        "러닝하기",
                        "알고리즘 문제 풀기",
                    ]}
                />

                <PlanCard
                    title="출근 후 오전 계획"
                    plans={[
                        "문제 풀이 과제",
                        "영어 단어 암기",
                    ]}
                />

                <PlanCard
                    title="출근 후 오후 계획"
                    plans={[
                        "메일 확인",
                        "PPT 작성",
                    ]}
                />

                <PlanCard
                    title="퇴근 후 계획"
                    plans={[
                        "운동",
                        "영어 단어 암기",
                    ]}
                />

                <PlanInput />
                
                <AnalyzeButton />
            </div>
    );
}