# AI Player — Fabric Mod

An AI-powered Minecraft companion for **Minecraft Java 1.21.11** using **Fabric** and **Java 21**.

The companion is an AI-controlled Minecraft entity with owner-specific behavior modes and cloud-AI chat.

## Features

- One companion is automatically created for each player when they join a world.
- Works in Survival, Creative, Adventure, and Spectator modes.
- Each automatically spawned companion is assigned to its owner.
- Follow, wander, and stay behavior modes.
- Avoids hostile mobs and reacts to nearby threats.
- Looks at nearby players and wanders when its mode allows it.
- Talk to the companion using its configured name prefix or `@BotName` in chat.
- Per-player conversation history while the server is running.
- AI requests run asynchronously.
- Settings screen opens with **M**.
- Supported providers: **Google Gemini, OpenRouter, DeepSeek, and OpenAI**.
- Custom companion skin can be loaded from a PNG file.

## Behavior commands

- `/aiplayer mode follow` — follow your owner.
- `/aiplayer mode wander` — explore nearby areas independently.
- `/aiplayer mode stay` — stay in place (it can still react to immediate threats).
- `/aiplayer status` — show your companion's mode and health.
- `/spawnai` — spawn a companion (operator permission required).
- `/spawnai <name>` — spawn a named companion (operator permission required).
- `/aiclear` — clear your conversation memory.

The behavior commands control the companion owned by the player who runs them. The companion must be in the same dimension and within 128 blocks.

## Installation

1. Install **Minecraft Java 1.21.11**.
2. Install **Fabric Loader** for Minecraft 1.21.11.
3. Install the matching **Fabric API**.
4. Download the AI Player JAR from the repository's [Releases](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/releases) or a successful [GitHub Actions build](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/actions).
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

**Keep the API key private.** Do not share it, post it in screenshots, or upload it to GitHub.

### Custom skin

1. Put a **64 × 64 PNG** Minecraft skin at `.minecraft/ai-playerv/skin.png`.
2. Restart Minecraft to load the changed texture.

## Settings

Press **M** in-game to open the AI Player settings screen. You can configure the provider, model ID, HTTPS endpoint, companion name, and personality.

Supported providers: Google Gemini, OpenRouter, DeepSeek, and OpenAI. The API key is read from `.minecraft/ai-playerv/api-key.txt`. Remote provider endpoints must use HTTPS.

## Multiplayer and server setup

AI chat requests are processed by the Minecraft server. For multiplayer, install the mod on the server and configure the server's own API key file. API usage may incur charges or be subject to provider rate limits.

## Build from source

Requirements: JDK 21 and internet access to download Gradle and Minecraft/Fabric dependencies.

```bash
./gradlew clean build
```

On Windows:

```powershell
.\gradlew.bat clean build
```

The mod JAR is placed in `build/libs/`. GitHub Actions builds the project when changes are pushed.

## Current limitations

- The companion is a Minecraft entity, not a real player account.
- It does not yet autonomously mine blocks, collect resources, craft, build, farm, trade, or execute arbitrary plans from chat.
- Follow/wander/stay are behavior modes, not full human-level gameplay.
- Conversation history is temporary and provider context/output limits still apply; truly unlimited AI tokens are not possible.
- Microphone input / speech-to-text through Simple Voice Chat is not implemented yet. The companion currently receives text chat only and does not speak.
- A working internet connection and a valid API key are required for cloud AI providers.

## Security

- Keep `api-key.txt` private.
- Do not paste API keys into public issues, chat messages, screenshots, or source files.
- Review your AI provider's privacy and billing policies.
- The mod uses HTTPS for remote provider requests.

## Links

- [Source code](https://github.com/veniaminminaj-tech/AIPlayer-Fabric)
- [Issues and bug reports](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/issues)
- [GitHub Actions builds](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/actions)
