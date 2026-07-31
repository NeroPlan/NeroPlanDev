// layouts/MainLayout.tsx

import { Outlet } from "react-router-dom";
import Header from "../components/common/Header";
import BottomNavigation from "../components/common/BottomNavigation";

export default function MainLayout() {
    return (
        <div className="min-h-screen bg-gray-100">
            <Header />

            <main className="max-w-md mx-auto p-5">
                <Outlet />
            </main>
            <BottomNavigation />
        </div>
    );
}