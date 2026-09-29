package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long productId);

    List<Product> findAll();

    List<Product> findAll(int limit, int offset);

    List<Product> findBySearch(String search, String categoryCode, int limit, int offset);

    void deleteById(Long productId);
}
