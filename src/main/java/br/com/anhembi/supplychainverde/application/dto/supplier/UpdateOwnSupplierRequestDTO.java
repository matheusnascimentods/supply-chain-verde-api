package br.com.anhembi.supplychainverde.application.dto.supplier;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
public record UpdateOwnSupplierRequestDTO(String name, String phone, AddressRequestDTO address, String userName, String currentPassword, String newPassword) {}
