package br.com.anhembi.supplychainverde.infrastructure.persistence;

import br.com.anhembi.supplychainverde.TestcontainersConfiguration;
import br.com.anhembi.supplychainverde.application.dto.product.ProductResponseDTO;
import br.com.anhembi.supplychainverde.application.usecase.product.ListProductsUseCase;
import br.com.anhembi.supplychainverde.domain.entity.AuditLog;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.entity.User;
import br.com.anhembi.supplychainverde.domain.repository.AuditLogRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** RN-01 a RN-06 sobre o seed (o FULLTEXT só vê linhas commitadas, então nada aqui é transacional). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FullTextSearchIT {
    private static final String KANE = "h.kane@bayern.com";

    @Autowired
    private ListProductsUseCase listProducts;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void productSearchIgnoresAccents() {
        assertThat(productIds("graos")).isNotEmpty().isEqualTo(productIds("grãos"));
        assertThat(productIds("organico")).isNotEmpty().isEqualTo(productIds("orgânico"));
        assertThat(productIds("cafe de montanha")).containsExactly(13L); // "de" não está no índice
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

    @Test
    void supplierSearchMatchesCnpjPrefixWithOrWithoutMask() {
        assertThat(companyRepository.findBySearch("12.345")).extracting(Company::getName)
                .containsExactly("Cooperativa Verde de Hortifruti");
        assertThat(companyRepository.findBySearch("12345")).extracting(Company::getName)
                .containsExactly("Cooperativa Verde de Hortifruti");
        assertThat(companyRepository.findBySearch("78000195")).isEmpty(); // meio do CNPJ não casa
    }

    @Test
    void supplierSearchWithoutLettersOrDigitsListsEverything() {
        assertThat(companyRepository.findBySearch("--")).hasSameSizeAs(companyRepository.findAllSuppliers());
        assertThat(companyRepository.countBySearch("--", false, null)).isEqualTo(companyRepository.countBySearch("", false, null));
    }

    @Test
    void userSearchMatchesEmailWordsIgnoringCase() {
        assertThat(userEmails("KANE")).containsExactly(KANE);
        assertThat(userEmails("bayern kane")).containsExactly(KANE);
        assertThat(userEmails(KANE)).containsExactly(KANE);
        assertThat(userRepository.countPage("KANE", null)).isEqualTo(1);
        assertThat(userRepository.findPage("zpkninguem", null, 100, 0)).isEmpty();
        assertThat(userRepository.countPage("zpkninguem", null)).isZero();
        assertThat(userRepository.countPage("--", null)).isEqualTo(userRepository.countPage(null, null));
    }

    @Test
    void auditSearchMatchesEmailWordsIgnoringCase() {
        Long kaneId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, KANE);
        // O trigger grava o ator da sessão (@app_user_id); o endereço some, a auditoria fica.
        transactionTemplate.executeWithoutResult(status -> {
            jdbcTemplate.update("SET @app_user_id = ?", kaneId.toString());
            jdbcTemplate.update("INSERT INTO address (street, number, neighborhood, zip_code, city, state)"
                    + " VALUES ('Rua Busca', '1', 'Centro', '01000000', 'São Paulo', 'SP')");
            jdbcTemplate.update("DELETE FROM address WHERE street = 'Rua Busca'");
            jdbcTemplate.update("SET @app_user_id = NULL");
        });
        LocalDate today = LocalDate.now();

        for (String search : List.of("KANE", KANE)) {
            List<AuditLog> logs = auditLogRepository.findByFilters(today, today, null, search, null, 100, 0);
            assertThat(logs).isNotEmpty().allSatisfy(log -> assertThat(log.getUser().getEmail()).isEqualTo(KANE));
            assertThat(auditLogRepository.countByFilters(today, today, null, search, null)).isEqualTo(logs.size());
        }
        assertThat(auditLogRepository.countByFilters(today, today, null, "zpkninguem", null)).isZero();
    }

    private List<String> userEmails(String search) {
        return userRepository.findPage(search, null, 100, 0).stream().map(User::getEmail).toList();
    }

    private List<Long> productIds(String search) {
        return listProducts.execute(1000, 0, search, null).items().stream().map(ProductResponseDTO::productId).toList();
    }
}
