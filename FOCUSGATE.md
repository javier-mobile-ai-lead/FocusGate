# FocusGate

Android app que bloquea apps distractoras (TikTok, WhatsApp, etc.) hasta que completes una sesión diaria de práctica de inglés enfocada en listening y speaking.

---

## Cómo funciona

1. **AppBlockerService** (AccessibilityService) escucha eventos de cambio de ventana en primer plano.
2. Cuando detecta una app bloqueada y la sesión del día no está completa → lanza **BlockOverlayActivity** (pantalla de bloqueo).
3. El usuario debe completar **3 frases en inglés**: escuchar con TTS y repetir con el micrófono (umbral de 60% de coincidencia de palabras).
4. Al completar, **SessionManager** marca la sesión como hecha (SharedPreferences con clave de fecha). Se reinicia a medianoche.
5. Al volver a `BlockOverlayActivity` con sesión completa, la pantalla se cierra automáticamente.

---

## Archivos creados

```
app/src/main/java/com/pe/learnai/
├── MainActivity.kt           — Dashboard: estado de sesión, lista de apps bloqueadas, setup
├── PracticeActivity.kt       — Sesión de práctica (3 rondas listen + speak)
├── BlockOverlayActivity.kt   — Pantalla de bloqueo (sin botón back)
├── AppBlockerService.kt      — AccessibilityService que detecta la app en primer plano
├── BlockedApps.kt            — Set de paquetes bloqueados
├── SessionManager.kt         — Estado diario en SharedPreferences
└── data/
    └── PracticeContent.kt    — 15 frases en inglés nivel B1–B2 (rotan por día)

app/src/main/res/xml/
└── accessibility_service_config.xml  — Configuración del AccessibilityService
```

---

## Apps bloqueadas

| App | Package |
|-----|---------|
| TikTok | `com.zhiliaoapp.musically` |
| WhatsApp | `com.whatsapp` |
| Instagram | `com.instagram.android` |
| YouTube | `com.google.android.youtube` |
| X / Twitter | `com.twitter.android` |
| Facebook | `com.facebook.katana` |
| Snapchat | `com.snapchat.android` |
| Reddit | `com.reddit.frontpage` |
| Messenger | `com.facebook.orca` |

---

## Flujo de la sesión de práctica

```
Ronda 1/3
  → Tap "Listen"  → TTS lee la frase en voz alta
  → Tap "Speak"   → SpeechRecognizer captura tu voz
  → Si ≥60% de palabras coinciden → ✅ Ronda pasada
  → Si no → "Try Again" (intentos ilimitados)
Ronda 2/3 → igual
Ronda 3/3 → igual
→ Sesión completa → apps desbloqueadas por el resto del día
```

---

## Setup en el dispositivo

1. Instalar el APK: `app/build/outputs/apk/debug/app-debug.apk`
2. Abrir FocusGate → tocar **"Open Accessibility Settings"**
3. Activar **"FocusGate App Blocker"** en la lista
4. Conceder permiso de micrófono cuando lo pida la app

---

## Permisos en el Manifest

| Permiso | Para qué |
|---------|---------|
| `RECORD_AUDIO` | SpeechRecognizer para la práctica de speaking |
| `BIND_ACCESSIBILITY_SERVICE` | Detectar qué app está en primer plano |

---

## Notas técnicas

- **compileSdk** subido de 36.1 → 37 (requerido por `core:1.19.0` y `lifecycle-runtime-ktx:2.11.0`)
- El bloqueo usa `AccessibilityService` + lanzar una Activity en lugar de `SYSTEM_ALERT_WINDOW` (no requiere permiso especial adicional)
- `BlockOverlayActivity` corre en su propio task (`taskAffinity="com.pe.learnai.blocker"`) para cubrir la app bloqueada sin mezclar stacks
- Las frases de práctica rotan diariamente usando `DAY_OF_YEAR` como índice
