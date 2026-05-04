package org.example.QuanLyMuaVu.Service;

import org.example.QuanLyMuaVu.module.financial.dto.request.ExpenseRequest;
import org.example.QuanLyMuaVu.module.financial.dto.response.ExpenseResponse;
import org.example.QuanLyMuaVu.module.financial.entity.Expense;
import org.example.QuanLyMuaVu.module.season.entity.Season;
import org.example.QuanLyMuaVu.module.identity.entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.module.financial.repository.ExpenseRepository;
import org.example.QuanLyMuaVu.module.identity.port.IdentityQueryPort;
import org.example.QuanLyMuaVu.module.season.port.SeasonQueryPort;
import org.example.QuanLyMuaVu.module.financial.service.ExpenseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ExpenseService.
 * 
 * Tests cover:
 * - CREATE: Happy path, user not found, season not found, total cost
 * calculation
 * - READ: Get by ID (success, not found), Get all, Search by name
 * - UPDATE: Happy path, expense not found, user not found, season not found
 * - DELETE: Happy path, not found
 * - Edge cases: null values, boundary values, empty lists
 * 
 * Follows AAA (Arrange-Act-Assert) pattern.
 */
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private IdentityQueryPort identityQueryPort;

    @Mock
    private SeasonQueryPort seasonQueryPort;

    @InjectMocks
    private ExpenseService expenseService;

    // Test fixtures
    private User testUser;
    private Season testSeason;
    private Expense testExpense;
    private ExpenseRequest testRequest;

    @BeforeEach
    void setUp() {
        // Arrange: Common test data for all tests
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .build();

        testSeason = Season.builder()
                .id(1)
                .seasonName("Spring 2024")
                .build();

        testExpense = Expense.builder()
                .id(1)
                .user(testUser)
                .season(testSeason)
                .itemName("Fertilizer")
                .unitPrice(BigDecimal.valueOf(100))
                .quantity(5)
                .totalCost(BigDecimal.valueOf(500))
                .expenseDate(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .build();

        testRequest = ExpenseRequest.builder()
                .userId(1L)
                .seasonId(1)
                .itemName("Fertilizer")
                .unitPrice(BigDecimal.valueOf(100))
                .quantity(5)
                .expenseDate(LocalDate.now())
                .build();
    }

    // =========================================================================
    // CREATE EXPENSE TESTS
    // =========================================================================

    @Nested
    @DisplayName("createExpense() Tests")
    class CreateExpenseTests {

        @Test
        @DisplayName("Happy Path: Creates expense with valid data and calculates total cost")
        void createExpense_WithValidRequest_ReturnsExpenseResponse() {
            // Arrange
            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
            when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> {
                Expense saved = invocation.getArgument(0);
                saved.setId(1);
                return saved;
            });

            // Act
            ExpenseResponse response = expenseService.createExpense(testRequest);

            // Assert
            assertNotNull(response, "Response should not be null");
            assertEquals("testuser", response.getUserName(), "Username should match");
            assertEquals("Spring 2024", response.getSeasonName(), "Season name should match");
            assertEquals("Fertilizer", response.getItemName(), "Item name should match");
            assertEquals(BigDecimal.valueOf(100), response.getUnitPrice(), "Unit price should match");
            assertEquals(5, response.getQuantity(), "Quantity should match");
            // Verify total cost is calculated as unitPrice * quantity
            assertEquals(BigDecimal.valueOf(500), response.getTotalCost(), "Total cost should be 100 * 5 = 500");

            // Verify repository interactions
            verify(identityQueryPort).findUserById(1L);
            verify(seasonQueryPort).findSeasonById(1);
            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when user not found")
        void createExpense_WhenUserNotFound_ThrowsRuntimeException() {
            // Arrange
            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.createExpense(testRequest),
                    "Should throw RuntimeException when user not found");

            assertEquals("User not found", exception.getMessage());
            verify(seasonQueryPort, never()).findSeasonById(any());
            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when season not found")
        void createExpense_WhenSeasonNotFound_ThrowsRuntimeException() {
            // Arrange
            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
            when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.createExpense(testRequest),
                    "Should throw RuntimeException when season not found");

            assertEquals("Season not found", exception.getMessage());
            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Edge Case: Calculates total cost correctly with decimal unit price")
        void createExpense_WithDecimalUnitPrice_CalculatesTotalCostCorrectly() {
            // Arrange
            ExpenseRequest decimalRequest = ExpenseRequest.builder()
                    .userId(1L)
                    .seasonId(1)
                    .itemName("Seeds")
                    .unitPrice(BigDecimal.valueOf(12.50))
                    .quantity(8)
                    .expenseDate(LocalDate.now())
                    .build();

            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
            when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            when(expenseRepository.save(expenseCaptor.capture())).thenAnswer(invocation -> {
                Expense saved = invocation.getArgument(0);
                saved.setId(1);
                return saved;
            });

            // Act
            expenseService.createExpense(decimalRequest);

            // Assert
            Expense captured = expenseCaptor.getValue();
            // 12.50 * 8 = 100.00
            assertEquals(0, BigDecimal.valueOf(100.00).compareTo(captured.getTotalCost()),
                    "Total cost should be 12.50 * 8 = 100.00");
        }

        @Test
        @DisplayName("Edge Case: Handles quantity of 1 correctly")
        void createExpense_WithQuantityOne_TotalCostEqualsUnitPrice() {
            // Arrange
            ExpenseRequest singleItemRequest = ExpenseRequest.builder()
                    .userId(1L)
                    .seasonId(1)
                    .itemName("Single Tool")
                    .unitPrice(BigDecimal.valueOf(250))
                    .quantity(1)
                    .expenseDate(LocalDate.now())
                    .build();

            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
            when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));

            ArgumentCaptor<Expense> expenseCaptor = ArgumentCaptor.forClass(Expense.class);
            when(expenseRepository.save(expenseCaptor.capture())).thenAnswer(invocation -> {
                Expense saved = invocation.getArgument(0);
                saved.setId(1);
                return saved;
            });

            // Act
            expenseService.createExpense(singleItemRequest);

            // Assert
            Expense captured = expenseCaptor.getValue();
            assertEquals(BigDecimal.valueOf(250), captured.getTotalCost(),
                    "Total cost should equal unit price when quantity is 1");
        }
    }

    // =========================================================================
    // GET EXPENSE BY ID TESTS
    // =========================================================================

    @Nested
    @DisplayName("getExpenseById() Tests")
    class GetExpenseByIdTests {

        @Test
        @DisplayName("Happy Path: Returns expense when found")
        void getExpenseById_WhenFound_ReturnsExpenseResponse() {
            // Arrange
            when(expenseRepository.findById(1)).thenReturn(Optional.of(testExpense));

            // Act
            ExpenseResponse response = expenseService.getExpenseById(1);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getId());
            assertEquals("testuser", response.getUserName());
            assertEquals("Spring 2024", response.getSeasonName());
            assertEquals("Fertilizer", response.getItemName());
            verify(expenseRepository).findById(1);
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when expense not found")
        void getExpenseById_WhenNotFound_ThrowsRuntimeException() {
            // Arrange
            when(expenseRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.getExpenseById(999),
                    "Should throw RuntimeException when expense not found");

            assertEquals("Expense not found", exception.getMessage());
        }
    }

    // =========================================================================
    // GET ALL EXPENSES TESTS
    // =========================================================================

    @Nested
    @DisplayName("getAllExpenses() Tests")
    class GetAllExpensesTests {

        @Test
        @DisplayName("Happy Path: Returns list of all expenses")
        void getAllExpenses_WhenExpensesExist_ReturnsList() {
            // Arrange
            Expense expense2 = Expense.builder()
                    .id(2)
                    .user(testUser)
                    .season(testSeason)
                    .itemName("Pesticide")
                    .unitPrice(BigDecimal.valueOf(200))
                    .quantity(2)
                    .totalCost(BigDecimal.valueOf(400))
                    .expenseDate(LocalDate.now())
                    .createdAt(LocalDateTime.now())
                    .build();

            when(expenseRepository.findAll()).thenReturn(List.of(testExpense, expense2));

            // Act
            List<ExpenseResponse> responses = expenseService.getAllExpenses();

            // Assert
            assertNotNull(responses);
            assertEquals(2, responses.size(), "Should return 2 expenses");
            assertEquals("Fertilizer", responses.get(0).getItemName());
            assertEquals("Pesticide", responses.get(1).getItemName());
        }

        @Test
        @DisplayName("Edge Case: Returns empty list when no expenses exist")
        void getAllExpenses_WhenNoExpenses_ReturnsEmptyList() {
            // Arrange
            when(expenseRepository.findAll()).thenReturn(Collections.emptyList());

            // Act
            List<ExpenseResponse> responses = expenseService.getAllExpenses();

            // Assert
            assertNotNull(responses, "Response should not be null");
            assertTrue(responses.isEmpty(), "List should be empty");
        }
    }

    // =========================================================================
    // SEARCH EXPENSES BY NAME TESTS
    // =========================================================================

    @Nested
    @DisplayName("searchExpensesByName() Tests")
    class SearchExpensesByNameTests {

        @Test
        @DisplayName("Happy Path: Returns matching expenses for search term")
        void searchExpensesByName_WithMatchingTerm_ReturnsResults() {
            // Arrange
            when(expenseRepository.findByItemNameContainingIgnoreCase("Fert"))
                    .thenReturn(List.of(testExpense));

            // Act
            List<ExpenseResponse> responses = expenseService.searchExpensesByName("Fert");

            // Assert
            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals("Fertilizer", responses.get(0).getItemName());
        }

        @Test
        @DisplayName("Edge Case: Returns empty list when no match found")
        void searchExpensesByName_WithNoMatch_ReturnsEmptyList() {
            // Arrange
            when(expenseRepository.findByItemNameContainingIgnoreCase("NonExistent"))
                    .thenReturn(Collections.emptyList());

            // Act
            List<ExpenseResponse> responses = expenseService.searchExpensesByName("NonExistent");

            // Assert
            assertNotNull(responses);
            assertTrue(responses.isEmpty());
        }
    }

    // =========================================================================
    // UPDATE EXPENSE TESTS
    // =========================================================================

    @Nested
    @DisplayName("updateExpense() Tests")
    class UpdateExpenseTests {

        @Test
        @DisplayName("Happy Path: Updates expense with valid data")
        void updateExpense_WithValidData_ReturnsUpdatedExpense() {
            // Arrange
            ExpenseRequest updateRequest = ExpenseRequest.builder()
                    .userId(1L)
                    .seasonId(1)
                    .itemName("Updated Fertilizer")
                    .unitPrice(BigDecimal.valueOf(150))
                    .quantity(10)
                    .expenseDate(LocalDate.now().plusDays(1))
                    .build();

            when(expenseRepository.findById(1)).thenReturn(Optional.of(testExpense));
            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
            when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.of(testSeason));
            when(expenseRepository.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ExpenseResponse response = expenseService.updateExpense(1, updateRequest);

            // Assert
            assertNotNull(response);
            assertEquals("Updated Fertilizer", response.getItemName());
            assertEquals(BigDecimal.valueOf(150), response.getUnitPrice());
            assertEquals(10, response.getQuantity());
            // Verify total cost recalculated: 150 * 10 = 1500
            assertEquals(BigDecimal.valueOf(1500), response.getTotalCost());

            verify(expenseRepository).findById(1);
            verify(expenseRepository).save(any(Expense.class));
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when expense not found")
        void updateExpense_WhenExpenseNotFound_ThrowsRuntimeException() {
            // Arrange
            when(expenseRepository.findById(999)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.updateExpense(999, testRequest),
                    "Should throw RuntimeException when expense not found");

            assertEquals("Expense not found", exception.getMessage());
            verify(identityQueryPort, never()).findUserById(any());
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when user not found during update")
        void updateExpense_WhenUserNotFound_ThrowsRuntimeException() {
            // Arrange
            when(expenseRepository.findById(1)).thenReturn(Optional.of(testExpense));
            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.updateExpense(1, testRequest),
                    "Should throw RuntimeException when user not found");

            assertEquals("User not found", exception.getMessage());
            verify(expenseRepository, never()).save(any());
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when season not found during update")
        void updateExpense_WhenSeasonNotFound_ThrowsRuntimeException() {
            // Arrange
            when(expenseRepository.findById(1)).thenReturn(Optional.of(testExpense));
            when(identityQueryPort.findUserById(1L)).thenReturn(Optional.of(testUser));
            when(seasonQueryPort.findSeasonById(1)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.updateExpense(1, testRequest),
                    "Should throw RuntimeException when season not found");

            assertEquals("Season not found", exception.getMessage());
            verify(expenseRepository, never()).save(any());
        }
    }

    // =========================================================================
    // DELETE EXPENSE TESTS
    // =========================================================================

    @Nested
    @DisplayName("deleteExpense() Tests")
    class DeleteExpenseTests {

        @Test
        @DisplayName("Happy Path: Deletes expense when exists")
        void deleteExpense_WhenExists_DeletesSuccessfully() {
            // Arrange
            when(expenseRepository.existsById(1)).thenReturn(true);
            doNothing().when(expenseRepository).deleteById(1);

            // Act
            expenseService.deleteExpense(1);

            // Assert
            verify(expenseRepository).existsById(1);
            verify(expenseRepository).deleteById(1);
        }

        @Test
        @DisplayName("Negative Case: Throws RuntimeException when expense not found")
        void deleteExpense_WhenNotFound_ThrowsRuntimeException() {
            // Arrange
            when(expenseRepository.existsById(999)).thenReturn(false);

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> expenseService.deleteExpense(999),
                    "Should throw RuntimeException when expense not found");

            assertEquals("Expense not found", exception.getMessage());
            verify(expenseRepository, never()).deleteById(any());
        }
    }

    // =========================================================================
    // MAPPER TESTS
    // =========================================================================

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Verifies all fields are correctly mapped to response")
        void mapToResponse_MapsAllFieldsCorrectly() {
            // Arrange
            LocalDate expenseDate = LocalDate.of(2024, 6, 15);
            LocalDateTime createdAt = LocalDateTime.of(2024, 6, 15, 10, 30);

            Expense fullExpense = Expense.builder()
                    .id(42)
                    .user(testUser)
                    .season(testSeason)
                    .itemName("Complete Test Item")
                    .unitPrice(BigDecimal.valueOf(99.99))
                    .quantity(7)
                    .totalCost(BigDecimal.valueOf(699.93))
                    .expenseDate(expenseDate)
                    .createdAt(createdAt)
                    .build();

            when(expenseRepository.findById(42)).thenReturn(Optional.of(fullExpense));

            // Act
            ExpenseResponse response = expenseService.getExpenseById(42);

            // Assert - verify all fields are mapped
            assertEquals(42, response.getId());
            assertEquals("testuser", response.getUserName());
            assertEquals("Spring 2024", response.getSeasonName());
            assertEquals("Complete Test Item", response.getItemName());
            assertEquals(BigDecimal.valueOf(99.99), response.getUnitPrice());
            assertEquals(7, response.getQuantity());
            assertEquals(BigDecimal.valueOf(699.93), response.getTotalCost());
            assertEquals(expenseDate, response.getExpenseDate());
            assertEquals(createdAt, response.getCreatedAt());
        }
    }
}


