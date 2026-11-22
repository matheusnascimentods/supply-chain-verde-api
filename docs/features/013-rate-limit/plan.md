# Plano técnico — Rate limit

> Requisitos em [`spec.md`](spec.md); passo a passo em [`tasks.md`](tasks.md).

## 1. Onde o limite fica

Na API, e não no Caddy. O Caddy só limita com o plugin `caddy-ratelimit`, o que exige gerar uma imagem própria do Caddy no repositório do web. Além disso, o Caddy não sabe quem é o usuário do JWT, então não conseguiria aplicar a RN-02.

Não entra dependência nova. Com uma instância só (RN-08), um `ConcurrentHashMap` resolve, e o Bucket4j só se pagaria com Redis e várias réplicas.

## 2. `RateLimitFilter`

Fica em `infrastructure/security/RateLimitFilter`, ao lado do `JwtAuthenticationFilter`, e estende `OncePerRequestFilter`.

```java
// ponytail: janela fixa em memória. Aceita até 2x o limite na virada do minuto e vale para uma instância só.
// Com mais de uma réplica, trocar por Bucket4j + Redis.
private static final long WINDOW_MS = 60_000;
private static final int GLOBAL_LIMIT = 120;
private static final Map<String, Integer> STRICT_LIMITS = Map.of(
        "POST /api/v1/auth/login", 5,
        "POST /api/v1/invites/accept", 5,
        "GET /api/v1/invites/", 10,
        "POST /api/v1/companies", 3);

private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
private final LongSupplier now;                 // System::currentTimeMillis; nos testes, um relógio controlado
private volatile long windowStart;
```

A cada requisição:

1. **Janela:** se `now - windowStart >= WINDOW_MS`, faz `hits.clear()` e `windowStart = now`, com double-checked lock em `synchronized`. Zerar o mapa inteiro mantém o tamanho dele limitado aos clientes de um minuto, sem rotina de limpeza.
2. **Chave global:** `"u:" + userId` quando o `SecurityContextHolder` tem um `CustomUserPrincipal`; se não, `"ip:" + request.getRemoteAddr()` (RN-02).
3. **Chave estrita:** monta `método + " " + URI` e procura em `STRICT_LIMITS` com `startsWith`. É isso que cobre o `{token}` de `GET /invites/{token}`. Se achar, a chave é `"ip:" + remoteAddr + " " + regra` (RN-04).
4. **Contagem:** `computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet() > limite`. A global conta primeiro. Se ela já estourou, a estrita nem conta.
5. **Estourou:** responde `429` com `Retry-After = (windowStart + WINDOW_MS - now) / 1000 + 1` e o JSON da RN-07, e não chama `chain.doFilter`.

Os limites são constantes no código: ninguém pediu para mudá-los sem deploy.

## 3. Registro na cadeia do Spring Security

O filtro precisa rodar **depois** do `JwtAuthenticationFilter`, que é quem preenche o `SecurityContextHolder` com o usuário. Por isso ele entra na cadeia do `SecurityConfig` e **não** leva `@Component`. Com `@Component`, o Spring Boot também o registraria como filtro do servlet, antes da autenticação, e todo mundo contaria pelo IP.

```java
@Value("${rate-limit.enabled}") boolean rateLimitEnabled;
...
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
if (rateLimitEnabled) http.addFilterAfter(new RateLimitFilter(System::currentTimeMillis), JwtAuthenticationFilter.class);
```

Nessa posição:

- O `CorsFilter` (de `.cors(...)`) roda antes. Ele responde a preflight `OPTIONS` sem seguir a cadeia (RN-06), e o `429` sai com os headers de CORS, então o front em `localhost:4200` consegue ler a mensagem.
- A autorização roda depois. Requisições que terminam em 401/403 já foram contadas (RN-06).

## 4. IP real

Nada muda: o `server.forward-headers-strategy=framework` (feature 012, RN-08) já faz o `getRemoteAddr()` devolver o IP do `X-Forwarded-For`. O `reverse_proxy` do Caddy, sem `trusted_proxies`, sobrescreve o `X-Forwarded-For` que vem do cliente com o IP da conexão. Isso cobre a RN-03.

## 5. Configuração

```properties
# application.properties: ligado em qualquer ambiente
rate-limit.enabled=true
```

```properties
# src/test/resources/config/application.properties: desligado nos testes
rate-limit.enabled=false
```

Não há variável de ambiente: ninguém precisa desligar o limite em produção. A propriedade existe por causa dos testes. Todos os ITs rodam com o IP `127.0.0.1` do MockMvc e fazem login várias vezes. Com o limite ligado, o 6º login de toda a suíte receberia `429`, e o resultado dependeria da ordem dos testes.

## 6. Testes

### Unitário: `RateLimitFilterTest` (sem Spring)

Usa `MockHttpServletRequest`, `MockHttpServletResponse` e `MockFilterChain`, e um relógio controlado (`AtomicLong` passado como `LongSupplier`). Assim a virada da janela é testada sem `sleep`. Casos:

- 120 requisições autenticadas passam, e a 121ª recebe `429`, com `Retry-After` e o JSON, sem chamar a chain.
- Dois usuários no mesmo IP têm contadores separados.
- Sem autenticação, a contagem é por IP: dois IPs têm contadores separados.
- O 6º `POST /auth/login` do mesmo IP recebe `429`, enquanto um `GET` de outra rota, do mesmo IP, passa.
- `GET /invites/abc` e `GET /invites/xyz` contam na mesma regra estrita.
- Depois que o relógio avança 60s, o cliente bloqueado volta a passar.
- O `Retry-After` diminui conforme o relógio avança dentro da janela.

### Integração: `RateLimitIT` (Testcontainers + MockMvc)

Usa `@TestPropertySource(properties = "rate-limit.enabled=true")`. Isso gera um contexto Spring próprio e não afeta os outros ITs. Cada teste usa um IP diferente (`request.setRemoteAddr(...)` via `with(...)`), então um teste não consome o limite do outro. O IT prova o que o unitário não alcança, que é o registro na cadeia:

- O filtro enxerga o usuário do JWT: dois usuários reais, logados pelo mesmo IP, têm limites separados. Isso só funciona se ele roda depois do `JwtAuthenticationFilter`.
- O 6º login no mesmo minuto recebe `429` com o corpo no formato dos outros erros.
- Uma requisição com JWT inválido conta pelo IP e não derruba o filtro.

O IT faz 121 requisições, o que leva poucos segundos com MockMvc.

### Manual, contra o compose local (Caddy na frente)

É o único teste que passa pelo Caddy de verdade e valida a RN-03. O roteiro fica no README (Task 2):

```bash
# 1. Login: 5 respostas 401 e depois 429
for i in $(seq 1 7); do
  curl -sk -o /dev/null -w '%{http_code}\n' -X POST https://localhost/api/v1/auth/login \
    -H 'Content-Type: application/json' -d '{"email":"x@x.com","password":"errada"}'
done

# 2. X-Forwarded-For forjado não escapa: continua 429
curl -sk -o /dev/null -w '%{http_code}\n' -X POST https://localhost/api/v1/auth/login \
  -H 'X-Forwarded-For: 203.0.113.9' -H 'Content-Type: application/json' -d '{"email":"x@x.com","password":"errada"}'

# 3. Global por usuário: com um token válido, a 121ª chamada recebe 429
for i in $(seq 1 125); do
  curl -sk -o /dev/null -w '%{http_code}\n' https://localhost/api/v1/users/me -H "Authorization: Bearer $TOKEN"
done | sort | uniq -c
```
