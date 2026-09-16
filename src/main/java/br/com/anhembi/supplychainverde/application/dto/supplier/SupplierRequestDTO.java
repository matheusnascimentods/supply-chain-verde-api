package br.com.anhembi.supplychainverde.application.dto.supplier;
import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;
public record SupplierRequestDTO(String name, String cnpj, AddressRequestDTO address, String phone) {}
