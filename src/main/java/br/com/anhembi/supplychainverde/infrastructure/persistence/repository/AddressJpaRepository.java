package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.AddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressJpaRepository extends JpaRepository<AddressJpaEntity, Long> {
}
