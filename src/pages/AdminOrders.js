// src/pages/AdminOrders.js
import React, { useEffect, useState } from "react";

function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [message, setMessage] = useState("");
  const [editOrder, setEditOrder] = useState(null);

  const token = localStorage.getItem("token");

  // Fetch all orders
  const fetchOrders = async () => {
    try {
      const response = await fetch("http://localhost:8080/orders/orders", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) throw new Error("Failed to fetch orders");

      const data = await response.json();
      setOrders(data);
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  // Handle status update
  const handleUpdate = async () => {
    try {
      const response = await fetch(`http://localhost:8080/orders/orders/${editOrder.id}`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(editOrder),
      });

      if (!response.ok) throw new Error("Failed to update order");

      setMessage("✅ Order updated successfully");
      setEditOrder(null);
      fetchOrders();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  // Handle delete
  const handleDelete = async (id) => {
    if (!window.confirm("Delete this order?")) return;

    try {
      const response = await fetch(`http://localhost:8080/orders/orders/${id}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) throw new Error("Failed to delete order");

      setMessage("✅ Order deleted");
      fetchOrders();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  return (
    <div>
      <h2>Manage Orders</h2>
      {message && <p>{message}</p>}

      {orders.length === 0 ? (
        <p>No orders found.</p>
      ) : (
        <ul>
          {orders.map((order) => (
            <li key={order.id}>
              <strong>Order #{order.id}</strong><br />
              Customer ID: {order.customerId}<br />
              Restaurant ID: {order.restaurantId}<br />
              Amount: ₹{order.totalAmount}<br />
              Status: {order.status}<br />
              Payment: {order.paymentMethod}<br />
              <button onClick={() => setEditOrder({ ...order })}>Edit</button>{" "}
              <button onClick={() => handleDelete(order.id)}>Delete</button>
              <hr />
            </li>
          ))}
        </ul>
      )}

      {editOrder && (
        <div style={{ marginTop: "20px" }}>
          <h3>Edit Order #{editOrder.id}</h3>
          <label>Status:</label>
          <select
            value={editOrder.status}
            onChange={(e) => setEditOrder({ ...editOrder, status: e.target.value })}
          >
            <option value="PENDING">PENDING</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="DELIVERED">DELIVERED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>
          <br /><br />
          <button onClick={handleUpdate}>Save</button>{" "}
          <button onClick={() => setEditOrder(null)}>Cancel</button>
        </div>
      )}
    </div>
  );
}

export default AdminOrders;
