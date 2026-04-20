package com.bmsoftware.shared.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class PaymentCreatedEvent {
    private UUID paymentId;
    private BigDecimal amount;
    private String currency;
    private String recipientAccount;
    private String senderAccount;
    private LocalDateTime createdAt;
    private String status;

    public PaymentCreatedEvent() {
    }

    public PaymentCreatedEvent(UUID paymentId, BigDecimal amount, String currency, String recipientAccount, String senderAccount, LocalDateTime createdAt, String status) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.currency = currency;
        this.recipientAccount = recipientAccount;
        this.senderAccount = senderAccount;
        this.createdAt = createdAt;
        this.status = status;
    }

    public UUID getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(UUID paymentId) {
        this.paymentId = paymentId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRecipientAccount() {
        return recipientAccount;
    }

    public void setRecipientAccount(String recipientAccount) {
        this.recipientAccount = recipientAccount;
    }

    public String getSenderAccount() {
        return senderAccount;
    }

    public void setSenderAccount(String senderAccount) {
        this.senderAccount = senderAccount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentCreatedEvent that = (PaymentCreatedEvent) o;
        return Objects.equals(paymentId, that.paymentId) && Objects.equals(amount, that.amount) && Objects.equals(currency, that.currency) && Objects.equals(recipientAccount, that.recipientAccount) && Objects.equals(senderAccount, that.senderAccount) && Objects.equals(createdAt, that.createdAt) && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId, amount, currency, recipientAccount, senderAccount, createdAt, status);
    }

    @Override
    public String toString() {
        return "PaymentCreatedEvent{" +
                "paymentId=" + paymentId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", recipientAccount='" + recipientAccount + '\'' +
                ", senderAccount='" + senderAccount + '\'' +
                ", createdAt=" + createdAt +
                ", status='" + status + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID paymentId;
        private BigDecimal amount;
        private String currency;
        private String recipientAccount;
        private String senderAccount;
        private LocalDateTime createdAt;
        private String status;

        public Builder paymentId(UUID paymentId) {
            this.paymentId = paymentId;
            return this;
        }

        public Builder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public Builder currency(String currency) {
            this.currency = currency;
            return this;
        }

        public Builder recipientAccount(String recipientAccount) {
            this.recipientAccount = recipientAccount;
            return this;
        }

        public Builder senderAccount(String senderAccount) {
            this.senderAccount = senderAccount;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder status(String status) {
            this.status = status;
            return this;
        }

        public PaymentCreatedEvent build() {
            return new PaymentCreatedEvent(paymentId, amount, currency, recipientAccount, senderAccount, createdAt, status);
        }
    }
}
