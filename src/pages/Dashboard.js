import React from "react";
import { Link, Outlet, useNavigate } from "react-router-dom";

function Dashboard() {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("token"); // ✅ Clear token on logout
    navigate("/"); // ✅ Redirect to login
  };

  return (
    <div>
      <h2>User Dashboard</h2>
      <nav>
        <ul>
          <li><Link to="restaurants">View Restaurants</Link></li>
          <li><Link to="orders">Order History</Link></li>
          <li><Link to="place-order">Place Order</Link></li>
          <li><Link to="payments">Payments</Link></li>
          <li><button onClick={handleLogout}>Logout</button></li>
        </ul>
      </nav>
      <hr />
      <Outlet />
    </div>
  );
}

export default Dashboard;
