package com.Terminator.FraudArena.service;

import com.Terminator.FraudArena.dto.AccountResponse;
import com.Terminator.FraudArena.dto.CreateAccountRequest;
import com.Terminator.FraudArena.entity.Account;
import com.Terminator.FraudArena.exception.ResourceNotFoundException;
import com.Terminator.FraudArena.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account(request.getName(), request.getInitialBalance());
        Account savedAccount = accountRepository.save(account);
        return mapToResponse(savedAccount);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        return mapToResponse(account);
    }

    private AccountResponse mapToResponse(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getBalance());
    }
}
