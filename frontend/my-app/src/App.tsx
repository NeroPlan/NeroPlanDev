// import { useState } from 'react'
import './App.css'

function App() {
  // const [count, setCount] = useState(0)
    const handleLogin = () => {
        window.location.href = "http://localhost:4000/api/v1//auth/login/google"
    }

    return (
        <button onClick={handleLogin}>
            구글 로그인
        </button>
        )
}

export default App