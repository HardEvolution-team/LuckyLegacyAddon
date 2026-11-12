# LuckyLegacyAddon

Runs **Lucky Block 1.8.9** add-ons on **Lucky Block 1.12.2** (Cleanroom/Forge 1.12.2) without porting them by hand.
The original add-on files are never modified: everything is fixed in memory at load time.

## Features

- **Native loading** — put the original 1.8.9 `.zip` / `.jar` / `.rar` add-ons into `addons/lucky_block/`. Nothing is converted on disk.
- **Drop fixing** — legacy `drops.txt` syntax (`;` groups, `@luck`/`@chance` typos, brackets, junk lines), 1.8 → 1.12 entity/item/sound IDs, NBT (Riding, Equipment, potions, chests → loot tables), `summon`/`playsound` commands.
- **Structures** — repairs `.schematic` files (truncated gzip, wrong sizes, missing files) with fuzzy file matching.
- **Resources** — models, blockstates, lang files, textures, and texture name clashes between add-ons. 1.8.9 models are drawn by the 1.8.9 renderer ported into ArchaicLib, so held items, GUI and third-person view look like the original.
- **Built-in Loot++** — own implementation of Loot++ 1.8.9 configs (items, armor, bows, guns, thrown projectiles, multitools, food, blocks, drops, recipes, item effects), with behaviour checked against the original.
- **PotionCore effects** — registers `potioncore:*` effects when PotionCore itself is not installed.
- Fixes the Lucky Block 1.12.2 freeze caused by its dead update check.

## Installation

1. Minecraft 1.12.2 + Cleanroom (or Forge with MixinBooter).
2. Mods: [Lucky Block 1.12.2](https://www.minecraftascending.com/projects/lucky-block) (not included), **ArchaicLib**, **LuckyLegacyAddon**.
3. Put 1.8.9 add-ons into `.minecraft/addons/lucky_block/` as they are.

## Building

Requires [ArchaicLib](https://github.com/ElectrifiedDed/ArchaicLib) in a sibling folder (`../ArchaicLib`) and the Lucky Block 1.12.2 jar in `libs/` (`lucky-block-forge-1.12.2-2.0.jar`; not redistributed, Lucky Block is All Rights Reserved).

```
cd ../ArchaicLib && ./gradlew build
cd ../LuckyLegacyAddon && ./gradlew build
```

Test tools: `tools/test_addon.sh` (starts a server with given add-ons and checks the log), `tools/rcon.py`.

## Known limitations

- RAR5 archives are not supported (RAR4 is).
- Forge 1.8.9 add-ons with their own code (Doctor Who, LuckyItemsAddon) do not load yet.
- Bugs made by add-on authors that also failed in 1.8.9 are intentionally left as they are.

## License

HardEvolution Restricted Source-Available License (HE-RSAL) 1.1 — see [LICENSE.en.md](LICENSE.en.md) and [LICENSE.ru.md](LICENSE.ru.md) (the Russian version prevails).
Lucky Block belongs to its author; this project neither contains nor modifies it.

Author: **ElectrifiedDed** / [HardEvolution](https://github.com/HardEvolution-team)
