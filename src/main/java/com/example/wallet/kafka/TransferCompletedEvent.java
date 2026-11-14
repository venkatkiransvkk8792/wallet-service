package com.example.wallet.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        UUID transferId,
        String idempotencyKey,
        String fromWalletId,
        String toWalletId,
        BigDecimal amount,
        Instant occurredAt
) {}
