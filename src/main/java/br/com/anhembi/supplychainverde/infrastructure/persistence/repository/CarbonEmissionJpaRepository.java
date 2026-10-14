package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.CarbonEmissionJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface CarbonEmissionJpaRepository extends JpaRepository<CarbonEmissionJpaEntity, Long> {
    @Query(value = "SELECT SUM(co2_kg) FROM vw_stage_emission WHERE calculated_at BETWEEN :startsAt AND :endsAt", nativeQuery = true)
    BigDecimal sumCo2KgByCalculatedAtBetween(@Param("startsAt") LocalDate startsAt, @Param("endsAt") LocalDate endsAt);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    Optional<CarbonEmissionJpaEntity> findByChainChainId(Long chainId);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product", "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findByChainChainIdIn(Collection<Long> chainIds);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findByChainBatchBatchId(Long batchId);

    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findByChainBatchSupplierSupplierId(Long supplierId);

    @Override
    @EntityGraph(attributePaths = {
            "chain", "chain.batch", "chain.batch.product",
            "chain.batch.supplier", "chain.batch.supplier.address",
            "chain.originAddress", "chain.destinationAddress", "chain.responsibleUser"
    })
    List<CarbonEmissionJpaEntity> findAll();

    @Query(value = """
        SELECT to_char(month, 'YYYY-MM') AS "month", SUM(co2_kg) AS "co2Kg"
        FROM vw_monthly_emission
        WHERE month BETWEEN :from AND :to
          AND (CAST(:supplierId AS BIGINT) IS NULL OR supplier_id = :supplierId)
        GROUP BY month
        ORDER BY month
        """, nativeQuery = true)
    List<MonthlyEmissionProjection> sumMonthly(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("supplierId") Long supplierId);

    @Query(value = """
        SELECT CAST(stage_type AS TEXT) AS "stageType", SUM(co2_kg) AS "co2Kg"
        FROM vw_stage_emission
        WHERE supplier_id = :supplierId AND calculated_at BETWEEN :start AND :end
        GROUP BY stage_type
        ORDER BY 2 DESC
        """, nativeQuery = true)
    List<StageEmissionProjection> sumByStage(
            @Param("supplierId") Long supplierId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
        SELECT batch_id AS "batchId", product_name AS "productName", quantity AS "quantity",
               CAST(unit AS TEXT) AS "unit", SUM(co2_kg) AS "co2Kg"
        FROM vw_stage_emission
        WHERE supplier_id = :supplierId AND calculated_at BETWEEN :start AND :end
        GROUP BY batch_id, product_name, quantity, unit
        ORDER BY batch_id
        """, nativeQuery = true)
    List<BatchEmissionProjection> sumByBatch(
            @Param("supplierId") Long supplierId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query(value = """
        SELECT CAST(calculation_method AS TEXT) AS "calculationMethod", emission_factor AS "emissionFactor",
               SUM(co2_kg) AS "co2Kg"
        FROM vw_stage_emission
        WHERE supplier_id = :supplierId AND calculated_at BETWEEN :start AND :end
        GROUP BY calculation_method, emission_factor
        ORDER BY 1, 2
        """, nativeQuery = true)
    List<MethodEmissionProjection> sumByMethod(
            @Param("supplierId") Long supplierId, @Param("start") LocalDate start, @Param("end") LocalDate end);

    interface StageEmissionProjection {
        String getStageType();

        BigDecimal getCo2Kg();
    }

    interface BatchEmissionProjection {
        Long getBatchId();

        String getProductName();

        BigDecimal getQuantity();

        String getUnit();

        BigDecimal getCo2Kg();
    }

    interface MethodEmissionProjection {
        String getCalculationMethod();

        BigDecimal getEmissionFactor();

        BigDecimal getCo2Kg();
    }
}
