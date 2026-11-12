![Simply Mirrors Plus](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/header.png)

Craftable full-block mirrors that show a live reflection of the world around them. Every face of the block reflects, and neighboring mirrors join into one surface, so you can build mirrored walls, floors and ceilings.

Craft a mirror from eight glass blocks around an iron ingot, or take one from the Simply Mirrors Plus creative tab.

Simply Mirrors Plus is based on [Simply Mirrors](https://www.curseforge.com/minecraft/mc-mods/simply-mirrors) by NsTut.

![Features](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/section-features.png)

### Mirrors inside mirrors

A mirror you can see in a reflection reflects too. Two bounces are rendered, and deeper ones reuse the mirror's latest image, so facing mirrors keep going instead of ending on the block texture.

![Two facing mirrors reflecting each other](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/mirror-in-mirror.png)

### Players Only

Turn this on and mirrors show only players in front of the End sky, like a VRChat mirror. The world is not drawn in the reflection at all. It works for all mirrors from the settings, or for single mirrors with the per-mirror mode below.

![A mirror in Players Only mode](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/players-only.png)

### Per-mirror mode and redstone

Sneak and right-click a mirror with an empty hand to switch it between Normal, Players Only and Off. Every mirror block connected to the one you click switches with it. A mirror block powered by redstone stops reflecting until the power is removed.

### Lighter reflections

Each mirror keeps its own image and only redraws it at the rate you choose. Only the part of the screen the mirror covers is rendered, and images of mirrors you have not seen for five seconds are freed. Between refreshes the last image is reused, and at most two mirror surfaces refresh in a single frame.

![Settings](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/section-settings.png)

Press `M` in game to open the settings, or use the config button in Mod Menu (Fabric) or the NeoForge and Forge mod lists. You can change the key under Controls. The mirrors stay visible behind the menu, so you see each change as you make it.

![The Mirror Settings menu](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/settings-menu.png)

| Setting | Values |
| --- | --- |
| Mirrors | On or off |
| Resolution | 25% to 100% of the screen, in 5% steps |
| Reflection FPS | 30, 60, 90 or 120 |
| Players Only (End Sky) | On or off |

Settings are saved in `config/simplymirrorsplus.properties`.

![Versions](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/section-versions.png)

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

Every version has the same features. Fabric builds need Fabric API.

Sodium and Iris are supported. With a shader pack active, reflections render at 100% resolution and a mirror seen inside a reflection shows its block texture.

![Credits](https://raw.githubusercontent.com/malithedeveloper/simplymirrorsplus/1.21.4/docs/curseforge/section-credits.png)

Original mod: [Simply Mirrors](https://www.curseforge.com/minecraft/mc-mods/simply-mirrors) by NsTut. Port, new features and fixes by malidev.

Source code is on [GitHub](https://github.com/malithedeveloper/simplymirrorsplus) under the MIT license.
