package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.product.ListProductsUseCase;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** RN-01 a RN-03 e RN-06 sobre o seed (o FULLTEXT só vê linhas commitadas, então nada aqui é transacional). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FullTextSearchIT {
    @Autowired
    private ListProductsUseCase listProducts;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Test
    void productSearchIgnoresAccents() {
        assertThat(productIds("graos")).isNotEmpty().isEqualTo(productIds("grãos"));
        assertThat(productIds("organico")).isNotEmpty().isEqualTo(productIds("orgânico"));
    }

    @Test
    void productSearchWithoutLettersOrDigitsListsEverything() {
        assertThat(productIds("--")).hasSize((int) productRepository.countAll(null));
    }

    @Test
    void supplierSearchMatchesFromTheFirstLetterAndStopwords() {
        assertThat(companyRepository.findBySearch("c")).isNotEmpty();
        assertThat(companyRepository.findBySearch("com")).extracting(Company::getName)
                .contains("Composto Vivo Reciclagem Orgânica");
        assertThat(companyRepository.findBySearch("organica")).extracting(Company::getName)
                .contains("Composto Vivo Reciclagem Orgânica");
    }

    private List<Long> productIds(String search) {
        return listProducts.execute(1000, 0, search, null).items().stream().map(ProductResponseDTO::productId).toList();
    }
}
