# NEXUS77 — Arquitetura

App Android nativo (Kotlin, zero dependências externas). Um comando curto → a IA
gera um HTML single-file → o WebView renderiza em tela cheia.

## Componentes

| Arquivo | Responsabilidade |
|---|---|
| `MainActivity.kt` | Tela principal: título + botões **SALVAR/ABRIR**, **VU meter + KB** no status, **linha de sessão** (modelo ativo, barra de tokens, reset), WebView, barra de input + engrenagem. Orquestra o fluxo. |
| `SettingsActivity.kt` | Configurações: endpoint, API key, busca de modelos, temperatura, **nível de detalhe**, tema. |
| `AiClient.kt` | HTTP via `HttpURLConnection`: `chat()` em **streaming SSE** (progresso em bytes; fallback não-streaming) → `ChatResult` (conteúdo + tokens de usage) e `listModels()`. Lança `AiException`. |
| `Html.kt` | Prompt de sistema (+ `detailDirective` por nível de detalhe), mensagem do usuário com **página em tela** (`userPrompt`, refinamento), limpeza (`extractDocument`) e decoração (`decorate`) do HTML, welcome/error pages. |
| `NexusTheme.kt` | Paletas dos 15 temas (12 escuros + 3 claros) + helpers de cor/UI (`hex`, `hexA`, `rounded`, `dp`). |
| `Prefs.kt` | SharedPreferences `"nexus77"`: endpoint, key, modelo, temperatura, maxTokens, themeIndex, `detailLevel` (0/1/2), e consumo da sessão — `sessionTokens` (+ `sessionPromptTokens` / `sessionCompletionTokens`) com `SESSION_TOKEN_BUDGET` (1M). |
| `Store.kt` | Páginas salvas em `Downloads/nexus77`: MediaStore (API 29+) ou arquivo direto (API < 29); `save` / `list` / `read`. |
| `VuMeterView.kt` | Medidor VU (10 cubos, degradê, zigue-zague) exibido ao lado do status durante o carregamento. |

## Fluxo principal

```
comando do usuário
  → MainActivity.sendPrompt()
  → Html.systemPrompt(theme) + Html.userPrompt(comando, lastRendered) → AiClient.chat(...)
  → resposta: 1 HTML cru
  → Html.extractDocument()   (remove cercas de código, corta antes de <!DOCTYPE/<html)
  → lastRendered (guarda o doc cru, não decorado)
  → Html.decorate(theme)     (injeta CSS vars --nx-* + superfícies currentColor)
  → WebView.loadDataWithBaseURL("https://nexus77.local/", html, "text/html", "UTF-8", null)
  → shouldOverrideUrlLoading: nexus77.local/about: → interno; senão abre no navegador
```

- Sucesso → status `✓ <KB> KB • <s>s • <modelo>` (+ ` • X tok` quando a API
  informa usage); erro → `✗ <mensagem>` (+ página de erro se nada foi renderizado).
- **Refinamento (v1.3.0)**: havendo página em tela (`lastRendered != null`), o comando
  vai com o documento anexado (`Html.userPrompt`) — o modelo edita o atual e devolve o
  documento completo; status mostra `ajustando…` (página nova = `gerando…`).
- `onResume`: se o tema mudou, re-decorar `lastRendered` sem chamar a IA de novo.
- Durante o `chat`, `AiClient` reporta os **bytes baixados** (`onProgress`) → o cabeçalho
  atualiza o contador de **KB** e anima o **VU meter** (`VuMeterView`).

## Sessão (modelo / tokens / reset)

- Linha sob o título, criada em `buildUi` e repintada em `applyTheme()`:
  dot colorido (accent se há key configurada), nome do modelo, `ProgressBar`
  fina (tokens usados / **1M**) e botão **↺**.
- `AiClient.chat()` retorna `ChatResult` com `usage` (`prompt_tokens`,
  `completion_tokens`, `total`); `prefs.addSessionTokens(prompt, completion)`
  acumula **entrada/saída** e o total; `updateSessionUi()` redesenha a barra e o
  rótulo (`↑entrada ↓saída · total/1M`).
- **↺** (`resetSession`): zera a contagem, limpa `lastRendered`, volta à página
  inicial e mostra toast; bloqueado durante geração em andamento.

## Salvar / abrir (Downloads/nexus77)

- **SALVAR**: grava o documento em tela (HTML já decorado por `Html.decorate`) em
  `Downloads/nexus77`; popup (`AlertDialog` + `EditText`) para nomear
  (`Store.fileName` normaliza e força `.html`). API 29+ usa **MediaStore** (sem
  permissão); API < 29 grava direto na pasta pública (pede WRITE/READ_EXTERNAL_STORAGE).
- **ABRIR**: lista os salvos (`Store.list`, mais recentes primeiro) numa página própria
  (`Html.savedListPage`); tocar num item chama `Nexus77.openSaved(nome)` → `Store.read`
  → carrega no WebView e define `lastRendered`. `Nexus77.closeList()`/Voltar retorna à
  página atual.

## Bridge JS

- Nome: `"Nexus77"` — `WebView.addJavascriptInterface(NxBridge(), "Nexus77")`.
- Métodos: `setInput(text)` (devolve texto ao input), `openSaved(nome)` (abre um salvo)
  e `closeList()` (volta da lista de salvos).
- A página gerada pode chamar `Nexus77.setInput(...)` para devolver texto ao input.
- `web/welcome.html` usa o bridge com fallback para clipboard quando aberto fora do app.

## Temas

- `NexusTheme.ALL` (15: 12 escuros + 3 claros): escuros NEXUS·ciano, MATRIX·verde,
  NEON·roxo, OCEAN·azul, SUNSET·laranja, GOLD·dourado, MAGMA·vermelho, GRAFITE·cinza,
  ÍNDIGO·violeta, ESMERALDA·verde, VINHO·carmesim, CAFÉ·marrom; claros CLARO·branco,
  SAKURA·rosa, GELO·azul claro. Na tela são agrupados em ESCUROS/CLAROS (`isDark`).
- CSS vars injetadas: `--nx-bg`, `--nx-surface`, `--nx-surface-alt`, `--nx-accent`,
  `--nx-accent-soft`, `--nx-accent-faint`, `--nx-text`, `--nx-secondary`.
- Superfícies usam `currentColor` nos detalhes para os acentos funcionarem em qualquer paleta.

## Rede

- `AiClient` timeouts: conexão 20 s / leitura do chat 10 min / modelos 60 s.
- Payload: `{model, temperature, max_tokens?, stream:true, stream_options:{include_usage:true}, messages:[system,user]}` com Bearer token.
- Resposta (SSE): linhas `data:` com `choices[0].delta.content` concatenadas + `usage` no fim → HTML. Se o provedor recusar streaming (HTTP 4xx), repete sem stream (`choices[0].message.content`).

## Decisões

- **Zero dependências**: `android.app.Activity` + UI programática + `org.json`
  (kotlin-stdlib vem do plugin Kotlin). Builds de segundos, sem kapt/KSP.
- **HTML single-file com CDN permitido** (three.js unpkg 0.160.0): visual rico sem
  empacotar assets no APK.
- **Tema injetado no HTML** (não só no WebView): a página conversa com a paleta ativa.
