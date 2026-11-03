package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.pagination.OffsetPageResponseDTO;
import br.com.anhembi.supplychainverde.application.mapper.CertificationDtoMapper;
import br.com.anhembi.supplychainverde.application.mapper.SupplierDtoMapper;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.entity.Company;
import br.com.anhembi.supplychainverde.domain.enums.ProductCategory;
import br.com.anhembi.supplychainverde.domain.enums.ProductUnit;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository;
import br.com.anhembi.supplychainverde.domain.repository.CompanyRepository.SupplierScore;
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

    private final CompanyRepository supplierRepository;
    private final CertificationRepository certificationRepository;
    private final ReportRepository reportRepository;
    private final SupplierDtoMapper mapper;
    private final CertificationDtoMapper certificationMapper;

    // RN-13: o catálogo é global; marketId só calcula workedWith, conta os relatórios e, com workedWithOnly, filtra.
    // marketId nulo (PLATFORM_ADMIN) deixa workedWith sempre false (RF-06a).
    public List<SupplierRankingDTO> executeAll(boolean workedWithOnly, Long marketId) {
        List<Company> suppliers = supplierRepository.findAllSuppliers();
        List<SupplierRankingDTO> rankings = toRankingDTOs(suppliers, Map.of(), marketId);
        if (workedWithOnly) {
            rankings = rankings.stream().filter(SupplierRankingDTO::workedWith).collect(Collectors.toCollection(ArrayList::new));
        }
        rankings.sort(SCORE_ORDER);
        return rankings;
    }

    public OffsetPageResponseDTO<SupplierRankingDTO> execute(int limit, int offset, String search, boolean workedWithOnly,
            Long marketId) {
        String term = search == null ? "" : search.trim();
        // Ordem e paginação vêm da vw_supplier_sustainability; findAllById perde a ordem, por isso o sort da página.
        List<Long> ids = supplierRepository.findIdsRankedByScore(term, workedWithOnly, marketId, limit, offset);
        List<SupplierRankingDTO> items = toRankingDTOs(supplierRepository.findAllById(ids), Map.of(), marketId);
        items.sort(SCORE_ORDER);
        return OffsetPageResponseDTO.of(items, limit, offset, supplierRepository.countBySearch(term, workedWithOnly, marketId));
    }

    /** RN-21: a recomendação compara só por category + unit; o produto agora é de um fornecedor só. */
    public OffsetPageResponseDTO<SupplierRankingDTO> executeRecommended(ProductCategory category,
            ProductUnit unit, int limit, int offset, String search, boolean workedWithOnly, Long marketId) {
        String term = search == null ? "" : search.trim();
        // HashMap: fornecedores sem histórico têm co2 null, que Collectors.toMap não aceita
        Map<Long, BigDecimal> co2 = new HashMap<>();
        supplierRepository.findRankedByEmission(category, unit, term, workedWithOnly, marketId, limit, offset)
                .forEach(rank -> co2.put(rank.supplierId(), rank.co2KgPerUnit()));
        List<SupplierRankingDTO> items = toRankingDTOs(supplierRepository.findAllById(co2.keySet()), co2, marketId);
        items.sort(RECOMMENDED_ORDER);
        return OffsetPageResponseDTO.of(items, limit, offset, supplierRepository.countBySearch(term, workedWithOnly, marketId));
    }

    private List<SupplierRankingDTO> toRankingDTOs(List<Company> suppliers, Map<Long, BigDecimal> co2, Long marketId) {
        Set<Long> ids = suppliers.stream().map(Company::getCompanyId).collect(Collectors.toSet());
        Map<Long, Long> reportCounts = reportRepository.countBySupplierIds(ids, marketId);
        Set<Long> workedWith = supplierRepository.findWorkedWithIds(marketId, ids);
        Map<Long, SupplierScore> scores = supplierRepository.findScoresByIds(ids).stream()
                .collect(Collectors.toMap(SupplierScore::supplierId, Function.identity()));
        Map<Long, List<Certification>> certifications = certificationRepository.findBySupplierIds(ids).stream()
                .collect(Collectors.groupingBy(certification -> certification.getSupplier().getCompanyId()));

        List<SupplierRankingDTO> rankings = new ArrayList<>();
        for (Company supplier : suppliers) {
            SupplierScore score = scores.get(supplier.getCompanyId());
            rankings.add(mapper.toRanking(
                    supplier,
                    workedWith.contains(supplier.getCompanyId()),
                    score.sustainabilityScore(),
                    Math.toIntExact(score.activeCertificationCount()),
                    score.totalCo2Kg(),
                    reportCounts.getOrDefault(supplier.getCompanyId(), 0L),
                    certifications.getOrDefault(supplier.getCompanyId(), List.of()).stream()
                            .map(certificationMapper::toSupplierCertification).toList(),
                    co2.get(supplier.getCompanyId())
            ));
        }
        return rankings;
    }
}
