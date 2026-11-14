# Blocky — AI Player (Fabric)

Blocky is an AI companion mod for **Minecraft Java Edition 1.21.11**, built with **Fabric** and **Java 21**. It adds a companion entity that can follow its owner, collect nearby dropped items, gather some nearby logs, respond to chat, and enter a basic PvP training mode.

> **Edition:** This repository is for Minecraft **Java Edition** only. It is not a Minecraft Bedrock add-on and cannot be installed in the Bedrock Edition or Minecraft Preview.

## Features

- Automatically spawns one companion for a player when they join a world.
- The companion is associated with its owner and follows them.
- Chat with Blocky by starting a message with its configured name, for example `Blocky, hello!` or `@Blocky hello!`.
- AI chat through Gemini, OpenRouter, DeepSeek, or OpenAI-compatible chat-completion endpoints.
- Simple chat intents:
  - `Blocky, добудь дерево` — enable nearby log gathering.
  - `Blocky, припини рубати` — stop gathering.
  - `Blocky, покажи інвентар` — list Blocky's stored items.
  - `Blocky, давай PvP` — start basic medium-difficulty PvP training against the owner.
  - `Blocky, зупини PvP` — stop PvP training.
  - `Blocky, яка складність?` — report the PvP training state.
- Picks up nearby dropped items and stores them in its own inventory.
- Periodically asks the configured AI provider for a high-level survival objective.
- Settings screen, opened with **M**, for provider, model, endpoint, companion name, and personality.
- Optional custom 64×64 PNG skin.

The chat intent phrases above are examples in Ukrainian; English phrases such as `Blocky, start pvp`, `Blocky, stop gathering`, and `Blocky, show inventory` are also recognized.

## Requirements

- Minecraft **Java Edition 1.21.11**
- Fabric Loader compatible with Minecraft 1.21.11
- Fabric API matching Minecraft 1.21.11
- Java 21
- Internet access and an API key for AI-provider chat and planning

## Installation

1. Install Fabric Loader for Minecraft Java 1.21.11.
2. Install the matching Fabric API.
3. Download the latest successful JAR from [GitHub Actions](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/actions) or a published [Release](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/releases).
4. Put the mod JAR in the game's `mods` folder.
5. Launch the game.

For PojavLauncher, use a **Java Edition 1.21.11 Fabric** profile and put the JAR in that profile's `.minecraft/mods` directory. Bedrock profiles and Bedrock add-on folders are not compatible with this JAR.

## Spawn and chat

The mod automatically creates a companion for each player when they join a world.

There is one registered slash command:

```mcfunction
/aiplayerspawn
/aiplayerspawn Blocky
```

The command requires administrator/operator permission. It spawns a companion at the command source position.

To talk to the automatically spawned companion, start your chat message with its configured name or mention:

```text
Blocky, привіт!
@Blocky what should we do next?
Blocky, давай PvP
Blocky, зупини PvP
Blocky, добудь дерево
Blocky, припини рубати
Blocky, покажи інвентар
```

The companion must be nearby (within 64 blocks) to receive the chat message. AI-provider replies require a valid API key and a working network connection. The direct gathering, inventory, and PvP phrases are handled locally by the mod and do not require an AI-provider request.

## Configure the AI provider

On first launch, the mod creates:

```text
.minecraft/
├── config/
│   └── aiplayer.json
└── ai-playerv/
    ├── api-key.txt
    ├── skin.png
    └── README.txt
```

1. Open `.minecraft/ai-playerv/api-key.txt`.
2. Paste your provider API key into the file and save it.
3. In game, press **M** to open settings.
4. Choose a provider, enter a supported model ID and HTTPS endpoint, then save.
5. Use **Test connection** to check the provider configuration.

Supported provider presets:

| Provider | Default model | Endpoint |
|---|---|---|
| Gemini | `gemini-2.5-flash` | `https://generativelanguage.googleapis.com/v1beta/openai/chat/completions` |
| OpenRouter | `google/gemini-2.5-flash` | `https://openrouter.ai/api/v1/chat/completions` |
| DeepSeek | `deepseek-chat` | `https://api.deepseek.com/chat/completions` |
| OpenAI | `gpt-4.1-mini` | `https://api.openai.com/v1/chat/completions` |

The endpoint must use HTTPS. Localhost/Ollama HTTP endpoints are not supported by the current implementation. Never commit or share your API key.

## Custom skin

1. Place a Minecraft skin PNG at `.minecraft/ai-playerv/skin.png`.
2. Use a **64 × 64 PNG**.
3. Restart Minecraft after changing the file.

If the custom skin is unavailable or invalid, the built-in appearance is used.

## Multiplayer

Install the mod on the server. Configure the API key in the server's game directory under `ai-playerv/api-key.txt`. AI requests are sent by the server; a connecting player's client API key is not used. Keep the server key private. Provider usage may incur charges or rate limits.

## Build from source

Requirements: JDK 21 and internet access for Gradle/Minecraft/Fabric dependencies.

Linux/macOS:

```bash
./gradlew clean build
```

Windows PowerShell:

```powershell
.\gradlew.bat clean build
```

The built JAR is in `build/libs/`. GitHub Actions also builds the project; download the JAR from the **Artifacts** section of a successful run.

## Current limitations

- Blocky is a Minecraft mob/entity, not a real Minecraft player account.
- PvP training is a basic experimental melee behavior, not a full player-like combat AI; it has no advanced dodging, combos, or competitive combat logic.
- Gathering currently targets nearby log blocks and is limited by the implementation's tool/drop checks; it is not a general mining system.
- Blocky does not yet provide general-purpose crafting, building, or arbitrary block interaction.
- The companion's chat conversation history is held in server memory and is cleared when the player disconnects; it is not a persistent long-term memory store.
- Voice input and speech output are not implemented.
- This Fabric mod does not run on Minecraft Bedrock Edition.

## Security

- Keep `api-key.txt` private.
- Do not post API keys in screenshots, public issues, chat messages, or source files.
- Use trusted AI providers and review their privacy and billing policies.
- Remote provider requests use HTTPS.

## Links

- [Source code](https://github.com/veniaminminaj-tech/AIPlayer-Fabric)
- [Issues and bug reports](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/issues)
- [GitHub Actions builds](https://github.com/veniaminminaj-tech/AIPlayer-Fabric/actions)
