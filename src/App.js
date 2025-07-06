// src/App.js
import "./App.css";
import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import AdminDashboard from "./pages/AdminDashboard";

import Restaurants from "./pages/Restaurants";
import Orders from "./pages/Orders";
import Payments from "./pages/Payments";
import PaymentGateway from "./pages/PaymentGateway";
import PlaceOrder from "./pages/PlaceOrder";

import AdminRestaurants from "./pages/AdminRestaurants";
import AdminOrders from "./pages/AdminOrders";
import AdminPayments from "./pages/AdminPayments";

import ProtectedRoute from "./utils/ProtectedRoute";

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />

        {/* USER Dashboard Routes */}
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <Dashboard />
            </ProtectedRoute>
          }
        >
          <Route path="restaurants" element={<Restaurants />} />
          <Route path="orders" element={<Orders />} />
          <Route path="place-order" element={<PlaceOrder />} />
          <Route path="payments" element={<Payments />} />
        </Route>

        {/* ✅ Move this outside the dashboard route */}
        <Route
          path="/pay/:paymentId/order/:orderId"
          element={
            <ProtectedRoute>
              <PaymentGateway />
            </ProtectedRoute>
          }
        />

        {/* ADMIN Dashboard Routes */}
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <AdminDashboard />
            </ProtectedRoute>
          }
        >
          <Route path="restaurants" element={<AdminRestaurants />} />
          <Route path="orders" element={<AdminOrders />} />
          <Route path="payments" element={<AdminPayments />} />
        </Route>
      </Routes>
    </Router>
  );
}

export default App;
