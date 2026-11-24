package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.exception.BatchNotFoundException;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CancelBatchUseCaseTest {
    private final BatchRepository batches = mock(BatchRepository.class);
    private final CancelBatchUseCase useCase = new CancelBatchUseCase(batches, DtoMappers.BATCH);
    private final Batch batch = Batch.builder().batchId(1L).marketId(40L).status(BatchStatus.PENDING)
            .supplier(Company.builder().companyId(7L).build()).build();

    CancelBatchUseCaseTest() {
        when(batches.findById(1L)).thenReturn(Optional.of(batch));
        when(batches.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void marketOfTheBatchCancelsAPendingBatch() {
        assertThat(useCase.execute(1L, 40L, CompanyType.MARKET).status()).isEqualTo(BatchStatus.CANCELED);
        verify(batches).save(batch);
    }

    @Test
    void platformAdminCancels() {
        useCase.execute(1L, null, null);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.CANCELED);
    }

    @Test
    void supplierAndOtherMarketAreDenied() {
        assertThatThrownBy(() -> useCase.execute(1L, 7L, CompanyType.SUPPLIER)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> useCase.execute(1L, 99L, CompanyType.MARKET)).isInstanceOf(AccessDeniedException.class);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.PENDING);
        verify(batches, never()).save(any());
    }

    @Test
    void onlyPendingBatchIsCanceled() {
        batch.setStatus(BatchStatus.ACTIVE);

        assertThatThrownBy(() -> useCase.execute(1L, 40L, CompanyType.MARKET))
                .isInstanceOf(BatchStatusConflictException.class);
        verify(batches, never()).save(any());
    }

    @Test
    void missingBatchIsNotFound() {
        assertThatThrownBy(() -> useCase.execute(2L, 40L, CompanyType.MARKET)).isInstanceOf(BatchNotFoundException.class);
    }
}
