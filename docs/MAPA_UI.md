# Mapa da UI — NEXUS77

> **Estado:** v1.10.1 (versionCode 14, **15 temas**). Linhas referem-se a esta versão e
> são aproximadas — o que não muda é a **âncora** (trecho de código único). Se a linha
> divergir, busque pela âncora.
>
> **Novidades:** v1.9.0 — **15 temas** ESCUROS/CLAROS. v1.9.2 — **KB real** + **tokens ao
> vivo (`~`)**. v1.10.0 — Salvos: **abrir HTML do celular** + **excluir**. v1.10.1 —
> status **alinhado** (esquerda; linha de render à **direita**).
>
> **Regra de ouro:** pedido pontual (ex.: "mudar cor do cubo") = **edição cirúrgica no
> ponto exato**. Nunca reescrever o arquivo inteiro.

## Telas / páginas

| Tela | Onde vive | Conteúdo |
|---|---|---|
| Principal | `MainActivity.kt` — `buildUi()` :111-286 | título + SALVAR/ABRIR, VU meter + KB real no status, sessão, WebView, barra de comando |
| Configurações | `SettingsActivity.kt` — `buildUi()` :55-232 | API, geração (temperatura + nível de detalhe), tema (agrupado ESCUROS/CLAROS), salvar |
| Welcome (WebView) | `Html.kt` — `welcomePage()` :105-166 | cubo 3D, título, aviso, chips |
| Erro (WebView) | `Html.kt` — `errorPage()` :169-196 | mensagem de falha |
| Documento gerado | `Html.kt` — `decorate()` :77-98 + `show()` :100-102 | HTML da IA + CSS do app |
| Salvos (WebView) | `Html.kt` — `savedListPage()` :202-260 | lista de `Downloads/nexus77` (toque abre) |

`web/welcome.html` é **referência estática** (protótipo). Não editar para mudar o app.

---

## MainActivity.kt (899 linhas)

```
+--------------------------------------------+
| NEXUS77                                    |  titleBar :116-203
| (o) modelo atual     ↑12.4k ↓8.1k    [R] |    titleText :120-125, saveBtn :126, openBtn :127, statusRow(meter/KB/status) :128-151
|                                            |    sessionRow :152-200
+--------------------------------------------+      dotView :157-159, modelText :160-165,
|                                            |      tokenBar :166-169, tokenLabel :170-174,
|                 WebView                    |      resetBtn :175-179
|                                            |  web :205-234
+--------------------------------------------+
| [ Ex: criar cubo html 3d       ] [>] [gear]|  inputBar :236-240/:275-279
+--------------------------------------------+    input :241-255, sendBtn :256-262, gearBtn :263-268
```

### Elementos

| Elemento | Descrição | Linha | Âncora |
|---|---|---|---|
| titleBar | barra do topo (vertical) | :116-203 | `titleBar` |
| titleText | "NEXUS77" na tela | :120-125 | `titleText` |
| saveBtn | "SALVAR" — salva a página (popup) | :126 | `saveBtn` |
| openBtn | "ABRIR" — lista os salvos | :127 | `openBtn` |
| statusRow | linha de status: VU meter + KB + texto | :145-151 | `statusRow` |
| vuMeter | VU meter (10 cubos, degradê, zigue-zague) | :136-138 | `vuMeter` |
| kbLabel | tamanho real do documento em formação (accent/negrito) | :139-144 | `kbLabel` |
| statusText | status; alinhado à esquerda (render à direita) | :128-135 | `statusText` |
| sessionRow | linha de sessão (horizontal) | :152-200 | `sessionRow` |
| dotView | bolinha de status | :157-159 | `dotView` |
| modelText | modelo atual | :160-165 | `modelText` |
| tokenBar | barra de progresso de tokens | :166-169 | `tokenBar` |
| tokenLabel | sessão: `(~)↑entrada ↓saída · total/1M` | :170-174 | `tokenLabel` |
| resetBtn | "↺" zera contador da sessão | :175-179 | `resetBtn` |
| web | WebView (welcome / doc / erro / salvos) | :205-234 | `web` |
| input | campo de comando (hint) | :241-255 | `input` (hint :242) |
| sendBtn | botão enviar (`ic_send`) | :256-262 | `sendBtn` |
| gearBtn | abre Configurações (`ic_gear`) | :263-268 | `gearBtn` |

### Métodos principais

| Método | Papel | Linha |
|---|---|---|
| `onCreate` / `onResume` | setup / re-aplica tema e sessão | :86+ / :95+ |
| `buildUi` | constrói toda a tela (SALVAR/ABRIR + VU meter) | :111-286 |
| `applyTheme` | aplica cores do tema (título, botões, meter, fundos, sistema) | :288-341 |
| `sendPrompt` | comando + página em tela (`Html.userPrompt`) → `AiClient.chat` (streaming/progresso) → `extractDocument` → `decorate` → WebView | :343-426 |
| `failGeneration` | status de erro (cor `errorColor` :541) | :428-437 |
| `finishGeneration` | status ok + `setLoading(false)` | :439-449 |
| `setLoading` / `kbText` / `onGenProgress` | VU meter + **KB real** do documento + tokens `~` ao vivo | :451-482 |
| `showWelcome` | carrega `welcomePage` no WebView | :483-492 |
| `resetSession` | zera contadores da sessão (total/entrada/saída) | :498-511 |
| `updateSessionUi` | modelText + barra + rótulo `(~)↑entrada ↓saída · total/1M` | :513-532 |
| `formatTokens` | formata 1.2k / 1M | :534-539 |
| `barDrawable` / `dotDrawable` | drawables da barra/bolinha | :544-557 |
| `headerButton` / `headerBtnParams` | botões do cabeçalho (SALVAR/ABRIR) | :561-573 |
| `requestSave` / `showSaveDialog` | salvar: valida + popup de nome | :577-629 |
| `savePage` | grava em `Downloads/nexus77` (`Store.save`) | :631-651 |
| `openSavedList` | página "Salvos" (`Store.list` + `Html.savedListPage`) | :653-678 |
| `openSaved` | abre um salvo (`Store.read` → WebView) | :680-702 |
| `closeSavedList` | volta da lista para a página atual | :704-717 |
| `pickHtmlFile` / `onActivityResult` / `openPickedFile` | abrir HTML do celular (SAF) | :719-763 |
| `deleteSaved` | exclui um salvo (com confirmação) | :765-785 |
| `defaultSaveName` | nome sugerido (prompt + timestamp) | :787-795 |
| `withStorage` / `onRequestPermissionsResult` | permissão de armazenamento (API < 29) | :797-830 |
| `handleUrl` | links externos (abre fora) | :832-841 |
| `NxBridge` | ponte JS: `setInput`, `openSaved`, `closeList`, `pickFile`, `deleteSaved` | :843-879 |
| `onBackPressed` | volta da lista / back no WebView | :881-887 |

---

## SettingsActivity.kt (369 linhas)

```
+----------------------------------+
| Configurações                    |  header :64-71
| -- API --                        |
| [Endpoint              ]         |  endpointField :74-80
| [API key       ] [x] mostrar     |  keyField :81-87, showKey :89-106
| -- GERAÇÃO --                    |
| [Modelo        ] [Buscar]        |  modelField :109, fetchButton :111-120
| Temperatura [====|=====] 0.7     |  tempLabel :122-129, tempBar :131-147
| Nível de detalhe (o)( )( )       |  detailRadios :149-172 (Simples/Médio/Rico)
| [Max tokens            ]         |  tokensField :175-181
| -- TEMA --                       |
|   ESCUROS                        |  addThemeGroup :188-212
|   (o) NEXUS  ( ) MATRIX  ...     |  themeRadios escuros :213
|   CLAROS                         |
|   ( ) CLARO  ( ) SAKURA ...      |  themeRadios claros :214
| [ Salvar ]                       |  saveButton :217-227
+----------------------------------+
```

| Método | Papel | Linha |
|---|---|---|
| `fetchModels` | busca modelos no endpoint e preenche o campo | :236-269 |
| `showModelsDialog` | diálogo de escolha de modelo | :271-284 |
| `save` | grava em `Prefs` (`themeIndex` via id, `detailLevel`) e re-aplica tema | :286-306 |
| `applySystemBars` | status/nav bar na cor do tema | :308-325 |
| `section` / `addField` | helpers de layout | :327-334 / :336-362 |

---

## Html.kt (305 linhas)

| Item | Papel | Linha |
|---|---|---|
| `BASE` | base URL do `loadDataWithBaseURL` (`https://nexus77.local/`) | :12 |
| regexes | extração do HTML da resposta da IA | :14-16 |
| `BASE_PROMPT` | prompt do sistema (HTML puro, three.js CDN 0.160.0, pt-BR, regra de **edição contextual**) | :18-31 |
| `systemPrompt` | prompt + **nível de detalhe** + regras do tema | :34-45 |
| `detailDirective` | diretriz por nível (0 simples / 1 médio / 2 rico) | :47-52 |
| `userPrompt` | mensagem do usuário; **anexa a página em tela** quando há refinamento | :54-69 |
| `extractDocument` | tira o HTML do texto da resposta | :72-81 |
| `decorate` | injeta CSS do app no documento gerado | :85-106 |
| `show` | carrega doc no WebView | :108-110 |
| `welcomePage` | página inicial | :113-174 |
| `errorPage` | página de erro | :177-204 |
| `SavedEntry` | item da página "Abrir" (nome + meta) | :207 |
| `savedListPage` | página "Salvos": abrir/excluir itens + **abrir HTML do celular** | :210-280 |
| `varsCss` | CSS vars `--nx-*` do tema | :270-281 |
| `baseCss` | CSS base do documento gerado | :283-288 |
| `esc` / `escAttr` | escape de HTML / de valor de atributo | :290 / :292 |

Detalhe da `welcomePage`: vars do tema :114-120 · chips :121-127 · HTML :129-173 ·
**cubo 3D CSS :139-148** (`.scene`/`.cube`/`.face`, cores via interpolação `$accent`) ·
título `h1` :149 · script de giro :163-171.

---

## Store.kt (201 linhas)

Páginas salvas em `Downloads/nexus77` (API 29+ = **MediaStore**, sem permissão;
API < 29 = arquivo direto + permissão de armazenamento).

| Item | Papel | Linha |
|---|---|---|
| `FOLDER` / `Item` | pasta "nexus77" / item (nome+tamanho+data) | :24 / :28 |
| `needsPermission` | precisa pedir permissão? (só API < 29) | :31-35 |
| `fileName` | normaliza o nome digitado e força `.html` | :37-45 |
| `meta` | linha secundária do card (tamanho · data) | :47-51 |
| `save` / `list` / `read` | grava / lista / lê | :60-64 / :66-67 / :69-70 |
| `delete` | remove um salvo | :73-74 |
| `saveMedia` / `listMedia` / `readMedia` / `deleteMedia` | caminho MediaStore (API 29+) | :80-113 / :115-136 / :138-160 / :180-195 |
| `saveLegacy` / `listLegacy` / `readLegacy` / `deleteLegacy` | caminho direto (API < 29) | :162-166 / :168-173 / :175-178 / :197-199 |

---

## VuMeterView.kt (108 linhas)

Medidor estilo VU no cabeçalho, ativo durante o carregamento (trilho + degradê + "cap").

| Item | Papel | Linha |
|---|---|---|
| `barCount = 10` | 10 cubos (barras) | :20 |
| `setColors(strong, dim, track)` | cor do degradê (topo/base) e do trilho | :43-47 |
| `start` / `stop` | liga/desliga a animação (e a visibilidade) | :50-62 |
| `onDraw` | trilho + onda suave (soma de senos) + degradê base→topo + cap aceso | :68-107 |

Uso: `MainActivity.buildUi` cria em `statusRow` (`:137`) com altura `dp(20)`;
`applyTheme` (`MainActivity.kt` :280) define as cores.

---

## Temas — NexusTheme.kt (118 linhas)

- `data class` :11-20 · `onAccent` :22 · `hex` :24 · `hexA` :27-28 · `rounded` :30-35
- `ALL` (usada pelos radios e por `Prefs.themeIndex`) :38-114 · `of()` :116
- Na tela de Configurações os radios são agrupados por `isDark`: **ESCUROS** e **CLAROS**.

| # | Tema | Linha | Tipo |
|---|---|---|---|
| 0 | NEXUS · ciano | :39-43 | escuro |
| 1 | MATRIX · verde | :44-48 | escuro |
| 2 | NEON · roxo | :49-53 | escuro |
| 3 | OCEAN · azul | :54-58 | escuro |
| 4 | CLARO · branco | :59-63 | **claro** |
| 5 | SUNSET · laranja | :64-68 | escuro |
| 6 | SAKURA · rosa | :69-73 | **claro** |
| 7 | GOLD · dourado | :74-78 | escuro |
| 8 | GELO · azul claro | :79-83 | **claro** |
| 9 | MAGMA · vermelho | :84-88 | escuro |
| 10 | GRAFITE · cinza | :89-93 | escuro |
| 11 | ÍNDIGO · violeta | :94-98 | escuro |
| 12 | ESMERALDA · verde | :99-103 | escuro |
| 13 | VINHO · carmesim | :104-108 | escuro |
| 14 | CAFÉ · marrom | :109-113 | escuro |

---

## Prefs.kt (90 linhas)

`sp` (SharedPreferences `nexus77`) :13-14 · `SESSION_TOKEN_BUDGET = 1_000_000` :88.

| Propriedade | Linha |
|---|---|
| `endpoint` | :16-18 |
| `apiKey` | :20-22 |
| `model` | :24-26 |
| `temperature` | :28-30 |
| `maxTokens` | :32-34 |
| `themeIndex` | :36-38 |
| `detailLevel` | :41-44 |
| `sessionTokens` | :46-49 |
| `sessionPromptTokens` | :51-54 |
| `sessionCompletionTokens` | :56-59 |
| `addSessionTokens(prompt, completion)` | :61-66 |

---

## Recursos estáticos e build

| Item | Arquivo |
|---|---|
| Ícone launcher | `res/mipmap-anydpi-v26/ic_launcher.xml` + `res/mipmap/ic_launcher.xml` + `res/drawable/ic_launcher_foreground.xml` |
| Ícone enviar | `res/drawable/ic_send.xml` |
| Ícone engrenagem | `res/drawable/ic_gear.xml` |
| Cores / tema Android / app_name | `res/values/colors.xml` · `res/values/themes.xml` · `res/values/strings.xml` (:3) |
| Manifest | label :10, theme :12; MainActivity :15-24, SettingsActivity :26-30 |
| `app/build.gradle.kts` | versionCode 14 :23 · versionName "1.10.1" :24 · signing v2/v3 :40-42 · release R8 :47-56 · fast :61-69 |

---

## Receitas de mudança

### 1. Mudar a cor do cubo (welcome)
- **Só o cubo:** `Html.kt` `welcomePage` — bloco CSS do cubo **~:139-148**; trocar o
  uso de `$accent` por cor fixa. (Chips :121-127 continuam no accent do tema.)
- **App inteiro (cubo + título + botões + chips + WebView):** alterar o `accent` do
  tema em `NexusTheme.kt` (ex.: NEXUS :39-43 `accent = 0xFF00E5FF`; MATRIX :44-48
  `0xFF00FF41`). `applyTheme` (`MainActivity.kt` :280-327) propaga para título
  e botão enviar; `varsCss` (`Html.kt` :270-281) propaga para o WebView.

### 2. Trocar o placeholder do campo de comando
`MainActivity.kt` :194 — hint "Ex: criar cubo html 3d".

### 3. "NEXUS77" (título)
- na tela: `MainActivity.kt` :98-103 · welcome: `Html.kt` :123 · app/launcher: `strings.xml` :3.

### 4. Adicionar tema novo
`NexusTheme.kt` `ALL` :38-114 (copiar um bloco; `isDark` define o grupo) + atualizar
`README.md` e `docs/ARQUITETURA.md`. Os radios de `SettingsActivity` (agrupados em
ESCUROS/CLAROS, :183-214) se atualizam sozinhos.

### 5. Editar a welcome
`Html.kt` `welcomePage` :113-174 — **não** editar `web/welcome.html`.

### 6. Mensagem de erro na tela
`Html.kt` `errorPage` :177-204 + status em `MainActivity.kt` `failGeneration` :408-416.

### 7. Barra de comando
`MainActivity.kt` input :193-207 · sendBtn :208-214 · gearBtn :215-223.

### 8. Contexto de refinamento (v1.3.0)
- Regra no `BASE_PROMPT` (`Html.kt` :18-31): com "Current page", tratar o comando como
  edição e devolver o documento completo.
- `Html.userPrompt(command, currentDocument)` :54-69: anexa a página em tela à mensagem.
- Usado em `MainActivity.kt` `sendPrompt` :343 (`Html.userPrompt(prompt, lastRendered)`);
  status mostra "ajustando…" quando há página em tela.

### 9. Salvar / Abrir / Excluir (Downloads/nexus77)
- Storage: `Store.kt` — `save`/`list`/`read`/`delete` (MediaStore na API 29+; arquivo direto antes, :73).
- Botões do topo: `MainActivity.kt` `headerButton` :561 → `requestSave` :577 / `openSavedList` :653.
- Popup de nome: `showSaveDialog` :585; grava: `savePage` :631 (`Store.save`).
- Página "Salvos": `Html.savedListPage` :210 — abrir item (`openSaved` :680), excluir com
  confirmação (`deleteSaved` :765) e **abrir HTML do celular** (`pickHtmlFile` :719 /
  `openPickedFile` :740).
- Bridge: `Nexus77.openSaved` / `closeList` / `pickFile` / `deleteSaved` (`NxBridge` :843).
- Permissão (API < 29): `withStorage` :797 + manifest (`maxSdkVersion="28"`).

### 10. VU meter + KB de carregamento (v1.5.0; streaming v1.7.0; KB real v1.9.2)
- View: `VuMeterView.kt` (10 cubos, trilho + degradê + cap; tamanho definido em `buildUi`).
- Cabeçalho: `statusRow` (`MainActivity.kt` :145) = `vuMeter` :136 + `kbLabel` :139 + `statusText` :128.
  Fora do carregamento, meter/KB ficam **GONE** (o texto do status encosta na borda).
- Liga/desliga: `setLoading` :451 (em `sendPrompt` :343 / `finishGeneration` :439); KB: `kbText` :463; progresso ao vivo: `onGenProgress` :474.
- Alinhamento: status à **esquerda** (padrão); a linha de render `✓ KB • s • modelo` à **direita** (`Gravity.END` no `sendPrompt`).
- Progresso: `AiClient.read` :212 conta os **bytes de `delta.content`** (tamanho real da página) via `deltaPiece` :247; `parseChat` :79 monta o HTML. Fallback não-streaming em HTTP 4xx.

### 11. Nível de detalhe (v1.8.0)
- `Prefs.detailLevel` (`Prefs.kt` :41-44) — 0 Simples / 1 Médio / 2 Rico (padrão 1).
- Seletor: `SettingsActivity.buildUi` (`detailRadios` :149-172, abaixo da temperatura);
  salvo em `save()` (:272).
- Injetado no prompt: `Html.systemPrompt(theme, detailLevel)` (`Html.kt` :34-45) →
  `detailDirective` (:47-52); chamado em `MainActivity.sendPrompt` :343.

---

## Regras para agentes (edição cirúrgica)

1. Ler o trecho pela âncora antes; editar **só o bloco**; nunca rewrite do arquivo.
2. No mesmo arquivo, edits **sequenciais** (não em paralelo).
3. `web/welcome.html` = referência/protótipo; o welcome real é `Html.kt`.
4. Textos de UI em PT-BR.
5. Iterar com `assembleDebug`; release só para entrega (ver `AGENTS.md`).
6. Mudou a UI? Atualize este mapa (linha + âncora).
