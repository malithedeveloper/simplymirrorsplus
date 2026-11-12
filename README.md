# Simply Mirrors Plus

<img src="common-assets/src/main/resources/assets/simplymirrorsplus/icon.png" width="128" alt="Simply Mirrors Plus icon">

Craftable, frameless, full-block mirrors with live reflections for Minecraft. Every face of the block reflects, and adjacent mirrors form continuous surfaces, including floors and ceilings.

Based on [Simply Mirrors](https://github.com/UpperMoon0/Simply-Mirrors) by NsTut. The 1.21.4 port, new features and fixes are by malidev.

## Supported versions

| Minecraft | Loaders |
| --- | --- |
| 26.1.2 | Fabric, NeoForge |
| 1.21.11 | Fabric, NeoForge |
| 1.21.8 | Fabric, NeoForge |
| 1.21.5 | Fabric, NeoForge |
| 1.21.4 | Fabric, NeoForge |
| 1.21.1 | Fabric, NeoForge |
| 1.20.1 | Fabric, Forge |
| 1.16.5 | Fabric, Forge |

Every version has the same features.

## Installation

Put the jar for your version in your `mods` folder. Fabric builds also need [Fabric API](https://modrinth.com/mod/fabric-api). Craft a mirror from eight glass blocks around an iron ingot, or take one from the Simply Mirrors Plus creative tab.

## Using mirrors

Sneak and right-click a mirror with an empty hand to switch it between Normal, Players Only and Off. The whole mirror switches: every mirror block connected to the one you click. A mirror block powered by redstone stops reflecting until the power is removed.

## Settings

Press `M` in game to open the mirror settings. The key can be changed under Controls. The settings are also behind the config button in Mod Menu (Fabric) and in the NeoForge and Forge mod lists. They are stored in `config/simplymirrorsplus.properties`.

| Setting | Values |
| --- | --- |
| Mirrors | On or off |
| Resolution | 25% to 100% of the screen, in 5% steps; only the part of the screen the mirror covers is rendered |
| Reflection FPS | 30, 60, 90 or 120 |
| Players Only (End Sky) | Reflects only players, over the End sky, in every mirror not set to Off |

## Rendering

Mirrors visible inside a reflection also reflect, with two rendered bounces. Deeper bounces reuse the mirror's latest image, so the block texture does not show at the cutoff.

Each mirror plane keeps its own image, cropped to the part of the screen it covers, refreshed at the selected rate and reused between refreshes. At most two planes are refreshed per frame and six are drawn. Images of mirrors that have not been seen for five seconds are freed. Mirrors are found within 32 blocks.

Sodium and Iris are supported. When an Iris shader pack is active, reflections render at 100% resolution and a mirror seen inside a reflection shows its block texture.

## Building

Run `python3 tools/build.py` to build every target; each jar is written to its `<target>/build/libs/` folder. Targets need JDK 8, 17, 21 or 25, depending on the Minecraft version. To build one target, run `python3 tools/build.py --target fabric-1.21.4`.

## License

MIT. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
