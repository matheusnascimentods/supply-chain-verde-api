package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.repository.AddressRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.AddressJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.AddressJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AddressRepositoryImpl implements AddressRepository {
    private final AddressJpaRepository jpaRepository;
    private final AddressJpaMapper mapper;

    @Override
    public Address save(Address address) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(address)));
    }

    @Override
    public Optional<Address> findById(Long addressId) {
        return jpaRepository.findById(addressId).map(mapper::toDomain);
    }

    @Override
    public List<Address> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long addressId) {
        jpaRepository.deleteById(addressId);
    }
}
