# hytale-mod-template-config

## Overview

Runtime configuration flow, config-backed behavior switches, and safe defaults. This repository is a practical starting point for a Hytale mod.

## Main entrypoint

- Main class from manifest.json: net.hytaledepot.templates.mod.config.ConfigModPlugin
- Includes asset pack: true

## Source layout

- Java sources: src/main/java
- Manifest: src/main/resources/manifest.json
- Runtime jar output: build/libs/hytale-mod-template-config-1.0.0.jar

## Key classes

- ConfigModPlugin
- ConfigModTemplate

## Commands

- /hdconfigmoddemo
- /hdconfigmodstatus

## Build

1. Ensure the server jar is available in one of these locations:
   - HYTALE_SERVER_JAR
   - HYTALE_HOME/install/$patchline/package/game/latest/Server/HytaleServer.jar
   - workspace root HytaleServer.jar
   - libs/HytaleServer.jar
2. Run: ./gradlew clean build
3. Copy build/libs/hytale-mod-template-config-1.0.0.jar into your server mods/ folder.

## License

MIT
