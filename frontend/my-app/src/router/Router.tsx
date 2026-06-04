// router/Router.tsx

import {
    createBrowserRouter,
    RouterProvider,
} from "react-router-dom";

// URL과 컴포넌트를 연결하는 역할

import MainLayout from "../layouts/MainLayout";
import LoginPage from "../pages/LoginPage";
import PlanPage from "../pages/PlanPage";

const router = createBrowserRouter([
    {
        path: "/",
        children: [
            {
                index: true,
                element: <LoginPage />,
            },
        ],
    },
    {
        path: "/plan",
        element: <MainLayout />,
        children: [
            {
                index: true,
                element: <PlanPage />,
            },
        ],
    },
    // 다음 양식으로 사용
    // {
    //     path: "/",
    //     element: <LoginPage />,
    // }
    // {
    //     path: "/dashboard",
    //     element: <DashboardPage />,
    // },
]);

export default function AppRouter() {
    return <RouterProvider router={router} />;
}