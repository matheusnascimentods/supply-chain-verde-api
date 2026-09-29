package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.UserJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.UserJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {
    private final UserJpaRepository jpaRepository;
    private final UserJpaMapper mapper;
    private final EntityManager entityManager;

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
    @Transactional(readOnly = true)
    public List<User> findPage(String email, int limit, int offset) {
        StringBuilder jpql = new StringBuilder("SELECT user FROM UserJpaEntity user");
        if (email != null && !email.isBlank()) {
            jpql.append(" WHERE LOWER(user.email) LIKE :emailPattern");
        }
        jpql.append(" ORDER BY user.userId ASC");

        TypedQuery<UserJpaEntity> query =
                entityManager.createQuery(jpql.toString(), UserJpaEntity.class)
                        .setFirstResult(offset)
                        .setMaxResults(limit);
        if (email != null && !email.isBlank()) {
            query.setParameter("emailPattern", "%" + email.trim().toLowerCase(Locale.ROOT) + "%");
        }
        return query.getResultList().stream().map(mapper::toDomain).toList();
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
