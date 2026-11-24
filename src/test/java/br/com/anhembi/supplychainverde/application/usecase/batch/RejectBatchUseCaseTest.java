package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RejectBatchUseCaseTest {
    private final BatchRepository batches = mock(BatchRepository.class);
    private final RejectBatchUseCase useCase = new RejectBatchUseCase(batches, DtoMappers.BATCH);
    private final Batch batch = Batch.builder().batchId(1L).marketId(40L).status(BatchStatus.PENDING)
            .supplier(Company.builder().companyId(7L).build()).build();

    RejectBatchUseCaseTest() {
        when(batches.findById(1L)).thenReturn(Optional.of(batch));
        when(batches.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void supplierOfTheBatchRejects() {
        assertThat(useCase.execute(1L, "Sem capacidade", 7L, CompanyType.SUPPLIER).status()).isEqualTo(BatchStatus.REJECTED);
        assertThat(batch.getRejectionReason()).isEqualTo("Sem capacidade");
    }

    @Test
    void platformAdminRejects() {
        useCase.execute(1L, "Sem capacidade", null, null);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.REJECTED);
    }

    @Test
    void marketAndOtherSupplierAreDenied() {
        assertThatThrownBy(() -> useCase.execute(1L, "x", 40L, CompanyType.MARKET)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> useCase.execute(1L, "x", 99L, CompanyType.SUPPLIER)).isInstanceOf(AccessDeniedException.class);
        verify(batches, never()).save(any());
    }
}
