package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.UserJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final UserJpaMapper mapper;

    @Override
    public User save(User user) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(user)));
    }

    @Override
    public Optional<User> findById(Long userId) {
        return jpaRepository.findById(userId).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public void deleteById(Long userId) {
        jpaRepository.deleteById(userId);
    }
}
