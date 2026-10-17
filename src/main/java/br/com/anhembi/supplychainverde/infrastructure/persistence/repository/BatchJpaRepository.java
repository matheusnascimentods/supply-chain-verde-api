package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.BatchJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BatchJpaRepository extends JpaRepository<BatchJpaEntity, Long> {
    List<BatchJpaEntity> findBySupplierSupplierId(Long supplierId);
    List<BatchJpaEntity> findByProductProductId(Long productId);

    @Override
    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    Page<BatchJpaEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    Page<BatchJpaEntity> findBySupplierSupplierId(Long supplierId, Pageable pageable);

    long countBySupplierSupplierId(Long supplierId);

    @Query(value = """
            select count(*)
            from vw_batch_current_stage
            where stage_type <> 'RETAIL'
            """, nativeQuery = true)
    long countActiveBatches();

    @Query(value = """
            select b.id as "batchId",
                   p.name as "productName",
                   s.name as "supplierName",
                   b.quantity as "quantity",
                   cast(p.unit as text) as "unit",
                   cast(latest.stage_type as text) as "status"
            from batch b
            join product p on p.id = b.product_id
            join supplier s on s.id = b.supplier_id
            join vw_batch_current_stage latest on latest.batch_id = b.id
            order by b.produced_at desc, b.id desc
            """, nativeQuery = true)
    List<DashboardBatchProjection> findRecentWithLatestStage(Pageable pageable);
}
