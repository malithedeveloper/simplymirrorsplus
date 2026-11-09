# Simply Mirrors Plus

<img src="common-assets/src/main/resources/assets/simplymirrorsplus/icon.png" width="128" alt="Simply Mirrors Plus icon">

Craftable, frameless, full-block mirrors with live reflections for Minecraft. Every face of the block reflects, and adjacent mirrors form continuous surfaces, including floors and ceilings.

Based on [Simply Mirrors](https://github.com/UpperMoon0/Simply-Mirrors) by NsTut. The 1.21.4 port, new features and fixes are by malidev.

## Supported versions

| Minecraft | Loaders | Features |
| --- | --- | --- |
| 1.21.4 | Fabric | Full: settings menu, Players Only mode, mirror-in-mirror reflections |
| 26.1.2 | Fabric, NeoForge | Base mirrors |
| 1.21.1 | Fabric, NeoForge | Base mirrors |
| 1.20.1 | Fabric, Forge | Base mirrors |
| 1.16.5 | Fabric, Forge | Base mirrors |

The settings, Rendering notes and Players Only mode below apply to the 1.21.4 build.

## Installation

Put the jar for your version in your `mods` folder. Fabric builds also need [Fabric API](https://modrinth.com/mod/fabric-api). Craft a mirror from eight glass blocks around an iron ingot, or take one from the Simply Mirrors Plus creative tab.

## Settings

Press `M` in game to open the mirror settings. The key can be changed under Controls. Settings are stored in `config/simplymirrorsplus.properties`.

| Setting | Values |
| --- | --- |
| Mirrors | On or off |
| Resolution | 25% to 100% of the screen, in 5% steps |
| Reflection FPS | 30, 60, 90 or 120 |
| Players Only (End Sky) | Reflects only players, over the End sky |

## Rendering

Mirrors visible inside a reflection also reflect, with two rendered bounces. Deeper bounces reuse the mirror's latest image, so the block texture does not show at the cutoff.

Each mirror plane keeps its own image, which is refreshed at the selected rate and reused between refreshes. At most two planes are refreshed per frame and six are drawn. Mirrors are found within 32 blocks.

Sodium and Iris are supported. When an Iris shader pack is active, reflections render at 100% resolution and a mirror seen inside a reflection shows its block texture.

## Building

Run `python3 tools/build.py` to build every target; each jar is written to its `<target>/build/libs/` folder. Targets need JDK 8, 17, 21 or 25, depending on the Minecraft version. To build one target, run `python3 tools/build.py --target fabric-1.21.4`.

## License

MIT. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
