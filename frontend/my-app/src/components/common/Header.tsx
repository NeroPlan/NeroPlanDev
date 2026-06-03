// components/common/Header.tsx


//export default : 이 파일의 대표 객체를 외부로 공개 -> 다른 파일에서 import 가능

export default function Header() {
    return (
        <header className="w-full h-4 bg-white flex items-center justify-center px-6">
            <h1 className="text-2xl font-bold">NeroPlan</h1>
        </header>
    )
}
