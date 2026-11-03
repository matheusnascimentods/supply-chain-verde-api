package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import br.com.anhembi.supplychainverde.domain.repository.BatchRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.BatchJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.BatchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import java.util.List;
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
        return jpaRepository.findBySupplierCompanyId(supplierId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Batch> findBySupplierId(Long supplierId, int page, int size) {
        return jpaRepository.findBySupplierCompanyId(supplierId, pageRequest(page, size))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countBySupplierId(Long supplierId) {
        return jpaRepository.countBySupplierCompanyId(supplierId);
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

    @Override
    public long countActiveBatches(Long marketId, Long supplierId) {
        return jpaRepository.countActiveBatches(marketId, supplierId);
    }

    @Override
    public List<Batch> findPage(Long marketId, Long supplierId, BatchStatus status, int page, int size) {
        return jpaRepository.findPage(marketId, supplierId, status, pageRequest(page, size)).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countPage(Long marketId, Long supplierId, BatchStatus status) {
        return jpaRepository.findPage(marketId, supplierId, status, PageRequest.of(0, 1)).getTotalElements();
    }

    @Override
    public long countBySupplierIdAndStatus(Long supplierId, BatchStatus status) {
        return jpaRepository.countBySupplierCompanyIdAndStatus(supplierId, status);
    }

    @Override
    public List<RecentBatch> findRecentBatches(Long marketId, Long supplierId, int limit) {
        return jpaRepository.findRecentWithLatestStage(marketId, supplierId, PageRequest.of(0, limit)).stream()
                .map(batch -> new RecentBatch(
                        batch.getBatchId(),
                        batch.getProductName(),
                        batch.getSupplierName(),
                        batch.getQuantity(),
                        ProductUnit.valueOf(batch.getUnit()),
                        StageType.valueOf(batch.getStatus())
                ))
                .toList();
    }
}
