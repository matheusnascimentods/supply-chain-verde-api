package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.InviteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Native para comparar com o NOW(6) do banco, o mesmo relógio que gera expires_at.
public interface InviteJpaRepository extends JpaRepository<InviteJpaEntity, Long> {
    @Query(value = """
            SELECT * FROM invite
            WHERE token_hash = :tokenHash AND accepted = FALSE AND expires_at > NOW(6)
            """, nativeQuery = true)
    Optional<InviteJpaEntity> findActiveByTokenHash(@Param("tokenHash") String tokenHash);

    @Query(value = """
            SELECT COUNT(*) FROM invite i JOIN users u ON u.id = i.invited_by
            WHERE u.company_id = :companyId AND i.accepted = FALSE AND i.expires_at > NOW(6)
            """, nativeQuery = true)
    long countActiveByCompanyId(@Param("companyId") Long companyId);

    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM invite
                WHERE invited_for = :email AND accepted = FALSE AND expires_at > NOW(6)
            )
            """, nativeQuery = true)
    long existsActiveByEmail(@Param("email") String email);
}
