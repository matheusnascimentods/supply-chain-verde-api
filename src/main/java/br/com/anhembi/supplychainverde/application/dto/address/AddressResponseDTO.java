package br.com.anhembi.supplychainverde.application.dto.address;

import br.com.anhembi.supplychainverde.domain.entity.Address;

public record AddressResponseDTO(Long addressId, String street, String number, String neighborhood, String complement, String zipCode, String city, String state) {
    public static AddressResponseDTO from(Address address) {
        if (address == null) return null;
        return new AddressResponseDTO(address.getAddressId(), address.getStreet(), address.getNumber(),
                address.getNeighborhood(), address.getComplement(), address.getZipCode(), address.getCity(), address.getState());
    }
}
