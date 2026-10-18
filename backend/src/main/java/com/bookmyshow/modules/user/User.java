package com.bookmyshow.modules.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A user. Signs in with email and password, phone OTP, or Google. Role is USER, THEATRE_ADMIN, or ADMIN. */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String email;

    @Column(length = 16)
    private String phone;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "google_subject", length = 255)
    private String googleSubject;

    @Column(nullable = false, length = 20)
    private String role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected User() {}

    private User(String name, String role, Instant createdAt) {
        this.name = name;
        this.role = role;
        this.createdAt = createdAt;
    }

    static User withPassword(String name, String email, String passwordHash, String role, Instant now) {
        User user = new User(name, role, now);
        user.email = email;
        user.passwordHash = passwordHash;
        return user;
    }

    static User withPhone(String name, String phone, String role, Instant now) {
        User user = new User(name, role, now);
        user.phone = phone;
        return user;
    }

    static User withGoogle(String name, String email, String googleSubject, String role, Instant now) {
        User user = new User(name, role, now);
        user.email = email;
        user.googleSubject = googleSubject;
        return user;
    }

    void linkGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    void changeRole(String role) {
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
