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

    long countByCompanyId(Long companyId);

    long countByCompanyIdAndRole(Long companyId, UserRole role);

    boolean existsByEmail(String email);

    void deleteById(Long userId);
}
