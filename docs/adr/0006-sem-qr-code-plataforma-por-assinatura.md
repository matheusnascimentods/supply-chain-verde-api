# Sem QR Code: a plataforma atende mercados e fornecedores assinantes

*Template: decision record de Jeff Tyree e Art Akerman, “Architecture Decisions: Demystifying Architecture”.*

## Issue

A [spec base](../features/000-base/spec.md) previa um perfil público: o consumidor leria um QR Code na embalagem e veria a jornada do lote e a pegada de carbono. O QR Code nunca foi gerado. Não existe tela nem endpoint que o produza.

Depois disso o produto mudou. Com as features [010](../features/010-empresas-planos/spec.md) e [016](../features/016-assinatura-assentos/spec.md), virou uma plataforma por assinatura: mercados e fornecedores se cadastram, o mercado pede lotes e o fornecedor os produz. Quem usa o sistema é a companhia assinante, não o consumidor final.

## Decision

O QR Code sai do escopo. O consumidor final deixa de ser público-alvo da plataforma.

A rastreabilidade sem login continua como está: `GET /batches/{batchId}/traceability`, `GET /batches/{batchId}/carbon-footprint` e a página `/rastreio/:batchId` do web. Ela é acessada por link, e não por QR Code.

## Status

Decidida. Nenhum código removido, porque o QR Code nunca foi implementado.

## Group

Produto e escopo.

## Assumptions

- Os usuários da plataforma são os mercados e os fornecedores, cada um com até 10 usuários.
- Quem precisa comprovar a sustentabilidade de um lote para terceiros, como uma certificadora, usa o relatório em PDF da [feature 004](../features/004-relatorio-pdf/spec.md).

## Constraints

- O tema 10 do edital da A3 (Supply Chain Verde) pede rastreabilidade, pegada de carbono por etapa, recomendação de fornecedores e relatórios para certificação. O QR Code não é requisito desse tema: ele aparece no tema 19.

## Positions

1. **Gerar o QR Code na tela do lote.** Rejeitada: atende um usuário que o produto não tem mais.
2. **Tirar o QR Code do escopo e manter a página pública por link.** Escolhida.
3. **Tirar também a página e os endpoints públicos.** Fica em aberto: eles não atrapalham e servem para a demonstração.

## Argument

Um QR Code na embalagem só faz sentido se o consumidor final for usuário da plataforma. Num produto em que mercados e fornecedores trocam pedidos de lote, essa funcionalidade não tem quem a use. Ela consumiria tempo que vale mais nos entregáveis do edital.

## Implications

- O perfil "Público" (consumidor) sai da spec base. A rastreabilidade sem login fica registrada como acesso por link.
- O lote público continua identificado pelo id numérico, então dá para enumerar lotes trocando o número na URL. Isso era aceitável para um QR Code e continua igual por link. Um UUID público segue como decisão em aberto no [`plan.md`](../features/000-base/plan.md) da base.

## Related artifacts

- [`../features/000-base/spec.md`](../features/000-base/spec.md): contexto, perfis e fluxo de rastreabilidade pública.
- [`../features/010-empresas-planos/spec.md`](../features/010-empresas-planos/spec.md) e [`../features/016-assinatura-assentos/spec.md`](../features/016-assinatura-assentos/spec.md): o modelo por assinatura.
- [`README.md`](../../README.md) deste repositório e do `supply-chain-verde-web`.
