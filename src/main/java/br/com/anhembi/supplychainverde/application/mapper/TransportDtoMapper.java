package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.transport.TransportResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Transport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransportDtoMapper {
    Transport toDomain(TransportRequestDTO request);
    TransportResponseDTO toResponse(Transport transport);
    default TransportResponseDTO toDto(Transport transport) { return toResponse(transport); }
}
