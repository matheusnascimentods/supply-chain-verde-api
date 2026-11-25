package br.com.anhembi.supplychainverde.application.usecase.chain;

import java.time.LocalDate;
import org.springframework.security.access.AccessDeniedException;
import br.com.anhembi.supplychainverde.domain.exception.BatchStatusConflictException;
import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.enums.CompanyType;
import br.com.anhembi.supplychainverde.domain.exception.InvalidStageTransitionException;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
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
    private final AddressRepository addresses = mock(AddressRepository.class);
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final RegisterChainStageUseCase useCase = new RegisterChainStageUseCase(
            chains, batches, users, addresses, companies, DtoMappers.ADDRESS, DtoMappers.CHAIN);

    private final Address farm = Address.builder().addressId(70L).city("Amparo").state("SP").build();
    private final Address store = Address.builder().addressId(400L).city("São Paulo").state("SP").build();
    private final Batch batch = Batch.builder().batchId(1L).marketId(40L).status(BatchStatus.ACTIVE)
            .supplier(Company.builder().companyId(7L).address(farm).build()).build();

    @BeforeEach
    void setUp() {
        when(batches.findById(1L)).thenReturn(Optional.of(batch));
        when(users.findById(2L)).thenReturn(Optional.of(User.builder().userId(2L).companyId(7L).build()));
        when(users.findById(4L)).thenReturn(Optional.of(User.builder().userId(4L).companyId(40L).build()));
        when(users.findById(3L)).thenReturn(Optional.of(User.builder().userId(3L).companyId(99L).build()));
        when(chains.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(addresses.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(companies.findById(40L)).thenReturn(Optional.of(Company.builder().companyId(40L).address(store).build()));
    }

    @Test
    void productionRequiresACompleteDestinationAndStartsAtTheSupplier() {
        given();
        assertThatThrownBy(() -> register(StageType.PRODUCTION, null))
                .isInstanceOf(ValidationException.class).hasMessage("Destino da etapa é obrigatório.");
        assertThatThrownBy(() -> register(StageType.PRODUCTION,
                new AddressRequestDTO("Rua A", "1", "Centro", null, "13000000", " ", "SP")))
                .isInstanceOf(ValidationException.class).hasMessageStartingWith("Destino incompleto");
        verify(chains, never()).save(any());

        ChainResponseDTO saved = register(StageType.PRODUCTION,
                new AddressRequestDTO(" Rua A ", "1", "Centro", " ", "13000000", "Campinas", "sp"));

        assertThat(saved.originAddress().city()).isEqualTo("Amparo");
        assertThat(saved.destinationAddress().street()).isEqualTo("Rua A");
        assertThat(saved.destinationAddress().complement()).isNull();
        assertThat(saved.destinationAddress().state()).isEqualTo("SP");
        verify(addresses).save(any());
    }

    @Test
    void nextStageStartsWhereThePreviousOneEnded() {
        Address silo = Address.builder().addressId(5L).city("Campinas").state("SP").build();
        when(chains.findByBatchId(1L)).thenReturn(java.util.List.of(
                Chain.builder().batch(batch).stageType(StageType.PRODUCTION).destinationAddress(farm).build(),
                Chain.builder().batch(batch).stageType(StageType.STORAGE).destinationAddress(silo).build()));

        assertThat(register(StageType.PROCESSING).originAddress().city()).isEqualTo("Campinas");
    }

    @Test
    void retailEndsAtTheMarketAndRejectsAnInformedDestination() {
        given(StageType.PRODUCTION, StageType.STORAGE, StageType.PROCESSING,
                StageType.TRANSPORT, StageType.DISTRIBUTION);
        assertThatThrownBy(() -> useCase.execute(1L, 4L, new ChainRequestDTO(1L, destination(), StageType.RETAIL,
                LocalDateTime.now(), null), 40L, null, CompanyType.MARKET))
                .isInstanceOf(ValidationException.class).hasMessageContaining("endereço do mercado");
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);

        ChainResponseDTO retail = useCase.execute(1L, 4L, request(StageType.RETAIL), 40L, null, CompanyType.MARKET);

        assertThat(retail.destinationAddress().addressId()).isEqualTo(400L);
        verify(addresses, never()).save(any());
    }

    @Test
    void productionBySupplierActivatesAPendingBatch() {
        batch.setStatus(BatchStatus.PENDING);
        given();

        useCase.execute(1L, 2L, new ChainRequestDTO(1L, destination(), StageType.PRODUCTION,
                LocalDateTime.of(2026, 3, 4, 10, 0), null), null, 7L, CompanyType.SUPPLIER);

        assertThat(batch.getStatus()).isEqualTo(BatchStatus.ACTIVE);
        assertThat(batch.getProducedAt()).isEqualTo(LocalDate.of(2026, 3, 4));
        verify(batches).save(batch);
    }

    @Test
    void pendingBatchRejectsTheMarketAndRejectedBatchIsFinal() {
        batch.setStatus(BatchStatus.PENDING);
        given();
        assertThatThrownBy(() -> useCase.execute(1L, 4L, request(StageType.PRODUCTION), 40L, null, CompanyType.MARKET))
                .isInstanceOf(AccessDeniedException.class);

        batch.setStatus(BatchStatus.REJECTED);
        assertThatThrownBy(() -> register(StageType.PRODUCTION)).isInstanceOf(BatchStatusConflictException.class);
        verify(chains, never()).save(any());
    }

    @Test
    void rejectsBatchOfAnotherCompanyAndResponsibleOutsideTheBatch() {
        given();
        assertThatThrownBy(() -> useCase.execute(1L, 2L, request(StageType.PRODUCTION), 99L, null, CompanyType.SUPPLIER))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> useCase.execute(1L, 3L, request(StageType.PRODUCTION), 40L, null, CompanyType.MARKET))
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
    void marketRegistersRetailAndSupplierCannotRegisterIt() {
        given(StageType.PRODUCTION, StageType.STORAGE, StageType.PROCESSING,
                StageType.TRANSPORT, StageType.DISTRIBUTION);

        assertThatThrownBy(() -> useCase.execute(1L, 2L, request(StageType.RETAIL),
                null, 7L, CompanyType.SUPPLIER))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(useCase.execute(1L, 4L, request(StageType.RETAIL),
                40L, null, CompanyType.MARKET).stageType()).isEqualTo(StageType.RETAIL);
        assertThat(batch.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        verify(batches).save(batch);
    }

    @Test
    void completedBatchRejectsAnyNewStage() {
        batch.setStatus(BatchStatus.COMPLETED);
        given();

        assertThatThrownBy(() -> useCase.execute(1L, 4L, request(StageType.RETAIL),
                40L, null, CompanyType.MARKET))
                .isInstanceOf(BatchStatusConflictException.class);
        verify(chains, never()).save(any());
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
        return useCase.execute(1L, 2L, request(type), null, 7L, CompanyType.SUPPLIER);
    }

    private ChainResponseDTO register(StageType type, AddressRequestDTO destination) {
        return useCase.execute(1L, 2L, new ChainRequestDTO(1L, destination, type, LocalDateTime.now(), null),
                null, 7L, CompanyType.SUPPLIER);
    }

    /** RETAIL vai sem destino (RN-03); as outras etapas com um destino completo. */
    private ChainRequestDTO request(StageType type) {
        return new ChainRequestDTO(1L, type == StageType.RETAIL ? null : destination(), type, LocalDateTime.now(), null);
    }

    private AddressRequestDTO destination() {
        return new AddressRequestDTO("Rua A", "1", "Centro", null, "13000000", "Campinas", "SP");
    }
}
