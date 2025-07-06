// src/components/StripeCheckoutButton.js
import React from "react";
import { loadStripe } from "@stripe/stripe-js";

const stripePromise = loadStripe("pk_test_51NvEywSEE5Ljd1JATt7Yu2TiAPaGbivNbDBDJPzGpxLHDCrRZGctRGARHO81Emm2NSE2Em6TdStd86UTXkE7hfCi00uF6GEfNX");

function StripeCheckoutButton({ orderId, amount }) {
  const handleClick = async () => {
    try {
      const token = localStorage.getItem("token");

      const response = await fetch("http://localhost:8080/payments/payments/create-checkout-session", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          orderId,
          amount: amount.toString(), // ✅ Convert to string
          currency: "inr",
          successUrl: `http://localhost:3000/dashboard/payments?success=true&orderId=${orderId}`,
          cancelUrl: `http://localhost:3000/dashboard/payments?canceled=true&orderId=${orderId}`
        }),
      });

      const contentType = response.headers.get("content-type");

      // ✅ Defensive check
      if (!contentType || !contentType.includes("application/json")) {
        const text = await response.text();
        throw new Error("⚠️ Backend did not return JSON: " + text);
      }

      const data = await response.json();

      if (!response.ok || !data.url) {
        throw new Error(data.error || "Failed to create Stripe session");
      }

      // ✅ Redirect user to Stripe checkout page
      window.location.href = data.url;
    } catch (err) {
      console.error("Stripe redirect error", err);
      alert("❌ Stripe error: " + err.message);
    }
  };

  return <button onClick={handleClick}>💳 Pay with Stripe</button>;
}

export default StripeCheckoutButton;
