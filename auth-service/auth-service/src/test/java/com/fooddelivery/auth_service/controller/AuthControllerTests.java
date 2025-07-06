
package com.fooddelivery.auth_service.controller;

import com.fooddelivery.auth_service.model.User;
import com.fooddelivery.auth_service.repository.UserRepository;
import com.fooddelivery.auth_service.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailAlreadyExists() {
        User user = new User();
        user.setEmail("test@example.com");

        User existingUser = new User(1L, "Existing User", "test@example.com", "hashedpass", "USER");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(existingUser));

        ResponseEntity<String> response = authController.register(user);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Email already registered!", response.getBody());
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_ShouldReturnCreated_WhenNewUser() {
        User user = new User();
        user.setName("New User");
        user.setEmail("newuser@example.com");
        user.setPassword("plaintext");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plaintext")).thenReturn("encodedPassword");

        ResponseEntity<String> response = authController.register(user);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("User registered successfully!", response.getBody());
        assertEquals("encodedPassword", user.getPassword());
        assertEquals("USER", user.getRole());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenUserNotFound() {
        User user = new User();
        user.setEmail("nouser@example.com");
        user.setPassword("password");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

        ResponseEntity<String> response = authController.login(user);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password.", response.getBody());
    }

    @Test
    void login_ShouldReturnUnauthorized_WhenPasswordDoesNotMatch() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("wrongpassword");

        User storedUser = new User(1L, "Test User", "user@example.com", "encodedpassword", "USER");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("wrongpassword", "encodedpassword")).thenReturn(false);

        ResponseEntity<String> response = authController.login(user);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Invalid email or password.", response.getBody());
    }

    @Test
    void login_ShouldReturnToken_WhenSuccessful() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setPassword("correctpassword");

        User storedUser = new User(1L, "Test User", "user@example.com", "encodedpassword", "ADMIN");

        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("correctpassword", "encodedpassword")).thenReturn(true);
        // Updated to pass userId as third argument
        when(jwtService.generateToken("user@example.com", "ADMIN", 1L)).thenReturn("jwt-token-with-admin-role");

        ResponseEntity<String> response = authController.login(user);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jwt-token-with-admin-role", response.getBody());
    }
}
