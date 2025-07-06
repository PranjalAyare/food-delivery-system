
package com.fooddelivery.auth_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor; // Lombok: Generates constructor with all fields
import lombok.Data; // Lombok: Generates getters, setters, toString, equals, and hashCode
import lombok.NoArgsConstructor; // Lombok: Generates no-argument constructor

@Entity
@Table(name = "users") // Maps to a table named 'users' in the database
@Data // Automatically generates getters, setters, toString(), equals(), hashCode()
@NoArgsConstructor // Generates a constructor with no arguments
@AllArgsConstructor // Generates a constructor with all fields (id, name, email, password, role)
public class User {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments the ID
    private Long id;

    @Column(nullable = false) // Ensures the name column cannot be null
    private String name; // User's full name

    @Column(unique = true, nullable = false) // Ensures email is unique and cannot be null
    private String email; // User's unique email, often used as username for login

    @Column(nullable = false) // Ensures the password column cannot be null
    private String password; // Storing hashed password (never plaintext!)

    @Column(nullable = false) // Ensures the role column cannot be null
    private String role; // User's role (e.g., "USER", "ADMIN"). Using a single string.
                         // For multiple roles, a Set<String> with @ElementCollection would be used.
}
