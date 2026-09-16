package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
