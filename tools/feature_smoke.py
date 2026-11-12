"""Run the mirror features in a real client against a disposable server and record each scene.

    python3 tools/feature_smoke.py RUN_NAME TARGET [--quick] [--no-faces] [--stack] [--shaders] [--only 10,11]

Each scene sets a camera pose and mirror settings, waits for the reflection to settle, then writes
mirror-<scene>.png and result-<scene>.json into <target>/run/<run>-client/visual-evidence.
--stack adds the locked Sodium and Iris jars, --shaders also enables the locked shader pack.
"""
import argparse
import json
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tools"))
import visual_smoke as vs  # noqa: E402

BASE = {"x": .5, "y": 66.62, "z": -3.5, "yaw": 0, "pitch": 0}
MIRROR_A = "fill -2 65 0 2 68 0 simplymirrorsplus:mirror[facing=north{}]"
FEATURE_WORLD = [
    "fill -12 54 -12 12 79 12 air",
    "fill -12 64 -12 12 64 12 stone",
    MIRROR_A.format(""),
    "fill -2 65 -7 2 68 -7 simplymirrorsplus:mirror[facing=south]",
    "fill -5 65 -5 -4 69 -2 red_concrete",
    "fill 5 65 -5 6 69 -2 blue_concrete",
]
SCENES = {
    10: ("nested-120", {"fps": 120}),
    11: ("players-only", {"playersOnly": "true", "fps": 120}),
    12: ("resolution-25", {"resolution": 25, "fps": 120}),
    13: ("off", {"enabled": "false"}),
    14: ("spin-30", {"fps": 30, "spin": 15}),
    15: ("rate-30", {"fps": 30}),
    16: ("rate-60", {"fps": 60}),
    17: ("rate-90", {"fps": 90}),
    18: ("rate-120", {"fps": 120}),
    19: ("players-rate-120", {"playersOnly": "true", "fps": 120}),
    20: ("fabulous-50", {"fabulous": "true", "resolution": 50, "fps": 60}),
    21: ("fabulous-100", {"fabulous": "true", "fps": 120}),
    40: ("mode-off", {"fps": 120}, [MIRROR_A.format(",mode=off")]),
    41: ("mode-players", {"fps": 120}, [MIRROR_A.format(",mode=players_only")]),
    42: ("redstone", {"fps": 120}, [MIRROR_A.format(""), "setblock 0 66 1 redstone_block"]),
    43: ("mode-restored", {"fps": 120}, ["setblock 0 66 1 air", MIRROR_A.format("")]),
    # Needs a probe that sends the sneak and use packets (1.21.4 only); not part of --quick.
    44: ("connected-switch", {"fps": 120, "use": "true"}, [MIRROR_A.format("")],
         ["execute if block -2 65 0 simplymirrorsplus:mirror[mode=players_only]",
          "execute if block 2 68 0 simplymirrorsplus:mirror[mode=players_only]",
          "execute if block 0 66 -7 simplymirrorsplus:mirror[mode=normal]"]),
    30: ("menu", {"menu": "true", "enabled": "true", "resolution": 50, "fps": 30}),
}
QUICK = (10, 11, 12, 13, 15, 18, 20, 40, 41, 42, 43, 30)
FAILED_WORDS = ("Unknown", "Incorrect", "Invalid", "too many blocks")


def command(rcon, text):
    reply = rcon.command(text)
    if any(word in reply for word in FAILED_WORDS):
        raise RuntimeError(f"{text}: {reply}")
    return reply


def wait(result, client, seconds, label):
    deadline = time.monotonic() + seconds
    while time.monotonic() < deadline and not result.exists():
        if client.poll() is not None:
            raise RuntimeError(f"Client exited during {label}")
        time.sleep(.5)
    if not result.exists():
        raise TimeoutError(f"Timed out on {label}")
    time.sleep(.2)
    return json.loads(result.read_text())


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("run")
    parser.add_argument("target", choices=vs.TARGETS)
    parser.add_argument("--quick", action="store_true")
    parser.add_argument("--no-faces", action="store_true")
    parser.add_argument("--stack", action="store_true")
    parser.add_argument("--shaders", action="store_true")
    parser.add_argument("--only", help="comma separated scene numbers")
    args = parser.parse_args()
    spec = vs.TARGETS[args.target]
    minecraft = spec["minecraft"]
    scenes = dict(SCENES)
    if args.quick:
        scenes = {k: v for k, v in scenes.items() if k in QUICK}
    if args.only:
        wanted = {int(k) for k in args.only.split(",")}
        scenes = {k: v for k, v in scenes.items() if k in wanted}
    if args.shaders:
        scenes.pop(20, None)
        scenes.pop(21, None)

    module = ROOT / args.target
    cwd = ROOT / spec["build_root"]
    prefix = f":{args.target}:" if spec["build_root"] == "." else ""
    client_dir = module / f"run/{args.run}-client"
    server_dir = module / f"run/{args.run}-server"
    evidence = module / f"run/{args.run}-evidence"
    for directory in (client_dir, server_dir, evidence):
        directory.mkdir(parents=True, exist_ok=False)
    port, rcon_port = vs.free_port(), vs.free_port()
    (server_dir / "eula.txt").write_text("eula=true\n")
    props = {"server-ip": "127.0.0.1", "server-port": port, "enable-rcon": "true", "rcon.port": rcon_port,
             "rcon.password": "simplymirrorsplus-disposable-test", "online-mode": "false", "gamemode": "creative",
             "force-gamemode": "true", "allow-flight": "true", "spawn-protection": 0, "view-distance": 4,
             "simulation-distance": 4, "level-name": "visual-world",
             "level-type": "flat" if minecraft == "1.16.5" else "minecraft:flat", "max-players": 2}
    (server_dir / "server.properties").write_text("\n".join(f"{k}={v}" for k, v in props.items()) + "\n")
    gradle = ["./gradlew"] + ([f"-Ptarget={args.target}"] if prefix else []) + [
        "-PvisualTest", f"-PvisualMc={minecraft}", f"-PvisualLoader={spec['loader']}", f"-PvisualPort={port}",
        "--no-daemon", "--console=plain", "-Dorg.gradle.jvmargs=-Xmx1024m", "--max-workers=1",
        f"-PvisualRunName={args.run}", "-PmirrorsShaderStack=off"]
    modern = minecraft.startswith("26.") or minecraft == "1.21.11"
    server = client = rcon = None
    results = {}
    with (evidence / "server.log").open("w") as server_log, (evidence / "client.log").open("w") as client_log:
        try:
            server = subprocess.Popen(gradle + [prefix + "runVisualServer"], cwd=cwd, stdout=server_log, stderr=subprocess.STDOUT, start_new_session=True)
            deadline = time.monotonic() + 420
            while time.monotonic() < deadline:
                if server.poll() is not None:
                    raise RuntimeError("Server exited early")
                try:
                    rcon = vs.Rcon(rcon_port)
                    break
                except (OSError, ConnectionError):
                    time.sleep(1)
            else:
                raise TimeoutError("Server startup")
            print("server ready", flush=True)
            setup = ["gamerule advance_time false" if modern else "gamerule doDaylightCycle false",
                     "gamerule advance_weather false" if modern else "gamerule doWeatherCycle false",
                     "forceload add -16 -16 16 16", "time set noon", "weather clear", "fill -12 53 -12 12 53 12 stone"]
            if not modern:
                setup.append("gamerule spawnRadius 0")
            setup.append("setworldspawn -2 54 3")
            for text in setup + vs.fixture(0):
                command(rcon, text)
            vs.scene_file(client_dir, -1)
            if args.stack or args.shaders:
                import shader_stack
                jars, pack = shader_stack.prepare(args.target, ROOT / "build/shader-dependencies")
                print("extra mods", [m["file"] for m in vs.install_extra_client_mods(client_dir, jars)], flush=True)
                if args.shaders:
                    shader_stack.install_pack(client_dir, pack)
            client = subprocess.Popen(gradle + [prefix + "runVisualClient"], cwd=cwd, stdout=client_log, stderr=subprocess.STDOUT, start_new_session=True)
            print("client starting", flush=True)
            out = client_dir / "visual-evidence"
            if not args.no_faces:
                for scene, face in enumerate(vs.FACES):
                    if scene:
                        for text in vs.fixture(scene):
                            command(rcon, text)
                    vs.activate_scene(rcon, client_dir, scene, client)
                    data = wait(out / f"result-{scene}.json", client, 600 if scene == 0 else 180, face)
                    try:
                        data.update(vs.image_check(out / f"mirror-{scene}.png", shaders=args.shaders, face_index=scene))
                    except AssertionError as error:
                        data["imageError"] = str(error)
                    results[face] = data
                    print(face, json.dumps(data), flush=True)
            for text in FEATURE_WORLD:
                command(rcon, text)
            time.sleep(3)
            for scene, scene_spec in scenes.items():
                name, settings = scene_spec[0], scene_spec[1]
                for text in (scene_spec[2] if len(scene_spec) > 2 else []):
                    command(rcon, text)
                values = {**BASE, **settings}
                deadline = time.monotonic() + 600
                while time.monotonic() < deadline:
                    if client.poll() is not None:
                        raise RuntimeError("Client exited before feature scene")
                    if "Teleported" in rcon.command(f"tp @a {values['x']} 65 {values['z']} {values['yaw']} {values['pitch']}"):
                        break
                    time.sleep(1)
                (client_dir / "visual-feature.properties").write_text("".join(f"{k}={v}\n" for k, v in values.items()))
                vs.scene_file(client_dir, scene)
                data = wait(out / f"result-{scene}.json", client, 240, name)
                for check in (scene_spec[3] if len(scene_spec) > 3 else []):
                    data[check] = "passed" in rcon.command(check).lower()
                results[name] = data
                print(name, json.dumps(data), flush=True)
            vs.scene_file(client_dir, 6)
            print("client exit", client.wait(timeout=120), flush=True)
            (evidence / "summary.json").write_text(json.dumps(results, indent=2))
            print("FEATURE_SMOKE_DONE", flush=True)
        except BaseException as error:
            print("FEATURE_SMOKE_FAIL", repr(error), flush=True)
            raise
        finally:
            vs.stop(client)
            if rcon:
                try:
                    rcon.command("stop")
                except OSError:
                    pass
                rcon.close()
            if server:
                try:
                    server.wait(timeout=30)
                except subprocess.TimeoutExpired:
                    pass
            vs.stop(server)


if __name__ == "__main__":
    main()
