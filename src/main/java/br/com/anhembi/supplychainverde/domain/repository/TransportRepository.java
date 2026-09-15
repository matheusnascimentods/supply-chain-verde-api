package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Transport;

import java.util.List;
import java.util.Optional;

public interface TransportRepository {
    Transport save(Transport transport);

    Optional<Transport> findById(Long transportId);

    Optional<Transport> findByChainId(Long chainId);

    List<Transport> findAll();

    void deleteById(Long transportId);
}
