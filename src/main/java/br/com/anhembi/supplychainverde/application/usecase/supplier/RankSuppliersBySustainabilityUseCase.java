package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierCertificationDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import br.com.anhembi.supplychainverde.domain.service.SustainabilityScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankSuppliersBySustainabilityUseCase {
    private static final Comparator<SupplierRankingDTO> SCORE_ORDER =
            Comparator.comparing(SupplierRankingDTO::sustainabilityScore).reversed()
                    .thenComparing(SupplierRankingDTO::supplierId);
    private static final Comparator<SupplierRankingDTO> RECOMMENDED_ORDER =
            Comparator.comparing(SupplierRankingDTO::co2KgPerUnit, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(SupplierRankingDTO::sustainabilityScore, Comparator.reverseOrder())
                    .thenComparing(SupplierRankingDTO::supplierId);

    private final SupplierRepository supplierRepository;
    private final CertificationRepository certificationRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final SustainabilityScoreCalculator calculator = new SustainabilityScoreCalculator();

    public List<SupplierRankingDTO> execute() {
        List<SupplierRankingDTO> rankings = toRankingDTOs(supplierRepository.findAll(), Map.of());
        rankings.sort(SCORE_ORDER);
        return rankings;
    }

    public List<SupplierRankingDTO> executeAll() {
        return execute();
    }

    public OffsetPageResponseDTO<SupplierRankingDTO> execute(int limit, int offset, String search) {
        List<Supplier> suppliers = search == null || search.isBlank()
                ? supplierRepository.findAll()
                : supplierRepository.findBySearch(search.trim());
        List<SupplierRankingDTO> rankings = toRankingDTOs(suppliers, Map.of());
        rankings.sort(SCORE_ORDER);
        int fromIndex = Math.min(offset, rankings.size());
        int toIndex = (int) Math.min((long) fromIndex + limit, rankings.size());

        return OffsetPageResponseDTO.of(
                rankings.subList(fromIndex, toIndex), limit, offset, rankings.size());
    }

    public OffsetPageResponseDTO<SupplierRankingDTO> executeRecommended(Long productId, ProductCategory category,
            ProductUnit unit, int limit, int offset, String search) {
        if (productId != null && productRepository.findById(productId).isEmpty()) {
            throw new ResourceNotFoundException("Produto não encontrado: " + productId);
        }
        String term = search == null ? "" : search.trim();
        // HashMap: fornecedores sem histórico têm co2 null, que Collectors.toMap não aceita
        Map<Long, BigDecimal> co2 = new HashMap<>();
        supplierRepository.findRankedByEmission(category, unit, productId, term, limit, offset)
                .forEach(rank -> co2.put(rank.supplierId(), rank.co2KgPerUnit()));
        List<SupplierRankingDTO> items = toRankingDTOs(supplierRepository.findAllById(co2.keySet()), co2);
        items.sort(RECOMMENDED_ORDER);
        return OffsetPageResponseDTO.of(items, limit, offset, supplierRepository.countBySearch(term));
    }

    private List<SupplierRankingDTO> toRankingDTOs(List<Supplier> suppliers, Map<Long, BigDecimal> co2) {
        List<SupplierRankingDTO> rankings = new ArrayList<>();
        Map<Long, Long> reportCounts = reportRepository.countBySupplierIds(suppliers.stream()
                .map(Supplier::getSupplierId)
                .collect(Collectors.toSet()));
        for (Supplier supplier : suppliers) {
            List<Certification> certifications = certificationRepository.findBySupplierId(supplier.getSupplierId());
            List<CarbonEmission> emissions = carbonEmissionRepository.findBySupplierId(supplier.getSupplierId());
            BigDecimal score = calculator.calculate(certifications, emissions);
            rankings.add(new SupplierRankingDTO(
                    supplier.getSupplierId(),
                    supplier.getName(),
                    supplier.getCnpj() == null ? null : supplier.getCnpj().value(),
                    toAddressResponse(supplier.getAddress()),
                    supplier.getPhone(),
                    supplier.getRegisteredAt(),
                    score,
                    calculator.countActiveCertifications(certifications),
                    emissions.stream().map(CarbonEmission::getCo2Kg).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add),
                    reportCounts.getOrDefault(supplier.getSupplierId(), 0L),
                    certifications.stream().map(certification -> new SupplierCertificationDTO(
                            certification.getCertificationId(),
                            certification.getCertification(),
                            certification.getIssuingBody(),
                            certification.getIssuedAt(),
                            certification.getExpiresAt(),
                            certification.getStatus()
                    )).toList(),
                    co2.get(supplier.getSupplierId())
            ));
        }
        return rankings;
    }

    private AddressResponseDTO toAddressResponse(Address address) {
        if (address == null) return null;
        return new AddressResponseDTO(
                address.getAddressId(),
                address.getStreet(),
                address.getNumber(),
                address.getNeighborhood(),
                address.getComplement(),
                address.getZipCode(),
                address.getCity(),
                address.getState()
        );
    }

    public List<SupplierRankingDTO> rank() {
        return execute();
    }
}
