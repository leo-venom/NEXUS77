# NEXUS77 — Assinatura (keystore, build release, validação)

## Keystore de release

| Item | Valor |
|---|---|
| Arquivo | `android/keystore/nexus77-release.jks` |
| Alias | `nexus77` |
| Algoritmo | RSA 2048 |
| Validade | 10.950 dias (~30 anos) — gerado em 2026-10-16 |
| DNAME | `CN=NEXUS77, OU=Apps, O=Leo, L=Brasil, ST=BR, C=BR` |
| Senhas | `android/keystore.properties` (storePassword/keyPassword) — chmod 600 |
| SHA-256 | `3E:87:1E:BE:7C:27:8C:9D:6A:B6:05:C1:81:8A:F0:BF:CD:55:BF:34:6E:4E:6A:CC:F9:E5:F8:2C:F9:BB:2B:FB` |
| SHA-1 | `90:5B:2E:97:41:E9:1F:ED:8E:44:B1:B6:7A:DC:2D:2D:3B:D7:FF:D9` |
| MD5 | `66:61:19:07:37:2F:40:F5:3C:1D:EC:90:04:A7:E5` |

O `app/build.gradle.kts` lê `android/keystore.properties` e assina o release com
`storeFile` relativo a `android/`. Sem propriedades, cai no debug keystore
(só para desenvolvimento). **v1 desabilitada; v2 + v3 habilitadas.**

> Regra de ouro: nunca perder o keystore — sem ele não é possível publicar
> atualizações com a mesma assinatura. O backup fica em
> `~/Documents/Obsidian Vault/03_Memórias/Dicas/nexus-assinatura-backup/`
> (`nexus77-release.jks`, `nexus77-keystore.properties`, `nexus77-info.txt`).

## Build de entrega

```bash
cd android
./gradlew assembleRelease
```

- R8 + shrinkResources; validado (BUILD SUCCESSFUL ~38 s).
- `gradlew-fast` = release sem R8 para iteração.

## Validação pós-build

```bash
BT=~/Android/Sdk/build-tools/34.0.0
$BT/apksigner verify -v app/build/outputs/apk/release/app-release.apk
$BT/aapt dump badging app/build/outputs/apk/release/app-release.apk
```

Última verificação: `v2: true`, `v3: true` (v1 `false` por opção), 1 signer;
badging `com.leo.nexus77`, `versionCode 14`, `versionName 1.10.1`, targetSdk 34,
permissões `INTERNET` (+ `WRITE`/`READ_EXTERNAL_STORAGE` com `maxSdkVersion="28"`),
label `NEXUS77` (`app-v14-1.10.1-20261016-183858.apk`, ~67 KB).

## Entrega

- Manter **apenas o último APK** em `/home/leo/apk22/`, nomeado:
  `app-v{versionCode}-{versionName}-{timestamp}.apk`
  (ex.: `app-v14-1.10.1-20261016-183858.apk`, ~67 KB).
- Antes de copiar: `find /home/leo/apk22 -maxdepth 1 -name '*.apk' -delete`
  (o `122.jpg` da pasta é mantido).

## Configuração completa (para recriar o ambiente)

1. `keytool -genkeypair -keystore nexus77-release.jks -alias nexus77 -keyalg RSA -keysize 2048 -validity 10950 -dname "..."`.
2. Criar `android/keystore.properties` com `storeFile=keystore/nexus77-release.jks`,
   `storePassword`, `keyAlias=nexus77`, `keyPassword`; `chmod 600`.
3. `./gradlew assembleRelease` → assinar → validar → copiar para `/home/leo/apk22/`.
