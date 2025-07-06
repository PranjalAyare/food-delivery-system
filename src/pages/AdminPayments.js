// src/pages/AdminPayments.js
import React, { useEffect, useState } from "react";

function AdminPayments() {
  const [payments, setPayments] = useState([]);
  const [editId, setEditId] = useState(null);
  const [formData, setFormData] = useState({
    paymentMethod: "",
    status: "",
  });
  const [message, setMessage] = useState("");

  const token = localStorage.getItem("token");

  // Fetch all payments
  const fetchPayments = async () => {
    try {
      const response = await fetch("http://localhost:8080/payments/payments", {
        headers: { Authorization: `Bearer ${token}` },
      });
      if (!response.ok) throw new Error("Failed to fetch payments");

      const data = await response.json();
      setPayments(data);
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  useEffect(() => {
    fetchPayments();
  }, []);

  // Handle input change
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  // Handle edit button click
  const handleEdit = (payment) => {
    setEditId(payment.id);
    setFormData({
      paymentMethod: payment.paymentMethod || "",
      status: payment.status || "",
    });
  };

  // Handle update submit
  const handleUpdate = async (e) => {
    e.preventDefault();

    try {
      const response = await fetch(`http://localhost:8080/payments/payments/${editId}`, {
        method: "PUT",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(formData),
      });

      if (!response.ok) throw new Error("Failed to update payment");

      setMessage("✅ Payment updated!");
      setEditId(null);
      setFormData({ paymentMethod: "", status: "" });
      fetchPayments();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  // Handle delete
  const handleDelete = async (id) => {
    if (!window.confirm("Delete this payment?")) return;

    try {
      const response = await fetch(`http://localhost:8080/payments/payments/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (!response.ok) throw new Error("Failed to delete payment");

      setMessage("✅ Payment deleted!");
      fetchPayments();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  return (
    <div>
      <h2>💳 Manage Payments</h2>
      {message && <p>{message}</p>}

      {editId && (
        <form onSubmit={handleUpdate}>
          <h4>Edit Payment #{editId}</h4>
          <label>Status:</label>
          <select name="status" value={formData.status} onChange={handleChange}>
            <option value="">Select Status</option>
            <option value="INITIATED">INITIATED</option>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAILED">FAILED</option>
          </select>
          <br /><br />

          <label>Payment Method:</label>
          <select name="paymentMethod" value={formData.paymentMethod} onChange={handleChange}>
            <option value="">Select Method</option>
            <option value="CREDIT_CARD">CREDIT_CARD</option>
            <option value="DEBIT_CARD">DEBIT_CARD</option>
            <option value="UPI">UPI</option>
            <option value="CASH">CASH</option>
          </select>
          <br /><br />

          <button type="submit">Update Payment</button>{" "}
          <button onClick={() => setEditId(null)}>Cancel</button>
        </form>
      )}

      <hr />

      <ul>
        {payments.map((payment) => (
          <li key={payment.id}>
            <strong>Payment ID:</strong> {payment.id} <br />
            <strong>Order ID:</strong> {payment.orderId} <br />
            <strong>Method:</strong> {payment.paymentMethod} <br />
            <strong>Status:</strong> {payment.status} <br />
            <button onClick={() => handleEdit(payment)}>Edit</button>{" "}
            <button onClick={() => handleDelete(payment.id)}>Delete</button>
            <hr />
          </li>
        ))}
      </ul>
    </div>
  );
}

export default AdminPayments;
