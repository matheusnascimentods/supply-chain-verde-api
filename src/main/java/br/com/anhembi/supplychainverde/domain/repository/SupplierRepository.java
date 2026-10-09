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

    long countBySearch(String search);

    record SupplierEmissionRank(Long supplierId, BigDecimal co2KgPerUnit) {};
}
