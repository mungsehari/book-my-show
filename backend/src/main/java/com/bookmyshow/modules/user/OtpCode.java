package com.bookmyshow.modules.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A one-time login code sent to a phone. Only the hash of the code is stored. */
@Entity
@Table(name = "otp_codes")
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 16)
    private String phone;

    @Column(name = "code_hash", nullable = false, length = 100)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Integer attempts;

    @Column(nullable = false)
    private Boolean used;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected OtpCode() {}

    public OtpCode(String phone, String codeHash, Instant createdAt, Instant expiresAt) {
        this.phone = phone;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.attempts = 0;
        this.used = false;
    }

    /** True if the code can still be tried: not used, not expired, and attempts left. */
    boolean isUsable(Instant now, int maxAttempts) {
        return !used && now.isBefore(expiresAt) && attempts < maxAttempts;
    }

    void recordAttempt() {
        attempts++;
    }

    void markUsed() {
        used = true;
    }

    public String getPhone() {
        return phone;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public Boolean getUsed() {
        return used;
    }
}
