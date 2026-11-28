# Perfis de acesso na aplicação, com o PLATFORM_ADMIN no topo

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

O entregável 5 do edital da A3 (Plano de Segurança e Administração de Dados) pede a "definição de perfis de acesso (usuários, papéis, permissões)". O edital não diz se esses papéis são do banco ou da aplicação. Faltava decidir em qual camada eles ficam, quem pode atribuí-los e até onde vai a autoridade de cada um.

Num modelo multiempresa (features [010](../features/010-empresas-planos/spec.md) e [016](../features/016-assinatura-assentos/spec.md)), quem usa o sistema é a pessoa da companhia assinante, e não uma conta de banco. As regras de acesso dependem da companhia do usuário e do tipo dela (`MARKET` ou `SUPPLIER`), o que o `GRANT` do MySQL não enxerga.

## Decision

1. **Os perfis de acesso são da aplicação.** São cinco, no enum `UserRole`, verificados pelo Spring Security a cada requisição a partir do JWT. Não se cria `CREATE ROLE` no MySQL.
2. **A hierarquia tem o `PLATFORM_ADMIN` no topo.** Ele herda tudo o que o `ADMIN` pode (`ROLE_PLATFORM_ADMIN > ROLE_ADMIN`, em `SecurityConfig`) e não tem filtro por companhia.
3. **O `ADMIN` de cada companhia gerencia os usuários dela:** convida, troca perfil, desativa e reativa. O `PLATFORM_ADMIN` faz o mesmo em qualquer companhia.
4. **Ninguém cria ou altera um `PLATFORM_ADMIN` pela aplicação.** Ele só nasce por migration ou seed.
5. **No banco, a aplicação é uma conta só** (`supply_app`), com privilégio mínimo. Ver Implications.

### Perfis

| Perfil | Companhia | Papel |
|---|---|---|
| `PLATFORM_ADMIN` | Nenhuma | Administra a plataforma. Vê todas as companhias e gerencia os usuários de qualquer uma |
| `ADMIN` | `MARKET` ou `SUPPLIER` | Administra a própria companhia: dados, usuários e perfis. Numa fornecedora, também pode tudo o que o `SUPPLIER` pode |
| `MANAGER` | `MARKET` | Pede e cancela lotes, registra etapas e gera relatórios |
| `AUDITOR` | `MARKET` | Consulta lotes, certificações, relatórios e auditoria; gera relatórios |
| `SUPPLIER` | `SUPPLIER` | Cadastra produtos e certificações, recusa lotes e registra as etapas de produção |

### Matriz de permissões

Fonte: `SecurityConfig.authorizeHttpRequests` e os `@PreAuthorize` dos controllers. O `PLATFORM_ADMIN` tem tudo o que o `ADMIN` tem.

| Ação | Endpoint | `ADMIN` | `MANAGER` | `AUDITOR` | `SUPPLIER` |
|---|---|:-:|:-:|:-:|:-:|
| Convidar usuário | `POST /invites` | ✓ | | | |
| Trocar perfil / ativar e desativar usuário | `PATCH /users/{id}/role`, `/status` | ✓ | | | |
| Alterar dados da companhia | `PATCH /companies/me` | ✓ | | | |
| Listar usuários da companhia | `GET /users` | ✓ | ✓ | ✓ | ✓ |
| Cadastrar e editar produto | `POST`, `PUT /products` | ✓ | | | ✓ |
| Cadastrar e mudar status de certificação | `POST /suppliers/{id}/certifications`, `PATCH /certifications/{id}/status` | ✓ | | | ✓ |
| Consultar certificações | `GET /certifications` | ✓ | ✓ | ✓ | ✓ |
| Pedir e cancelar lote | `POST /batches`, `PATCH /batches/{id}/cancel` | ✓ | ✓ | | |
| Recusar lote | `PATCH /batches/{id}/reject` | ✓ | | | ✓ |
| Consultar lotes | `GET /batches` | ✓ | ✓ | ✓ | ✓ |
| Registrar etapa, transporte e emissão | `POST /batches/{id}/stages`, `/stages/{id}/transport`, `/stages/{id}/emission` | ✓ | ✓ | | ✓ |
| Gerar relatório | `POST /suppliers/{id}/reports` | ✓ | ✓ | ✓ | |
| Consultar relatórios | `GET /reports` | ✓ | ✓ | ✓ | ✓ |
| Consultar auditoria | `GET /audit-logs` | ✓ | | ✓ | |

Sem login: `POST /auth/login`, `POST /companies` (cadastro), `GET /invites/{token}`, `POST /invites/accept` e a rastreabilidade pública (`GET /batches/{id}/traceability` e `/carbon-footprint`, [ADR 0006](0006-sem-qr-code-plataforma-por-assinatura.md)).

### Regras de gestão de perfis

| Regra | Onde |
|---|---|
| O `ADMIN` só gerencia usuários da própria companhia; fora dela, 403 | `UpdateUserRoleUseCase`, `SetUserStatusUseCase` |
| O perfil precisa combinar com o tipo da companhia: `MANAGER` e `AUDITOR` só em `MARKET`, `SUPPLIER` só em `SUPPLIER`, `ADMIN` nas duas | `UserRole.allowedFor`, no convite e na troca de perfil |
| `PLATFORM_ADMIN` não é atribuível: `allowedFor` sempre recusa, e o perfil dele não muda por endpoint | `UserRole.allowedFor`, `UpdateUserRoleUseCase` |
| Toda companhia mantém pelo menos um `ADMIN` ativo (409). A verificação trava a linha da companhia, então duas trocas simultâneas não a deixam sem `ADMIN` | `UpdateUserRoleUseCase` |
| Usuário desativado não troca de perfil, não entra e não ocupa assento | `UpdateUserRoleUseCase`, feature 016 |
| Usuários ativos mais convites ativos nunca passam de `company.max_users` | `SeatPolicy`, feature 016 |
| Toda leitura é filtrada pela companhia do usuário logado, exceto para o `PLATFORM_ADMIN` | Casos de uso, feature 010 |

## Status

Decidida e implementada. Esta ADR documenta o comportamento existente; não muda código.

## Group

Segurança e administração de dados.

## Assumptions

- O JWT carrega `userId`, `role` e `companyId`, e é a única fonte de identidade da requisição.
- Ninguém acessa o banco de produção diretamente além de quem administra a VM ([ADR 0005](0005-deploy-na-aws-com-ssm.md)): a porta 3306 só escuta em `127.0.0.1`.

## Constraints

- O edital pede a definição dos perfis num plano, sem fixar a camada.
- O MySQL não conhece usuário final nem companhia. Um `GRANT` libera uma tabela inteira, e a regra "cada companhia só vê os próprios dados" é por linha.

## Positions

**Camada dos perfis**

1. **Perfis na aplicação, uma conta de serviço no banco.** Escolhida.
2. **Uma role do MySQL por perfil (`CREATE ROLE`), com a API trocando de role por requisição.** Rejeitada: o `GRANT` é por tabela e não resolve o filtro por companhia, que continuaria na aplicação. Seriam duas fontes de verdade para a mesma permissão, e um pool de conexões com uma conta por perfil.
3. **Uma conta do MySQL por usuário final.** Rejeitada: exigiria criar conta no banco a cada convite e acabaria com o pool de conexões.

**Quem gerencia os perfis**

1. **O `PLATFORM_ADMIN` no topo, e o `ADMIN` de cada companhia gerencia a própria.** Escolhida.
2. **Só o `PLATFORM_ADMIN` gerencia perfis.** Rejeitada: toda troca de perfil de toda companhia dependeria de uma pessoa da plataforma. Num modelo por assinatura, a companhia administra os próprios usuários.

## Argument

As permissões que importam neste sistema são de negócio: quem pede lote é o mercado, quem recusa é o fornecedor dono do lote, e ninguém vê dado de outra companhia. Essas regras dependem da linha (`company_id`, `supplier_id`, `market_id`) e do estado do lote. A aplicação já as aplica num ponto só, enquanto o banco teria só uma parte delas.

A hierarquia em dois níveis espelha o modelo de assinatura: a companhia é dona dos próprios usuários, e a plataforma tem uma autoridade acima de todas para suporte e moderação. Como o `PLATFORM_ADMIN` não é atribuível por nenhum endpoint, um `ADMIN` comprometido não consegue escalar para a plataforma.

## Implications

- **Camada do banco: privilégio mínimo por conta de serviço.** Não há roles do MySQL, mas o banco também se protege:

  | Conta | Uso | Privilégios |
  |---|---|---|
  | `root` | Flyway (migrations) e administração na VM | Total. Senha só no `.env` da VM |
  | `supply_app` | Datasource da API | `SELECT`, `INSERT`, `UPDATE` e `DELETE` tabela por tabela (V6 e seguintes); só `SELECT` em `audit_log` e nas views; `EXECUTE` nas procedures |
  | `supply_audit` | `DEFINER` das triggers e de `audit_write` | Única com `INSERT` em `audit_log`. Criada com `ACCOUNT LOCK`: ninguém faz login com ela ([ADR 0002](0002-migracao-para-mysql.md)) |

- **Rastreabilidade das trocas:** a trigger de `users` grava toda mudança de `role` e de `is_active` em `audit_log`, com o usuário que fez a mudança. Nem a API consegue apagar esse registro.
- **Um perfil novo** exige mudar o enum `UserRole` (com `allowedFor`), o `ENUM` da coluna `users.role` numa migration, a `SecurityConfig` e o `roleGuard` do web.
- **Um `PLATFORM_ADMIN` novo** só entra por migration, com revisão de PR.

## Related artifacts

- `src/main/java/.../infrastructure/security/SecurityConfig.java`: regras por endpoint e hierarquia.
- `src/main/java/.../domain/enums/UserRole.java`: perfis e `allowedFor`.
- `src/main/java/.../application/usecase/user/UpdateUserRoleUseCase.java`, `SetUserStatusUseCase.java` e `SeatPolicy.java`.
- `src/main/resources/db/migration/V6__grant_app_user.sql` e `docker/mysql/init/01-users.sh`: contas e privilégios do banco.
- [Feature 010](../features/010-empresas-planos/spec.md), [011](../features/011-convites/spec.md) e [016](../features/016-assinatura-assentos/spec.md): multiempresa, convites e assentos.
