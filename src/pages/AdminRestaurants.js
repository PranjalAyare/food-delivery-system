// src/pages/AdminRestaurants.js
import React, { useEffect, useState } from "react";

function AdminRestaurants() {
  const [restaurants, setRestaurants] = useState([]);
  const [formData, setFormData] = useState({
    name: "",
    location: "",
    cuisine: ""
  });
  const [editId, setEditId] = useState(null);
  const [message, setMessage] = useState("");

  const token = localStorage.getItem("token");

  const fetchRestaurants = async () => {
    try {
      const response = await fetch("http://localhost:8080/restaurant/restaurants", {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!response.ok) throw new Error("Failed to fetch restaurants");
      const data = await response.json();
      setRestaurants(data);
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  useEffect(() => {
    fetchRestaurants();
  }, []);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const url = editId
      ? `http://localhost:8080/restaurant/restaurants/${editId}`
      : "http://localhost:8080/restaurant/restaurants";
    const method = editId ? "PUT" : "POST";

    try {
      const response = await fetch(url, {
        method,
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json"
        },
        body: JSON.stringify(formData)
      });

      if (!response.ok) throw new Error("Failed to submit form");

      setMessage(`✅ Restaurant ${editId ? "updated" : "added"} successfully!`);
      setFormData({ name: "", location: "", cuisine: "" });
      setEditId(null);
      fetchRestaurants();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  const handleEdit = (restaurant) => {
    setFormData({
      name: restaurant.name,
      location: restaurant.location,
      cuisine: restaurant.cuisine
    });
    setEditId(restaurant.id);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this restaurant?")) return;

    try {
      const response = await fetch(`http://localhost:8080/restaurant/restaurants/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` }
      });

      if (!response.ok) throw new Error("Failed to delete restaurant");

      setMessage("✅ Restaurant deleted!");
      fetchRestaurants();
    } catch (err) {
      setMessage("❌ " + err.message);
    }
  };

  return (
    <div>
      <h2>Manage Restaurants</h2>
      {message && <p>{message}</p>}

      {/* Add / Edit Form */}
      <form onSubmit={handleSubmit}>
        <label>Name:</label>
        <input
          type="text"
          name="name"
          value={formData.name}
          onChange={handleChange}
          required
        /><br /><br />

        <label>Location:</label>
        <input
          type="text"
          name="location"
          value={formData.location}
          onChange={handleChange}
          required
        /><br /><br />

        <label>Cuisine:</label>
        <input
          type="text"
          name="cuisine"
          value={formData.cuisine}
          onChange={handleChange}
          required
        /><br /><br />

        <button type="submit">{editId ? "Update" : "Add"} Restaurant</button>
      </form>

      <hr />

      {/* Restaurant List */}
      <ul>
        {restaurants.map((restaurant) => (
          <li key={restaurant.id}>
            <strong>{restaurant.name}</strong> - {restaurant.location} - {restaurant.cuisine}{" "}
            <button onClick={() => handleEdit(restaurant)}>Edit</button>{" "}
            <button onClick={() => handleDelete(restaurant.id)}>Delete</button>
          </li>
        ))}
      </ul>
    </div>
  );
}

export default AdminRestaurants;
