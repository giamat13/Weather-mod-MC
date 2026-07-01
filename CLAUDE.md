# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Weather-mod-MC** (mod id `weather-mod`) is a **natural-disasters** Fabric mod for Minecraft 26.2. It adds tornadoes and hurricanes that wander the world, fling entities and tear blocks into the air, plus a craftable "Tornado & Hurricane Alerter" block that warns players before disasters strike.

**Key fact**: The alerter is a crafted **block** (not a multiblock or entity), registered as `tornado_hurricane_alerter`. It has three warning states (0/1/2) controlling its texture color (green/yellow/red) and alert range.

## Build and Development

### Build and Test
- **Build**: `./gradlew build` → produces `build/libs/weather-mod-1.0.0.jar`
- **Dev environment**: Gradle 9.5.1, Java 25, Fabric Loader 0.19.3, Loom 1.17-SNAPSHOT, Fabric API 0.152.2+26.2
- **Compile check**: `./gradlew compileJava` (use this to verify code; IDE errors are often false)

### Project Properties
- **Minecraft**: 26.2
- **Mod version**: 1.0.0
- **Maven group**: com.weather
- **Mod ID**: weather-mod

## Architecture

### Core Server-Side Components (all under `com.weather`)

**Entry point** (`WeatherMod.java`):
- Implements `ModInitializer`
- Registers all mod components via `ModRegistry.init()`
- Drives everything via `DisasterManager` hooked into `ServerTickEvents.END_SERVER_TICK`
- Provides static `id(String path)` helper for `Identifier` creation (use `Identifier.fromNamespaceAndPath`)

**Disaster System** (`disaster/` package):
- `DisasterType.java`: Enum defining TORNADO and HURRICANE with their properties (range, speed, damage, duration)
- `ActiveDisaster.java`: Represents a running disaster instance; handles entity flinging (motion packets), block destruction→FallingBlockEntity, particles, and thunder
- `ScheduledDisaster.java`: Scheduled disaster queued for future execution
- `DisasterManager.java`: Central orchestrator (accessible as `WeatherMod.DISASTERS`). Runs from server tick end; when thunderstorm starts, 15% chance to schedule a disaster 2 minutes (2400 ticks) ahead. Scans around online players for `tornado_hurricane_alerter` blocks and updates their WARNING state based on disaster proximity.

**Alerter Block** (`block/TornadoHurricaneAlerterBlock.java`):
- `HorizontalDirectionalBlock` with `WARNING` IntegerProperty (0=green, 1=yellow, 2=red)
- Registered in creative tab `natural_disasters`
- Front-face texture changes based on WARNING state (see `blockstates/tornado_hurricane_alerter.json` for texture mapping)
- Range-based warning: red when ≤100 blocks from disaster, yellow when ≤500

**Alerter Logic** (`disaster/Alerter.java`):
- Sets WARNING state on placed block and plays beep sound
- `DisasterManager` periodically scans for blocks and updates WARNING based on active disasters

**Command** (`command/NaturalDisastersCommand.java`):
- `/naturaldisasters <tornado|hurricane> [delaySeconds]`
- Gamemaster-only (via `Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)`)
- Triggers disaster at command caller's position

**Registry** (`registry/ModRegistry.java`):
- Registers two items:
  - `useless_alerter` (shaped recipe: redstone lamp center + glass above + iron ingots around)
  - `tornado_hurricane_alerter` block + block item (shapeless recipe: useless_alerter + wind_charge)
- Registers custom creative tab `natural_disasters`
- Uses modern Fabric item/block registration with `ResourceKey` + `Registry.register()`

### Meteors (`disaster/MeteorType.java`, `disaster/ActiveMeteor.java`)

Meteors are a distinct code path from the vortex disasters. `MeteorType` defines flavours, each with `commandId` (0 = random-only), impact tuning, and a `rarityWeight` used by `DisasterManager.rollMeteorType()`:
- `LIGHT` (command id 1): no explosion — rains loose **items** (`ItemEntity`) plus a fire trail. Most common.
- `CRATER` (id 2): strong fireball that carves a bowl-shaped crater (`carveCrater`).
- `EXTINCTION` (id 3): **very rare**. Boils all water within 100 blocks (recorded via `DisasterManager.reportDriedWater`, seeps back in `tickWaterRestore` — slow in rain, `WATER_RESTORE_STORM_MULT`× faster in a thunderstorm), scorches random blocks to lava, and kills all entities in range (`entity.kill(ServerLevel)`). Scheduled with `EXTINCTION_LEAD_TICKS` (6 min) so alerters warn far in advance.
- `DEBRIS`/`FIREBALL`: original two, random-only (`commandId = 0`).

Command: `/naturaldisasters meteor <delaySeconds> [1|2|3]`. Meteor type threads through `ScheduledDisaster.meteorType` (nullable → rolled at strike time). `DisasterManager.schedule(...)` has an overload taking `MeteorType`.

**Anti-Meteor Rocket** (`block/AntiMeteorRocketBlock.java`): a full-cube block (`simpleCodec`, no extra state). On a redstone signal (`neighborChanged` → `hasNeighborSignal`) it fires: small blast, flings nearby blocks skyward, consumes itself, and calls `DisasterManager.cancelNearestMeteor(level, pos)` to delete the nearest pending/active meteor of any type. Recipe: iron top-centre, iron+redstone+iron middle row, TNT in the two bottom corners.

### Mixins

**Server-side**:
- `CreativeModeTabMixin.java`: Injects at TAIL of `CreativeModeTab.buildContents` to add mod items to vanilla tabs (necessary because this Fabric API version lacks `ItemGroupEvents`). Matches tabs via `BuiltInRegistries.CREATIVE_MODE_TAB.getKey(self)`.

## Assets and Data

**Blockstates** (`blockstates/tornado_hurricane_alerter.json`):
- Defines texture variants for each WARNING state (green/yellow/red faces)

**Models** (`models/block/` + `models/item/`):
- Separate files for each warning state
- Item model uses `assets/weather-mod/items/tornado_hurricane_alerter.json` with type `minecraft:model`

**Textures** (`textures/block/` + `textures/item/`):
- Procedurally generated (via PowerShell System.Drawing)
- Front and side textures for alerter; item texture for useless_alerter

**Recipes** (`data/weather-mod/recipe/`):
- `useless_alerter.json`: Shaped recipe
- `tornado_hurricane_alerter.json`: Shapeless recipe (useless_alerter + wind_charge)

**Loot Tables** (`data/weather-mod/loot_table/blocks/`):
- Standard block loot for tornado_hurricane_alerter

**Language** (`lang/`):
- `en_us.json` + `he_il.json` (mod is Hebrew-driven)
- Translation keys: `block.weather-mod.tornado_hurricane_alerter`, `item.weather-mod.tornado_hurricane_alerter`, `item.weather-mod.useless_alerter`, etc.
- **Important**: BlockItems in 26.2 use `item.<ns>.<id>` keys, not `block.<ns>.<id>` — add both if needed

## Minecraft 26.2 API Quirks

This project targets a non-standard MC version with Mojang mappings that differ from usual. **Always verify symbols with javap before trusting IDE autocomplete**:

```bash
javap -classpath "$USERPROFILE/.gradle/caches/fabric-loom/26.2/minecraft-common.jar" <fully.qualified.ClassName>
```

### Key Deviations
- **Identifier**: Use `Identifier.fromNamespaceAndPath(namespace, path)` (not ResourceLocation)
- **Colored blocks**: No `Blocks.RED_STAINED_GLASS`. Use `Blocks.STAINED_GLASS.pick(DyeColor.RED)` or `.red()/.yellow()/.lime()`
- **Lightning rod**: `Blocks.LIGHTNING_ROD` is a `WeatheringCopperCollection<Block>` — test with `instanceof LightningRodBlock`
- **Entity collection**: No `Level.getEntitiesOfClass(Class, AABB, Predicate)`. Use `Level.getEntities((Entity)null, aabb, predicate)` instead
- **Game time**: No `Level.getGameTime()`. Keep a server-tick counter instead
- **Permissions**: No `hasPermission(int)` on `CommandSourceStack`. Use `.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))`
- **Messaging**: Use `sendSystemMessage(Component)` (no `displayClientMessage`)
- **Block registration**: `BlockBehaviour.Properties.of().setId(ResourceKey<Block>)` is **required**; use `RecordCodecBuilder.mapCodec(...)` for codec (no `simpleCodec`)
- **IDE errors**: After gradle builds, the IDE may show false "class file truncated" errors for Minecraft classes. **Ignore them**; `./gradlew compileJava` is authoritative

## Common Tasks

- **Add a new disaster type**: Extend enum in `DisasterType.java`, add properties and constructor parameter
- **Modify alerter appearance**: Edit textures or `blockstates/tornado_hurricane_alerter.json` variant mappings
- **Adjust warning ranges**: Edit constants in `DisasterManager` where WARNING states are set
- **Add new recipes**: Create JSON in `data/weather-mod/recipe/` and register via crafting table
- **Update translations**: Add keys to both `en_us.json` and `he_il.json`

## Mixin Configuration

Mixins are defined in `weather-mod.mixins.json` (server) and `weather-mod.client.mixins.json` (client). When adding new mixins:
- Add the fully qualified class name to the appropriate JSON config
- Use `@Inject` with proper `At` and `Slice` to target specific code
- Check IL with `javap` if targeting obfuscated MC code; mappings may be non-standard
