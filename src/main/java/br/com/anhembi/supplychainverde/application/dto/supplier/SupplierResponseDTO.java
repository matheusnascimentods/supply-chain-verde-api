package br.com.anhembi.supplychainverde.application.dto.supplier;
import java.time.LocalDate;
import br.com.anhembi.supplychainverde.application.dto.address.AddressResponseDTO;
public record SupplierResponseDTO(Long supplierId, String name, String cnpj, AddressResponseDTO address, String phone, LocalDate registeredAt) {}
