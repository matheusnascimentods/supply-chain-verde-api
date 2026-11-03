package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long productId);

    List<Product> findAll();

    /** supplierId nulo não filtra: para o mercado, produtos são catálogo (RN-12). */
    List<Product> findAll(Long supplierId, int limit, int offset);

    long countAll(Long supplierId);

    List<Product> findBySearch(Long supplierId, String search, String categoryCode, int limit, int offset);

    long countBySearch(Long supplierId, String search, String categoryCode);

    void deleteById(Long productId);
}
