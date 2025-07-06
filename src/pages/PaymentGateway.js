// // src/pages/PaymentGateway.js
// import React, { useEffect, useState } from "react";
// import { useParams, useNavigate } from "react-router-dom";

// function PaymentGateway() {
//   const { paymentId, orderId } = useParams();
//   const navigate = useNavigate();
//   const [status, setStatus] = useState("Processing payment...");
//   const [error, setError] = useState("");

//   useEffect(() => {
//     const simulatePayment = async () => {
//       try {
//         // Step 1: Update payment status
//         const paymentRes = await fetch(`http://localhost:8084/payments/${paymentId}/status`, {
//           method: "PATCH",
//           headers: {
//             "Content-Type": "application/json",
//             Authorization: `Bearer ${localStorage.getItem("token")}`
//           },
//           body: JSON.stringify("COMPLETED")
//         });

//         if (!paymentRes.ok) {
//           throw new Error("Failed to update payment status");
//         }

//         // Step 2: Update order status
//         const orderRes = await fetch(`http://localhost:8082/orders/${orderId}/status`, {
//           method: "PATCH",
//           headers: {
//             "Content-Type": "application/json",
//             Authorization: `Bearer ${localStorage.getItem("token")}`
//           },
//           body: JSON.stringify("CONFIRMED")
//         });

//         if (!orderRes.ok) {
//           throw new Error("Failed to update order status");
//         }

//         setStatus("✅ Payment successful! Order confirmed.");
//         setTimeout(() => navigate("/dashboard/payments"), 2000);
//       } catch (err) {
//         console.error(err);
//         setError("❌ Something went wrong while processing payment.");
//       }
//     };

//     simulatePayment();
//   }, [paymentId, orderId, navigate]);

//   return (
//     <div style={{ textAlign: "center", marginTop: "100px" }}>
//       <h2>Stripe Payment Gateway (Simulated)</h2>
//       {error ? <p style={{ color: "red" }}>{error}</p> : <p>{status}</p>}
//     </div>
//   );
// }

// export default PaymentGateway;
