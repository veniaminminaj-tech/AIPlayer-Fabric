# AIPlayer Fabric

A Fabric mod for Minecraft 1.21.11 (Java 21) with AI provider configuration.

## Planned features
- Press **P** to open AI settings.
- Gemini, Ollama, DeepSeek, and OpenAI provider support.
- Local config file at `config/aiplayer.json`.
- Async HTTP requests.
- GitHub Actions JAR builds.

## Build
Run `gradle build` with JDK 21. The JAR is created in `build/libs/`.

## Default endpoints
- Gemini: `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions`
- Ollama: `http://localhost:11434/v1/chat/completions`
- DeepSeek: `https://api.deepseek.com/chat/completions`
- OpenAI: `https://api.openai.com/v1/chat/completions`

Ollama must be running on a machine reachable by the Minecraft client. Never commit your API keys.
