# NEXUS77

App Android (**Kotlin nativo, zero dependências**) que transforma um comando curto
em uma página HTML viva, gerada por IA e renderizada em tela cheia num WebView.

> Você escreve: `criar cubo html 3d` → o app envia o prompt para uma API
> compatível com OpenAI → recebe um HTML single-file → injeta o tema ativo →
> renderiza na hora. Não é chat: é um gerador de páginas.

## Características

- **WebView shell**: build em segundos, sem Compose/Hilt/Room, sem kapt/KSP.
- **15 temas** (12 escuros + 3 claros), agrupados na tela em **ESCUROS** (NEXUS·ciano,
  MATRIX·verde, NEON·roxo, OCEAN·azul, SUNSET·laranja, GOLD·dourado, MAGMA·vermelho,
  GRAFITE·cinza, ÍNDIGO·violeta, ESMERALDA·verde, VINHO·carmesim, CAFÉ·marrom) e
  **CLAROS** (CLARO·branco, SAKURA·rosa, GELO·azul claro).
- **Configurações**: endpoint compatível OpenAI, API key, busca de modelos,
  temperatura, **nível de detalhe** (Simples/Médio/Rico) e max tokens.
- **Linha de sessão** sob o título: modelo de IA em uso (com dot de status),
  consumo da sessão em **entrada/saída** e o total numa barra de **1M tokens**
  (ex.: `↑12.4k ↓8.1k · 20.5k/1M`) e botão **↺** para reiniciar a sessão.
- **Prompt de sistema** com a paleta do tema injetada + limpeza/decoração do HTML.
- **Refinamento contextual (v1.3.0)**: ao enviar um novo comando com uma página em
  tela, o documento atual vai junto na mensagem — a IA **edita** o existente
  (ex.: "agora azul") em vez de gerar outra página do zero.
- **Salvar / Abrir (v1.4.0)**: **SALVAR** no topo grava a página em `Downloads/nexus77`
  (com popup para nomear); **ABRIR** abre uma página listando os arquivos salvos,
  tocando para reabrir.
- **VU meter + KB (v1.5.0 / streaming v1.7.0)**: ao gerar/ajustar, um medidor de 10
  cubos (degradê, zigue-zague) aparece **antes** do status, com os **bytes** recebidos
  ao vivo (B/KB/MB) — a resposta é lida em **streaming (SSE)**.
- **Nível de detalhe (v1.8.0)**: Simples / Médio / Rico — injeta uma diretriz no prompt
  para o resultado ficar consistente (sem libs ↔ three.js com sombras, luz e animação).
- **R8 + shrinkResources** no release; assinatura **v2 + v3**.

## Estrutura

```
NEXUS77/
├── AGENTS.md               # guia rápido para agentes/IA
├── README.md               # este arquivo
├── android/                # Projeto Gradle/Kotlin (com.leo.nexus77)
├── web/                    # welcome.html (referência estática do estado inicial)
└── docs/
    ├── DOCUMENTACAO.md      # documentação detalhada (arquitetura, fluxos, API interna)
    ├── BASE_REUTILIZAVEL.md # base p/ reusar em novos projetos (padrões que funcionaram)
    ├── ARQUITETURA.md       # visão de componentes e fluxo
    ├── MAPA_UI.md           # cada elemento de tela (arquivo:linha + âncora)
    ├── ASSINATURA.md        # keystore, build release e validação
    └── CHANGELOG.md         # histórico de versões
```

- [`docs/DOCUMENTACAO.md`](docs/DOCUMENTACAO.md) — **documentação detalhada**: visão
  geral, fluxos, referência técnica por arquivo, prompt/IA, temas, dados, build e FAQ.
- [`docs/MAPA_UI.md`](docs/MAPA_UI.md) mapeia cada elemento de tela
  (`arquivo:linha` + âncora) — consulte antes de pedir mudanças pontuais de UI
  (ex.: "mudar cor do cubo").
- [`docs/BASE_REUTILIZAVEL.md`](docs/BASE_REUTILIZAVEL.md) — **base reutilizável** para
  novos projetos (render adaptativo, tema injetado, confiabilidade de resposta).
- [`docs/CHANGELOG.md`](docs/CHANGELOG.md) — histórico de versões.

## Build

```bash
cd android
./gradlew assembleDebug    # iteração rápida (sem R8)
./gradlew assembleRelease  # entrega (R8 + shrink + assinatura v2/v3)
```

- `gradlew-fast` = atalho para `assembleFast` (release sem R8, sufixo `.fast`).
- Requisitos: JDK 17, Android SDK 34 (`local.properties` → `sdk.dir`).
- Assinatura: `android/keystore.properties` + `android/keystore/nexus77-release.jks`
  (ver `docs/ASSINATURA.md`).

## APK

Saída de entrega: `/home/leo/apk22/app-v14-1.10.1-<timestamp>.apk`
(`versionCode 14` / `versionName 1.10.1`, ~67 KB).

## Fluxo de uso

1. Abrir → página inicial (cubo + chips de sugestão) + linha de sessão com o
   modelo ativo e a barra de tokens.
2. Engrenagem → Configurações: colar endpoint + API key, buscar modelos, escolher
   tema e parâmetros.
3. Digitar um comando → **Enviar** → a página gerada substitui a inicial.
   Os tokens da resposta somam na barra da sessão.
4. **Refinar**: enviar outro comando com uma página aberta → o documento em tela
   acompanha a mensagem e a IA edita só o pedido (status "ajustando…").
5. **Salvar**: **SALVAR** (topo) → popup para nomear → grava em
   `Downloads/nexus77/<nome>.html` (sobrescreve o mesmo nome).
6. **Abrir**: **ABRIR** (topo) → página listando os arquivos salvos; toque em um
   para reabrir (Voltar ou botão do sistema retorna).
7. **↺** na linha de sessão → zera a contagem e volta à página inicial.
8. Tema trocado em Configurações é reaplicado à página atual ao voltar.
