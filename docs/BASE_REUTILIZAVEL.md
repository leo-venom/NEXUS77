# NEXUS77 — Base reutilizável

> Este projeto virou a **base de referência** para apps que geram HTML por IA e renderizam
> num WebView. Aqui estão os padrões que funcionaram muito bem, para reusar em projetos
> futuros.

## Por que este padrão é bom (avaliado na prática)
- **Fluidez excelente** do WebView.
- **Render 100% adaptativo** a qualquer tela.
- **Auto-adaptação do código gerado ao tema** (o HTML já nasce na paleta ativa).
- **Confiabilidade de resposta**: sem erros e sem respostas vazias.

## Arquitetura (zero dependências)
```
MainActivity (UI + fluxo) → AiClient (HTTP estilo OpenAI, streaming SSE + fallback)
        → Html (prompt/extração/decoração/páginas) → WebView
```
- `MainActivity` · `AiClient` · `Html` · `Store` · `NexusTheme` · `Prefs` · `VuMeterView`.

## 1. Render 100% adaptativo
- `web.loadDataWithBaseURL("https://nexus77.local/", html, "text/html", "UTF-8", null)`
  (`Html.BASE` + `Html.show`).
- `Html.decorate()` injeta `<meta name="viewport">` **se faltar** + CSS base.
- WebView: `useWideViewPort=true`, `loadWithOverviewMode=false`, zoom desligado,
  `overScrollMode=NEVER`, `hardwareAccelerated=true`.
- `BASE_PROMPT` exige viewport cheio, responsivo, centralizado e sem scrollbars.

**Para reusar:** copie `Html.decorate` + os ajustes de `WebSettings` do `buildUi`.

## 2. Auto-adaptação ao tema (o ponto sensacional)
O tema é aplicado **nos dois lados**:
1. **No system prompt** — `Html.systemPrompt(tema)` injeta as cores exatas (o modelo já
   gera com a paleta).
2. **No HTML** — `Html.varsCss(tema)` injeta `:root{ --nx-bg, --nx-accent, ... }` + base CSS.

Resultado: o HTML gerado herda o tema automaticamente; **trocar de tema re-decora a página
atual sem chamar a IA** (`onResume` → `Html.decorate(theme, lastRendered)`).

**Para reusar:** um `NexusTheme` (data class com as cores + `hex`/`hexA`) e `varsCss`/`decorate`.

## 3. Confiabilidade de resposta (sem erro / sem vazio)
- Prompt firme: *"Never ask questions and never refuse; escolha a interpretação mais
  impressionante"*.
- `Html.extractDocument()`: tira cercas ``` ``` ``` e corta até `<!DOCTYPE`/`<html`.
- `AiClient.chat()`: **streaming com fallback** para não-streaming em HTTP 4xx; timeouts
  generosos (connect 20 s / leitura ~10 min); valida `choices/message/content`.
- Erros tipados (`AiException` + `errorMessage` que desembrulha o JSON de erro da API).
- `failGeneration`: só mostra página de erro **se nada** foi renderizado; caso contrário
  mantém a página atual na tela.

## 4. Recursos que valem reaproveitar
- **Refinamento contextual**: reenvia o comando **com o documento em tela** → a IA **edita**
  o atual (status "ajustando…" vs "gerando…"). Implementação: `Html.userPrompt`.
- **Progresso ao vivo**: KB = bytes **decodificados** de `delta.content` (bate com o
  "✓ X KB" final); tokens ≈ bytes/4 **durante**, exato via `usage` ao final.
- **Nível de detalhe** (Simples/Médio/Rico) como diretriz no prompt (`Html.detailDirective`).
- **Salvar/Abrir**: `Downloads/<app>` (MediaStore na API 29+; arquivo direto + permissão
  antes) + página de lista com bridge JS (`Store` + `Html.savedListPage`).
- **Bridge JS** mínima: exponha só métodos inofensivos (`setInput`, `openSaved`, `closeList`).
- **Docs do projeto**: `DOCUMENTACAO.md`, `MAPA_UI.md` (arquivo:linha + âncora),
  `CHANGELOG.md`, `ASSINATURA.md`, `AGENTS.md`.

## 5. Checklist para um novo projeto nesta base
```
1. Projeto em /home/leo/Documents/Obsidian Vault/01_Projeto/{nome}/
   (android/ + web/ + docs/ + README.md)
2. WebView shell: minSdk 24, sem Compose/Hilt/Room (zero deps) → build de segundos
3. Copiar o trio: AiClient (SSE+fallback) · Html (prompt/extração/decorate) · tema+CSS vars
4. BASE_PROMPT com "never refuse" + viewport cheio/responsivo
5. Bridge JS mínima + docs (MAPA_UI com âncoras, DOCUMENTACAO, CHANGELOG)
6. Build: assembleDebug p/ iterar; assembleRelease (R8 + v2/v3) p/ entrega em /home/leo/apk22
```

## 6. Pitfalls a evitar no próximo projeto
- `usesCleartextTraffic=true` → prefira **HTTPS** no endpoint.
- API key em SharedPreferences texto puro → `allowBackup="false"` minimiza; cifrar exigiria dep.
- `decorate` não-idempotente (duplicaria `<style id="nx-base">` ao reabrir salvo/trocar tema)
  → checar a âncora antes de injetar.
- WebView sem `onDestroy`/`destroy()` → adicione o cleanup.
- Strings hardcoded → mover para `strings.xml` se houver i18n.
