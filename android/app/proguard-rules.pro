# NEXUS77 — regras R8/ProGuard

# Bridge JS -> Kotlin (addJavascriptInterface e reflexivo)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Activities (pontos de entrada)
-keep class com.leo.nexus77.MainActivity { *; }
-keep class com.leo.nexus77.SettingsActivity { *; }
