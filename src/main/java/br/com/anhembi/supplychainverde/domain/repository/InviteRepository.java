package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Invite;

import java.util.Optional;

/** "Ativo" é sempre accepted = FALSE AND expires_at > NOW(6), calculado pelo relógio do banco (RN-06). */
public interface InviteRepository {
    Invite save(Invite invite);

    Optional<Invite> findActiveByTokenHash(String tokenHash);

    /** Convites ativos enviados por usuários da companhia (RN-05). */
    long countActiveByCompanyId(Long companyId);

    boolean existsActiveByEmail(String email);
}
