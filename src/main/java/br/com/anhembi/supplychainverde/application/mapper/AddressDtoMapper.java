package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressDtoMapper {
    Address toDomain(AddressRequestDTO request);
    AddressResponseDTO toResponse(Address address);
    default AddressResponseDTO toDto(Address address) { return toResponse(address); }
}
