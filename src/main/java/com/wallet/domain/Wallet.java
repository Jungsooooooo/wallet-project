package com.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * 지갑 집계: 현재 잔액만 보관한다. 입출금 내역은 {@link WalletLedgerEntry} 원장이 담당한다.
 */
@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false)
    private long balance;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    protected Wallet() {
    }

    public Wallet(long initialBalance) {
        this.balance = initialBalance;
    }

    @PrePersist
    void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public String getId() {
        return id;
    }

    public long getBalance() {
        return balance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void add(long amount) {
        this.balance = Math.addExact(this.balance, amount);
        this.updatedAt = Instant.now();
    }

    public void subtract(long amount) {
        this.balance = Math.subtractExact(this.balance, amount);
        this.updatedAt = Instant.now();
    }
}
