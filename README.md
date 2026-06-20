# Natural Disasters 🌪

A Fabric mod for Minecraft 26.2 that adds tornadoes and hurricanes to your world. These devastating disasters wander through your territory, fling entities into the air, and tear blocks apart—plus a craftable **Tornado & Hurricane Alerter** block that warns you before they strike.

**[עברית](#עברית) • English**

---

## Features

### Disasters
- **Tornadoes and Hurricanes**: Wandering natural disasters that spawn during thunderstorms (15% chance)
  - Fling players, mobs, and items into the air using motion packets
  - Tear blocks into `FallingBlockEntity`s
  - Emit cloud and smoke particles
  - Produce thunder and chaos

### Alerter Block
- Craftable "Tornado & Hurricane Alerter" that warns you of incoming threats
- **Warning states**:
  - 🟢 **Green** (0): Safe — no danger
  - 🟡 **Yellow** (1): Caution — disaster within 500 blocks
  - 🔴 **Red** (2): Danger — disaster within 100 blocks
- Places on walls, glows when warning, and emits beep sound

### Commands
- `/naturaldisasters <tornado|hurricane> [delaySeconds]` — Manually trigger a disaster (gamemaster only)

---

## Installation

### Requirements
- Minecraft 26.2
- Fabric Loader 0.19.3+
- Java 25+

### Steps
1. Download the latest JAR from the releases
2. Place `weather-mod-1.0.0.jar` in your `mods/` folder
3. Launch Minecraft with Fabric

---

## Crafting Recipes

### 1. Useless Alerter
A plain alerter block (aesthetic only).

```
[I] [G] [I]
[I] [R] [I]
[I] [I] [I]
```

- **I**: Iron Ingot
- **R**: Redstone Lamp
- **G**: Glass

### 2. Tornado & Hurricane Alerter
Upgrade your useless alerter with a wind charge to create a working alarm.

```
[U] [W]
```

- **U**: Useless Alerter
- **W**: Wind Charge

---

## Usage

### Surviving a Disaster
1. **Craft an Alerter** using the recipe above
2. **Place the block** on a wall where you can see it
3. **Watch for color changes**:
   - Yellow means a disaster is approaching
   - Red means imminent danger—seek shelter!
4. **Take cover** indoors or underground when the warning turns red

### Manual Triggers
Use the command to spawn a disaster for testing or fun:
```
/naturaldisasters tornado
/naturaldisasters hurricane 60
```

---

## Configuration

Currently, this mod has no in-game configuration file. To adjust disaster behavior (spawn rates, ranges, damage), edit the source code in:
- `DisasterManager.java` — Spawn chance and delay timing
- `DisasterType.java` — Range, speed, and damage values
- `DisasterManager.java` — Warning distances (100 and 500 blocks)

---

## Development

### Building from Source
```bash
./gradlew build
```

Output: `build/libs/weather-mod-1.0.0.jar`

### Project Structure
- `src/main/java/com/weather/` — Main mod code
  - `disaster/` — Disaster logic and scheduling
  - `block/` — Alerter block implementation
  - `command/` — Command handling
  - `registry/` — Item and block registration
  - `mixin/` — Mixin configuration for integration
- `src/main/resources/` — Assets, recipes, lang files

### Language Support
The mod includes translations for:
- English (`en_us.json`)
- Hebrew (`he_il.json`)

To add more languages, create a new lang file in `assets/weather-mod/lang/` and update `fabric.mod.json`.

### Minecraft 26.2 Notes
This project uses Minecraft 26.2 with Mojang mappings. Some API calls differ from standard versions (see `CLAUDE.md` for details). Always verify with:
```bash
javap -classpath "$USERPROFILE/.gradle/caches/fabric-loom/26.2/minecraft-common.jar" <ClassName>
```

---

## Technical Details

### How It Works
1. When a thunderstorm begins, the mod has a 15% chance to schedule a disaster 2 minutes in the future
2. When the timer expires, an `ActiveDisaster` spawns and begins wandering
3. The disaster scans for entities and blocks in its path, flinging and destroying them
4. `DisasterManager` continuously scans for Alerter blocks around online players and updates their warning state based on disaster proximity
5. Players see the block color change and hear a beep when danger is near

### Server-Side Only
All disaster logic runs server-side. Clients receive:
- Entity motion packets (for flinging)
- Block state updates (for alerter color changes)
- Particle effects and sound

---

## License

CC0-1.0 Public Domain

---

---

# עברית

## Natural Disasters 🌪

מוד Fabric לדיוק Minecraft 26.2 שמוסיף טורנדו והוריקנים לעולם שלך. אסונות טבעיים אלו משוטטים בשטח, זורקים ישויות לאוויר ותורעים בלוקים — בתוספת בלוק **מתריע טורנדו והוריקן** שניתן לייצור ומוזהר אתכם לפני שהם מגיעים.

### תכונות

**אסונות:**
- **טורנדו והוריקנים**: אסונות טבעיים משוטטים שנוצרים במהלך סערות רעם (סיכוי 15%)
  - זורקים שחקנים, יצורים וחפצים לאוויר
  - תורעים בלוקים ל-`FallingBlockEntity`
  - פולטים חלקיקי עננים וקרח
  - מייצרים רעם וכאוס

**בלוק מתריע:**
- בלוק "מתריע טורנדו והוריקן" שניתן לייצור שמוזהר אתכם מפני איומים קרובים
- **מצבי אזהרה**:
  - 🟢 **ירוק** (0): בטוח — אין סכנה
  - 🟡 **צהוב** (1): זהירות — אסון בתוך 500 בלוקים
  - 🔴 **אדום** (2): סכנה — אסון בתוך 100 בלוקים
- מונח על קירות, זוהר כשמוזהר, וממוציא צליל ביפ

### פקודות
- `/naturaldisasters <tornado|hurricane> [delaySeconds]` — הפעל אסון באופן ידני (רק gamemaster)

### התקנה

**דרישות:**
- Minecraft 26.2
- Fabric Loader 0.19.3+
- Java 25+

**צעדים:**
1. הורד את ה-JAR האחרון מ-releases
2. הנח את `weather-mod-1.0.0.jar` בתיקיית `mods/`
3. הפעל את Minecraft עם Fabric

### מתכני ייצור

**1. מתריע חסר תועלת**
```
[I] [G] [I]
[I] [R] [I]
[I] [I] [I]
```

**2. מתריע טורנדו והוריקן**
```
[U] [W]
```

### שימוש

1. **יצור מתריע** באמצעות המתכן
2. **הנח את הבלוק** על קיר שתוכל לראות
3. **צפה לשינויי צבע**:
   - צהוב = אסון מתקרב
   - אדום = סכנה מיידית — חפש מקלט!
4. **חזור לבטחון** בבנייה או תחת האדמה כשהאזהרה הופכת לאדומה

---

## עזרה ובעיות

אם אתה נתקל בבעיות:
1. ודא שיש לך את הגרסה הנכונה של Minecraft ו-Fabric
2. בדוק שה-JAR נמצא בתיקיית `mods/`
3. פתח את ה-logs של Minecraft לפרטים נוספים

---

**Made with ❤️ for Fabric Modding**
