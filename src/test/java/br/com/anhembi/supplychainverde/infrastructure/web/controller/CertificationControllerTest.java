package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.certification.CertificationRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.certification.ListCertificationsUseCase;
import br.com.anhembi.supplychainverde.application.usecase.certification.RegisterCertificationUseCase;
import br.com.anhembi.supplychainverde.application.usecase.certification.UpdateCertificationStatusUseCase;
import br.com.anhembi.supplychainverde.infrastructure.security.CustomUserPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class CertificationControllerTest {
    private final RegisterCertificationUseCase registerCertification = mock(RegisterCertificationUseCase.class);
    private final CertificationController controller = new CertificationController(
            registerCertification, mock(UpdateCertificationStatusUseCase.class), mock(ListCertificationsUseCase.class));
    private final CertificationRequestDTO request = new CertificationRequestDTO(null, "ISO 14001", "Órgão", null, null);
    private final CustomUserPrincipal supplier = new CustomUserPrincipal(3L, "s@example.com", "SUPPLIER", 7L);

    @Test
    void shouldDenyCertificationForAnotherSupplier() {
        assertThatThrownBy(() -> controller.create(99L, request, supplier))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(registerCertification);
    }

    @Test
    void shouldRegisterCertificationForLinkedSupplier() {
        controller.create(7L, request, supplier);

        verify(registerCertification).execute(7L, request);
    }

    @Test
    void shouldRegisterCertificationForAnySupplierAsAdmin() {
        CustomUserPrincipal admin = new CustomUserPrincipal(1L, "a@example.com", "ADMIN", null);

        controller.create(99L, request, admin);

        verify(registerCertification).execute(99L, request);
    }
}
