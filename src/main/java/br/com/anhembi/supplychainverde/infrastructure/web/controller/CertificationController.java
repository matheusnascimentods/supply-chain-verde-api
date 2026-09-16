package br.com.anhembi.supplychainverde.infrastructure.web.controller;

import br.com.anhembi.supplychainverde.application.dto.certification.*;
import br.com.anhembi.supplychainverde.application.usecase.certification.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CertificationController {
    private final RegisterCertificationUseCase register;
    private final UpdateCertificationStatusUseCase updateStatus;
    private final ListExpiringCertificationsUseCase expiring;

    @PostMapping("/suppliers/{supplierId}/certifications")
    @ResponseStatus(HttpStatus.CREATED)
    public CertificationResponseDTO create(@PathVariable Long supplierId, @RequestBody CertificationRequestDTO request) { return register.execute(supplierId, request); }

    @PatchMapping("/certifications/{certificationId}/status")
    public CertificationResponseDTO updateStatus(@PathVariable Long certificationId, @RequestBody UpdateCertificationStatusRequestDTO request) { return updateStatus.execute(certificationId, request); }

    @GetMapping("/certifications/expiring")
    public List<CertificationResponseDTO> expiring() { return expiring.execute(); }
}
