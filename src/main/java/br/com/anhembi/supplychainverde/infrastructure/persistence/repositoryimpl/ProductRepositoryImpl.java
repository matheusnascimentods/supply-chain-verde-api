package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.ProductJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository jpaRepository;
    private final ProductJpaMapper mapper;

    @Override
    public Product save(Product product) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpaEntity(product)));
    }

    @Override
    public Optional<Product> findById(Long productId) {
        return jpaRepository.findById(productId).map(mapper::toDomain);
    }

    @Override
    public List<Product> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long productId) {
        jpaRepository.deleteById(productId);
    }
}
