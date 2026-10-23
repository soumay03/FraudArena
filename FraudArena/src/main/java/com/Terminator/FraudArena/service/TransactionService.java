package com.Terminator.FraudArena.service;

import com.Terminator.FraudArena.dto.TransferRequest;
import com.Terminator.FraudArena.dto.TransferResponse;
import com.Terminator.FraudArena.entity.Account;
import com.Terminator.FraudArena.entity.Transaction;
import com.Terminator.FraudArena.entity.TransactionStatus;
import com.Terminator.FraudArena.exception.InsufficientBalanceException;
import com.Terminator.FraudArena.exception.InvalidTransactionException;
import com.Terminator.FraudArena.exception.ResourceNotFoundException;
import com.Terminator.FraudArena.repository.AccountRepository;
import com.Terminator.FraudArena.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public TransactionService(AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        // 1. Validate that sender and receiver are not the same account
        if (request.getFromAccountId().equals(request.getToAccountId())) {
            throw new InvalidTransactionException("Sender and receiver accounts must be different");
        }

        // 2. Validate that amount is positive
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransactionException("Transfer amount must be greater than zero");
        }

        // 3. Verify sender account exists
        Account sender = accountRepository.findById(request.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Sender account not found with id: " + request.getFromAccountId()));

        // 4. Verify receiver account exists
        Account receiver = accountRepository.findById(request.getToAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver account not found with id: " + request.getToAccountId()));

        // 5. Ensure sender has sufficient balance
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in sender account. Available: " + sender.getBalance() + ", Requested: " + request.getAmount()
            );
        }

        // 6. Deduct amount from sender
        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        accountRepository.save(sender);

        // 7. Add amount to receiver
        receiver.setBalance(receiver.getBalance().add(request.getAmount()));
        accountRepository.save(receiver);

        // 8. Save transaction record with timestamp and status
        Transaction transaction = new Transaction(
                sender.getId(),
                receiver.getId(),
                request.getAmount(),
                LocalDateTime.now(),
                TransactionStatus.SUCCESS
        );
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 9. Return response confirming the transfer
        return new TransferResponse(
                savedTransaction.getId(),
                savedTransaction.getFromAccountId(),
                savedTransaction.getToAccountId(),
                savedTransaction.getAmount(),
                savedTransaction.getTimestamp(),
                savedTransaction.getStatus(),
                "Transfer of " + savedTransaction.getAmount() + " completed successfully from account " 
                        + sender.getId() + " to account " + receiver.getId()
        );
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        return new TransferResponse(
                transaction.getId(),
                transaction.getFromAccountId(),
                transaction.getToAccountId(),
                transaction.getAmount(),
                transaction.getTimestamp(),
                transaction.getStatus(),
                "Transaction retrieved successfully"
        );
    }
}
