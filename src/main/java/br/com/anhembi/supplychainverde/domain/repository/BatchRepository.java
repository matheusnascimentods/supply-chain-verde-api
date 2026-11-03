package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.enums.BatchStatus;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.enums.StageType;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface BatchRepository {
    Batch save(Batch batch);

    Optional<Batch> findById(Long batchId);

    List<Batch> findAll();

    List<Batch> findAll(int page, int size);

    long countAll();

    List<Batch> findBySupplierId(Long supplierId);

    List<Batch> findBySupplierId(Long supplierId, int page, int size);

    /** marketId, supplierId e status nulos não filtram. */
    List<Batch> findPage(Long marketId, Long supplierId, BatchStatus status, int page, int size);

    long countPage(Long marketId, Long supplierId, BatchStatus status);

    long countBySupplierIdAndStatus(Long supplierId, BatchStatus status);

    long countBySupplierId(Long supplierId);


    List<Batch> findByProductId(Long productId);

    void deleteById(Long batchId);

    long countActiveBatches(Long marketId, Long supplierId);

    List<RecentBatch> findRecentBatches(Long marketId, Long supplierId, int limit);

    record RecentBatch(Long batchId, String productName, String supplierName, BigDecimal quantity, ProductUnit unit, StageType status) {}
}
