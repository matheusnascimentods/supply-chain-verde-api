package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.TestPrincipals;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.RejectBatchRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.batch.RejectBatchUseCase;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.application.usecase.batch.GetBatchTraceabilityUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.ListBatchesUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.RegisterBatchUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BatchControllerTest {
    private final RegisterBatchUseCase registerBatch = mock(RegisterBatchUseCase.class);
    private final ListBatchesUseCase listBatches = mock(ListBatchesUseCase.class);
    private final RejectBatchUseCase rejectBatch = mock(RejectBatchUseCase.class);
    private final BatchController controller = new BatchController(
            registerBatch, mock(GetBatchTraceabilityUseCase.class), listBatches, rejectBatch);

    @Test
    void shouldListOnlyLinkedSupplierBatchesForSupplier() {
        CustomUserPrincipal supplier = TestPrincipals.of(3L, "s@example.com", "SUPPLIER", 7L);

        controller.list(0, 20, 99L, BatchStatus.PENDING, supplier);

        verify(listBatches).execute(0, 20, null, 7L, BatchStatus.PENDING);
    }

    @Test
    void shouldScopeMarketToItsOwnBatches() {
        controller.list(0, 20, 99L, null, TestPrincipals.market(2L, "MANAGER", 40L));

        verify(listBatches).execute(0, 20, 40L, 99L, null);
    }

    @Test
    void shouldRequestBatchesForTheLoggedMarket() {
        BatchRequestDTO request = new BatchRequestDTO(5L, 7L, new BigDecimal("10"));

        controller.create(request, TestPrincipals.market(2L, "MANAGER", 40L));

        verify(registerBatch).execute(request, 40L);
    }

    @Test
    void shouldRejectWithTheLoggedSupplier() {
        controller.reject(4L, new RejectBatchRequestDTO("Sem capacidade"), TestPrincipals.of(3L, "s@example.com", "SUPPLIER", 7L));

        verify(rejectBatch).execute(4L, "Sem capacidade", 7L);
    }
}
