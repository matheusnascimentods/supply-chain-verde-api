package br.com.anhembi.supplychainverde.application.mapper;

/** Implementações reais geradas pelo MapStruct, para testes que montam use cases com {@code new}. */
public final class DtoMappers {
    public static final AddressDtoMapper ADDRESS = new AddressDtoMapperImpl();
    public static final AuditLogDtoMapper AUDIT_LOG = new AuditLogDtoMapperImpl();
    public static final BatchDtoMapper BATCH = new BatchDtoMapperImpl();
    public static final CarbonEmissionDtoMapper CARBON_EMISSION = new CarbonEmissionDtoMapperImpl();
    public static final CertificationDtoMapper CERTIFICATION = new CertificationDtoMapperImpl();
    public static final ProductDtoMapper PRODUCT = new ProductDtoMapperImpl();
    public static final ReportDtoMapper REPORT = new ReportDtoMapperImpl();
    public static final TransportDtoMapper TRANSPORT = new TransportDtoMapperImpl();
    public static final UserDtoMapper USER = new UserDtoMapperImpl();
    public static final SupplierDtoMapper SUPPLIER = new SupplierDtoMapperImpl(ADDRESS);
    public static final ChainDtoMapper CHAIN = new ChainDtoMapperImpl(ADDRESS, TRANSPORT, CARBON_EMISSION);

    private DtoMappers() {}
}
