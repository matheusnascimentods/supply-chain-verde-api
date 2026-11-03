package br.com.anhembi.supplychainverde.application.usecase.plan;

import br.com.anhembi.supplychainverde.application.dto.plan.PlanResponseDTO;
import br.com.anhembi.supplychainverde.domain.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListPlansUseCase {
    private final PlanRepository planRepository;

    public List<PlanResponseDTO> execute() {
        return planRepository.findAll().stream().map(PlanResponseDTO::of).toList();
    }
}
