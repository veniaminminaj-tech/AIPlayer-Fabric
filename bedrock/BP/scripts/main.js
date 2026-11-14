import {
  CommandPermissionLevel,
  CustomCommandStatus,
  system,
  world
} from "@minecraft/server";

const ENTITY_ID = "blocky:companion";
const OWNER_PREFIX = "blocky_owner_";
const STOP_TAG = "blocky_stay";
const CHAT_PREFIX = "blocky";

function ownedCompanion(player) {
  const tag = OWNER_PREFIX + player.id;
  try {
    return player.dimension.getEntities({ type: ENTITY_ID }).find(entity => entity.hasTag(tag));
  } catch {
    return undefined;
  }
}

function spawnFor(player) {
  const existing = ownedCompanion(player);
  if (existing) {
    player.sendMessage("§b[Blocky] §fЯ вже тут!");
    return;
  }

  const p = player.location;
  const entity = player.dimension.spawnEntity(ENTITY_ID, {
    x: p.x + 1.5,
    y: p.y,
    z: p.z + 1.5
  });
  entity.nameTag = "Blocky";
  entity.addTag(OWNER_PREFIX + player.id);
  player.sendMessage("§b[Blocky] §fЯ приєднався до світу!");
}

system.beforeEvents.startup.subscribe(({ customCommandRegistry }) => {
  customCommandRegistry.registerCommand(
    {
      name: "blocky:aiplayerspawn",
      description: "Spawn your Blocky AI companion",
      permissionLevel: CommandPermissionLevel.Any,
      cheatsRequired: false
    },
    (origin) => {
      const player = origin.sourceEntity;
      if (!player || player.typeId !== "minecraft:player") {
        return {
          status: CustomCommandStatus.Failure,
          message: "Цю команду може виконати тільки гравець."
        };
      }
      system.run(() => spawnFor(player));
      return {
        status: CustomCommandStatus.Success,
        message: "Викликаю Blocky..."
      };
    }
  );
});

// Chat controls are handled after the message is received. The actual world
// changes are deferred with system.run to leave the event callback safely.
try {
  world.afterEvents.chatSend.subscribe((event) => {
    const player = event.sender;
    const text = event.message.trim();
    const lower = text.toLocaleLowerCase();

    if (!lower.startsWith(CHAT_PREFIX)) return;
    const command = lower.slice(CHAT_PREFIX.length).replace(/^[\s,:]+/, "");

    if (command.includes("spawn") || command.includes("поклич") || command.includes("з'явись")) {
      system.run(() => spawnFor(player));
      return;
    }

    const companion = ownedCompanion(player);
    if (!companion) {
      player.sendMessage("§b[Blocky] §fСпочатку виклич мене: /aiplayerspawn");
      return;
    }

    if (command.includes("stop") || command.includes("стій") || command.includes("зупинись")) {
      system.run(() => companion.addTag(STOP_TAG));
      player.sendMessage("§b[Blocky] §fДобре, чекатиму тут.");
    } else if (command.includes("follow") || command.includes("йди за мною") || command.includes("ходи за мною")) {
      system.run(() => companion.removeTag(STOP_TAG));
      player.sendMessage("§b[Blocky] §fЙду за тобою!");
    } else if (command.includes("hello") || command.includes("привіт")) {
      player.sendMessage("§b[Blocky] §fПривіт! Я готовий досліджувати світ.");
    } else {
      player.sendMessage("§b[Blocky] §fЯ поки розумію: spawn, follow, stop, hello.");
    }
  });
} catch (error) {
  console.warn("Blocky chat listener could not be registered: " + error);
}

// Keep Blocky near its owner without relying on a client-side player entity.
system.runInterval(() => {
  for (const player of world.getPlayers()) {
    const companion = ownedCompanion(player);
    if (!companion || companion.hasTag(STOP_TAG)) continue;

    const a = companion.location;
    const b = player.location;
    const dx = b.x - a.x;
    const dy = b.y - a.y;
    const dz = b.z - a.z;
    const distanceSquared = dx * dx + dy * dy + dz * dz;

    if (distanceSquared > 7 * 7) {
      try {
        companion.teleport(
          { x: b.x + 1, y: b.y, z: b.z + 1 },
          { dimension: player.dimension }
        );
      } catch (error) {
        console.warn("Blocky follow teleport failed: " + error);
      }
    }
  }
}, 20);
