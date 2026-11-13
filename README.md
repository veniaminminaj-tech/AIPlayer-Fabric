# AI Player — Fabric Mod

An AI-powered Minecraft companion for **Minecraft Java 1.21.11** using **Fabric** and **Java 21**.

The companion can follow its owner, wander around, react to nearby threats, and answer messages through an external AI provider.

## Features

- One companion is automatically created for each player when they join a world.
- Works in Survival, Creative, Adventure, and Spectator modes.
- Each companion is assigned to its owner rather than simply following the nearest player.
- Companion movement includes following, looking at players, wandering, and avoiding hostile mobs.
- Talk to the companion using its name prefix or `@BotName` in chat.
- Conversation context is kept separately for each player while the server is running.
- AI requests run asynchronously.
- Settings screen opens with **M**.
- Supported providers: **Google Gemini, OpenRouter, DeepSeek, and OpenAI**.
- Custom companion skin can be loaded from a PNG file.

## Installation

1. Install **Minecraft Java 1.21.11**.
2. Install **Fabric Loader** for Minecraft 1.21.11.
3. Install the matching **Fabric API**.
4. Download the AI Player JAR from the repository's [Releases](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/releases) or the latest successful [GitHub Actions build](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/actions).
5. Put the JAR in your Minecraft `mods` folder.
6. Launch Minecraft once. The mod creates its data folder automatically.

## Configure the API key and skin

The mod creates this folder in your Minecraft game directory (usually `.minecraft`):

```text
.minecraft/
└── ai-playerv/
    ├── api-key.txt
    ├── skin.png
    └── README.txt
```

### API key

1. Open `.minecraft/ai-playerv/api-key.txt`.
2. Paste your API key into the file.
3. Save it and restart Minecraft or reconnect to the server.

The key is read from this text file. **Do not share it, post it in screenshots, or commit it to GitHub.**

### Custom skin

1. Put your skin image in `.minecraft/ai-playerv/`.
2. Name the file exactly `skin.png`.
3. Use a **64 × 64 PNG** Minecraft skin.
4. Restart Minecraft to load the changed texture.

If the file is missing or has an unsupported size, the mod uses its built-in Steve/Alex appearance.

## Settings

Press **M** in-game to open the AI Player settings screen.

You can configure:

- AI provider
- Model ID
- HTTPS API endpoint
- Companion name
- Companion personality

Choose the same provider as the API key you placed in `api-key.txt`. The model ID must be supported by that provider. Save the settings before testing the connection.

### Default provider endpoints

| Provider | Endpoint |
|---|---|
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions` |
| OpenRouter | `https://openrouter.ai/api/v1/chat/completions` |
| DeepSeek | `https://api.deepseek.com/chat/completions` |
| OpenAI | `https://api.openai.com/v1/chat/completions` |

Remote provider endpoints must use **HTTPS**. Localhost/Ollama endpoints are not supported.

## Chat and commands

Address the companion by its configured name or use the `@BotName` prefix in chat.

Available commands:

- `/spawnai` — request a companion spawn (operator permission required).
- `/spawnai <name>` — request a companion with a specified name (operator permission required).
- `/aiclear` — clear your conversation memory.

The exact command permissions are controlled by the mod and Minecraft server.

## Multiplayer and server setup

AI chat requests are processed by the Minecraft server. For multiplayer, install the mod on the server and configure the server's own `.minecraft/ai-playerv/api-key.txt` (or the game directory used by that server).

The API key is **not** sent from a connecting player's client to the server. Keep the server's key private and do not commit it to the repository. API usage may incur charges or be subject to provider rate limits.

## Build from source

Requirements:

- JDK 21
- Git
- Internet access to download Gradle and Minecraft/Fabric dependencies

Build:

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The built mod JAR is placed in:

```text
build/libs/
```

GitHub Actions builds the project when changes are pushed. Download the JAR from the **Artifacts** section of a successful workflow run.

## Current limitations

- The companion is a Minecraft entity, not a real player account.
- It does not currently perform AI-controlled mining, crafting, building, or combat.
- Conversation memory is temporary and is not retained after the server stops.
- Custom skin changes require a Minecraft restart.
- A working internet connection and a valid API key are required for cloud AI providers.

## Security

- Keep `api-key.txt` private.
- Do not paste API keys into public issues, chat messages, screenshots, or source files.
- Use a trusted AI provider and review its privacy and billing policies.
- The mod uses HTTPS for remote provider requests.

## Links

- [Source code](https://github.com/veniaminminaj-tech/AIPlayer-Fabric)
- [Issues and bug reports](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/issues)
- [GitHub Actions builds](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/actions)

