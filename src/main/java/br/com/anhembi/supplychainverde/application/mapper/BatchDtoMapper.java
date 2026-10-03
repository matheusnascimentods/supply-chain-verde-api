package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BatchDtoMapper {
    Batch toDomain(BatchRequestDTO request);

    @Mapping(target = "productId", source = "product.productId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "supplierId", source = "supplier.supplierId")
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "currentStage", ignore = true)
    @Mapping(target = "stages", ignore = true)
    BatchResponseDTO toResponse(Batch batch);
    default BatchResponseDTO toDto(Batch batch) { return toResponse(batch); }
}
