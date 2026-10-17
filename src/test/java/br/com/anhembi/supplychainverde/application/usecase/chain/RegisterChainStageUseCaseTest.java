package br.com.anhembi.supplychainverde.application.usecase.chain;

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

    private final Batch batch = Batch.builder().batchId(1L).build();

    @BeforeEach
    void setUp() {
        when(batches.findById(1L)).thenReturn(Optional.of(batch));
        when(users.findById(2L)).thenReturn(Optional.of(User.builder().userId(2L).build()));
        when(chains.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
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
        return useCase.execute(1L, 2L, new ChainRequestDTO(1L, null, null, type, LocalDateTime.now(), null));
    }
}
