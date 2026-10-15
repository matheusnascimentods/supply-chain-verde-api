package br.com.anhembi.supplychainverde.application.mapper;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
import br.com.anhembi.supplychainverde.domain.entity.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AddressDtoMapper {
    @Mapping(target = "addressId", ignore = true)
    Address toDomain(AddressRequestDTO request);

    @Mapping(target = "addressId", ignore = true)
    void update(AddressRequestDTO request, @MappingTarget Address address);

    AddressResponseDTO toResponse(Address address);
}
