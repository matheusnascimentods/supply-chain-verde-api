package br.com.anhembi.supplychainverde.domain.repository;

import br.com.anhembi.supplychainverde.domain.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product);

    Optional<Product> findById(Long productId);

    List<Product> findAll();

    void deleteById(Long productId);
}
