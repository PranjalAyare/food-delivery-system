import React, { useEffect, useState } from "react";
import StripeCheckoutButton from "../components/StripeCheckoutButton";
import { useLocation } from "react-router-dom";

function Payments() {
  const [payments, setPayments] = useState([]);
  const [error, setError] = useState("");

  const location = useLocation();
  const queryParams = new URLSearchParams(location.search);
  const success = queryParams.get("success");
  const canceled = queryParams.get("canceled");

  useEffect(() => {
    const fetchPayments = async () => {
      try {
        const response = await fetch("http://localhost:8080/payments/payments", {
          method: "GET",
          headers: {
            Authorization: `Bearer ${localStorage.getItem("token")}`,
            "Content-Type": "application/json",
          },
        });

        if (!response.ok) throw new Error("Failed to fetch payments");

        const data = await response.json();
        setPayments(data);
      } catch (err) {
        setError(err.message);
      }
    };

    fetchPayments();
  }, []);

  return (
    <div>
      <h2>Payment History</h2>

      {/* ✅ Show Stripe payment status messages */}
      {success && <p style={{ color: "green" }}>✅ Payment successful!</p>}
      {canceled && <p style={{ color: "red" }}>❌ Payment canceled by user.</p>}

      {error && <p style={{ color: "red" }}>{error}</p>}

      {payments.length === 0 ? (
        <p>No payment records found.</p>
      ) : (
        <ul>
          {payments.map((payment) => (
            <li key={payment.id}>
              <strong>Payment ID:</strong> {payment.id}<br />
              <strong>Order ID:</strong> {payment.orderId}<br />
              <strong>Amount:</strong> ₹{payment.amount}<br />
              <strong>Status:</strong> {payment.status}<br />
              <strong>Method:</strong> {payment.paymentMethod}<br />
              {payment.status === "PENDING" && (
                <StripeCheckoutButton orderId={payment.orderId} amount={payment.amount} />
              )}
              <hr />
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default Payments;
