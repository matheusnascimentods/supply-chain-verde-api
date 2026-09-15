package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CertificationRepository {
    Certification save(Certification certification);

    Optional<Certification> findById(Long certificationId);

    List<Certification> findAll();

    List<Certification> findBySupplierId(Long supplierId);

    List<Certification> findBySupplierIdAndStatus(Long supplierId, CertificationStatus status);

    List<Certification> findByExpiresAtBetween(LocalDate startsAt, LocalDate endsAt);

    void deleteById(Long certificationId);
}
