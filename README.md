# Natural Disasters 🌪

A Fabric mod for Minecraft 26.2 that adds tornadoes, hurricanes, and earthquakes. Disasters wander the world, fling entities, and tear blocks apart — plus craftable **Alerter** blocks that warn you before each disaster strikes.

**[עברית](#עברית) • English**

---

## Features

| Disaster | Trigger | Radius | Duration |
|----------|---------|--------|----------|
| **Tornado** | Thunderstorm (15% chance) | 8 blocks | 30 sec |
| **Hurricane** | Thunderstorm (15% chance) | 18 blocks | 60 sec |
| **Earthquake** | Command only | 40 blocks | 20 sec |

### Alerter Blocks

| Block | Warns About | Recipe |
|-------|-------------|--------|
| **Tornado & Hurricane Alerter** | Tornadoes, Hurricanes | Useless Alerter + Wind Charge |
| **Earthquake Alerter** | Earthquakes | Useless Alerter + Dirt |

Warning states: 🟢 Safe · 🟡 Within 500 blocks · 🔴 Within 100 blocks (+ beep sounds)

### Commands
`/naturaldisasters <tornado|hurricane|earthquake> [delaySeconds]` — gamemaster only

---

## Installation

- Minecraft 26.2, Fabric Loader 0.19.3+, Fabric API 0.152.2+26.2, Java 25+
- Place `weather-mod-1.0.0.jar` in your `mods/` folder

---

## Crafting Recipes

**Useless Alerter** (required base for all alerters):
```
[I] [G] [I]
[I] [R] [I]
[I] [I] [I]
```
I = Iron Ingot · R = Redstone Lamp · G = Glass

**Tornado & Hurricane Alerter**: `[Useless Alerter] [Wind Charge]`

**Earthquake Alerter**: `[Useless Alerter] [Dirt]`

---

---

# עברית

## Natural Disasters 🌪

מוד Fabric לדיוק Minecraft 26.2 שמוסיף טורנדו, הוריקנים ורעידות אדמה. האסונות משוטטים בשטח, זורקים ישויות לאוויר ותורעים בלוקים — בתוספת בלוקי **מתריע** שמזהירים לפני כל אסון.

### אסונות

| אסון | טריגר | רדיוס | משך |
|------|--------|-------|------|
| **טורנדו** | סערת רעם (15%) | 8 בלוקים | 30 שניות |
| **הוריקן** | סערת רעם (15%) | 18 בלוקים | 60 שניות |
| **רעידת אדמה** | פקודה בלבד | 40 בלוקים | 20 שניות |

### בלוקי מתריע

| בלוק | מזהיר על | מתכון |
|------|-----------|-------|
| **מתריע טורנדו והוריקן** | טורנדו, הוריקנים | מתריע חסר תועלת + Wind Charge |
| **מתריע רעידות אדמה** | רעידות אדמה | מתריע חסר תועלת + עפר |

מצבי אזהרה: 🟢 בטוח · 🟡 בתוך 500 בלוקים · 🔴 בתוך 100 בלוקים (+ ביפים)

### פקודות
`/naturaldisasters <tornado|hurricane|earthquake> [delaySeconds]` — רק gamemaster

### התקנה
- Minecraft 26.2, Fabric Loader 0.19.3+, Fabric API 0.152.2+26.2, Java 25+
- הנח את `weather-mod-1.0.0.jar` בתיקיית `mods/`

### מתכני ייצור

**מתריע חסר תועלת:**
```
[I] [G] [I]
[I] [R] [I]
[I] [I] [I]
```
I = ברזל · R = מנורת אבן האדמה · G = זכוכית

**מתריע טורנדו והוריקן**: `[מתריע חסר תועלת] [Wind Charge]`

**מתריע רעידות אדמה**: `[מתריע חסר תועלת] [עפר]`

---

**Made with ❤️ for Fabric Modding**
