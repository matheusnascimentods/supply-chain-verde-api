package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.batch.BatchTraceabilityResponseDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Batch;
import br.com.anhembi.supplychainverde.domain.enums.StageType;
import java.math.BigDecimal;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BatchDtoMapper {
    @Mapping(target = "productId", source = "batch.product.productId")
    @Mapping(target = "productName", source = "batch.product.name")
    @Mapping(target = "supplierId", source = "batch.supplier.supplierId")
    @Mapping(target = "supplierName", source = "batch.supplier.name")
    BatchResponseDTO toResponse(Batch batch, StageType currentStage, List<ChainResponseDTO> stages);

    @Mapping(target = "productName", source = "batch.product.name")
    @Mapping(target = "supplierName", source = "batch.supplier.name")
    BatchTraceabilityResponseDTO toTraceability(Batch batch, List<ChainResponseDTO> stages, BigDecimal totalCo2Kg);
}
