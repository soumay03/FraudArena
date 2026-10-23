package com.Terminator.FraudArena.controller;

import com.Terminator.FraudArena.dto.TransferRequest;
import com.Terminator.FraudArena.dto.TransferResponse;
import com.Terminator.FraudArena.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = transactionService.transfer(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransferResponse> getTransactionById(@PathVariable Long id) {
        TransferResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }
}
