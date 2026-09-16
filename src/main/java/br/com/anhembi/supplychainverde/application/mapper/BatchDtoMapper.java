package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BatchDtoMapper {
    Batch toDomain(BatchRequestDTO request);
    BatchResponseDTO toResponse(Batch batch);
    default BatchResponseDTO toDto(Batch batch) { return toResponse(batch); }
}
