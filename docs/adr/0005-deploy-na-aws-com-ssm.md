# Deploy na AWS: uma EC2 com Docker, atualizada pelo SSM

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

O [plano de deploy](../features/012-deploy/plan.md) define três containers (Caddy com o Angular, API e MySQL) num `docker-compose.yml` só, rodando numa VM Linux de qualquer provedor. Faltava decidir onde essa VM roda, como ela é criada e como uma imagem nova chega até ela depois de um merge.

A conta escolhida é uma conta AWS no plano gratuito, com US$ 100 de créditos iniciais (mais US$ 20 por atividade concluída) e validade de 6 meses. Ela usa a experiência simplificada de *projects* da AWS, que esconde parte dos recursos do console. O orçamento precisa caber nos créditos.

## Decision

1. **Uma instância EC2 `t3.small` com Ubuntu 24.04**, rodando o mesmo compose do desenvolvimento. O MySQL roda num container dentro dela, sem RDS.
2. **IP fixo (Elastic IP) e domínio `sslip.io`** até existir um domínio próprio. O Caddy emite o certificado do Let's Encrypt sozinho.
3. **Deploy automático pelo SSM Run Command.** Depois do `publish`, um job `deploy` no CI dos dois repositórios manda a VM rodar `git pull && docker compose pull <serviço> && docker compose up -d <serviço>`.
4. **A credencial do CI é um usuário IAM com chave de acesso**, que só pode mandar comando para esta instância. O OIDC seria preferível, mas está bloqueado na conta.

### A VM

| Item | Valor | Motivo |
|---|---|---|
| Região | `us-east-2` (Ohio) | Região padrão da conta. Mais barata que `sa-east-1` e com latência aceitável para uma demonstração |
| AMI | Ubuntu Server 24.04 LTS, x86_64 | A imagem no GHCR é só `linux/amd64`. O script oficial do Docker suporta Ubuntu sem passos extras. A 24.04 é madura e tem suporte até 2029 |
| Tipo | `t3.small` (2 vCPU, 2 GiB) | Cabe MySQL (~450 MB), JVM (~750 MB) e Caddy. A `t3.micro` (1 GiB) não comporta a JVM e o MySQL juntos |
| Créditos de CPU | Standard | No modo Unlimited, CPU acima da linha de base é cobrada à parte (até ~US$ 70/mês com um processo a 100%). No Standard, a CPU fica mais lenta quando os créditos acabam, e o custo é previsível |
| Disco | ~20 GiB gp3 | Sistema, imagens Docker, volume do MySQL e swap. Com 8 GiB, sobraria pouco espaço |
| Swap | 2 GiB, criado pelo user data | Reserva para picos de memória: evita que o kernel mate o MySQL ou a JVM |
| User data | Instala o Docker (`get.docker.com`) e cria o swap | A VM sai pronta do primeiro boot |
| Termination protection | Ligada | O volume do MySQL é apagado junto com a instância. A proteção impede um *terminate* por engano |
| Shutdown behavior | Stop | Um `shutdown` dentro da VM nunca apaga a instância |
| Metadata | IMDSv2 obrigatório | Padrão mais seguro, e a aplicação não lê o metadata |
| Compra | On-Demand | Spot pode ser interrompido pela AWS a qualquer momento |

### Rede

- **Security group:**
  - SSH (22) liberado só para o IP de quem administra.
  - HTTP (80) e HTTPS (443) abertos para todos.
  - Nenhuma outra porta: a 3306 escuta só em `127.0.0.1`, e a 8080 não é publicada.
- **Elastic IP:** sem ele, o IP público muda a cada *stop/start*, e com isso quebram o DNS, o certificado e o SSH. Custa o mesmo que o IP temporário (US$ 0,005/h), que ele substitui.
- **Domínio:** `<ip-com-hífens>.sslip.io` resolve para o próprio IP sem configuração, e o Let's Encrypt emite certificado para ele. Para trocar por um domínio próprio: registro `A` apontando para o Elastic IP, novo `SITE_ADDRESS` no `.env` e `docker compose up -d`. Os links de convite já enviados apontam para o domínio antigo e deixam de funcionar.

### Deploy automático

```
merge na main
  → CI: testes (API: Maven + Testcontainers; web: tests, build, audit, lint)
  → publish: imagem no GHCR com as tags :latest e :<sha>
  → deploy: aws ssm send-command para a instância
      → o agente do SSM na VM executa, como ubuntu:
         cd /home/ubuntu/supply-chain-verde-api
         git pull -q && docker compose pull <serviço> && docker compose up -d <serviço>
  → o job espera até 10 min, mostra a saída e falha se o status não for Success
```

O job está no `ci.yml` dos dois repositórios: aqui, com `SERVICE: api`, e no `supply-chain-verde-web`, com `SERVICE: web`. Cada merge recria só o serviço do repositório que mudou. O MySQL nunca reinicia num deploy.

Peças na AWS:

| Peça | Para quê |
|---|---|
| Agente do SSM | Já vem no Ubuntu da AWS (snap `amazon-ssm-agent`). Mantém uma conexão **de saída** com a AWS e executa os comandos recebidos. Nenhuma porta de entrada é aberta |
| Role `supply-chain-verde-ec2-ssm` | Associada à instância como instance profile, com a policy gerenciada `AmazonSSMManagedInstanceCore`. É a identidade da VM perante o SSM |
| Policy `supply-chain-verde-deploy` | Permite só `ssm:SendCommand` nesta instância com o documento `AWS-RunShellScript`, e `ssm:GetCommandInvocation` |
| Usuário IAM `github-deploy` | Sem acesso ao console, com a policy acima. A chave fica nos secrets `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` dos dois repositórios |

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": [
        "arn:aws:ec2:us-east-2:<account-id>:instance/<instance-id>",
        "arn:aws:ssm:us-east-2::document/AWS-RunShellScript"
      ]
    },
    { "Effect": "Allow", "Action": "ssm:GetCommandInvocation", "Resource": "*" }
  ]
}
```

## Status

Implementada. O deploy automático foi validado com o PR #109 do web: depois do merge, o container `web` foi recriado com o bundle novo, e a API e o MySQL seguiram rodando.

## Group

Infraestrutura, entrega e segurança.

## Assumptions

- O projeto é uma demonstração acadêmica com pouco tráfego. Uma instância só, sem alta disponibilidade, é aceitável.
- Alguns segundos de indisponibilidade a cada deploy são aceitáveis.
- O repositório e os pacotes do GHCR são públicos. A VM clona e baixa as imagens sem credencial.
- A conta continua no plano gratuito. Quando os créditos acabam ou o prazo termina, a conta é encerrada se não houver upgrade para o plano pago.

## Constraints

- **Orçamento:** os créditos precisam cobrir os 6 meses. Um budget mensal de US$ 25 avisa por e-mail em 85% e 100% do gasto real e em 100% do previsto. A opção *Credits* fica desmarcada, para o budget medir o custo real e não o valor já abatido.
- **Banco gerenciado fora:** a [spec do deploy](../features/012-deploy/spec.md) deixa o RDS de fora. As migrations criam triggers com `DEFINER` de outro usuário e um `EVENT` ([ADR 0002](0002-migracao-para-mysql.md)), o que serviços gerenciados costumam restringir.
- **OIDC bloqueado:** na experiência de *projects*, *IAM → Identity providers* exige "advanced features". Ativá-las é irreversível, exige upgrade para o plano pago e remove o limite de gasto.
- **SSH restrito:** a porta 22 só aceita o IP de quem administra. Um runner do GitHub não chega nela.

## Positions

**Hospedagem**

1. **EC2 com o compose do desenvolvimento.** Escolhida.
2. **EC2 + RDS.** Rejeitada: o RDS sozinho custa mais que a VM, e as triggers com `DEFINER` e o `EVENT` exigiriam adaptação.
3. **Kubernetes (EKS) com ArgoCD.** Rejeitada: só o control plane do EKS custa cerca de US$ 73/mês, mais que o triplo do orçamento inteiro.

**Atualização da VM**

1. **Job no CI que chama o SSM Run Command.** Escolhida.
2. **Job no CI que entra por SSH.** Rejeitada: exigiria abrir a porta 22 para os IPs dos runners do GitHub (na prática, a internet inteira) e guardar a chave `.pem` como secret.
3. **Cron na VM fazendo `docker compose pull && up -d` a cada 5 minutos.** É um modelo *pull*, parecido com o ArgoCD: não precisa de credencial nem de permissão na AWS. Rejeitada como principal porque a falha não aparece no GitHub e o deploy atrasa até 5 minutos. É o plano B se o SSM deixar de estar disponível.

**Credencial do CI**

1. **OIDC (`token.actions.githubusercontent.com`) + role com trust por repositório e branch.** Preferível, porque não guarda chave nenhuma. Bloqueada na conta (Constraints).
2. **Usuário IAM com chave de acesso e policy mínima.** Escolhida.

## Argument

O SSM inverte a direção da conexão: é o agente na VM que fala com a AWS, então a pipeline atualiza a máquina sem nenhuma porta de entrada nova. O SSH continua restrito ao IP de quem administra. O Run Command e o registro da instância no tier Standard são gratuitos, então o custo não muda.

A chave do `github-deploy` é a parte mais sensível, e a policy limita o estrago: com ela só se manda um shell script para esta instância. Esse script roda como root na VM, então quem tiver a chave controla a VM. Mas não cria recursos, não lê dados de outros serviços e não mexe na conta. Ela fica só nos secrets do GitHub. Se vazar, basta desativá-la no IAM e gerar outra.

Detalhes do job:

- **`runuser -u ubuntu`:** o SSM executa como root, e o clone na VM pertence ao `ubuntu`. Como root, o `git pull` recusaria o repositório por ter outro dono.
- **Espera própria:** o `aws ssm wait command-executed` desiste em 100 s, e o pull de uma imagem pode passar disso. O job consulta o status a cada 5 s, por até 10 min.
- **`docker compose up -d <serviço>`** só recria o container se a imagem mudou. Um merge que não altera a imagem (por exemplo, só o `ci.yml`) gera a mesma imagem pelo cache, e o compose mantém o container rodando.

## Implications

**Custo mensal estimado** (sob demanda, `us-east-2`):

| Item | US$/mês |
|---|---|
| EC2 `t3.small` (730 h) | 15,18 |
| EBS gp3 20 GiB | 1,60 |
| IPv4 público (Elastic IP) | 3,65 |
| Tráfego de saída (< 100 GB), SSM, IAM, CloudWatch básico | 0 |
| **Total** | **≈ 20,50** |

**Por deploy:**

- **API:** fica fora por cerca de 30-60 s, enquanto a JVM sobe e o Flyway roda.
- **Web:** o Caddy reinicia em 1-2 s. Os certificados ficam no volume `caddy_data` e não são reemitidos.
- **Migrations:** rodam sozinhas quando a API reinicia com uma imagem nova, e o Flyway não as desfaz. Faça um `mysqldump` antes de mergear uma migration.

**Rollback:**

- Reverter o PR: o revert passa pelo mesmo fluxo e publica a versão anterior.
- Ou, na VM: trocar `:latest` por `:<sha>` no compose e rodar `docker compose up -d <serviço>`.

**Mudanças no `docker-compose.yml` ou no `docker/mysql/init`:** chegam pelo `git pull` do job. Uma mudança no init só vale para volumes novos.

**Operação manual:**

- `ssh -i <chave>.pem ubuntu@<elastic-ip>`. Se o IP de quem administra mudar, atualize a regra SSH do security group.
- O status dos comandos fica em *Systems Manager → Run Command → Command history*. A instância aparece em *Fleet Manager*.

**Para não gerar custo extra:**

- Não ativar o tier Advanced do SSM.
- Não mandar a saída dos comandos para CloudWatch Logs ou S3.
- Ao apagar a instância, fazer *Release* do Elastic IP: um IP alocado sem uso continua sendo cobrado.

**Sem backup automático.** Perder o volume `mysql_data` apaga os dados. Até existir uma rotina, o backup é um `mysqldump` manual.

## Related artifacts

- [`../features/012-deploy/plan.md`](../features/012-deploy/plan.md) e [`spec.md`](../features/012-deploy/spec.md): compose único, imagem da API e publicação no GHCR.
- `.github/workflows/ci.yml`: jobs `publish` e `deploy`.
- `supply-chain-verde-web/.github/workflows/ci.yml`: o mesmo job, com `SERVICE: web`.
- Seção **Deploy** do [`README.md`](../../README.md).
