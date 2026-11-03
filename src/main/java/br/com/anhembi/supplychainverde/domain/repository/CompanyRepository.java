package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;

import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;

import java.math.BigDecimal;
import java.util.Collection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CompanyRepository {
    Company save(Company company);

    Optional<Company> findById(Long companyId);

    /** Só companhias SUPPLIER: o id de um mercado não serve onde a regra pede fornecedor (RN-02). */
    Optional<Company> findSupplierById(Long supplierId);

    List<Company> findAllSuppliers();

    /** SELECT ... FOR UPDATE: serializa as regras que contam usuários ou admins da companhia (RN-04, RN-08). */
    void lockById(Long companyId);

    /** Abre a assinatura ativa da companhia (RN-03). */
    void subscribe(Long companyId, Long planId);

    List<Company> findBySearch(String search);

    boolean existsByCnpj(Cnpj cnpj);

    List<Company> findAllById(Collection<Long> supplierIds);

    List<SupplierEmissionRank> findRankedByEmission(ProductCategory category, ProductUnit unit, Long productId, String search,
            boolean workedWithOnly, Long marketId, int limit, int offset);

    /** workedWithOnly restringe aos fornecedores que têm ou tiveram lote com marketId (RN-13). */
    long countBySearch(String search, boolean workedWithOnly, Long marketId);
    List<Long> findIdsRankedByScore(String search, boolean workedWithOnly, Long marketId, int limit, int offset);
    Set<Long> findWorkedWithIds(Long marketId, Collection<Long> supplierIds);
    List<SupplierScore> findScoresByIds(Collection<Long> supplierIds);
    record SupplierScore(Long supplierId, BigDecimal sustainabilityScore, Long activeCertificationCount, BigDecimal totalCo2Kg) {}

    record SupplierEmissionRank(Long supplierId, BigDecimal co2KgPerUnit) {};

}
