package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Address;

import java.util.List;
import java.util.Optional;

public interface AddressRepository {
    Address save(Address address);

    Optional<Address> findById(Long addressId);

    List<Address> findAll();

    void deleteById(Long addressId);
}
