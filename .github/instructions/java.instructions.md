---
applyTo: "src/**/*.java"
---

# Convenções Java

- Respeite a direção `domain <- application <- infrastructure`.
- Prefira tipos de domínio existentes (`Cnpj`, `EmissionFactor`, enums) em vez de primitivos ou strings livres.
- Use Lombok conforme o padrão do pacote; não adicione boilerplate manual quando o projeto já usa uma anotação equivalente.
- Entidades JPA não podem ser records, devem permitir proxy/lazy loading e possuir construtor sem argumentos.
- DTOs devem permanecer imutáveis e ser records.
- Interfaces `*Mapper` devem ser MapStruct; não edite classes geradas em `target/generated-sources`.
- Casos de uso devem ter responsabilidade única, validar pré-condições e propagar exceções de domínio/aplicação.
- Controllers devem permanecer finos: receber DTO, delegar ao caso de uso e retornar a resposta HTTP apropriada.
- Use `@Transactional` na camada de aplicação/infraestrutura conforme o padrão já adotado, não espalhe transações em entidades de domínio.
- Preserve nomes `supplierId`, `batchId`, `originAddressId`, `destinationAddressId` e o padrão temporal `createdAt`, `issuedAt`, `periodStartAt`.
- Ao adicionar um recurso, considere todas as superfícies: entidade, enum/VO, contrato de domínio, caso de uso, DTO, mapper, persistência, controller, segurança, auditoria e testes.

