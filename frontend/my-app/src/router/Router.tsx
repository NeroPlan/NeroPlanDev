// router/Router.tsx

import {
    createBrowserRouter,
    RouterProvider,
} from "react-router-dom";

// URL과 컴포넌트를 연결하는 역할

import MainLayout from "../layouts/MainLayout";
import LoginPage from "../pages/LoginPage";

const router = createBrowserRouter([
    {
        path: "/",
        element: <MainLayout />,
        children: [
            {
                index: true,
                element: <LoginPage />,
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