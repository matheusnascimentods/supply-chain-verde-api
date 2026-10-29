package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.mapper.DtoMappers;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository.SupplierEmissionRank;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository.SupplierScore;
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
    private final SupplierRepository suppliers = mock(SupplierRepository.class);
    private final CertificationRepository certifications = mock(CertificationRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final RankSuppliersBySustainabilityUseCase useCase =
            new RankSuppliersBySustainabilityUseCase(suppliers, certifications, reports, products,
                    DtoMappers.SUPPLIER, DtoMappers.CERTIFICATION);

    @Test
    void ordersRecommendedPageByCo2PerUnitWithSuppliersWithoutHistoryLast() {
        when(suppliers.findRankedByEmission(ProductCategory.AGRICULTURE, ProductUnit.KG, null, "", 20, 0))
                .thenReturn(List.of(
                        new SupplierEmissionRank(3L, new BigDecimal("0.4000")),
                        new SupplierEmissionRank(8L, new BigDecimal("0.6000")),
                        new SupplierEmissionRank(5L, null)));
        when(suppliers.findAllById(any())).thenReturn(List.of(supplier(5L), supplier(8L), supplier(3L)));
        when(suppliers.countBySearch("", false)).thenReturn(3L);
        stubScores();
        when(reports.countBySupplierIds(any())).thenReturn(Map.of());

        var response = useCase.executeRecommended(null, ProductCategory.AGRICULTURE, ProductUnit.KG, 20, 0, null);

        assertThat(response.items()).extracting(SupplierRankingDTO::supplierId).containsExactly(3L, 8L, 5L);
        assertThat(response.items()).extracting(SupplierRankingDTO::co2KgPerUnit)
                .containsExactly(new BigDecimal("0.4000"), new BigDecimal("0.6000"), null);
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void trimsSearchBeforeQueryingAndCounting() {
        when(products.findById(12L)).thenReturn(Optional.of(Product.builder().productId(12L).build()));
        when(suppliers.findRankedByEmission(null, null, 12L, "verde", 10, 0)).thenReturn(List.of());
        when(suppliers.findAllById(any())).thenReturn(List.of());
        when(reports.countBySupplierIds(any())).thenReturn(Map.of());

        useCase.executeRecommended(12L, null, null, 10, 0, "  verde ");

        verify(suppliers).findRankedByEmission(null, null, 12L, "verde", 10, 0);
        verify(suppliers).countBySearch("verde", false);
    }

    @Test
    void rejectsUnknownProduct() {
        when(products.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executeRecommended(99L, null, null, 20, 0, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(suppliers);
    }

    @Test
    void listsOnlySuppliersWithoutLinkedUserWhenRequested() {
        Supplier linked = supplier(3L);
        linked.setUserId(10L);
        when(suppliers.findAll()).thenReturn(List.of(linked, supplier(5L)));
        stubScores();
        when(reports.countBySupplierIds(any())).thenReturn(Map.of());

        when(suppliers.findIdsRankedByScore("", true, 20, 0)).thenReturn(List.of(5L));
        when(suppliers.findAllById(List.of(5L))).thenReturn(List.of(supplier(5L)));

        assertThat(useCase.executeAll(true)).extracting(SupplierRankingDTO::supplierId).containsExactly(5L);
        assertThat(useCase.execute(20, 0, null, true).items()).extracting(SupplierRankingDTO::supplierId).containsExactly(5L);
        assertThat(useCase.executeAll(false)).hasSize(2);
    }

    // A view tem uma linha por fornecedor; o mock devolve um score para cada id pedido.
    @SuppressWarnings("unchecked")
    private void stubScores() {
        when(suppliers.findScoresByIds(any())).thenAnswer(invocation -> ((Collection<Long>) invocation.getArgument(0)).stream()
                .map(id -> new SupplierScore(id, new BigDecimal("100.00"), 0L, BigDecimal.ZERO))
                .toList());
    }

    private static Supplier supplier(Long supplierId) {
        return Supplier.builder().supplierId(supplierId).name("Fornecedor " + supplierId).build();
    }
}
