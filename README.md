# AIPlayer Fabric

Fabric mod for Minecraft 1.21.11, built with Java 21 and Mojang mappings.

## Included
- `/spawnai` (operator permission level 2) spawns a humanoid AI companion.
- The companion wanders and looks at nearby players.
- Chat within 32 blocks of the companion is sent to the configured AI provider; the reply is returned in chat.
- Press **P** to open the provider settings screen.
- Providers: Google Gemini, Ollama, DeepSeek, and OpenAI.
- Configure provider, model, endpoint, and API key.
- Toggle the companion's Steve/Alex texture preference in settings.
- Settings are saved to `config/aiplayer.json`.
- HTTP calls run asynchronously.

## Build
JDK 21 and Gradle 9.2.1 are required. Run `gradle build`; the JAR is written to `build/libs/`. GitHub Actions builds and uploads the JAR as a workflow artifact on each push to `main`.

## Default endpoints
- Gemini: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
- Ollama: `http://localhost:11434/v1/chat/completions`
- DeepSeek: `https://api.deepseek.com/chat/completions`
- OpenAI: `https://api.openai.com/v1/chat/completions`

Ollama must be running on a machine reachable from the Minecraft server. For a remote server, configure its reachable LAN/host address instead of localhost.

## API key safety
Keys are saved in the local `config/aiplayer.json` file. Do not commit or share that file. On multiplayer servers, the server needs its own provider configuration because chat responses are generated server-side.

## Notes
This is an initial implementation. The skin option selects the built-in Steve/Alex texture; a full custom PNG skin uploader and advanced AI-driven movement/action system are not included yet.
