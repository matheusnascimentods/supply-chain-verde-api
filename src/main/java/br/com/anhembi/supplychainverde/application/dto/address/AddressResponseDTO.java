package br.com.anhembi.supplychainverde.application.dto.address;
public record AddressResponseDTO(Long addressId, String street, String number, String neighborhood, String complement, String zipCode, String city, String state) {}
