package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository.SupplierEmissionRank;
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
    void ordersRecommendedPageByCo2PerUnitWithSuppliersWithoutHistoryLast() {
        when(suppliers.findRankedByEmission(ProductCategory.AGRICULTURE, ProductUnit.KG, "", false, 40L, 20, 0))
                .thenReturn(List.of(
                        new SupplierEmissionRank(3L, new BigDecimal("0.4000")),
                        new SupplierEmissionRank(8L, new BigDecimal("0.6000")),
                        new SupplierEmissionRank(5L, null)));
        when(suppliers.findAllById(any())).thenReturn(List.of(supplier(5L), supplier(8L), supplier(3L)));
        when(suppliers.countBySearch("", false, 40L)).thenReturn(3L);
        stubScores();
        when(reports.countBySupplierIds(any(), any())).thenReturn(Map.of());

        var response = useCase.executeRecommended(ProductCategory.AGRICULTURE, ProductUnit.KG, 20, 0, null, false, 40L);

        assertThat(response.items()).extracting(SupplierRankingDTO::supplierId).containsExactly(3L, 8L, 5L);
        assertThat(response.items()).extracting(SupplierRankingDTO::co2KgPerUnit)
                .containsExactly(new BigDecimal("0.4000"), new BigDecimal("0.6000"), null);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void trimsSearchBeforeQueryingAndCounting() {
        when(suppliers.findRankedByEmission(ProductCategory.OTHER, ProductUnit.KG, "verde", false, null, 10, 0)).thenReturn(List.of());
        when(suppliers.findAllById(any())).thenReturn(List.of());
        when(reports.countBySupplierIds(any(), any())).thenReturn(Map.of());

        useCase.executeRecommended(ProductCategory.OTHER, ProductUnit.KG, 10, 0, "  verde ", false, null);

        verify(suppliers).findRankedByEmission(ProductCategory.OTHER, ProductUnit.KG, "verde", false, null, 10, 0);
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
