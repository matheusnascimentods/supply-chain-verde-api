package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransportDtoMapper {
    @Mapping(target = "transportId", ignore = true)
    @Mapping(target = "chain", ignore = true)
    Transport toDomain(TransportRequestDTO request);

    @Mapping(target = "chainId", source = "chain.chainId")
    TransportResponseDTO toResponse(Transport transport);
}
