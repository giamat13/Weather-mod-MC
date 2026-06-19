# Natural Disasters by giamatamat 🌪

A Fabric mod for **Minecraft 26.2** that adds natural disasters, an early-warning *Alerter*
multiblock, and a season cycle. Everything is toggled with `/Naturaldisasters`.

## The Alerter (מתריע)

Build a **3×3×3 cube** out of iron blocks with:

- a **Redstone Lamp** in the very centre, and
- a **Red Stained-Glass** block directly above the lamp (the indicator),
- **Iron Blocks** for the remaining 25 positions.

```
 layer +1 (top)      layer 0 (middle)     layer -1 (bottom)
 I I I               I I I                I I I
 I R I   ← red glass I L I  ← lamp        I I I
 I I I               I I I                I I I
```

Right-click the structure with a **module item** to teach it what to warn about:

| Module item        | Warns about                    |
|--------------------|--------------------------------|
| Lightning Rod      | Tornado & Hurricane            |
| Dirt               | Earthquake                     |
| Fire Charge        | Wildfire                       |
| Stone              | Meteor                         |
| Stone + Fire Charge| Volcanic Eruption (combined)   |

The indicator light above the alerter shows the threat level for any disaster it is configured
for, starting **2 minutes before impact**:

- 🟢 **Green** – nothing nearby
- 🟡 **Yellow** – a disaster within 500 blocks
- 🔴 **Red** – a disaster within 100 blocks (also beeps loudly)

## Features

1. **Tornadoes & Hurricanes 🌪** – every storm has a 15% chance to spawn one. Hurricanes are bigger
   and stronger. They travel across the surface, fling entities into the funnel, and rip surface
   blocks up as thrown falling-blocks.
2. **Earthquakes 🌋** – shake and launch nearby entities (height/distance scale with magnitude) and
   crack open the ground chunk by chunk. Occur naturally (rarely) or via command.
3. **Seasons** – Spring → Summer → Autumn → Winter, one every 30 Minecraft days. Each season biases
   the weather. Change it with `/Season`.
4. **Wildfires 🔥** – any fire burning in a forest counts as a wildfire and trips fire alerters. In
   summer, flammable blocks have a tiny (0.000009%) chance to spontaneously ignite.
5. **Meteors ☄** – every 45–90 minutes there is a 10% chance a meteor streaks down at render
   distance and explodes into a burning, magma-lined crater.
6. **Volcanic Eruptions** – during an earthquake a region may convert into a **geyser field**, built
   from Minecraft 26.2's native `potent_sulfur` geysers (≈1 per chunk; 10% are lava-rimmed).
7. **Sandstorms** – when it "rains" in a desert, it becomes a sandstorm: directional wind that speeds
   up players moving with it and slows those moving against it (stronger during thunderstorms), plus
   blowing sand particles. If [blocky13](https://github.com/giamat13/blocky13) is installed, it also
   deposits `blocky13:sand_layer` blocks.

## Commands

```
/Naturaldisasters                      show status (season, day, active events)
/Naturaldisasters on | off             enable/disable the whole system (ops)
/Naturaldisasters trigger <type>       spawn a disaster at your position (ops)
/Naturaldisasters trigger earthquake <1-10>   spawn a quake of a given magnitude (ops)

/Season                                show the current season and day
/Season set <spring|summer|autumn|winter>     set the season (ops)
/Season next                           advance to the next season (ops)
```

`<type>` is one of `tornado, hurricane, earthquake, fire, meteor, volcano, sandstorm`.

## Building

```
./gradlew build
```

The mod jar is written to `build/libs/`. Requires Java 25 and Fabric Loader ≥ 0.19.3.

## License

CC0-1.0.
