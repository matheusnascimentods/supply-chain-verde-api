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
    public List<User> findPage(String email, Long companyId, int limit, int offset) {
        List<Long> emailUserIds = jpaRepository.findIdsByEmailFilter(email);
        if (emailUserIds != null && emailUserIds.isEmpty()) return List.of();
        TypedQuery<UserJpaEntity> query = entityManager
                .createQuery("SELECT user FROM UserJpaEntity user" + where(emailUserIds, companyId) + " ORDER BY user.userId ASC", UserJpaEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit);
        bindFilters(query, emailUserIds, companyId);
        return query.getResultList().stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countPage(String email, Long companyId) {
        List<Long> emailUserIds = jpaRepository.findIdsByEmailFilter(email);
        if (emailUserIds != null && emailUserIds.isEmpty()) return 0;
        var query = entityManager.createQuery("SELECT COUNT(user) FROM UserJpaEntity user" + where(emailUserIds, companyId), Long.class);
        bindFilters(query, emailUserIds, companyId);
        return query.getSingleResult();
    }

    @Override
    public long countActiveByCompanyId(Long companyId) {
        return jpaRepository.countByCompanyIdAndActiveTrue(companyId);
    }

    @Override
    public long countActiveByCompanyIdAndRole(Long companyId, UserRole role) {
        return jpaRepository.countByCompanyIdAndRoleAndActiveTrue(companyId, role);
    }

    @Override
    public boolean isActive(Long userId) {
        return jpaRepository.findActiveById(userId).orElse(false);
    }

    private String where(List<Long> emailUserIds, Long companyId) {
        List<String> conditions = new ArrayList<>();
        if (emailUserIds != null) conditions.add("user.userId IN :emailUserIds");
        if (companyId != null) conditions.add("user.companyId = :companyId");
        return conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);
    }

    private void bindFilters(Query query, List<Long> emailUserIds, Long companyId) {
        if (emailUserIds != null) query.setParameter("emailUserIds", emailUserIds);
        if (companyId != null) query.setParameter("companyId", companyId);
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
