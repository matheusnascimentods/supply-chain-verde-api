package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);

    Optional<User> findById(Long userId);

    Optional<User> findByEmail(String email);

    List<User> findAll();

    boolean existsByEmail(String email);

    void deleteById(Long userId);
}
