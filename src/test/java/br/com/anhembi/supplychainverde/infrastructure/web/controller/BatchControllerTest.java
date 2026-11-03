package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.batch.GetBatchTraceabilityUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.ListBatchesUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.RegisterBatchUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BatchControllerTest {
    private final RegisterBatchUseCase registerBatch = mock(RegisterBatchUseCase.class);
    private final ListBatchesUseCase listBatches = mock(ListBatchesUseCase.class);
    private final BatchController controller = new BatchController(
            registerBatch, mock(GetBatchTraceabilityUseCase.class), listBatches);

    @Test
    void shouldListOnlyLinkedSupplierBatchesForSupplier() {
        CustomUserPrincipal supplier = TestPrincipals.of(3L, "s@example.com", "SUPPLIER", 7L);

        controller.list(0, 20, 99L, supplier);

        verify(listBatches).execute(0, 20, null, 7L);
    }

    @Test
    void shouldUseRequestedSupplierFilterForNonSupplier() {
        CustomUserPrincipal admin = TestPrincipals.of(1L, "a@example.com", "ADMIN", null);

        controller.list(0, 20, 99L, admin);

        verify(listBatches).execute(0, 20, null, 99L);
    }

    @Test
    void shouldScopeMarketToItsOwnBatches() {
        controller.list(0, 20, 99L, br.com.anhembi.supplychainverde.TestPrincipals.market(2L, "MANAGER", 40L));

        verify(listBatches).execute(0, 20, 40L, 99L);
    }

    @Test
    void shouldRegisterBatchForLinkedSupplierIgnoringBodySupplierId() {
        CustomUserPrincipal supplier = TestPrincipals.of(3L, "s@example.com", "SUPPLIER", 7L);

        controller.create(batchFor(99L), supplier);

        verify(registerBatch).execute(batchFor(7L));
    }

    @Test
    void shouldRegisterBatchForBodySupplierIdForNonSupplier() {
        CustomUserPrincipal admin = TestPrincipals.of(1L, "a@example.com", "ADMIN", null);

        controller.create(batchFor(99L), admin);

        verify(registerBatch).execute(batchFor(99L));
    }

    private BatchRequestDTO batchFor(Long supplierId) {
        return new BatchRequestDTO(5L, supplierId, new BigDecimal("10"), LocalDate.of(2026, 10, 1));
    }
}
