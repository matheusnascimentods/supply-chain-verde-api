package br.com.anhembi.supplychainverde.infrastructure.persistence.repository;

import br.com.anhembi.supplychainverde.infrastructure.persistence.jpa.BatchJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BatchJpaRepository extends JpaRepository<BatchJpaEntity, Long> {
    List<BatchJpaEntity> findBySupplierSupplierId(Long supplierId);
    List<BatchJpaEntity> findByProductProductId(Long productId);
}
