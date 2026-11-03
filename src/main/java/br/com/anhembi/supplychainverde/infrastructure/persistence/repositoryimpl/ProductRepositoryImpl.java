package br.com.anhembi.supplychainverde.infrastructure.persistence.repositoryimpl;

import br.com.anhembi.supplychainverde.domain.entity.Product;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.infrastructure.persistence.mapper.ProductJpaMapper;
import br.com.anhembi.supplychainverde.infrastructure.persistence.repository.FullTextSearch;
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
    public List<Product> findAll(Long supplierId, int limit, int offset) {
        return jpaRepository.findPage(supplierId, limit, offset).stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll(Long supplierId) {
        return jpaRepository.countAllProducts(supplierId);
    }

    @Override
    public List<Product> findBySearch(Long supplierId, String search, String categoryCode, int limit, int offset) {
        return jpaRepository.findBySearch(supplierId, FullTextSearch.booleanQuery(search), categoryCode, limit, offset).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public long countBySearch(Long supplierId, String search, String categoryCode) {
        return jpaRepository.countBySearch(supplierId, FullTextSearch.booleanQuery(search), categoryCode);
    }

    @Override
    public void deleteById(Long productId) {
        jpaRepository.deleteById(productId);
    }
}
