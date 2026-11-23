package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long productId);

    List<Product> findAll();

    /** supplierId nulo não filtra: para o mercado, produtos são catálogo (RN-12). */
    List<Product> findAll(Long supplierId, int limit, int offset);

    long countAll(Long supplierId);

    List<Product> findBySearch(Long supplierId, String search, String categoryCode, int limit, int offset);

    long countBySearch(Long supplierId, String search, String categoryCode);

    void deleteById(Long productId);

    /**
     * Catálogo do mercado (RN-01/RN-02). marketId nulo conta os lotes de todos os mercados (PLATFORM_ADMIN, RN-03);
     * supplierId, category e unit nulos não filtram.
     */
    List<CatalogItem> findCatalog(Long supplierId, Long marketId, ProductCategory category, ProductUnit unit,
            String search, String categoryCode, int limit, int offset);

    long countCatalog(Long supplierId, ProductCategory category, ProductUnit unit, String search, String categoryCode);

    interface CatalogItem {
        Long getProductId();
        Long getSupplierId();
        String getName();
        ProductCategory getCategory();
        ProductUnit getUnit();
        String getDescription();
        String getSupplierName();
        BigDecimal getCo2KgPerUnit();
        Long getAcceptedBatchesLast30Days();
    }
}
