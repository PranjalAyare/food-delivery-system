// src/pages/AdminDashboard.js
import React from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";

function AdminDashboard() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token");
    navigate("/");
  };

  return (
    <div style={{ padding: "20px" }}>
      <h2>👑 Admin Dashboard</h2>
      <nav>
        <ul style={{ listStyle: "none", padding: 0 }}>
          <li><Link to="restaurants">🍽 Manage Restaurants</Link></li>
          <li><Link to="orders">📦 Manage Orders</Link></li>
          <li><Link to="payments">💳 Manage Payments</Link></li>
          <li><button onClick={handleLogout}>🚪 Logout</button></li>
        </ul>
      </nav>
      <hr />
      <Outlet /> {/* Nested component will render here */}
    </div>
  );
}

export default AdminDashboard;
