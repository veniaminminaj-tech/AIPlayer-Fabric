# AIPlayer Fabric

Fabric mod for Minecraft **1.21.11**, using Java 21 and Mojang mappings.

## Current features
- `/spawnai` (operator permission level 2) spawns a humanoid AI companion.
- The companion wanders and looks at nearby players.
- Chat within 32 blocks of the companion is sent to the selected AI provider; the reply is returned to the player.
- Press **P** to open provider settings.
- Providers: Google Gemini, Ollama, DeepSeek, and OpenAI.
- Configure provider, model, endpoint, and API key.
- Steve/Alex built-in skin preference.
- Settings saved to `config/aiplayer.json`.
- Provider requests run asynchronously.

## Build
JDK 21 and Gradle 9.2.1 are used by CI. Run `gradle clean build`; the remapped mod JAR is written to `build/libs/`. GitHub Actions runs the same build on every push to `main` and uploads the JAR as an artifact.

## Default endpoints
- Gemini: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
- Ollama: `http://localhost:11434/v1/chat/completions`
- DeepSeek: `https://api.deepseek.com/chat/completions`
- OpenAI: `https://api.openai.com/v1/chat/completions`

Ollama must be running on a machine reachable from the Minecraft server. For a remote server, configure its reachable address instead of localhost.

## API key safety
Keys are saved in the local `config/aiplayer.json` file. Do not commit or share that file. In multiplayer, the server performs AI requests and needs its own provider configuration.

## Limitations
This is an initial implementation. Custom PNG skin importing and AI-directed navigation/mining/combat are not included yet.
