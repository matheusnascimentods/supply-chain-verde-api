package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.enums.CertificationStatus;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.CertificationJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.CertificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CertificationRepositoryImpl implements CertificationRepository {
    private final CertificationJpaRepository jpaRepository;
    private final CertificationJpaMapper mapper;

    @Override
    public Certification save(Certification certification) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(certification)));
    }

    @Override
    public Optional<Certification> findById(Long certificationId) {
        return jpaRepository.findById(certificationId).map(mapper::toDomain);
    }

    @Override
    public List<Certification> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Certification> findBySupplierId(Long supplierId) {
        return jpaRepository.findBySupplierSupplierId(supplierId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Certification> findBySupplierIdAndStatus(Long supplierId, CertificationStatus status) {
        return jpaRepository.findBySupplierSupplierIdAndStatus(supplierId, status).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Certification> findAll(int page, int size) {
        return jpaRepository.findAll(pageRequest(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Certification> findByStatus(CertificationStatus status, int page, int size) {
        return jpaRepository.findByStatus(status, pageRequest(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    @Override
    public long countByExpiresAtBetween(LocalDate startsAt, LocalDate endsAt) {
        return jpaRepository.countByExpiresAtBetween(startsAt, endsAt);
    }

    @Override
    public long countByStatus(CertificationStatus status) {
        return jpaRepository.countByStatus(status);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("issuedAt"),
                Sort.Order.desc("certificationId")
        ));
    }

    @Override
    public void deleteById(Long certificationId) {
        jpaRepository.deleteById(certificationId);
    }
}
