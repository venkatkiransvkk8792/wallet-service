package com.example.wallet.service;

import com.example.wallet.api.TransferHistoryItem;
import com.example.wallet.dynamodb.TransferReadModel;
import com.example.wallet.dynamodb.TransferReadModelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransferReadService {
    private final TransferReadModelRepository repository;

    public TransferReadService(TransferReadModelRepository repository) {
        this.repository = repository;
    }

    public List<TransferHistoryItem> history(String walletId, int limit) {
        return repository.findByWalletId(walletId, Math.min(Math.max(limit, 1), 100))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private TransferHistoryItem toResponse(TransferReadModel item) {
        return new TransferHistoryItem(
                item.getTransferId(),
                item.getDirection(),
                item.getCounterpartyWalletId(),
                item.getAmount(),
                item.getOccurredAt(),
                item.getIdempotencyKey());
    }
}
