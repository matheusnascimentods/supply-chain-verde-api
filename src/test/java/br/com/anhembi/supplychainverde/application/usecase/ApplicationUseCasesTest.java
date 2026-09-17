package br.com.anhembi.supplychainverde.application.usecase;

import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.auth.LoginRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.carbonemission.CarbonEmissionRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.CertificationRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.certification.UpdateCertificationStatusRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.product.ProductRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.report.ReportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UpdateUserRoleRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.user.UserRequestDTO;
import br.com.anhembi.supplychainverde.application.usecase.audit.ListAuditLogsUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.GetBatchTraceabilityUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.ListBatchesBySupplierUseCase;
import br.com.anhembi.supplychainverde.application.usecase.batch.RegisterBatchUseCase;
import br.com.anhembi.supplychainverde.application.usecase.certification.ListExpiringCertificationsUseCase;
import br.com.anhembi.supplychainverde.application.usecase.certification.RegisterCertificationUseCase;
import br.com.anhembi.supplychainverde.application.usecase.certification.UpdateCertificationStatusUseCase;
import br.com.anhembi.supplychainverde.application.usecase.chain.ListChainStagesByBatchUseCase;
import br.com.anhembi.supplychainverde.application.usecase.chain.RegisterChainStageUseCase;
import br.com.anhembi.supplychainverde.application.usecase.emission.CalculateCarbonEmissionUseCase;
import br.com.anhembi.supplychainverde.application.usecase.emission.GetBatchCarbonFootprintUseCase;
import br.com.anhembi.supplychainverde.application.usecase.product.ListProductsUseCase;
import br.com.anhembi.supplychainverde.application.usecase.product.RegisterProductUseCase;
import br.com.anhembi.supplychainverde.application.usecase.product.UpdateProductUseCase;
import br.com.anhembi.supplychainverde.application.usecase.report.GenerateSustainabilityReportUseCase;
import br.com.anhembi.supplychainverde.application.usecase.report.GetReportUseCase;
import br.com.anhembi.supplychainverde.application.usecase.report.ListReportsBySupplierUseCase;
import br.com.anhembi.supplychainverde.application.usecase.supplier.GetSupplierUseCase;
import br.com.anhembi.supplychainverde.application.usecase.supplier.ListSuppliersUseCase;
import br.com.anhembi.supplychainverde.application.usecase.supplier.RankSuppliersBySustainabilityUseCase;
import br.com.anhembi.supplychainverde.application.usecase.supplier.RegisterSupplierUseCase;
import br.com.anhembi.supplychainverde.application.usecase.supplier.UpdateSupplierUseCase;
import br.com.anhembi.supplychainverde.application.usecase.transport.RegisterTransportUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.AuthenticateUserUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.RegisterUserUseCase;
import br.com.anhembi.supplychainverde.application.usecase.user.UpdateUserRoleUseCase;
import br.com.anhembi.supplychainverde.domain.entity.*;
import br.com.anhembi.supplychainverde.domain.enums.*;
import br.com.anhembi.supplychainverde.domain.repository.*;
import br.com.anhembi.supplychainverde.domain.service.PasswordHasher;
import br.com.anhembi.supplychainverde.infrastructure.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApplicationUseCasesTest {

    @Test
    void shouldRegisterAndQuerySupplier() {
        SupplierRepository suppliers = mock(SupplierRepository.class);
        AddressRepository addresses = mock(AddressRepository.class);
        Address address = Address.builder().addressId(1L).city("São Paulo").state("SP").build();
        Supplier saved = Supplier.builder().supplierId(2L).name("Verde").cnpj(new br.com.anhembi.supplychainverde.domain.valueobject.Cnpj("11.222.333/0001-81")).address(address).build();
        when(addresses.save(any())).thenReturn(address);
        when(suppliers.save(any())).thenReturn(saved);
        when(suppliers.findById(2L)).thenReturn(Optional.of(saved));
        when(suppliers.findAll()).thenReturn(List.of(saved));

        SupplierRequestDTO request = new SupplierRequestDTO("Verde", "11.222.333/0001-81",
                new AddressRequestDTO("Rua A", "1", "Centro", null, "01000-000", "São Paulo", "SP"), "11999999999");

        assertThat(new RegisterSupplierUseCase(suppliers, addresses).execute(request).supplierId()).isEqualTo(2L);
        assertThat(new GetSupplierUseCase(suppliers).execute(2L).name()).isEqualTo("Verde");
        assertThat(new ListSuppliersUseCase(suppliers).execute()).hasSize(1);
        verify(suppliers).save(any(Supplier.class));
    }

    @Test
    void shouldRegisterAndUpdateProduct() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = Product.builder().productId(3L).name("Café").category(ProductCategory.AGRICULTURE).unit(ProductUnit.KG).build();
        when(products.save(any())).thenReturn(product);
        when(products.findById(3L)).thenReturn(Optional.of(product));
        when(products.findAll()).thenReturn(List.of(product));

        ProductRequestDTO request = new ProductRequestDTO("Café", ProductCategory.AGRICULTURE, ProductUnit.KG, "Orgânico");
        assertThat(new RegisterProductUseCase(products).execute(request).productId()).isEqualTo(3L);
        assertThat(new UpdateProductUseCase(products).execute(3L, request).name()).isEqualTo("Café");
        assertThat(new ListProductsUseCase(products).execute()).hasSize(1);
    }

    @Test
    void shouldRegisterBatchAndListTraceability() {
        BatchRepository batches = mock(BatchRepository.class);
        ProductRepository products = mock(ProductRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        ChainRepository chains = mock(ChainRepository.class);
        Product product = Product.builder().productId(1L).name("Café").build();
        Supplier supplier = Supplier.builder().supplierId(2L).name("Verde").build();
        Batch batch = Batch.builder().batchId(4L).product(product).supplier(supplier).quantity(new BigDecimal("10")).build();
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(suppliers.findById(2L)).thenReturn(Optional.of(supplier));
        when(batches.save(any())).thenReturn(batch);
        when(batches.findBySupplierId(2L)).thenReturn(List.of(batch));
        when(batches.findById(4L)).thenReturn(Optional.of(batch));
        when(chains.findByBatchId(4L)).thenReturn(List.of());

        assertThat(new RegisterBatchUseCase(batches, products, suppliers)
                .execute(new BatchRequestDTO(1L, 2L, new BigDecimal("10"), LocalDate.now())).batchId()).isEqualTo(4L);
        assertThat(new ListBatchesBySupplierUseCase(batches).execute(2L)).hasSize(1);
        assertThat(new GetBatchTraceabilityUseCase(batches, chains).execute(4L).batchId()).isEqualTo(4L);
    }

    @Test
    void shouldRegisterAndUpdateCertification() {
        CertificationRepository certifications = mock(CertificationRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        Supplier supplier = Supplier.builder().supplierId(2L).build();
        Certification certification = Certification.builder().certificationId(5L).supplier(supplier).status(CertificationStatus.ACTIVE).build();
        when(suppliers.findById(2L)).thenReturn(Optional.of(supplier));
        when(certifications.save(any())).thenReturn(certification);
        when(certifications.findById(5L)).thenReturn(Optional.of(certification));
        when(certifications.findByExpiresAtBetween(any(), any())).thenReturn(List.of(certification));

        CertificationRequestDTO request = new CertificationRequestDTO(2L, "ISO", "ABNT", LocalDate.now(), LocalDate.now().plusDays(10));
        assertThat(new RegisterCertificationUseCase(certifications, suppliers).execute(2L, request).status())
                .isEqualTo(CertificationStatus.ACTIVE);
        assertThat(new UpdateCertificationStatusUseCase(certifications)
                .execute(5L, new UpdateCertificationStatusRequestDTO(CertificationStatus.SUSPENDED)).status())
                .isEqualTo(CertificationStatus.SUSPENDED);
        assertThat(new ListExpiringCertificationsUseCase(certifications).execute()).hasSize(1);
    }

    @Test
    void shouldRegisterTransportAndCalculateEmission() {
        TransportRepository transports = mock(TransportRepository.class);
        ChainRepository chains = mock(ChainRepository.class);
        CarbonEmissionRepository emissions = mock(CarbonEmissionRepository.class);
        Chain chain = Chain.builder().chainId(6L).build();
        Transport transport = Transport.builder().transportId(7L).chain(chain).distance(new BigDecimal("100")).transportMode(TransportMode.ROAD).fuelType(FuelType.DIESEL).build();
        CarbonEmission emission = CarbonEmission.builder().emissionId(8L).chain(chain).emissionFactor(new br.com.anhembi.supplychainverde.domain.valueobject.EmissionFactor(new BigDecimal("0.15"))).co2Kg(new BigDecimal("15.00")).calculationMethod(CalculationMethod.IPCC).build();
        when(chains.findById(6L)).thenReturn(Optional.of(chain));
        when(transports.save(any())).thenReturn(transport);
        when(transports.findByChainId(6L)).thenReturn(Optional.of(transport));
        when(emissions.save(any())).thenReturn(emission);
        when(emissions.findByBatchId(9L)).thenReturn(List.of(emission));

        TransportRequestDTO transportRequest = new TransportRequestDTO(6L, TransportMode.ROAD, new BigDecimal("100"), FuelType.DIESEL, new BigDecimal("20"));
        assertThat(new RegisterTransportUseCase(transports, chains).execute(transportRequest).transportId()).isEqualTo(7L);
        assertThat(new CalculateCarbonEmissionUseCase(emissions, chains, transports)
                .execute(6L, new CarbonEmissionRequestDTO(6L, CalculationMethod.IPCC)).co2Kg())
                .isEqualByComparingTo(new BigDecimal("15.00"));
        assertThat(new GetBatchCarbonFootprintUseCase(emissions).execute(9L).totalCo2Kg())
                .isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void shouldRegisterAndListChainStagesAndRankSuppliers() {
        ChainRepository chains = mock(ChainRepository.class);
        BatchRepository batches = mock(BatchRepository.class);
        UserRepository users = mock(UserRepository.class);
        AddressRepository addresses = mock(AddressRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        CertificationRepository certifications = mock(CertificationRepository.class);
        CarbonEmissionRepository emissions = mock(CarbonEmissionRepository.class);

        Batch batch = Batch.builder().batchId(9L).build();
        User user = User.builder().userId(11L).name("Operator").build();
        Chain stage = Chain.builder().chainId(13L).batch(batch).responsibleUser(user).stageType(StageType.PRODUCTION).build();
        Supplier supplier = Supplier.builder().supplierId(2L).name("Verde").build();

        when(batches.findById(9L)).thenReturn(Optional.of(batch));
        when(users.findById(11L)).thenReturn(Optional.of(user));
        when(chains.save(any())).thenReturn(stage);
        when(chains.findByBatchId(9L)).thenReturn(List.of(stage));
        when(suppliers.findAll()).thenReturn(List.of(supplier));
        when(certifications.findBySupplierId(2L)).thenReturn(List.of());
        when(emissions.findBySupplierId(2L)).thenReturn(List.of());

        ChainRequestDTO request = new ChainRequestDTO(9L, null, null, StageType.PRODUCTION, LocalDateTime.now(), null);
        assertThat(new RegisterChainStageUseCase(chains, batches, users, addresses)
                .execute(9L, 11L, request).chainId()).isEqualTo(13L);
        assertThat(new ListChainStagesByBatchUseCase(chains).execute(9L)).hasSize(1);
        assertThat(new RankSuppliersBySustainabilityUseCase(suppliers, certifications, emissions)
                .execute()).singleElement().satisfies(ranking -> assertThat(ranking.supplierId()).isEqualTo(2L));
    }

    @Test
    void shouldGenerateAndQueryReports() {
        ReportRepository reports = mock(ReportRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        Supplier supplier = Supplier.builder().supplierId(2L).build();
        Report report = Report.builder().reportId(10L).supplier(supplier).periodStartAt(LocalDate.now().minusDays(30)).periodEndAt(LocalDate.now()).totalCo2Kg(BigDecimal.ZERO).trackedProductCount(1).generatedAt(LocalDateTime.now()).build();
        when(suppliers.findById(2L)).thenReturn(Optional.of(supplier));
        when(reports.save(any())).thenReturn(report);
        when(reports.findById(10L)).thenReturn(Optional.of(report));
        when(reports.findBySupplierId(2L)).thenReturn(List.of(report));

        ReportRequestDTO request = new ReportRequestDTO(2L, report.getPeriodStartAt(), report.getPeriodEndAt());
        assertThat(new GenerateSustainabilityReportUseCase(reports, suppliers).execute(2L, request).reportId()).isEqualTo(10L);
        assertThat(new GetReportUseCase(reports).execute(10L).reportId()).isEqualTo(10L);
        assertThat(new ListReportsBySupplierUseCase(reports).execute(2L)).hasSize(1);
    }

    @Test
    void shouldRegisterAuthenticateAndUpdateUser() {
        UserRepository users = mock(UserRepository.class);
        PasswordHasher hasher = mock(PasswordHasher.class);
        JwtTokenProvider jwt = mock(JwtTokenProvider.class);
        User user = User.builder().userId(11L).name("Admin").email("admin@example.com").password("hash").role(UserRole.ADMIN).build();
        when(hasher.hash("secret")).thenReturn("hash");
        when(hasher.matches("secret", "hash")).thenReturn(true);
        when(users.save(any())).thenReturn(user);
        when(users.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(users.findById(11L)).thenReturn(Optional.of(user));
        when(jwt.generateToken(user)).thenReturn("token");

        assertThat(new RegisterUserUseCase(users, hasher)
                .execute(new UserRequestDTO("Admin", "admin@example.com", "secret", UserRole.ADMIN)).userId()).isEqualTo(11L);
        assertThat(new AuthenticateUserUseCase(users, hasher, jwt)
                .execute(new LoginRequestDTO("admin@example.com", "secret")).token()).isEqualTo("token");
        assertThat(new UpdateUserRoleUseCase(users)
                .execute(11L, new UpdateUserRoleRequestDTO(UserRole.AUDITOR)).role()).isEqualTo(UserRole.AUDITOR);
    }

    @Test
    void shouldMapAuditLogs() {
        AuditLogRepository logs = mock(AuditLogRepository.class);
        User user = User.builder().userId(11L).build();
        AuditLog log = AuditLog.builder().logId(12L).user(user).action(AuditAction.INSERT).affectedTable("supplier").build();
        when(logs.findAll()).thenReturn(List.of(log));

        assertThat(new ListAuditLogsUseCase(logs).execute()).singleElement()
                .satisfies(dto -> {
                    assertThat(dto.logId()).isEqualTo(12L);
                    assertThat(dto.userId()).isEqualTo(11L);
                });
    }
}
