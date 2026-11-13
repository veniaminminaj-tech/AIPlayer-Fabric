# AIPlayer Fabric

A configurable AI companion mod for **Minecraft Java 1.21.11**, built with **Fabric**, **Mojang mappings**, and **Java 21**.

## Features
- Press **P** to open the settings screen.
- Providers: Google Gemini, Ollama, DeepSeek, and OpenAI-compatible chat APIs.
- Configure provider, model ID, endpoint, API key, companion name, and personality.
- Test the provider connection from the settings screen.
- Choose the built-in Steve or Alex appearance.
- Run `/spawnai` or `/spawnai <name>` (operator permission level 2) to spawn a companion.
- The companion wanders and looks at nearby players.
- When a player chats within 32 blocks of a companion, the mod sends that message and a small game context (dimension, position, health) to the configured AI provider.
- Conversation memory is kept per player in server memory, bounded by the configured number of recent messages. Use `/aiclear` to clear your conversation memory.
- Provider HTTP calls run asynchronously so the game server thread is not blocked.

## Build
Requires JDK 21. CI uses Gradle 9.2.1 and runs `gradle clean build --no-daemon --stacktrace`. The remapped JAR is produced in `build/libs/`. Every push to `main` runs the GitHub Actions build and uploads the JAR as an artifact.

## Provider configuration
Default endpoints:
- Gemini: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
- Ollama: `http://localhost:11434/v1/chat/completions`
- DeepSeek: `https://api.deepseek.com/chat/completions`
- OpenAI: `https://api.openai.com/v1/chat/completions`

Ollama must be running on a machine reachable from the Minecraft server. For a remote server, configure a reachable Ollama address instead of localhost.

## Important multiplayer note
The server handles chat and sends requests to the AI provider. Therefore the server must have its own `config/aiplayer.json` with the provider settings and API key. The in-game settings screen writes the local game's config; it does not transmit secrets to a remote server. Do not commit or share API keys.

## Current limitations
- Steve/Alex are the built-in appearances; importing arbitrary PNG skins is not implemented yet.
- The companion currently wanders and looks at players; AI-directed pathfinding, mining, crafting, combat, voice, and tab-list player integration are not implemented.
- Conversation memory lasts only until the server stops and is intentionally bounded.
