package com.swiggy.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiggy.user.dto.LoginRequest;
import com.swiggy.user.dto.RegisterRequest;
import com.swiggy.user.model.UserRole;
import com.swiggy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-End Integration Tests for Authentication & Authorization.
 *
 * <p>WHAT THIS TEST PROVES:
 * 1. Proves user registration creates a database row in genuine PostgreSQL with a BCrypt hashed password.
 * 2. Proves Bean Validation rejects malformed inputs with standardized ValidationError payloads.
 * 3. Proves duplicate email registration returns 409 Conflict.
 * 4. Proves login verifies password hash and issues cryptographically valid JWT tokens.
 * 5. Proves protected endpoints reject unauthenticated requests and grant access to valid JWT bearers.
 *
 * <p>WHICH FAILURE IT GUARDS AGAINST:
 * Prevents password plaintext leaks, broken authentication filter chains, and authorization bypass vulnerabilities.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Should register a new customer and return a signed JWT token")
    void shouldRegisterUserAndReturnJwtToken() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "customer1@swiggy.com",
                "SecurePassword123!",
                UserRole.CUSTOMER,
                "Adil Customer",
                "+919876543210"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-Correlation-Id", "test-reg-trace-01"))
                .andExpect(status().isCreated())
                .andExpect(header().string("X-Correlation-Id", "test-reg-trace-01"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.email").value("customer1@swiggy.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andReturn();

        // INVARIANT: Password must be stored as BCrypt hash in PostgreSQL, never plaintext
        var persistedUser = userRepository.findByEmailIgnoreCase("customer1@swiggy.com").orElseThrow();
        assertThat(persistedUser.getPasswordHash()).startsWith("$2a$12$");
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo("SecurePassword123!");
    }

    @Test
    @DisplayName("Should return 409 Conflict when attempting to register with an existing email")
    void shouldFailRegistrationWhenEmailAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "duplicate@swiggy.com",
                "SecurePassword123!",
                UserRole.CUSTOMER,
                "First Registration",
                "+919876543210"
        );

        // First registration succeeds
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Duplicate registration fails with CONFLICT
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("Should reject registration with invalid email format and short password")
    void shouldRejectInvalidRegistrationPayload() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "not-an-email",
                "short",
                UserRole.CUSTOMER,
                "A",
                "invalid-phone"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.validationErrors").isArray());
    }

    @Test
    @DisplayName("Should login successfully with valid credentials and return JWT token")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "login.test@swiggy.com",
                "CorrectPassword123!",
                UserRole.RESTAURANT_OWNER,
                "Restaurant Boss",
                "+919876543210"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest("login.test@swiggy.com", "CorrectPassword123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.role").value("RESTAURANT_OWNER"));
    }

    @Test
    @DisplayName("Should reject login with incorrect password with 401 Unauthorized")
    void shouldRejectLoginWithIncorrectPassword() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "wrong.pass@swiggy.com",
                "RightPassword123!",
                UserRole.RIDER,
                "Speedy Rider",
                "+919876543210"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        LoginRequest wrongLogin = new LoginRequest("wrong.pass@swiggy.com", "WrongPassword999!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(wrongLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Should successfully access protected /api/v1/users/me with valid Bearer JWT")
    void shouldAccessProtectedEndpointWithValidJwt() throws Exception {
        RegisterRequest register = new RegisterRequest(
                "authenticated@swiggy.com",
                "Password123!",
                UserRole.CUSTOMER,
                "Authenticated User",
                "+919876543210"
        );

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = regResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

        // Access protected endpoint with Bearer token
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("authenticated@swiggy.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.name").value("Authenticated User"));
    }

    @Test
    @DisplayName("Should reject protected endpoint request when Authorization header is absent")
    void shouldRejectProtectedEndpointWithoutJwt() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }
}
