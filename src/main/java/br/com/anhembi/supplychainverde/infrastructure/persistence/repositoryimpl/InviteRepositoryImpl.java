package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Invite;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.InviteJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.InviteJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InviteRepositoryImpl implements InviteRepository {
    private final InviteJpaRepository jpaRepository;
    private final InviteJpaMapper mapper;

    @Override
    public Invite save(Invite invite) {
        return mapper.toDomain(jpaRepository.saveAndFlush(mapper.toJpaEntity(invite)));
    }

    @Override
    public Optional<Invite> findActiveByTokenHash(String tokenHash) {
        return jpaRepository.findActiveByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public long countActiveByCompanyId(Long companyId) {
        return jpaRepository.countActiveByCompanyId(companyId);
    }

    @Override
    public boolean existsActiveByEmail(String email) {
        return jpaRepository.existsActiveByEmail(email) > 0;
    }
}
