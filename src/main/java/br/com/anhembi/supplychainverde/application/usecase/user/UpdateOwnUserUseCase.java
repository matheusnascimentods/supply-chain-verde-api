package br.com.anhembi.supplychainverde.application.usecase.user;

import br.com.anhembi.supplychainverde.application.dto.user.UpdateOwnUserRequestDTO;
import br.com.anhembi.supplychainverde.application.exception.ValidationException;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOwnUserUseCase {
    private final UserRepository userRepository;
    private final OwnAccountChanges ownAccountChanges;

    @Transactional
    public void execute(Long userId, UpdateOwnUserRequestDTO request) {
        if (request == null) throw new ValidationException("Requisição de atualização é obrigatória.");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + userId));

        ownAccountChanges.validate(user, request.userName(), request.currentPassword(), request.newPassword());
        if (ownAccountChanges.apply(user, request.userName(), request.newPassword())) {
            userRepository.save(user);
        }
    }
}
