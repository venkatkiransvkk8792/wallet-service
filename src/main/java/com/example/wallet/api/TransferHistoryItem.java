package com.example.wallet.api;

import java.math.BigDecimal;

public record TransferHistoryItem(
        String transferId,
        String direction,
        String counterpartyWalletId,
        BigDecimal amount,
        String occurredAt,
        String idempotencyKey
) {}
