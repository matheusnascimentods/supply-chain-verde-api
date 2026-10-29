package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;

import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;

import java.math.BigDecimal;
import java.util.Collection;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
    Supplier save(Supplier supplier);

    Optional<Supplier> findById(Long supplierId);

    Optional<Supplier> findByCnpj(Cnpj cnpj);

    List<Supplier> findAll();

    List<Supplier> findBySearch(String search);

    boolean existsByCnpj(Cnpj cnpj);

    void deleteById(Long supplierId);

    List<Supplier> findAllById(Collection<Long> supplierIds);

    List<SupplierEmissionRank> findRankedByEmission(ProductCategory category, ProductUnit unit, Long productId, String search, int limit, int offset);

    long countBySearch(String search, boolean withoutUser);
    List<Long> findIdsRankedByScore(String search, boolean withoutUser, int limit, int offset);
    List<SupplierScore> findScoresByIds(Collection<Long> supplierIds);
    record SupplierScore(Long supplierId, BigDecimal sustainabilityScore, Long activeCertificationCount, BigDecimal totalCo2Kg) {}

    record SupplierEmissionRank(Long supplierId, BigDecimal co2KgPerUnit) {};

    Optional<Supplier> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Supplier> findByUserIdIn(Collection<Long> userIds);
}
