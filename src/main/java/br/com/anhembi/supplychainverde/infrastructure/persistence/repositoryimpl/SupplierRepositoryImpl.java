package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.SupplierJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.SupplierJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SupplierRepositoryImpl implements SupplierRepository {
    private final SupplierJpaRepository jpaRepository;
    private final SupplierJpaMapper mapper;

    @Override
    public Supplier save(Supplier supplier) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(supplier)));
    }

    @Override
    public Optional<Supplier> findById(Long supplierId) {
        return jpaRepository.findById(supplierId).map(mapper::toDomain);
    }

    @Override
    public Optional<Supplier> findByCnpj(Cnpj cnpj) {
        return jpaRepository.findByCnpj(cnpj.value()).map(mapper::toDomain);
    }

    @Override
    public List<Supplier> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> findBySearch(String search) {
        String cnpjSearch = search.replaceAll("\\D", "");
        return jpaRepository.findBySearch(search, cnpjSearch).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpaRepository.existsByCnpj(cnpj.value());
    }

    @Override
    public void deleteById(Long supplierId) {
        jpaRepository.deleteById(supplierId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Supplier> findAllById(Collection<Long> supplierIds) {
        return jpaRepository.findAllById(supplierIds).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<SupplierEmissionRank> findRankedByEmission(ProductCategory category, ProductUnit unit, Long productId, String search, int limit, int offset) {
        return jpaRepository.findRankedByEmission(
                productId,
                category == null ? null : category.name(),
                unit == null ? null : unit.name(),
                search,
                search.replaceAll("\\D", ""),
                limit,
                offset)
            .stream()
            .map(row -> new SupplierEmissionRank(row.getSupplierId(), row.getCo2KgPerUnit()))
            .toList();
    }

    @Override
    public long countBySearch(String search) {
        return jpaRepository.countBySearch(search, search.replaceAll("\\D", ""));
    }
}
