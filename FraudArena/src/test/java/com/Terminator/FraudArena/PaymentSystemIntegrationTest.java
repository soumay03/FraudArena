package com.Terminator.FraudArena;

import com.Terminator.FraudArena.dto.AccountResponse;
import com.Terminator.FraudArena.dto.CreateAccountRequest;
import com.Terminator.FraudArena.dto.TransferRequest;
import com.Terminator.FraudArena.dto.TransferResponse;
import com.Terminator.FraudArena.entity.TransactionStatus;
import com.Terminator.FraudArena.repository.AccountRepository;
import com.Terminator.FraudArena.repository.TransactionRepository;
import com.Terminator.FraudArena.service.AccountService;
import com.Terminator.FraudArena.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class PaymentSystemIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private TransactionService transactionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("Complete Happy Path: Create User A & B, Transfer 1,000, verify balances and transaction record")
    void testCompleteHappyPathTransfer() throws Exception {
        // 1. Create User A with 10,000.00
        String userAJson = "{\"name\":\"User A\",\"initialBalance\":10000.00}";
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userAJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("User A"))
                .andExpect(jsonPath("$.balance").value(10000.00));

        Long userAId = accountRepository.findAll().stream()
                .filter(a -> a.getName().equals("User A"))
                .findFirst().orElseThrow().getId();

        // 2. Create User B with 5,000.00
        String userBJson = "{\"name\":\"User B\",\"initialBalance\":5000.00}";
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userBJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("User B"))
                .andExpect(jsonPath("$.balance").value(5000.00));

        Long userBId = accountRepository.findAll().stream()
                .filter(a -> a.getName().equals("User B"))
                .findFirst().orElseThrow().getId();

        // 3. Transfer 1,000.00 from User A to User B
        String transferJson = String.format("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":1000.00}", userAId, userBId);
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.fromAccountId").value(userAId))
                .andExpect(jsonPath("$.toAccountId").value(userBId))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value(TransactionStatus.SUCCESS.name()));

        Long transactionId = transactionRepository.findAll().get(0).getId();

        // 4. Retrieve User A and verify balance is 9,000.00
        mockMvc.perform(get("/api/accounts/" + userAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userAId))
                .andExpect(jsonPath("$.name").value("User A"))
                .andExpect(jsonPath("$.balance").value(9000.00));

        // 4. Retrieve User B and verify balance is 6,000.00
        mockMvc.perform(get("/api/accounts/" + userBId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userBId))
                .andExpect(jsonPath("$.name").value("User B"))
                .andExpect(jsonPath("$.balance").value(6000.00));

        // 5. Retrieve the transaction record and verify its details
        mockMvc.perform(get("/api/transactions/" + transactionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.fromAccountId").value(userAId))
                .andExpect(jsonPath("$.toAccountId").value(userBId))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.status").value(TransactionStatus.SUCCESS.name()))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Insufficient balance: transfer fails and balances remain unchanged")
    void testInsufficientBalanceTransfer() throws Exception {
        AccountResponse accA = accountService.createAccount(new CreateAccountRequest("User A", new BigDecimal("100.00")));
        AccountResponse accB = accountService.createAccount(new CreateAccountRequest("User B", new BigDecimal("500.00")));

        // Attempt transfer of 500 from User A who only has 100
        String transferJson = String.format("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":500.00}", accA.getId(), accB.getId());
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient balance")));

        // Verify balances remain unchanged
        mockMvc.perform(get("/api/accounts/" + accA.getId()))
                .andExpect(jsonPath("$.balance").value(100.00));
        mockMvc.perform(get("/api/accounts/" + accB.getId()))
                .andExpect(jsonPath("$.balance").value(500.00));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Non-existent sender or receiver account fails with 404")
    void testNonExistentAccountTransfer() throws Exception {
        AccountResponse accA = accountService.createAccount(new CreateAccountRequest("User A", new BigDecimal("1000.00")));

        // Non-existent receiver
        String invalidReceiverJson = String.format("{\"fromAccountId\":%d,\"toAccountId\":99999,\"amount\":100.00}", accA.getId());
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidReceiverJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Receiver account not found")));

        // Non-existent sender
        String invalidSenderJson = String.format("{\"fromAccountId\":99999,\"toAccountId\":%d,\"amount\":100.00}", accA.getId());
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidSenderJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sender account not found")));

        // Balance remains unchanged
        mockMvc.perform(get("/api/accounts/" + accA.getId()))
                .andExpect(jsonPath("$.balance").value(1000.00));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Transfer to same account fails with 400 Bad Request")
    void testTransferToSameAccount() throws Exception {
        AccountResponse accA = accountService.createAccount(new CreateAccountRequest("User A", new BigDecimal("1000.00")));

        String sameAccountJson = String.format("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":100.00}", accA.getId(), accA.getId());
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameAccountJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Sender and receiver accounts must be different"));

        mockMvc.perform(get("/api/accounts/" + accA.getId()))
                .andExpect(jsonPath("$.balance").value(1000.00));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Zero or negative transfer amount fails with 400 Bad Request")
    void testZeroOrNegativeTransferAmount() throws Exception {
        AccountResponse accA = accountService.createAccount(new CreateAccountRequest("User A", new BigDecimal("1000.00")));
        AccountResponse accB = accountService.createAccount(new CreateAccountRequest("User B", new BigDecimal("500.00")));

        // Transfer 0.00
        String zeroAmountJson = String.format("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":0.00}", accA.getId(), accB.getId());
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(zeroAmountJson))
                .andExpect(status().isBadRequest());

        // Transfer negative amount
        String negativeAmountJson = String.format("{\"fromAccountId\":%d,\"toAccountId\":%d,\"amount\":-50.00}", accA.getId(), accB.getId());
        mockMvc.perform(post("/api/transactions/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeAmountJson))
                .andExpect(status().isBadRequest());

        // Verify balances unchanged
        mockMvc.perform(get("/api/accounts/" + accA.getId()))
                .andExpect(jsonPath("$.balance").value(1000.00));
        mockMvc.perform(get("/api/accounts/" + accB.getId()))
                .andExpect(jsonPath("$.balance").value(500.00));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Invalid account creation validation errors")
    void testInvalidAccountCreation() throws Exception {
        // Blank name
        String blankNameJson = "{\"name\":\"\",\"initialBalance\":100.00}";
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blankNameJson))
                .andExpect(status().isBadRequest());

        // Negative initial balance
        String negativeBalanceJson = "{\"name\":\"User Invalid\",\"initialBalance\":-50.00}";
        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(negativeBalanceJson))
                .andExpect(status().isBadRequest());
    }
}
