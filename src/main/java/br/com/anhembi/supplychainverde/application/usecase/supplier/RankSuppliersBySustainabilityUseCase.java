package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.exception.ResourceNotFoundException;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.ProductRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository.SupplierScore;
import br.com.anhembi.supplychainverde.domain.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
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
    private final ReportRepository reportRepository;
    private final ProductRepository productRepository;
    private final SupplierDtoMapper mapper;
    private final CertificationDtoMapper certificationMapper;

    public List<SupplierRankingDTO> execute() {
        return executeAll(false);
    }

    public List<SupplierRankingDTO> executeAll(boolean withoutUser) {
        List<Supplier> suppliers = supplierRepository.findAll();
        if (withoutUser) {
            suppliers = suppliers.stream().filter(supplier -> supplier.getUserId() == null).toList();
        }
        List<SupplierRankingDTO> rankings = toRankingDTOs(suppliers, Map.of());
        rankings.sort(SCORE_ORDER);
        return rankings;
    }

    public OffsetPageResponseDTO<SupplierRankingDTO> execute(int limit, int offset, String search, boolean withoutUser) {
        String term = search == null ? "" : search.trim();
        // Ordem e paginação vêm da vw_supplier_sustainability; findAllById perde a ordem, por isso o sort da página.
        List<Long> ids = supplierRepository.findIdsRankedByScore(term, withoutUser, limit, offset);
        List<SupplierRankingDTO> items = toRankingDTOs(supplierRepository.findAllById(ids), Map.of());
        items.sort(SCORE_ORDER);
        return OffsetPageResponseDTO.of(items, limit, offset, supplierRepository.countBySearch(term, withoutUser));
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
        return OffsetPageResponseDTO.of(items, limit, offset, supplierRepository.countBySearch(term, false));
    }

    private List<SupplierRankingDTO> toRankingDTOs(List<Supplier> suppliers, Map<Long, BigDecimal> co2) {
        Set<Long> ids = suppliers.stream().map(Supplier::getSupplierId).collect(Collectors.toSet());
        Map<Long, Long> reportCounts = reportRepository.countBySupplierIds(ids);
        Map<Long, SupplierScore> scores = supplierRepository.findScoresByIds(ids).stream()
                .collect(Collectors.toMap(SupplierScore::supplierId, Function.identity()));
        Map<Long, List<Certification>> certifications = certificationRepository.findBySupplierIds(ids).stream()
                .collect(Collectors.groupingBy(certification -> certification.getSupplier().getSupplierId()));

        List<SupplierRankingDTO> rankings = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            SupplierScore score = scores.get(supplier.getSupplierId());
            rankings.add(mapper.toRanking(
                    supplier,
                    score.sustainabilityScore(),
                    Math.toIntExact(score.activeCertificationCount()),
                    score.totalCo2Kg(),
                    reportCounts.getOrDefault(supplier.getSupplierId(), 0L),
                    certifications.getOrDefault(supplier.getSupplierId(), List.of()).stream()
                            .map(certificationMapper::toSupplierCertification).toList(),
                    co2.get(supplier.getSupplierId())
            ));
        }
        return rankings;
    }
}
