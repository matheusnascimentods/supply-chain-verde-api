# Especificação — Deploy em VM com Docker

> Arquitetura em [`plan.md`](plan.md) e execução em [`tasks.md`](tasks.md). Contraparte no frontend: `supply-chain-verde-web/docs/features/011-deploy/`.

## 1. Problema

- A API só roda na máquina de quem desenvolve. Não existe imagem Docker dela, e o `docker-compose.yml` sobe só o MySQL.
- `docker/mysql/init/01-users.sql` tem a senha do `supply_app` escrita no arquivo. Num servidor, a senha do banco seria a mesma do repositório público.
- `application.properties` tem um valor padrão para `JWT_SECRET`. Se esquecerem de definir a variável, a API sobe assinando tokens com uma chave que está no GitHub.
- Atrás de um proxy com HTTPS, a API não sabe que a requisição original era `https`. Os links que ela mesma gera (o Swagger, por exemplo) saem com `http://`.

## 2. Objetivo

Subir banco, API e front numa VM Linux qualquer que tenha Docker, sob um domínio só e com HTTPS. A mesma configuração deve servir para Hostinger, DigitalOcean, AWS, GCP ou qualquer outra VM. Só o passo de levar a imagem até o servidor muda de um provedor para outro, e ele fica fora desta feature.

## 3. Glossário

| Termo | Significado |
|---|---|
| Imagem | Imagem Docker da API, publicada no GitHub Container Registry (`ghcr.io`) a cada push na `main`. |
| Compose de produção | `docker-compose.prod.yml`, que sobe MySQL, API e web na VM. É separado do `docker-compose.yml` de desenvolvimento. |
| Front door | O container do web (Caddy). Ele é o único com portas abertas, serve o Angular e repassa `/api` para a API. |
| `.env` do servidor | Arquivo com os segredos de produção, ao lado do compose na VM. Nunca é versionado. O `.env.example` documenta as variáveis. |

## 4. Regras

| ID | Regra |
|---|---|
| RN-01 | Nenhum segredo de produção fica no repositório. Senhas, `JWT_SECRET` e chave do SendGrid vêm do `.env` do servidor. |
| RN-02 | `JWT_SECRET` é obrigatória e não tem valor padrão. Sem ela, a API não sobe, como já acontece com `SENDGRID_*`. Os testes usam um valor próprio, na configuração de testes. |
| RN-03 | A senha do `supply_app` vem da variável `MYSQL_APP_PASSWORD`, lida pelo init do MySQL. O desenvolvimento local e os testes continuam usando `supply_app`. |
| RN-04 | Os padrões de desenvolvimento (`root`/`root` do Flyway e `supply_app` do datasource) continuam no `application.properties`. O compose de produção não depende deles: exige cada variável e falha se alguma faltar. |
| RN-05 | O MySQL e a API não publicam portas na VM. Só o front door abre 80 e 443. |
| RN-06 | Front e API ficam no mesmo domínio: `https://<domínio>/` para o Angular e `https://<domínio>/api/...` para a API. Não há subdomínio de API, e o CORS deixa de ser necessário em produção. |
| RN-07 | O Swagger continua público em produção (`/swagger-ui.html` e `/v3/api-docs`), passando pelo front door. |
| RN-08 | A API respeita os cabeçalhos `X-Forwarded-*` do proxy, então os links gerados saem com `https` e com o domínio público. |
| RN-09 | Os usuários das seeds (V2, V10, V11) continuam em produção como contas de demonstração, com as senhas documentadas no README. Isolar o que eles fazem (sandbox) fica para uma feature futura. |
| RN-10 | A imagem só é publicada depois de os testes passarem, e só a partir da `main`. |

## 5. Contrato

Nenhum endpoint muda. Em produção, as URLs públicas ficam assim:

| URL | Destino |
|---|---|
| `https://<domínio>/api/v1/...` | API |
| `https://<domínio>/swagger-ui.html` e `/swagger-ui/**` | Swagger da API |
| `https://<domínio>/v3/api-docs/**` | OpenAPI da API |
| `https://<domínio>/` e qualquer outra rota | Angular (web) |

## 6. Fora do escopo

- O passo de deploy que depende do provedor: SSH, OIDC, Terraform ou qualquer automação que leve a imagem nova até a VM. Por enquanto, a atualização é manual (`docker compose pull && up -d`).
- Backup automático do MySQL.
- Banco gerenciado (RDS, Cloud SQL, Managed MySQL). As migrations criam triggers com `DEFINER` de outro usuário e um `EVENT`, o que esses serviços costumam restringir. Isso precisa ser testado antes de qualquer migração.
- Sandbox para as contas de demonstração (RN-09).
- Observabilidade, alertas e mais de uma réplica.

## 7. Critérios de aceite

- Numa VM limpa com Docker, um `.env` preenchido e `docker compose -f docker-compose.prod.yml up -d` sobem banco, API e web. As migrations rodam do zero.
- `https://<domínio>/` abre o Angular com certificado válido, e o login com um usuário de demonstração funciona.
- `https://<domínio>/swagger-ui.html` abre, e as chamadas pelo Swagger saem em `https`.
- `grep -rn "supply_app'" docker/mysql/init` não acha senha, e a API não sobe sem `JWT_SECRET`.
- As portas 3306 e 8080 não respondem de fora da VM.
- Um push na `main` com testes verdes publica `ghcr.io/matheusnascimentods/supply-chain-verde-api:latest`.
- O `./mvnw verify` continua verde, e o desenvolvimento local continua funcionando com o `docker compose up -d` de sempre (mais o `JWT_SECRET` exportado).
