// components/common/Logo.tsx
// 단색 검정 로고 - 체크 표시 끝이 뉴런의 마디(점)로 이어지는 모양 (Neuro + Plan)
// 색은 currentColor라 글자색(text-black 등)을 따라감

interface LogoProps {
    size?: number; // 마크 한 변(px)
    withText?: boolean; // 옆에 NeroPlan 글자 표시
    className?: string;
}

export function LogoMark({ size = 32, className = "" }: { size?: number; className?: string }) {
    return (
        <svg
            width={size}
            height={size}
            viewBox="0 0 32 32"
            className={className}
            role="img"
            aria-label="NeroPlan"
        >
            <rect width="32" height="32" rx="9" fill="currentColor" />
            {/* 체크 표시 */}
            <path
                d="M8.5 16.5 13.5 21.5 22 11"
                fill="none"
                stroke="white"
                strokeWidth="3.2"
                strokeLinecap="round"
                strokeLinejoin="round"
            />
            {/* 뉴런 마디 - 체크가 끝나는 지점 */}
            <circle cx="22.5" cy="10.5" r="3.2" fill="white" />
            <circle cx="22.5" cy="10.5" r="1.3" fill="currentColor" />
        </svg>
    );
}

export default function Logo({ size = 32, withText = true, className = "" }: LogoProps) {
    return (
        <span className={`inline-flex items-center gap-2.5 text-black ${className}`}>
            <LogoMark size={size} />
            {withText && (
                <span className="font-extrabold tracking-tight leading-none" style={{ fontSize: size * 0.68 }}>
                    NeroPlan
                </span>
            )}
        </span>
    );
}
