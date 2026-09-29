package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.BatchJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.BatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BatchRepositoryImpl implements BatchRepository {
    private final BatchJpaRepository jpaRepository;
    private final BatchJpaMapper mapper;

    @Override
    public Batch save(Batch batch) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(batch)));
    }

    @Override
    public Optional<Batch> findById(Long batchId) {
        return jpaRepository.findById(batchId).map(mapper::toDomain);
    }

    @Override
    public List<Batch> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Batch> findAll(int page, int size) {
        return jpaRepository.findAll(pageRequest(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return jpaRepository.count();
    }

    @Override
    public List<Batch> findBySupplierId(Long supplierId) {
        return jpaRepository.findBySupplierSupplierId(supplierId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Batch> findBySupplierId(Long supplierId, int page, int size) {
        return jpaRepository.findBySupplierSupplierId(supplierId, pageRequest(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countBySupplierId(Long supplierId) {
        return jpaRepository.countBySupplierSupplierId(supplierId);
    }

    @Override
    public long countBySupplierIdAndProducedAtBetween(Long supplierId, LocalDate from, LocalDate to) {
        return jpaRepository.countBySupplierSupplierIdAndProducedAtBetween(supplierId, from, to);
    }

    @Override
    public List<Batch> findByProductId(Long productId) {
        return jpaRepository.findByProductProductId(productId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long batchId) {
        jpaRepository.deleteById(batchId);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("producedAt"),
                Sort.Order.desc("batchId")
        ));
    }
}
