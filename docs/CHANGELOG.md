# NEXUS77 — Changelog

Histórico de versões (mais recente primeiro). Formato inspirado em *Keep a Changelog*.
Cada linha `vN` é o `versionCode`; o APK entregue fica em `/home/leo/apk22/`.

---

## [1.10.1] — v14 — 2026-10-16
### Alterado
- **Alinhamento do status**: "pronto — descreva o que quer ver" à **esquerda**; a linha
  pós-renderização (`✓ KB • s • modelo`) à **direita**.
- VU meter/KB só ocupam espaço durante o carregamento (ficam `GONE` quando idle), então o
  texto do status encosta na borda esquerda.
### Notas
- APK: `app-v14-1.10.1-20261016-183858.apk`

## [1.10.0] — v13 — 2026-10-16
### Adicionado
- Página **Salvos**: botão **"Abrir HTML…"** (à direita, na mesma linha do ‹ Voltar) que abre
  o **seletor de arquivos do celular** (SAF) para carregar qualquer `.html` externo.
- Botão **excluir (✕)** em cada item, com **confirmação** (`Store.delete`).
### Notas
- APK: `app-v13-1.10.0-20261016-181413.apk`

## [1.9.2] — v12 — 2026-10-16
### Alterado
- **KB do carregamento = tamanho real do documento**: o progresso passou a contar os bytes
  **decodificados** de `delta.content` (não mais o corpo SSE), batendo com o "✓ X KB" final.
- **Tokens contando ao vivo**: durante a geração o rótulo mostra `~↑ / ~↓ · total/1M`
  (estimativa ≈ bytes/4) e é trocado pelo valor **exato** da API (`usage`) ao finalizar.
### Notas
- APK: `app-v12-1.9.2-20261016-174305.apk`

## [1.9.1] — v11 — 2026-10-16
### Alterado
- **VU meter redesenhado**: trilho de fundo, preenchimento em degradê base→topo, "cap"
  aceso no topo e animação por **onda suave** (soma de senos) em vez do triângulo seco.
- **Contador de KB em destaque** (cor de destaque + negrito), posicionado à frente de
  "gerando…/ajustando…".
### Notas
- APK: `app-v11-1.9.1-20261016-173129.apk`

## [1.9.0] — v10 — 2026-10-16
### Adicionado
- **+5 temas escuros**: GRAFITE·cinza, ÍNDIGO·violeta, ESMERALDA·verde, VINHO·carmesim,
  CAFÉ·marrom (**15 temas no total** = 12 escuros + 3 claros).
- Temas **agrupados** na tela de Configurações: **ESCUROS** e **CLAROS**.
### Notas
- Índices dos temas antigos preservados (novos entraram no fim de `ALL`) → quem já tinha
  um tema salvo continua com o mesmo.
- APK: `app-v10-1.9.0-20261016-165322.apk`

## [1.8.0] — v9 — 2026-10-16
### Adicionado
- Seletor **Nível de detalhe** (Simples / Médio / Rico) em Configurações, **abaixo da
  temperatura**; persistido em `Prefs.detailLevel` e injetado no prompt
  (`Html.detailDirective`).
### Notas
- APK: `app-v9-1.8.0-20261016-164305.apk`

## [1.7.0] — v8 — 2026-10-16
### Corrigido
- **KB do carregamento ficava em 0.0**: a requisição era não-streaming (o corpo só chega
  no fim). Agora o `chat` usa **streaming (SSE)** e os bytes chegam em tempo real.
### Adicionado
- `AiClient` faz *parse* de **SSE** (`data:` / `delta.content`) e captura `usage`;
  **fallback** automático para não-streaming em HTTP 4xx.
- Rótulo de KB inicia em `…` e usa **B/KB/MB**.
### Notas
- APK: `app-v8-1.7.0-20261016-162804.apk`

## [1.6.0] — v7 — 2026-10-16
### Alterado
- Orçamento da sessão: **100k → 1M** tokens (`SESSION_TOKEN_BUDGET`).
- Consumo discriminado em **↑entrada (prompt)** e **↓saída (completion)**; rótulo
  `↑X ↓Y · total/1M`.
- `formatTokens` passa a exibir sufixo **M**.
### Notas
- APK: `app-v7-1.6.0-20261016-162318.apk`

## [1.5.0] — v6 — 2026-10-16
### Adicionado
- **VU meter** no cabeçalho (10 cubos, degradê, zigue-zague) antes do texto de status
  (`VuMeterView`).
- **Contador de KB** ao vivo (via progresso de bytes do `AiClient`).
### Notas
- APK: `app-v6-1.5.0-20261016-161637.apk`

## [1.4.0] — v5 — 2026-10-16
### Adicionado
- **SALVAR / ABRIR** no topo do cabeçalho.
- **SALVAR**: popup para nomear → grava a página em **`Downloads/nexus77`**
  (`Store.kt`; MediaStore na API 29+, arquivo direto antes).
- **ABRIR**: página listando os salvos (`Html.savedListPage`), com bridge
  `Nexus77.openSaved`/`closeList`.
### Notas
- APK: `app-v5-1.4.0-20261016-155925.apk`

## [1.3.0] — v4 — 2026-10-16
### Corrigido
- **Refinamento fora de contexto** ("agora azul" gerava página nova): agora o comando vai
  com o documento em tela (`Html.userPrompt`) e a IA **edita** o atual.
### Adicionado
- Regra de edição contextual no `BASE_PROMPT`; status **"ajustando…"** (vs "gerando…").
### Notas
- APK: `app-v4-1.3.0-20261016-154257.apk`

## [1.2.0] — v3 — 2026-10-16
### Adicionado
- **Linha de sessão** sob o título: modelo ativo (dot), barra de tokens (100k) e botão
  **↺** para reiniciar a sessão.
- **10 temas** e **mapa da UI** (`docs/MAPA_UI.md`) + `AGENTS.md`.
### Notas
- APK: `app-v3-1.2.0-20261016-150921.apk`

## [1.1.0] e anteriores — bootstrap
- Versões iniciais do app: **shell WebView** + geração de HTML por IA (compatível OpenAI),
  limpeza/decoração do documento e tela de **Configurações** (endpoint, API key, modelo,
  temperatura, max tokens). Não documentadas em detalhe aqui.
