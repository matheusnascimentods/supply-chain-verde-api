package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.BatchJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BatchJpaRepository extends JpaRepository<BatchJpaEntity, Long> {
    List<BatchJpaEntity> findBySupplierCompanyId(Long supplierId);
    List<BatchJpaEntity> findByProductProductId(Long productId);

    @Override
    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    Page<BatchJpaEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    Page<BatchJpaEntity> findBySupplierCompanyId(Long supplierId, Pageable pageable);

    long countBySupplierCompanyId(Long supplierId);

    long countBySupplierCompanyIdAndStatus(Long supplierId, String status);

    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    @Query("""
            select b from BatchJpaEntity b
            where (:marketId is null or b.marketId = :marketId)
              and (:supplierId is null or b.supplier.companyId = :supplierId)
            """)
    Page<BatchJpaEntity> findPage(@Param("marketId") Long marketId, @Param("supplierId") Long supplierId, Pageable pageable);

    @Query(value = """
            select count(*)
            from vw_batch_current_stage latest
            join batch b on b.id = latest.batch_id
            where latest.stage_type <> 'RETAIL'
              and (:marketId is null or b.market_id = :marketId)
              and (:supplierId is null or b.supplier_id = :supplierId)
            """, nativeQuery = true)
    long countActiveBatches(@Param("marketId") Long marketId, @Param("supplierId") Long supplierId);

    @Query(value = """
            select b.id as batchId,
                   p.name as productName,
                   s.name as supplierName,
                   b.quantity as quantity,
                   p.unit as unit,
                   latest.stage_type as status
            from batch b
            join product p on p.id = b.product_id
            join company s on s.id = b.supplier_id
            join vw_batch_current_stage latest on latest.batch_id = b.id
            where (:marketId is null or b.market_id = :marketId)
              and (:supplierId is null or b.supplier_id = :supplierId)
            order by b.produced_at desc, b.id desc
            """, nativeQuery = true)
    List<DashboardBatchProjection> findRecentWithLatestStage(@Param("marketId") Long marketId,
            @Param("supplierId") Long supplierId, Pageable pageable);
}
