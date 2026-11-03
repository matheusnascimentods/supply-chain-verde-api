package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;

import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;

import java.math.BigDecimal;
import java.util.Collection;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);

    Optional<Company> findById(Long companyId);

    /** Só companhias SUPPLIER: o id de um mercado não serve onde a regra pede fornecedor (RN-02). */
    Optional<Company> findSupplierById(Long supplierId);

    List<Company> findAllSuppliers();

    /** Abre a assinatura ativa da companhia (RN-03). */
    void subscribe(Long companyId, Long planId);

    List<Company> findBySearch(String search);

    boolean existsByCnpj(Cnpj cnpj);

    List<Company> findAllById(Collection<Long> supplierIds);

    List<SupplierEmissionRank> findRankedByEmission(ProductCategory category, ProductUnit unit, Long productId, String search, int limit, int offset);

    long countBySearch(String search, boolean withoutUser);
    List<Long> findIdsRankedByScore(String search, boolean withoutUser, int limit, int offset);
    List<SupplierScore> findScoresByIds(Collection<Long> supplierIds);
    record SupplierScore(Long supplierId, BigDecimal sustainabilityScore, Long activeCertificationCount, BigDecimal totalCo2Kg) {}

    record SupplierEmissionRank(Long supplierId, BigDecimal co2KgPerUnit) {};

    Optional<Company> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Company> findByUserIdIn(Collection<Long> userIds);
}
