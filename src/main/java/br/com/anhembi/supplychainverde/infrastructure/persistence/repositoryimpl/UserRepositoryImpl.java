package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.enums.UserRole;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.UserJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.UserJpaEntity;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    public List<User> findPage(String email, UserRole role, int limit, int offset) {
        TypedQuery<UserJpaEntity> query = entityManager
                .createQuery("SELECT user FROM UserJpaEntity user" + where(email, role) + " ORDER BY user.userId ASC", UserJpaEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit);
        bindFilters(query, email, role);
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String email, UserRole role) {
        var query = entityManager.createQuery("SELECT COUNT(user) FROM UserJpaEntity user" + where(email, role), Long.class);
        bindFilters(query, email, role);
        return query.getSingleResult();
    }

    private String where(String email, UserRole role) {
        List<String> conditions = new ArrayList<>();
        if (email != null && !email.isBlank()) conditions.add("LOWER(user.email) LIKE :emailPattern");
        if (role != null) conditions.add("user.role = :role");
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private void bindFilters(Query query, String email, UserRole role) {
        if (email != null && !email.isBlank()) {
            query.setParameter("emailPattern", "%" + email.trim().toLowerCase(Locale.ROOT) + "%");
        }
        if (role != null) query.setParameter("role", role);
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
