package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import java.util.List;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {
    Optional<UserJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    long countByCompanyId(Long companyId);
    long countByCompanyIdAndRole(Long companyId, UserRole role);

    @Query(value = "SELECT id FROM users WHERE MATCH(email) AGAINST (:booleanSearch IN BOOLEAN MODE)", nativeQuery = true)
    List<Long> findIdsByEmailSearch(@Param("booleanSearch") String booleanSearch);

    /** Ids dos usuários cujo email casa com a busca; null quando não há filtro (vazio ou sem letras nem dígitos, RN-06). */
    default List<Long> findIdsByEmailFilter(String email) {
        String booleanSearch = email == null ? "" : FullTextSearch.booleanQuery(email);
        return booleanSearch.isEmpty() ? null : findIdsByEmailSearch(booleanSearch);
    }
}
