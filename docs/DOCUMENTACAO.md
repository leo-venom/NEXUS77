# NEXUS77 — Documentação detalhada

> Versão do documento: v1.10.1 · App: `com.leo.nexus77` · Dono: Leo (PT-BR)

Este é o documento **completo** do projeto: visão geral, fluxos, referência técnica por
arquivo, prompt/IA, temas, armazenamento, build e FAQ. Para o mapa de tela com
`arquivo:linha` + âncora, veja [`MAPA_UI.md`](MAPA_UI.md).

---

## 1. Visão geral

**NEXUS77** é um app Android **nativo (Kotlin, zero dependências externas)** que transforma
um comando curto em uma **página HTML única e viva**, gerada por IA e renderizada em tela
cheia num **WebView**. Não é um chat: é um **gerador de páginas**.

- Você escreve: `criar cubo html 3d`
- O app envia o prompt para uma API **compatível com OpenAI**
- Recebe um HTML *single-file*, injeta o **tema ativo** e renderiza na hora
- Dá para **refinar** ("agora azul"), **salvar/abrir** páginas e escolher o **nível de detalhe**

---

## 2. Requisitos e stack

| Item | Valor |
|---|---|
| Linguagem | Kotlin (kotlin-stdlib) + `org.json` (SDK) |
| Dependências externas | **nenhuma** |
| minSdk / target / compile | **24** / 34 / 34 |
| JDK / AGP | 17 / 8.5.0 |
| Otimização | R8 + `shrinkResources` no release |
| Assinatura | APK Signature **v2 + v3** (v1 desabilitada) |
| Saída | `/home/leo/apk22/app-v{versionCode}-{versionName}-{timestamp}.apk` |

---

## 3. Estrutura do projeto

```
NEXUS77/
├── AGENTS.md                  # guia rápido para agentes/IA
├── README.md                  # visão geral + como usar
├── android/                   # projeto Gradle/Kotlin (com.leo.nexus77)
│   ├── app/build.gradle.kts    # versão, assinatura, R8, buildTypes
│   ├── app/proguard-rules.pro  # keeps da bridge JS e das Activities
│   ├── app/src/main/java/com/leo/nexus77/
│   │   ├── MainActivity.kt      # UI principal + fluxo
│   │   ├── SettingsActivity.kt  # tela de configurações
│   │   ├── AiClient.kt          # HTTP (chat SSE + models)
│   │   ├── Html.kt              # prompts, extração, decoração, páginas
│   │   ├── Store.kt             # páginas salvas (Downloads/nexus77)
│   │   ├── NexusTheme.kt        # 15 temas + helpers de cor
│   │   ├── Prefs.kt             # SharedPreferences "nexus77"
│   │   └── VuMeterView.kt       # medidor VU do carregamento
│   └── app/src/main/res/       # ícones, cores, strings, tema Android
├── web/welcome.html            # protótipo estático (referência; NÃO é o welcome real)
└── docs/
    ├── DOCUMENTACAO.md         # este documento
    ├── ARQUITETURA.md          # visão de componentes e fluxo
    ├── MAPA_UI.md              # mapa de tela (arquivo:linha + âncora)
    ├── ASSINATURA.md           # keystore, build e validação
    └── CHANGELOG.md            # histórico de versões
```

---

## 4. Como funciona (fluxos)

### 4.1 Gerar uma página
```
input (comando) ──▶ MainActivity.sendPrompt()
   ├── system = Html.systemPrompt(tema, detailLevel)      (prompt + detalhe + paleta)
   └── user   = Html.userPrompt(comando, lastRendered)    (contexto, se houver)
        │
        ▼  AiClient.chat(...)   → HTTP POST em /chat/completions com streaming (SSE)
   resposta ──▶ Html.extractDocument()      (tira cercas markdown, corta até <!DOCTYPE/<html)
        ├── lastRendered = doc (cru)        (fonte da verdade da página)
        └── Html.decorate(tema, doc) ─▶ Html.show(web, html)
   status: "✓ 8 KB • 2.1s • modelo • ↑1.2k ↓8.4k tok"
```

### 4.2 Refinar (contexto)
Com uma página em tela (`lastRendered != null`), o comando seguinte vai **com o documento
anexado** (`Html.userPrompt`): a IA trata como **edição** e devolve o documento completo.
Status mostra **"ajustando…"**; sem página em tela, **"gerando…"**.

### 4.3 Tema
O `accent`/paleta do tema entram **no prompt** (cores exigidas) **e** no CSS injetado
(`--nx-*`). Trocar de tema **re-decora** a página atual sem chamar a IA (`onResume`).

### 4.4 Sessão (modelo / tokens / reset)
Linha sob o título: dot de status, **modelo ativo**, **barra de tokens** (`total/1M`),
rótulo `↑entrada ↓saída · total/1M` e botão **↺** (zera a sessão). Durante a geração os
tokens aparecem **estimados** (`~`, ≈ bytes/4); ao final, se a API informar `usage`, o valor
**exato** substitui (sem `usage`, permanece só o que foi contado).

### 4.5 Carregamento (VU meter + KB + tokens ao vivo)
Durante a geração, um **VU meter** (10 cubos) aparece **antes** do texto. O rótulo mostra o
**tamanho real do documento** em formação (`B/KB/MB`) — bytes **decodificados** de
`delta.content` — batendo com o "✓ X KB" final. Os **tokens** aparecem estimados
(`~↑ / ~↓ · total/1M`) durante a geração e viram o valor **exato** da API (`usage`) ao final.

### 4.6 Salvar / Abrir / Excluir
- **SALVAR** (topo): popup para nomear → grava o HTML da tela em **`Downloads/nexus77`**.
- **ABRIR** (topo): página com a lista dos salvos (nome, tamanho, data) → toque reabre.
- **Abrir HTML…** (na página Salvos, à direita do ‹ Voltar): abre o **seletor de arquivos do
  celular** (SAF) para carregar qualquer `.html` de fora do app.
- **Excluir** (✕ por item): pede **confirmação** e remove o arquivo (MediaStore ou arquivo direto).
- Sobrescreve o mesmo nome; o arquivo salvo é a página **já decorada** (abre em qualquer navegador).

---

## 5. Arquitetura de componentes

| Arquivo | Responsabilidade |
|---|---|
| `MainActivity.kt` | Tela principal: cabeçalho (SALVAR/ABRIR), VU meter/KB, sessão, WebView, barra de comando. Orquestra o fluxo. |
| `SettingsActivity.kt` | Configurações: endpoint, API key, modelo, temperatura, **nível de detalhe**, max tokens, tema (ESCUROS/CLAROS). |
| `AiClient.kt` | HTTP via `HttpURLConnection`: `chat()` (streaming SSE + fallback) e `listModels()`. Lança `AiException`. |
| `Html.kt` | Prompts (`systemPrompt`/`detailDirective`/`userPrompt`), limpeza (`extractDocument`), decoração (`decorate`), páginas (`welcomePage`/`errorPage`/`savedListPage`). |
| `Store.kt` | Páginas salvas em `Downloads/nexus77`: MediaStore (API 29+) ou arquivo direto (API < 29). |
| `VuMeterView.kt` | Medidor VU (10 cubos, degradê, zigue-zague). |
| `NexusTheme.kt` | 15 temas + helpers (`hex`, `hexA`, `rounded`, `onAccent`). |
| `Prefs.kt` | Preferências em SharedPreferences `"nexus77"`. |

---

## 6. Referência técnica por arquivo

> Linhas são aproximadas e mudam a cada versão; para `arquivo:linha` exatos use o
> [`MAPA_UI.md`](MAPA_UI.md).

### MainActivity.kt (~770 linhas)
- Campos de UI: `titleBar`, `titleText`, `saveBtn`, `openBtn`, `statusRow`, `vuMeter`,
  `kbLabel`, `statusText`, `sessionRow`, `dotView`, `modelText`, `tokenBar`, `tokenLabel`,
  `resetBtn`, `web`, `inputBar`, `input`, `sendBtn`, `gearBtn`.
- Estado: `lastRendered` (doc cru atual), `lastPrompt`, `generating`, `showingList`.
- Ciclo: `onCreate` (build UI + tema) · `onResume` (re-aplica tema; re-decora se mudou).
- Métodos-chave: `buildUi`, `applyTheme`, `sendPrompt`, `failGeneration`,
  `finishGeneration`, `setLoading`, `kbText`, `showWelcome`, `resetSession`,
  `updateSessionUi`, `formatTokens`, `headerButton`, `requestSave`, `showSaveDialog`,
  `savePage`, `openSavedList`, `openSaved`, `closeSavedList`, `defaultSaveName`,
  `withStorage`, `onRequestPermissionsResult`, `handleUrl`, `NxBridge`, `onBackPressed`.

### SettingsActivity.kt (~369 linhas)
- Campos: `endpointField`, `keyField`, `showKey`, `modelField`, `fetchButton`, `tempLabel`,
  `tempBar`, `detailRadios`, `tokensField`, `themeRadios`.
- `fetchModels()` → `AiClient.listModels`; `showModelsDialog()`.
- `save()` grava em `Prefs` (`themeIndex` lido pelo **id** do radio; `detailLevel`) e re-aplica tema.

### AiClient.kt (~230 linhas)
- `chat(endpoint, apiKey, model, temperature, maxTokens, system, user, onProgress)`.
- **Streaming**: envia `stream:true` + `stream_options.include_usage`; `parseChat` lê SSE
  (`data:` com `choices[0].delta.content`) e captura `usage` no fim.
- **Fallback**: em `HTTP 4xx`, repete **sem streaming** (`parseNonStream`).
- `read()` conta os bytes lidos e chama `onProgress(total)`.
- Timeouts: connect **20s** · chat **600s** · models **60s**.
- `ChatResult(content, promptTokens, completionTokens, totalTokens)`.

### Html.kt (~293 linhas)
- `BASE = "https://nexus77.local/"` (base do `loadDataWithBaseURL`).
- `BASE_PROMPT`: regras do gerador (HTML puro, single-file, three.js opcional, pt-BR,
  edição contextual).
- `systemPrompt(tema, detailLevel)`: BASE_PROMPT + `detailDirective` + paleta do tema.
- `detailDirective(level)`: 0 Simples · 1 Médio (nenhuma) · 2 Rico.
- `userPrompt(comando, currentDocument)`: anexa a página em tela quando há refinamento.
- `extractDocument`, `decorate` (injeta viewport + `--nx-*`), `show`.
- `welcomePage`, `errorPage`, `savedListPage`; `esc`/`escAttr`.

### Store.kt (~175 linhas)
- `FOLDER = "nexus77"`, `Item(name, size, modified)`, `fileName()` (normaliza e força `.html`).
- `save()`/`list()`/`read()` — MediaStore (`MediaStore.Downloads`, `RELATIVE_PATH`) na
  API 29+; arquivo direto em `getExternalStoragePublicDirectory(DOWNLOADS)` na API < 29.
- `needsPermission()` (só API < 29).

### VuMeterView.kt (~82 linhas)
- 10 barras, `LinearGradient` por barra, animação em onda triangular (zigue-zague),
  `start()`/`stop()`.

### NexusTheme.kt (~118 linhas)
- `data class NexusTheme(name, background, surface, surfaceAlt, accent, text, secondary, isDark)`.
- Helpers: `onAccent`, `hex`, `hexA`, `rounded`.
- `ALL` (15 temas) e `of(index)`.

### Prefs.kt (~90 linhas)
- SharedPreferences `"nexus77"`; chaves: `endpoint`, `api_key`, `model`, `temperature`,
  `max_tokens`, `theme`, `detail_level`, `session_tokens`, `session_prompt_tokens`,
  `session_completion_tokens`.
- `addSessionTokens(prompt, completion)`; `SESSION_TOKEN_BUDGET = 1_000_000`.

---

## 7. Prompt e IA

### O que é enviado (`system`)
`BASE_PROMPT` (regras do gerador) **+** `detailDirective(detailLevel)` **+** paleta do tema
(`background`, `accent`, `text`, `secondary`, `surface`, `surfaceAlt`).

### O que é enviado (`user`)
- Comando cru (geração nova), **ou**
- `Current page (the HTML document currently displayed on screen):` + documento em
  ```` ```html ... ``` ```` + `Command: <comando>` (refinamento).

### Payload HTTP
```json
{ "model": "...", "temperature": 0.7, "max_tokens": 4096,
  "stream": true, "stream_options": { "include_usage": true },
  "messages": [ {"role":"system","content":"..."}, {"role":"user","content":"..."} ] }
```
- `max_tokens` omitido quando 0 (sem limite).
- `Authorization: Bearer <apiKey>`.
- Resposta **SSE** (`text/event-stream`): `data:` com deltas; `data: [DONE]` no fim.

### Papel dos parâmetros
| Parâmetro | Efeito |
|---|---|
| `temperature` | 0 = previsível · 2 = criativo (padrão 0.7) |
| `max_tokens` | teto da resposta (0 = sem limite) |
| `detailLevel` | diretriz de complexidade (Simples/Médio/Rico) |
| `model` | capacidade/consistência do resultado |

> A "complexidade" do resultado depende de: interpretação do prompt + aleatoriedade
> (temperature) + técnica escolhida (CSS 3D vs three.js) + capacidade do modelo. O
> **Nível de detalhe** e uma `temperature` baixa reduzem a variação.

---

## 8. Temas (15 = 12 escuros + 3 claros)

Agrupados na tela de Configurações por `isDark` (**ESCUROS** / **CLAROS**). Índices
preservados por versão (novos temas entram no fim de `ALL`).

| # | Tema | Tipo | # | Tema | Tipo |
|---|---|---|---|---|---|
| 0 | NEXUS · ciano | escuro | 8 | GELO · azul claro | **claro** |
| 1 | MATRIX · verde | escuro | 9 | MAGMA · vermelho | escuro |
| 2 | NEON · roxo | escuro | 10 | GRAFITE · cinza | escuro |
| 3 | OCEAN · azul | escuro | 11 | ÍNDIGO · violeta | escuro |
| 4 | CLARO · branco | **claro** | 12 | ESMERALDA · verde | escuro |
| 5 | SUNSET · laranja | escuro | 13 | VINHO · carmesim | escuro |
| 6 | SAKURA · rosa | **claro** | 14 | CAFÉ · marrom | escuro |
| 7 | GOLD · dourado | escuro | | | |

---

## 9. Configurações (tela)

| Campo | Chave (`Prefs`) | Padrão | Observação |
|---|---|---|---|
| Endpoint | `endpoint` | `https://api.openai.com/v1/chat/completions` | precisa começar com `http` |
| API key | `api_key` | vazio | obrigatória para gerar |
| Modelo | `model` | `gpt-4o-mini` | botão "Buscar modelos" consulta `/models` |
| Temperatura | `temperature` | `0.7` | 0.0–2.0 |
| **Nível de detalhe** | `detail_level` | `1` (Médio) | Simples/Médio/Rico |
| Max tokens | `max_tokens` | `0` | 0 = sem limite |
| Tema | `theme` | `0` (NEXUS) | agrupado ESCUROS/CLAROS |

---

## 10. Armazenamento e dados

### Preferências (SharedPreferences `"nexus77"`)
Todas as chaves da tabela acima + `session_tokens`, `session_prompt_tokens`,
`session_completion_tokens`. **Nota:** a API key é guardada em **texto puro** (o app é
zero-deps; `allowBackup="false"` evita vazamento por backup).

### Páginas salvas — `Downloads/nexus77`
- **API 29+**: `MediaStore.Downloads` (`RELATIVE_PATH=Download/nexus77/`) — **sem permissão**.
- **API < 29**: arquivo direto na pasta pública — pede `WRITE/READ_EXTERNAL_STORAGE`
  (declaradas com `maxSdkVersion="28"`).
- Nome: `Store.fileName()` normaliza e força `.html`; salvar com o mesmo nome **sobrescreve**.
- O arquivo salvo é o HTML **decorado** (abre standalone em navegador).

---

## 11. Permissões e segurança

| Permissão | Uso |
|---|---|
| `INTERNET` | chamadas à API |
| `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`) | salvar em Downloads (só API < 29) |
| `READ_EXTERNAL_STORAGE` (`maxSdkVersion=28`) | listar/abrir salvos (só API < 29) |

Mitigações já presentes: `allowBackup="false"` · `allowFileAccess=false` ·
`allowContentAccess=false` · keystore e `keystore.properties` com `chmod 600` ·
`javaScriptCanOpenWindowsAutomatically=false`.

**Atenção:** `usesCleartextTraffic="true"` (permite `http://`); se o endpoint for HTTP, a
API key trafega em claro. Recomenda-se usar **HTTPS** no endpoint.

---

## 12. Bridge JS

`WebView.addJavascriptInterface(NxBridge(), "Nexus77")` — métodos chamáveis pela página:

| Método | Uso |
|---|---|
| `setInput(text)` | devolve texto ao campo de comando (chips do welcome) |
| `openSaved(name)` | abre uma página salva (a partir da lista "Abrir") |
| `closeList()` | volta da lista de salvos para a página atual |

---

## 13. Build, assinatura e entrega

```bash
cd android
./gradlew assembleDebug     # iteração (sem R8) — segundos
./gradlew assembleRelease   # entrega (R8 + shrink + assinatura v2/v3)
```

Entrega (regra do projeto):
1. limpar `*.apk` antigos de `/home/leo/apk22/` (**preservar `122.jpg`**):
   `find /home/leo/apk22 -maxdepth 1 -name '*.apk' -delete`
2. copiar o release como `app-v{versionCode}-{versionName}-{timestamp}.apk`
3. validar:
   ```bash
   BT=~/Android/Sdk/build-tools/34.0.0
   $BT/apksigner verify -v <apk>      # esperado: v2 true, v3 true, 1 signer
   $BT/aapt dump badging <apk>        # versionCode/versionName corretos
   ```

Detalhes do keystore em [`ASSINATURA.md`](ASSINATURA.md).

---

## 14. Troubleshooting / FAQ

**O medidor de KB fica em 0.0 durante a geração.**
Provavelmente o endpoint não suporta streaming → o app cai no fallback não-streaming e os
bytes só chegam no fim. Verifique se o provedor aceita `stream:true`.

**Os tokens (↑↓ / 1M) não somam.**
A API não retornou `usage`. Sem `usage` (ou sem `include_usage` no streaming), nada é
contado.

**"Configure sua API key em ⚙".**
Falta a API key em Configurações.

**Falha ao salvar / listar.**
Em API < 29 é preciso conceder a permissão de armazenamento. Verifique também se a pasta
`Downloads/nexus77` é gravável.

**A página gerada não abre em outro app.**
A página é HTML single-file; abre em qualquer navegador. Recursos de CDN (ex.: three.js)
precisam de internet.

**Build lento/demorado.**
Não use `--no-daemon` nem `clean` por hábito. Itere com `assembleDebug`.

---

## 15. Limitações conhecidas / roadmap

- Sem `seed`/`top_p` → resultados variam entre execuções (a "complexidade" oscila).
- Tokens dependem do `usage` do provedor.
- `decorate` não é idempotente: abrir um salvo (já decorado) e trocar tema injeta um
  segundo `<style id="nx77-base">` (visualmente inofensivo).
- WebView sem `onDestroy`/`destroy()` explícito.
- `usesCleartextTraffic="true"` (ver §11).
- Strings hardcoded (só `app_name` em `strings.xml`); sem i18n.
- Sem testes automatizados.

---

## 16. Glossário

- **Refinamento**: reenviar um comando com a página atual em tela para a IA **editar**.
- **lastRendered**: documento HTML **cru** da página atual (base para decoração e refinamento).
- **Decorar**: injetar viewport + CSS do tema (`--nx-*`) no HTML gerado.
- **SSE**: *Server-Sent Events* — protocolo de streaming usado pelo `chat`.
- **VU meter**: medidor animado (10 cubos) do carregamento.
- **Budget (1M)**: escala visual da barra de tokens da sessão (não é limite).
