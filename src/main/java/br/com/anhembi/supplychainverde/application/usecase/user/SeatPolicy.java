package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.exception.ConflictException;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.repository.InviteRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * RN-02 (016): usuários e convites ativos ocupam assento, e a soma nunca passa de company.maxUsers. Quem chama
 * trava a companhia antes (companyRepository.lockById), para duas checagens simultâneas não furarem o teto.
 */
@Component
@RequiredArgsConstructor
public class SeatPolicy {
    private final UserRepository userRepository;
    private final InviteRepository inviteRepository;

    public long seatsUsed(Long companyId) {
        return userRepository.countByCompanyId(companyId) + inviteRepository.countActiveByCompanyId(companyId);
    }

    public void requireFreeSeat(Company company) {
        long used = seatsUsed(company.getCompanyId());
        if (used >= company.getMaxUsers()) throw full(used, company.getMaxUsers());
    }

    public static ConflictException full(long used, int maxUsers) {
        return new ConflictException("Sem assentos livres: " + used + " de " + maxUsers + " em uso.");
    }
}
