package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    List<User> findPage(String email, Long companyId, int limit, int offset);

    long countPage(String email, Long companyId);

    /** Usuários ativos: ocupam assento (RN-02 da 016). */
    long countActiveByCompanyId(Long companyId);

    /** RN-07 (016): a companhia sempre tem um ADMIN ativo. */
    long countActiveByCompanyIdAndRole(Long companyId, UserRole role);

    /** RN-11 (016): usuário inexistente conta como inativo. */
    boolean isActive(Long userId);

    boolean existsByEmail(String email);

    void deleteById(Long userId);
}
