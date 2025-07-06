// src/pages/Orders.js
import React, { useEffect, useState } from "react";

function Orders() {
  const [orders, setOrders] = useState([]);
  const [error, setError] = useState("");
  const [editingOrder, setEditingOrder] = useState(null);
  const [message, setMessage] = useState("");

  const fetchOrders = async () => {
    try {
      const response = await fetch("http://localhost:8080/orders/orders", {
        headers: {
          "Authorization": `Bearer ${localStorage.getItem("token")}`,
        },
      });
      if (!response.ok) throw new Error("Failed to fetch orders");
      const data = await response.json();
      setOrders(data);
    } catch (err) {
      setError(err.message);
    }
  };

  useEffect(() => {
    fetchOrders();
  }, []);

  const handleDelete = async (id) => {
    try {
      const response = await fetch(`http://localhost:8080/orders/orders/${id}`, {
        method: "DELETE",
        headers: {
          "Authorization": `Bearer ${localStorage.getItem("token")}`,
        },
      });
      if (!response.ok) throw new Error("Failed to delete order");
      setMessage(`Order ${id} deleted.`);
      fetchOrders();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  const handleEditClick = (order) => {
    setEditingOrder({ ...order });
    setMessage("");
  };

  const handleEditChange = (e) => {
    const { name, value } = e.target;
    setEditingOrder({ ...editingOrder, [name]: value });
  };

  const handleUpdate = async () => {
    try {
      const response = await fetch(`http://localhost:8080/orders/orders/${editingOrder.id}`, {
        method: "PUT",
        headers: {
          "Authorization": `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          restaurantId: parseInt(editingOrder.restaurantId),
          totalAmount: parseFloat(editingOrder.totalAmount),
          status: editingOrder.status,
          customerId: editingOrder.customerId,
        }),
      });

      if (!response.ok) throw new Error("Failed to update order");

      setMessage("✅ Order updated!");
      setEditingOrder(null);
      fetchOrders();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  return (
    <div>
      <h2>Order History</h2>
      {error && <p style={{ color: "red" }}>{error}</p>}
      {message && <p>{message}</p>}
      {orders.length === 0 ? (
        <p>No orders found.</p>
      ) : (
        <ul>
          {orders.map((order) => (
            <li key={order.id}>
              {editingOrder?.id === order.id ? (
                <div>
                  <strong>Editing Order {order.id}</strong><br />
                  <label>Restaurant ID: </label>
                  <input
                    type="number"
                    name="restaurantId"
                    value={editingOrder.restaurantId}
                    onChange={handleEditChange}
                  /><br />
                  <label>Total Amount: </label>
                  <input
                    type="number"
                    name="totalAmount"
                    value={editingOrder.totalAmount}
                    onChange={handleEditChange}
                  /><br />
                  <label>Status: </label>
                  <select
                    name="status"
                    value={editingOrder.status}
                    onChange={handleEditChange}
                  >
                    <option value="PENDING">PENDING</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select><br />
                  <button onClick={handleUpdate}>Save</button>
                  <button onClick={() => setEditingOrder(null)}>Cancel</button>
                </div>
              ) : (
                <div>
                  <strong>Order ID:</strong> {order.id} <br />
                  <strong>Restaurant:</strong> {order.restaurantId} <br />
                  <strong>Status:</strong> {order.status} <br />
                  <strong>Total:</strong> ₹{order.totalAmount} <br />
                  <button onClick={() => handleEditClick(order)}>Edit</button>{" "}
                  <button onClick={() => handleDelete(order.id)}>Delete</button>
                </div>
              )}
              <hr />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default Orders;
