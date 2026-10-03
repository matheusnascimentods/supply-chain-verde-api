package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.BatchJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.time.LocalDate;

public interface BatchJpaRepository extends JpaRepository<BatchJpaEntity, Long> {
    List<BatchJpaEntity> findBySupplierSupplierId(Long supplierId);
    List<BatchJpaEntity> findByProductProductId(Long productId);

    @Override
    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    Page<BatchJpaEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"product", "supplier", "supplier.address"})
    Page<BatchJpaEntity> findBySupplierSupplierId(Long supplierId, Pageable pageable);

    long countBySupplierSupplierId(Long supplierId);

    long countBySupplierSupplierIdAndProducedAtBetween(Long supplierId, LocalDate from, LocalDate to);

    @Query(value = """
            select count(*)
            from batch b
            cross join lateral (
                select c.stage_type
                from chain c
                where c.batch_id = b.batch_id
                order by c.started_at desc, c.chain_id desc
                limit 1
            ) latest
            where latest.stage_type <> 'RETAIL'
            """, nativeQuery = true)
    long countActiveBatches();

    @Query(value = """
            select b.batch_id as "batchId",
                   p.name as "productName",
                   s.name as "supplierName",
                   b.quantity as "quantity",
                   cast(p.unit as text) as "unit",
                   cast(latest.stage_type as text) as "status"
            from batch b
            join product p on p.product_id = b.product_id
            join supplier s on s.supplier_id = b.supplier_id
            cross join lateral (
                select c.stage_type
                from chain c
                where c.batch_id = b.batch_id
                order by c.started_at desc, c.chain_id desc
                limit 1
            ) latest
            order by b.produced_at desc, b.batch_id desc
            """, nativeQuery = true)
    List<DashboardBatchProjection> findRecentWithLatestStage(Pageable pageable);
}
