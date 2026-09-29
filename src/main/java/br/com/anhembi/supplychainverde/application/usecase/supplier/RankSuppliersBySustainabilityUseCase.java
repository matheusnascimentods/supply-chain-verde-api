package br.com.anhembi.supplychainverde.application.usecase.supplier;

import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingDTO;
import br.com.anhembi.supplychainverde.application.dto.supplier.SupplierRankingPageDTO;
import br.com.anhembi.supplychainverde.domain.entity.CarbonEmission;
import br.com.anhembi.supplychainverde.domain.entity.Certification;
import br.com.anhembi.supplychainverde.domain.entity.Supplier;
import br.com.anhembi.supplychainverde.domain.repository.CarbonEmissionRepository;
import br.com.anhembi.supplychainverde.domain.repository.CertificationRepository;
import br.com.anhembi.supplychainverde.domain.repository.SupplierRepository;
import br.com.anhembi.supplychainverde.domain.service.SustainabilityScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RankSuppliersBySustainabilityUseCase {
    private final SupplierRepository supplierRepository;
    private final CertificationRepository certificationRepository;
    private final CarbonEmissionRepository carbonEmissionRepository;
    private final SustainabilityScoreCalculator calculator = new SustainabilityScoreCalculator();

    public List<SupplierRankingDTO> execute() {
        return rank(supplierRepository.findAll());
    }

    public SupplierRankingPageDTO execute(int limit, int offset, String search) {
        List<Supplier> suppliers = search == null || search.isBlank()
                ? supplierRepository.findAll()
                : supplierRepository.findBySearch(search.trim());
        List<SupplierRankingDTO> rankings = rank(suppliers);
        int fromIndex = Math.min(offset, rankings.size());
        int toIndex = (int) Math.min((long) fromIndex + limit, rankings.size());

        return new SupplierRankingPageDTO(
                rankings.subList(fromIndex, toIndex),
                limit,
                offset,
                toIndex < rankings.size()
        );
    }

    private List<SupplierRankingDTO> rank(List<Supplier> suppliers) {
        List<SupplierRankingDTO> rankings = new ArrayList<>();
        for (Supplier supplier : suppliers) {
            List<Certification> certifications = certificationRepository.findBySupplierId(supplier.getSupplierId());
            List<CarbonEmission> emissions = carbonEmissionRepository.findBySupplierId(supplier.getSupplierId());
            BigDecimal score = calculator.calculate(certifications, emissions);
            rankings.add(new SupplierRankingDTO(
                    supplier.getSupplierId(),
                    supplier.getName(),
                    score,
                    calculator.countActiveCertifications(certifications),
                    emissions.stream().map(CarbonEmission::getCo2Kg).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add)
            ));
        }
        rankings.sort(Comparator.comparing(SupplierRankingDTO::sustainabilityScore).reversed()
                .thenComparing(SupplierRankingDTO::supplierId));
        return rankings;
    }

    public List<SupplierRankingDTO> rank() {
        return execute();
    }
}
