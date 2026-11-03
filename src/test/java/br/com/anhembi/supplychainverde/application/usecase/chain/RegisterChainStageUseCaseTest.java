package br.com.anhembi.supplychainverde.application.usecase.chain;

import java.time.LocalDate;
import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.exception.InvalidStageTransitionException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.ChainRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterChainStageUseCaseTest {
    private final ChainRepository chains = mock(ChainRepository.class);
    private final BatchRepository batches = mock(BatchRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RegisterChainStageUseCase useCase = new RegisterChainStageUseCase(
            chains, batches, users, mock(AddressRepository.class), DtoMappers.CHAIN);

    private final Batch batch = Batch.builder().batchId(1L).marketId(40L).status(BatchStatus.ACTIVE)
            .supplier(Company.builder().companyId(7L).build()).build();

    @BeforeEach
    void setUp() {
        when(batches.findById(1L)).thenReturn(Optional.of(batch));
        when(users.findById(2L)).thenReturn(Optional.of(User.builder().userId(2L).companyId(40L).build()));
        when(users.findById(3L)).thenReturn(Optional.of(User.builder().userId(3L).companyId(99L).build()));
        when(chains.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void productionBySupplierActivatesAPendingBatch() {
        batch.setStatus(BatchStatus.PENDING);
        given();

        useCase.execute(1L, 2L, new ChainRequestDTO(1L, null, null, StageType.PRODUCTION,
                LocalDateTime.of(2026, 3, 4, 10, 0), null), null, 7L);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);
        assertThat(batch.getProducedAt()).isEqualTo(LocalDate.of(2026, 3, 4));
        verify(batches).save(batch);
    }

    @Test
    void pendingBatchRejectsTheMarketAndRejectedBatchIsFinal() {
        batch.setStatus(BatchStatus.PENDING);
        given();
        assertThatThrownBy(() -> register(StageType.PRODUCTION)).isInstanceOf(AccessDeniedException.class);

        batch.setStatus(BatchStatus.REJECTED);
        assertThatThrownBy(() -> register(StageType.PRODUCTION)).isInstanceOf(BatchStatusConflictException.class);
        verify(chains, never()).save(any());
    }

    @Test
    void rejectsBatchOfAnotherCompanyAndResponsibleOutsideTheBatch() {
        given();
        assertThatThrownBy(() -> useCase.execute(1L, 2L, request(StageType.PRODUCTION), 99L, null))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> useCase.execute(1L, 3L, request(StageType.PRODUCTION), 40L, null))
                .isInstanceOf(AccessDeniedException.class);
        verify(chains, never()).save(any());
    }

    @Test
    void emptyBatchAcceptsOnlyProduction() {
        given();
        assertThat(register(StageType.PRODUCTION).stageType()).isEqualTo(StageType.PRODUCTION);
        assertThatThrownBy(() -> register(StageType.STORAGE))
                .isInstanceOf(InvalidStageTransitionException.class)
                .hasMessageContaining("deve começar em PRODUCTION");
    }

    @Test
    void acceptsOnlyTheNextStage() {
        given(StageType.PRODUCTION, StageType.STORAGE);
        assertThat(register(StageType.PROCESSING).stageType()).isEqualTo(StageType.PROCESSING);
    }

    @Test
    void rejectsSkippedRepeatedAndPostRetailStages() {
        given(StageType.PRODUCTION);
        assertThatThrownBy(() -> register(StageType.PROCESSING)).isInstanceOf(InvalidStageTransitionException.class);
        assertThatThrownBy(() -> register(StageType.PRODUCTION)).isInstanceOf(InvalidStageTransitionException.class);

        given(StageType.values());
        assertThatThrownBy(() -> register(StageType.RETAIL)).isInstanceOf(InvalidStageTransitionException.class);
        verify(chains, never()).save(any());
    }

    private void given(StageType... existing) {
        when(chains.findByBatchId(1L)).thenReturn(Arrays.stream(existing)
                .map(type -> Chain.builder().batch(batch).stageType(type).build())
                .toList());
    }

    private ChainResponseDTO register(StageType type) {
        return useCase.execute(1L, 2L, request(type), 40L, null);
    }

    private ChainRequestDTO request(StageType type) {
        return new ChainRequestDTO(1L, null, null, type, LocalDateTime.now(), null);
    }
}
