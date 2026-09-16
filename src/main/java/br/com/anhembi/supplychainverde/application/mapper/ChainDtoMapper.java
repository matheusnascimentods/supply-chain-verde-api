package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.chain.ChainResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Chain;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChainDtoMapper {
    Chain toDomain(ChainRequestDTO request);
    ChainResponseDTO toResponse(Chain chain);
    default ChainResponseDTO toDto(Chain chain) { return toResponse(chain); }
}
