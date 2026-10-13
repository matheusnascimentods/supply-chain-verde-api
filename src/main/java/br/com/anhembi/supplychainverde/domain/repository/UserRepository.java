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

    List<User> findPage(String email, UserRole role, int limit, int offset);

    long countPage(String email, UserRole role);

    boolean existsByEmail(String email);

    void deleteById(Long userId);
}
