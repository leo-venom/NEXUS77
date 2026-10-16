# NEXUS77 — guia rápido para agentes

App Android Kotlin nativo (zero dependências externas): shell WebView + chat de IA que
gera HTML. Dono: Leo (PT-BR). Responder em português.

## Essenciais
- Raiz: `/home/leo/Documents/Obsidian Vault/01_Projeto/NEXUS77/` — o path tem espaço, sempre aspas.
- `android/` = projeto Gradle (Kotlin DSL): `com.leo.nexus77`, minSdk 24, target/compile 34,
  Kotlin 1.9.24, AGP 8.5.0, Java 17.
- Arquitetura: `MainActivity` (UI + fluxo) → `AiClient` (HttpURLConnection, API estilo
  OpenAI chat/completions) → `Html` (prompt, extração, páginas) → WebView.
  Refinamento (v1.3.0+): `Html.userPrompt(comando, lastRendered)` anexa a página em tela
  ao comando para a IA editar o que já está aberto.
  Carregamento: `VuMeterView` (10 cubos) + `AiClient.chat(onProgress)` → KB ao vivo (v1.5.0+).
  Config: `SettingsActivity`; persistência: `Prefs`; páginas: `Store` (`Downloads/nexus77`).
  Temas: `NexusTheme.kt` (15 = 12 escuros + 3 claros; agrupados em ESCUROS/CLAROS na tela).
- **Mapa da UI: `docs/MAPA_UI.md`** — todo elemento de tela com `arquivo:linha` + âncora.
  Consultar antes de mexer em qualquer UI.

## Regras
- Pedido pontual (ex.: "mudar cor do cubo") → edição cirúrgica no ponto exato.
  NUNCA reescrever o arquivo inteiro.
- Welcome real é `Html.kt` (`welcomePage`); `web/welcome.html` é só referência/protótipo.
- Textos de UI em PT-BR. Edits no mesmo arquivo: sequenciais (não em paralelo).
- Write tool pode falhar em arquivo longo (JSON Parse error) → usar heredoc (`cat > arq <<'EOF'`).
- Docs: `README.md`, `docs/DOCUMENTACAO.md` (detalhada), `docs/BASE_REUTILIZAVEL.md`
  (base p/ novos projetos), `docs/ARQUITETURA.md`, `docs/MAPA_UI.md`,
  `docs/ASSINATURA.md`, `docs/CHANGELOG.md`.

## Build & entrega
- Iterar: `./gradlew assembleDebug` (segundos). Sem `clean`, sem `--no-daemon`.
- Entrega: `./gradlew assembleRelease` (R8 + shrink, assinatura v2+v3), depois:
  limpar os `*.apk` antigos de `/home/leo/apk22/` (**preservar `122.jpg`**), copiar o novo
  como `app-v{versionCode}-{versionName}-{timestamp}.apk` e validar com
  `apksigner verify -v` + `aapt dump badging`.
