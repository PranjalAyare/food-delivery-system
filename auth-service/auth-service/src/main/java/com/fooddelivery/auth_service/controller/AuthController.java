
package com.fooddelivery.auth_service.controller;

import com.fooddelivery.auth_service.model.User;
import com.fooddelivery.auth_service.repository.UserRepository;
import com.fooddelivery.auth_service.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody User user) {
        log.info("Received registration request for email: {}", user.getEmail());
        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent()) {
            log.warn("Registration failed: Email already registered: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already registered!");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("USER"); // Set default role
        userRepository.save(user);
        log.info("User registered successfully with email: {} and role: {}", user.getEmail(), user.getRole());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully!");
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User user) {
        log.info("Received login request for email: {}", user.getEmail());
        Optional<User> existing = userRepository.findByEmail(user.getEmail());
        if (existing.isPresent() && passwordEncoder.matches(user.getPassword(), existing.get().getPassword())) {
            // Generate token including the user's role and userId
            String token = jwtService.generateToken(
                existing.get().getEmail(),
                existing.get().getRole(),
                existing.get().getId()   // <-- pass userId here
            );
            log.info("Login successful for email: {}", user.getEmail());
            return ResponseEntity.ok(token);
        }

        log.warn("Login failed for email: {}. Invalid credentials.", user.getEmail());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password.");
    }
}
