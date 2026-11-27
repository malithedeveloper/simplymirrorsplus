"""
Generates everything that depends on the forms of the mirror, from one 16x16 mask per outline:

  core/.../math/MirrorShapeMasks.java      the masks the reflection is cut to
  assets/.../models/block/mirror_*.json    the block models, built from the same pixels
  assets/.../blockstates/mirror.json       every placement of every form
  assets/.../models/item, assets/.../items one item per form
  assets/.../lang/en_us.json               the names of the forms (other entries are kept)
  data/.../recipe/mirror_*.json            the stonecutter recipes, and the crafting grid's one step to the next form
  data/.../loot_table/blocks/mirror.json   each form drops its own item
  data/.../tags/item/mirrors.json          every form, as the stonecutter's input
  data/.../chiseling_recipes/mirror.json   the forms as one group for the Rechiseled mod's chisel

Run it again after changing a shape:  python3 tools/shapes.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/simplymirrorsplus"
DATA = ROOT / "src/main/resources/data/simplymirrorsplus"
JAVA = ROOT / "core/src/main/java/com/malidev/simplymirrorsplus/math/MirrorShapeMasks.java"
SIZE = 16


def mask(inside):
    """Rows from the bottom up; a pixel belongs to the shape when its centre does."""
    return [[bool(inside((x + .5) / SIZE, (y + .5) / SIZE)) for x in range(SIZE)] for y in range(SIZE)]


def heart(x, y):
    x, y = (x - .5) * 2.6, (y - .46) * 2.6
    return (x * x + y * y - 1) ** 3 - x * x * y ** 3 <= 0


# Outlines, in the order of MirrorShape after FULL and SLAB; each but the first is a block and a panel there.
# Quarter shapes have their corner at the bottom left.
SHAPES = {
    "panel": mask(lambda x, y: True),
    "circle": mask(lambda x, y: math.hypot(x - .5, y - .5) <= .5),
    "half_circle": mask(lambda x, y: math.hypot(x - .5, y) <= .5),
    "quarter_circle": mask(lambda x, y: math.hypot(x, y) <= 1),
    "inverse_quarter_circle": mask(lambda x, y: math.hypot(x - 1, y - 1) >= 1),
    "triangle": mask(lambda x, y: x + y <= 1),
    "diamond": mask(lambda x, y: abs(x - .5) + abs(y - .5) <= .5),
    "octagon": mask(lambda x, y: abs(x - .5) + abs(y - .5) <= .72),
    "arch": mask(lambda x, y: y <= .5 or math.hypot(x - .5, y - .5) <= .5),
    "ring": mask(lambda x, y: .22 <= math.hypot(x - .5, y - .5) <= .5),
    "heart": mask(heart),
}
# A round mirror joined to its neighbours: bit 0 squares the bottom left corner, 1 the bottom right, 2 the top left, 3 the top right.
for corners in range(1, 16):
    SHAPES[f"circle_c{corners}"] = mask(lambda x, y, c=corners: math.hypot(x - .5, y - .5) <= .5 or bool(c >> ((x >= .5) + 2 * (y >= .5)) & 1))
NAMES = {"slab": "Mirror Slab", "panel": "Mirror Panel", "circle": "Round Mirror", "half_circle": "Half Round Mirror",
         "quarter_circle": "Quarter Round Mirror", "inverse_quarter_circle": "Mirror Corner Fill", "triangle": "Triangular Mirror",
         "diamond": "Diamond Mirror", "octagon": "Octagonal Mirror", "arch": "Arched Mirror", "ring": "Ring Mirror", "heart": "Heart Mirror"}


def rotated(rows, quarter_turns):
    """Clockwise, seen from the front."""
    for _ in range(quarter_turns % 4):
        rows = [[rows[x][SIZE - 1 - y] for x in range(SIZE)] for y in range(SIZE)]
    return rows


def spans(row):
    """(start, end) of every run of pixels in a row."""
    found, start = [], None
    for x, filled in enumerate(row + [False]):
        if filled and start is None:
            start = x
        if not filled and start is not None:
            found.append((start, x))
            start = None
    return found


def rectangles(rows):
    """Row spans, with identical spans on neighbouring rows joined into taller rectangles."""
    open_spans, done = {}, []
    for y, row in enumerate(rows + [[False] * SIZE]):
        current = set(spans(row))
        for span in list(open_spans):
            if span not in current:
                done.append((span[0], open_spans.pop(span), span[1], y))
        for span in current:
            open_spans.setdefault(span, y)
    return sorted(done, key=lambda r: (r[1], r[0]))


def model(rows, plate):
    """The outline on the block, its mirror face looking north: one pixel thick on the south side, or all the way through."""
    elements = []
    for x0, y0, x1, y1 in rectangles(rows):
        # Seen from the north, the model's x axis runs to the left, so the mask is laid out mirrored along it.
        a, b = SIZE - x1, SIZE - x0
        faces = {side: {"texture": "#all"} for side in ("north", "south", "east", "west", "up", "down")}
        faces["north"]["uv"] = [x0, SIZE - y1, x1, SIZE - y0]
        faces["south"]["uv"] = [a, SIZE - y1, b, SIZE - y0]
        faces["south"]["cullface"] = "south"
        if not plate:
            faces["north"]["cullface"] = "north"
        for side, at_edge in (("west", a == 0), ("east", b == SIZE), ("down", y0 == 0), ("up", y1 == SIZE)):
            if at_edge:
                faces[side]["cullface"] = side
        elements.append({"from": [a, y0, 15 if plate else 0], "to": [b, y1, 16], "faces": faces})
    return {"parent": "minecraft:block/block", "textures": {"all": "simplymirrorsplus:block/mirror", "particle": "simplymirrorsplus:block/mirror"},
            "elements": elements}


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def main():
    # Every form but the full block: its name, its outline and whether it is a plate.
    forms = [("panel", "panel", True)]
    for name in list(SHAPES)[1:11]:
        forms += [(name, name, False), (name + "_panel", name, True)]
    # The joined forms of the round mirror are placed by the game, never by the player: no item, no turning.
    joined = [(f"circle_c{corners}", f"circle_c{corners}", False) for corners in range(1, 16)]
    joined += [(f"circle_panel_c{corners}", f"circle_c{corners}", True) for corners in range(1, 16)]
    variants = {"shape=full": {"model": "simplymirrorsplus:block/mirror"}, "shape=double_slab": {"model": "simplymirrorsplus:block/mirror"}}
    turns = {"north": 0, "east": 90, "south": 180, "west": 270}
    write_json(ASSETS / "models/block/mirror_slab.json", {"parent": "minecraft:block/slab", "textures": {
        "bottom": "simplymirrorsplus:block/mirror", "top": "simplymirrorsplus:block/mirror", "side": "simplymirrorsplus:block/mirror"}})
    variants["shape=slab,face=floor"] = {"model": "simplymirrorsplus:block/mirror_slab"}
    variants["shape=slab,face=ceiling"] = {"model": "simplymirrorsplus:block/mirror_slab", "x": 180}
    for facing, y in turns.items():
        # A slab against a wall: the bottom half, tipped so it lies on the side the mirror is attached to.
        variants[f"shape=slab,face=wall,facing={facing}"] = {"model": "simplymirrorsplus:block/mirror_slab", "x": 90, "y": y}
    for stale in (ASSETS / "models/block").glob("mirror_*_[0-3].json"):
        stale.unlink()
    for form, outline, plate in forms:
        for rotation in range(4):
            write_json(ASSETS / f"models/block/mirror_{form}_{rotation}.json", model(rotated(SHAPES[outline], rotation), plate))
            for facing, y in turns.items():
                base = {"model": f"simplymirrorsplus:block/mirror_{form}_{rotation}"}
                variants[f"shape={form},face=wall,facing={facing},rotation={rotation}"] = {**base, **({"y": y} if y else {})}
                # Turning the model about x by 270 lays it on the bottom of the block with its face up; 90 puts it on top.
                variants[f"shape={form},face=floor,facing={facing},rotation={rotation}"] = {**base, "x": 270, **({"y": y} if y else {})}
                variants[f"shape={form},face=ceiling,facing={facing},rotation={rotation}"] = {**base, "x": 90, **({"y": y} if y else {})}
    for form, outline, plate in joined:
        write_json(ASSETS / f"models/block/mirror_{form}_0.json", model(SHAPES[outline], plate))
        for facing, y in turns.items():
            base = {"model": f"simplymirrorsplus:block/mirror_{form}_0"}
            variants[f"shape={form},face=wall,facing={facing}"] = {**base, **({"y": y} if y else {})}
            variants[f"shape={form},face=floor,facing={facing}"] = {**base, "x": 270, **({"y": y} if y else {})}
            variants[f"shape={form},face=ceiling,facing={facing}"] = {**base, "x": 90, **({"y": y} if y else {})}
    write_json(ASSETS / "blockstates/mirror.json", {"variants": variants})

    # One item per form. Its model is the block model; item definitions (items/) exist from Minecraft 1.21.4.
    lang_path = ASSETS / "lang/en_us.json"
    lang = {key: value for key, value in json.loads(lang_path.read_text()).items() if not key.startswith("block.simplymirrorsplus.mirror.")}
    items = {"mirror": ("full", "simplymirrorsplus:block/mirror"), "mirror_slab": ("slab", "simplymirrorsplus:block/mirror_slab")}
    lang["block.simplymirrorsplus.mirror.slab"] = NAMES["slab"]
    for form, outline, plate in forms:
        items["mirror_" + form] = (form, f"simplymirrorsplus:block/mirror_{form}_0")
        lang["block.simplymirrorsplus.mirror." + form] = NAMES[outline] + (" Panel" if plate and outline != "panel" else "")
    lang_path.write_text(json.dumps(lang, indent=2) + "\n")
    for stale in list(DATA.glob("recipe/mirror_*.json")) + list((ASSETS / "models/item").glob("mirror_*.json")) + list((ASSETS / "items").glob("mirror_*.json")):
        stale.unlink()
    def pool(item, form):
        return {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"simplymirrorsplus:{item}", "functions": [
            {"function": "minecraft:copy_nbt", "source": "block_entity", "ops": [{"source": "Frame", "target": "MirrorFrame", "op": "replace"}]}]}],
            "conditions": [{"condition": "minecraft:survives_explosion"},
                           {"condition": "minecraft:block_state_property", "block": "simplymirrorsplus:mirror", "properties": {"shape": form}}]}

    # Two slabs in one block drop as two slabs; a joined round mirror drops as the round mirror it was placed as.
    pools = [pool("mirror_slab", "double_slab"), pool("mirror_slab", "double_slab")]
    pools += [pool("mirror_circle_panel" if plate else "mirror_circle", form) for form, outline, plate in joined]
    for item, (form, parent) in items.items():
        write_json(ASSETS / f"models/item/{item}.json", {"parent": parent})
        write_json(ASSETS / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"simplymirrorsplus:item/{item}"}})
        if item != "mirror":
            # The crafted mirror is the full block; the stonecutter turns any form into any other, one for one.
            write_json(DATA / f"recipe/{item}.json", {"type": "minecraft:stonecutting", "ingredient": "#simplymirrorsplus:mirrors",
                                                      "result": {"id": f"simplymirrorsplus:{item}", "count": 1}})
        pools.append(pool(item, form))
    # In the crafting grid a mirror on its own becomes the next form, so every form can also be reached by hand.
    order = list(items)
    for item, following in zip(order, order[1:] + order[:1]):
        write_json(DATA / f"recipe/mirror_next_{items[following][0]}.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                   "ingredients": [f"simplymirrorsplus:{item}"], "result": {"id": f"simplymirrorsplus:{following}", "count": 1}})
    write_json(DATA / "recipe/mirror_full.json", {"type": "minecraft:stonecutting", "ingredient": "#simplymirrorsplus:mirrors",
                                                  "result": {"id": "simplymirrorsplus:mirror", "count": 1}})
    write_json(DATA / "loot_table/blocks/mirror.json", {"type": "minecraft:block", "pools": pools})
    # Rechiseled reads this folder when it is installed: its chisel then turns any form into any other.
    write_json(DATA / "chiseling_recipes/mirror.json", {"type": "rechiseled:chiseling", "overwrite": False,
                                                        "entries": [{"item": f"simplymirrorsplus:{item}"} for item in items]})
    write_json(DATA / "tags/item/mirrors.json", {"values": [f"simplymirrorsplus:{item}" for item in items]})

    lines = ["package com.malidev.simplymirrorsplus.math;", "",
             "/** Generated by tools/shapes.py from the same pixels as the block models. Do not edit. */", "final class MirrorShapeMasks {",
             "    /** One entry per outline: 16 rows from the bottom up, bit x set when pixel x belongs to the shape. */",
             "    static final int[][] ROWS={"]
    for name in SHAPES:
        bits = [sum(1 << x for x in range(SIZE) if row[x]) for row in SHAPES[name]]
        lines.append("        {" + ",".join(f"0x{value:04X}" for value in bits) + "}, // " + name)
    lines += ["    };", "", "    private MirrorShapeMasks() { }", "}", ""]
    JAVA.write_text("\n".join(lines))
    print(f"{len(items)} forms, {len(variants)} block states")


if __name__ == "__main__":
    main()
