package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.valueobject.Cnpj;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository {
    Supplier save(Supplier supplier);

    Optional<Supplier> findById(Long supplierId);

    Optional<Supplier> findByCnpj(Cnpj cnpj);

    List<Supplier> findAll();

    boolean existsByCnpj(Cnpj cnpj);

    void deleteById(Long supplierId);
}
