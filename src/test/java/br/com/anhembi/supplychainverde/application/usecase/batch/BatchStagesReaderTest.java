package br.com.anhembi.supplychainverde.application.usecase.batch;

import br.com.anhembi.supplychainverde.domain.entity.*;
import br.com.anhembi.supplychainverde.domain.enums.*;
import br.com.anhembi.supplychainverde.domain.repository.*;
import br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BatchStagesReaderTest {
    @Test
    void shouldReturnChronologicalStagesWithTransportAndEmissionInBulk() {
        ChainRepository chains = mock(ChainRepository.class);
        TransportRepository transports = mock(TransportRepository.class);
        CarbonEmissionRepository emissions = mock(CarbonEmissionRepository.class);
        BatchStagesReader reader = new BatchStagesReader(chains, transports, emissions);

        Batch batch = Batch.builder().batchId(10L).build();
        User user = User.builder().userId(7L).name("Ana").build();
        Address origin = Address.builder().addressId(3L).city("Amparo").state("SP").build();
        Chain second = Chain.builder().chainId(22L).batch(batch).responsibleUser(user).stageType(StageType.TRANSPORT)
                .startedAt(LocalDateTime.parse("2026-09-21T06:00:00")).build();
        Chain first = Chain.builder().chainId(21L).batch(batch).responsibleUser(user).originAddress(origin)
                .stageType(StageType.PRODUCTION).startedAt(LocalDateTime.parse("2026-09-20T08:00:00")).build();
        Transport transport = Transport.builder().transportId(33L).chain(second).transportMode(TransportMode.ROAD)
                .distance(new BigDecimal("180.0")).fuelType(FuelType.BIODIESEL).capacity(new BigDecimal("1200.0")).build();
        CarbonEmission emission = CarbonEmission.builder().emissionId(44L).chain(first)
                .emissionFactor(new EmissionFactor(new BigDecimal("0.25"))).co2Kg(new BigDecimal("12.5"))
                .calculationMethod(CalculationMethod.GHG_PROTOCOL).calculatedAt(LocalDate.parse("2026-09-20")).build();
        when(chains.findByBatchIds(List.of(10L))).thenReturn(List.of(second, first));
        when(transports.findByChainIds(Set.of(21L, 22L))).thenReturn(List.of(transport));
        when(emissions.findByChainIds(Set.of(21L, 22L))).thenReturn(List.of(emission));

        var stages = reader.findByBatchIds(List.of(10L)).get(10L);

        assertThat(stages).extracting("chainId").containsExactly(21L, 22L);
        assertThat(stages.get(0).originAddress().city()).isEqualTo("Amparo");
        assertThat(stages.get(0).emission().co2Kg()).isEqualByComparingTo("12.5");
        assertThat(stages.get(0).transport()).isNull();
        assertThat(stages.get(1).transport().transportMode()).isEqualTo(TransportMode.ROAD);
        assertThat(stages.get(1).emission()).isNull();
        verify(chains).findByBatchIds(List.of(10L));
        verify(transports).findByChainIds(Set.of(21L, 22L));
        verify(emissions).findByChainIds(Set.of(21L, 22L));
    }

    @Test
    void shouldSkipRelatedQueriesWhenNoStagesExist() {
        ChainRepository chains = mock(ChainRepository.class);
        TransportRepository transports = mock(TransportRepository.class);
        CarbonEmissionRepository emissions = mock(CarbonEmissionRepository.class);
        BatchStagesReader reader = new BatchStagesReader(chains, transports, emissions);
        when(chains.findByBatchIds(List.of(10L))).thenReturn(List.of());

        assertThat(reader.findByBatchIds(List.of(10L))).isEmpty();

        verifyNoInteractions(transports, emissions);
    }
}
