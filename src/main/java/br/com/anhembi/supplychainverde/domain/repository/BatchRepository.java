package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Batch;

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

    long countBySupplierId(Long supplierId);

    List<Batch> findByProductId(Long productId);

    void deleteById(Long batchId);
}
