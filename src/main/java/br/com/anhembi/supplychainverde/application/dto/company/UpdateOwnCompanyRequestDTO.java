package br.com.anhembi.supplychainverde.application.dto.company;

import br.com.anhembi.supplychainverde.application.dto.address.AddressRequestDTO;

/** Campos opcionais: só os enviados mudam. CNPJ e tipo não são editáveis (RN-05). */
public record UpdateOwnCompanyRequestDTO(String name, String email, String phone, AddressRequestDTO address) {}
