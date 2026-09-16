package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.SupplierJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.SupplierJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
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
    public boolean existsByCnpj(Cnpj cnpj) {
        return jpaRepository.existsByCnpj(cnpj.value());
    }

    @Override
    public void deleteById(Long supplierId) {
        jpaRepository.deleteById(supplierId);
    }
}
