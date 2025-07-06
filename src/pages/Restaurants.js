import React, { useEffect, useState } from "react";

function Restaurants() {
  const [restaurants, setRestaurants] = useState([]);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchRestaurants = async () => {
      try {
        const token = localStorage.getItem("token");
        const response = await fetch("http://localhost:8080/restaurant/restaurants", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });

        if (!response.ok) {
          throw new Error("Failed to fetch restaurants");
        }

        const data = await response.json();
        setRestaurants(data);
      } catch (err) {
        setError(err.message);
      }
    };

    fetchRestaurants();
  }, []);

  return (
    <div>
      <h2>Available Restaurants</h2>
      {error && <p style={{ color: "red" }}>{error}</p>}
      {restaurants.length === 0 ? (
        <p>No restaurants available</p>
      ) : (
        <ul>
          {restaurants.map((restaurant) => (
            <li key={restaurant.id}>
              <strong>{restaurant.name}</strong> - {restaurant.location}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

export default Restaurants;
