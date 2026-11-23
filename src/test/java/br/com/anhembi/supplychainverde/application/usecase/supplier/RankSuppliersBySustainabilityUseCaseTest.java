package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository.SupplierScore;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RankSuppliersBySustainabilityUseCaseTest {
    private final CompanyRepository suppliers = mock(CompanyRepository.class);
    private final CertificationRepository certifications = mock(CertificationRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final RankSuppliersBySustainabilityUseCase useCase =
            new RankSuppliersBySustainabilityUseCase(suppliers, certifications, reports,
                    DtoMappers.SUPPLIER, DtoMappers.CERTIFICATION);

    @Test
    void trimsSearchBeforeQueryingAndCounting() {
        when(suppliers.findIdsRankedByScore("verde", false, null, 10, 0)).thenReturn(List.of());
        when(suppliers.findAllById(any())).thenReturn(List.of());
        when(reports.countBySupplierIds(any(), any())).thenReturn(Map.of());

        useCase.execute(10, 0, "  verde ", false, null);

        verify(suppliers).findIdsRankedByScore("verde", false, null, 10, 0);
        verify(suppliers).countBySearch("verde", false, null);
    }

    @Test
    void marksAndFiltersSuppliersTheMarketWorkedWith() {
        when(suppliers.findAllSuppliers()).thenReturn(List.of(supplier(3L), supplier(5L)));
        stubScores();
        when(reports.countBySupplierIds(any(), any())).thenReturn(Map.of());
        when(suppliers.findWorkedWithIds(any(), any())).thenReturn(java.util.Set.of(5L));

        when(suppliers.findIdsRankedByScore("", true, 40L, 20, 0)).thenReturn(List.of(5L));
        when(suppliers.findAllById(List.of(5L))).thenReturn(List.of(supplier(5L)));

        assertThat(useCase.executeAll(true, 40L)).extracting(SupplierRankingDTO::supplierId).containsExactly(5L);
        assertThat(useCase.execute(20, 0, null, true, 40L).items()).extracting(SupplierRankingDTO::workedWith).containsExactly(true);
        assertThat(useCase.executeAll(false, 40L)).extracting(SupplierRankingDTO::workedWith).containsExactlyInAnyOrder(true, false);
    }

    // A view tem uma linha por fornecedor; o mock devolve um score para cada id pedido.
    @SuppressWarnings("unchecked")
    private void stubScores() {
        when(suppliers.findScoresByIds(any())).thenAnswer(invocation -> ((Collection<Long>) invocation.getArgument(0)).stream()
                .map(id -> new SupplierScore(id, new BigDecimal("100.00"), 0L, BigDecimal.ZERO))
                .toList());
    }

    private static Company supplier(Long supplierId) {
        return Company.builder().companyId(supplierId).name("Fornecedor " + supplierId).build();
    }
}
