import React, { useState } from "react";

function PlaceOrder() {
  const [order, setOrder] = useState({
    restaurantId: "",
    totalAmount: "",
    paymentMethod: "CASH",
    items: [{ itemId: "", quantity: "" }],
  });

  const [message, setMessage] = useState("");

  const handleOrderChange = (e) => {
    setOrder({ ...order, [e.target.name]: e.target.value });
  };

  const handleItemChange = (index, e) => {
    const updatedItems = [...order.items];
    updatedItems[index][e.target.name] = e.target.value;
    setOrder({ ...order, items: updatedItems });
  };

  const addItem = () => {
    setOrder({ ...order, items: [...order.items, { itemId: "", quantity: "" }] });
  };

  const removeItem = (index) => {
    const updatedItems = [...order.items];
    updatedItems.splice(index, 1);
    setOrder({ ...order, items: updatedItems });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      const response = await fetch("http://localhost:8080/orders/orders", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${localStorage.getItem("token")}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          restaurantId: parseInt(order.restaurantId),
          totalAmount: parseFloat(order.totalAmount),
          paymentMethod: order.paymentMethod,
          items: order.items.map((item) => ({
            itemId: parseInt(item.itemId),
            quantity: parseInt(item.quantity),
          })),
        }),
      });

      if (!response.ok) throw new Error("Failed to place order");

      setMessage("✅ Order placed successfully!");
      setOrder({
        restaurantId: "",
        totalAmount: "",
        paymentMethod: "CASH",
        items: [{ itemId: "", quantity: "" }],
      });
    } catch (err) {
      setMessage("❌ Error: " + err.message);
    }
  };

  return (
    <div>
      <h2>Place New Order</h2>
      {message && <p>{message}</p>}
      <form onSubmit={handleSubmit}>
        <label>Restaurant ID:</label>
        <input
          type="number"
          name="restaurantId"
          value={order.restaurantId}
          onChange={handleOrderChange}
          required
        /><br /><br />

        <label>Total Amount:</label>
        <input
          type="number"
          name="totalAmount"
          value={order.totalAmount}
          onChange={handleOrderChange}
          required
        /><br /><br />

        <label>Payment Method:</label>
        <select name="paymentMethod" value={order.paymentMethod} onChange={handleOrderChange}>
          <option value="CASH">Cash</option>
          <option value="CREDIT_CARD">Credit Card</option>
          <option value="UPI">UPI</option>
        </select>
        <br /><br />

        <h4>Items:</h4>
        {order.items.map((item, index) => (
          <div key={index} style={{ marginBottom: "10px" }}>
            <label>Item ID:</label>
            <input
              type="number"
              name="itemId"
              value={item.itemId}
              onChange={(e) => handleItemChange(index, e)}
              required
            />

            <label style={{ marginLeft: "10px" }}>Quantity:</label>
            <input
              type="number"
              name="quantity"
              value={item.quantity}
              onChange={(e) => handleItemChange(index, e)}
              required
            />

            {order.items.length > 1 && (
              <button type="button" onClick={() => removeItem(index)} style={{ marginLeft: "10px" }}>
                Remove
              </button>
            )}
          </div>
        ))}

        <button type="button" onClick={addItem}>Add Another Item</button>
        <br /><br />

        <button type="submit">Place Order</button>
      </form>
    </div>
  );
}

export default PlaceOrder;
