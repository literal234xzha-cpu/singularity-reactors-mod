# APK Decompilation Report

## Herramientas instaladas

- **JADX** v1.5.6 → `tools/bin/jadx` (dex-to-Java decompiler)
- **Apktool** v3.0.3 → `tools/apktool.jar` (resource/smalyi extractor)
- **Androguard** 4.1.4 (Python static analysis)

## APK Analizado

**Archivo:** `Epic Stickman Mod MWahyudiStar.apk`
- **App Name:** Epic Stickman
- **Package:** `com.fansipan.epic.stickman.survival.rpg.idle.game`
- **Tamaño APK:** ~205 MB (214,434,290 bytes)
- **Build:** 7/3/2026 from "Build's Mac Pro"
- **Motor:** Unity (libil2cpp.so = 149MB @ arm64-v8a)

## Mod Agregado (GameKiller)

- **Package:** `com.game.killer`
- **Entry Point:** `gamesActivity` (LAUNCHER/MAIN activity)
  - Splash screen con texto `⚡GAMEKILLERAPP.COM⚡`
  - Botón "GAMEKILLERAPP HOME" → abre `https://gamekillerapp.com`
  - Botón "OK" → lanza `UnityPlayerActivity` (juego principal)
- **TipDialog** → diálogo emergente post-splash con los mismos textos y enlaces

## String Obfuscation (XOR + Base64)

```java
char[] cArr = new char[length];
for (int i = 0; i < length; i++) {
    cArr[i] = (char)(shortArray[start + i] ^ key);
}
return new String(cArr);
```

Strings decodificadas:
| Ubicación | XOR Key | Texto |
|-----------|---------|-------|
| gamesActivity:42 | 856 (base64) | `GameKillerApp.com` |
| gamesActivity:67 | 797 | `⚡GAMEKILLERAPP.COM⚡` |
| gamesActivity:76 | 1569 | `GAMEKILLER MOD APK Games for Android` |
| gamesActivity:92 | 573 | `GAMEKILLERAPP HOME` |
| gamesActivity:130 | 2286 | `OK` |
| gamesActivity$1:123 | 1747 | `https://gamekillerapp.com` |
| TipDialog:53 | 549 | `⚡GAMEKILLERAPP.COM⚡` |
| TipDialog:62 | 490 | `GAMEKILLER MOD APK Games for Android` |
| TipDialog:78 | 926 | `GAMEKILLERAPP HOME` |
| TipDialog:124 | 1590 | `OK` |
| TipDialog$1:117 | 2254 | `https://gamekillerapp.com` |

## SDKs de Publicidad

AdMob, AppLovin, Unity Ads, IronSource, Vungle, Facebook Audience Network,
Bytedance (Pangle/TT), Mintegral, Fyber, Appsflyer (analytics),
Facebook Analytics, Firebase.

## Permisos Sospechosos

- ACCESS_FINE_LOCATION / ACCESS_COARSE_LOCATION
- READ/WRITE_EXTERNAL_STORAGE
- POST_NOTIFICATIONS
- BLUETOOTH / BLUETOOTH_ADMIN
- CHANGE_WIFI_STATE
- com.android.vending.CHECK_LICENSE (Google license check bypass)
- ACCESS_ADSERVICES_* (Android 12+ ad tracking)

## Estructura de Output

```
decompiler/
├── apks/                          # APKs descargados
├── tools/                         # Herramientas
│   ├── apktool.jar                # Apktool 3.0.3
│   └── bin/                       # JADX CLI
├── output/
│   ├── epic_stickman_decompiled/  # JADX output (30,524 .java files)
│   │   ├── sources/               # Java decompiled
│   │   └── resources/             # Resources + AndroidManifest
│   └── epic_stickman_apktool/     # Apktool output (6,395 .smali files)
│       ├── smali_classes*/        # Smali by DEX
│       ├── AndroidManifest.xml
│       ├── assets/
│       ├── lib/                   # 25 .so files (arm64-v8a)
│       └── res/
└── decompile.py                   # Wrapper script
```

## DEX Files

| DEX | Contenido principal |
|-----|---------------------|
| classes2.dex | UnityPlayerActivity |
| classes3.dex | UnityPlayer, UnityCoreAssetPacks |
| classes4.dex | Google Play Services (games, ads) |
| classes5.dex | Firebase, crashlytics |
| classes6.dex | AppLovin ad SDK |
| classes7.dex | Facebook Unity Ads |
| classes8.dex | Appsflyer, adjust |
| classes9.dex | IronSource, Unity Ads mediation |
| classes10.dex | Bytedance (Pangle/TT) |
| classes11.dex | GameKiller mod (game/killer) |
| classes12.dex | Mintegral |
| classes13.dex | GameKiller mod (duplicate) |
| classes14.dex | Fyber, Vungle |
| assets/audience_network.dex | Facebook Audience Network |

## Notas

- JADX terminó con 100 errores (clases con nombres Unicode corruptos)
  pero produjo 30,524 archivos .java legibles.
- El mod GameKiller añade una capa de splascreen entre el launcher
  y el juego Unity, con redireccionamiento a su sitio web.
- El código del mod está ofuscado con:
  1. XOR de strings (short[] + integer key)
  2. Base64 para URLs
  3. Nombres de métodos con caracteres Unicode
  4. Control flow obfuscation (if conditions con constantes)
- El il2cpp.so (149MB) contiene el código C# del juego Unity, no analizado
  en este descompilado (requiere herramientas de il2cpp specíficas).
