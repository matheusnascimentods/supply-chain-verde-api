package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository.SupplierEmissionRank;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
    private final CarbonEmissionRepository emissions = mock(CarbonEmissionRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final RankSuppliersBySustainabilityUseCase useCase =
            new RankSuppliersBySustainabilityUseCase(suppliers, certifications, emissions, reports, products);

    @Test
    void ordersRecommendedPageByCo2PerUnitWithSuppliersWithoutHistoryLast() {
        when(suppliers.findRankedByEmission(ProductCategory.AGRICULTURE, ProductUnit.KG, null, "", 20, 0))
                .thenReturn(List.of(
                        new SupplierEmissionRank(3L, new BigDecimal("0.4000")),
                        new SupplierEmissionRank(8L, new BigDecimal("0.6000")),
                        new SupplierEmissionRank(5L, null)));
        when(suppliers.findAllById(any())).thenReturn(List.of(supplier(5L), supplier(8L), supplier(3L)));
        when(suppliers.countBySearch("")).thenReturn(3L);
        when(certifications.findBySupplierId(anyLong())).thenReturn(List.of());
        when(emissions.findBySupplierId(anyLong())).thenReturn(List.of());
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
        verify(suppliers).countBySearch("verde");
    }

    @Test
    void rejectsUnknownProduct() {
        when(products.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executeRecommended(99L, null, null, 20, 0, null))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(suppliers);
    }

    private static Supplier supplier(Long supplierId) {
        return Supplier.builder().supplierId(supplierId).name("Fornecedor " + supplierId).build();
    }
}
