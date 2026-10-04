"""Reproducible original NV voxel companions. No downloaded models or textures.

Coordinates use Bedrock units (16 units = 1 block).
Head and faces are oriented towards -Z (Minecraft entity forward).
Atlas uses crisp lossless tiles without dirty border artifacts.
"""
import json
import math
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/resources/assets/nv"
FACES = ("north", "south", "east", "west", "up", "down")


def cube(origin, size, color=0, rotation=None, pivot=None):
    # Map face UVs cleanly to the 32x32 color tile with 1px inset to eliminate bleeding
    u0 = (color % 4) * 32 + 1
    v0 = (color // 4) * 32 + 1
    result = {"origin": origin, "size": size,
              "uv": {face: {"uv": [u0, v0], "uv_size": [30, 30]} for face in FACES}}
    if rotation:
        result.update(rotation=rotation, pivot=pivot or [origin[i] + size[i] / 2 for i in range(3)])
    return result


def bone(name, cubes=(), parent="root", pivot=(0, 0, 0), rotation=None):
    result = {"name": name, "pivot": list(pivot), "cubes": list(cubes)}
    if parent:
        result["parent"] = parent
    if rotation:
        result["rotation"] = rotation
    return result


def atlas(path, palette):
    # Generates a pristine 128x128 palette atlas without destructive dark border seams
    data = bytearray()
    for y in range(128):
        data.append(0)
        for x in range(128):
            tile = (y // 32) * 4 + x // 32
            base = palette[tile % len(palette)]
            u, v = x % 32, y % 32
            # Smooth gentle vertical gradient with subtle micro-grain
            grain = ((x * 17 + y * 11) % 5 - 2) * 0.25
            vert_shading = (15.5 - v) * 0.35 + grain
            # Soft center highlight
            dist_center = math.hypot(u - 15.5, v - 15.5) / 16.0
            highlight = max(0.0, 1.0 - dist_center) * 4.0
            r = max(0, min(255, round(base[0] + vert_shading + highlight)))
            g = max(0, min(255, round(base[1] + vert_shading + highlight)))
            b = max(0, min(255, round(base[2] + vert_shading + highlight)))
            data.extend([r, g, b, 255])

    def chunk(kind, payload):
        return struct.pack(">I", len(payload)) + kind + payload + struct.pack(">I", zlib.crc32(kind + payload))

    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", 128, 128, 8, 6, 0, 0, 0))
                     + chunk(b"IDAT", zlib.compress(bytes(data), 9)) + chunk(b"IEND", b""))


def save(name, bones, palette, animation_bones):
    description = {
        "identifier": f"geometry.nv_{name}",
        "texture_width": 128,
        "texture_height": 128,
        "visible_bounds_width": 3,
        "visible_bounds_height": 3,
        "visible_bounds_offset": [0, 0.5, 0]
    }
    model = {"format_version": "1.12.0", "minecraft:geometry": [{"description": description, "bones": bones}]}
    flight_anim = {"loop": True, "animation_length": 2, "bones": animation_bones}
    idle_anim = {"loop": True, "animation_length": 2, "bones": animation_bones}
    anim = {"format_version": "1.8.0", "animations": {"flight": flight_anim, "idle": idle_anim, "accelerate": flight_anim}}
    for path, obj in ((ROOT / f"geckolib/models/pets/{name}.geo.json", model),
                      (ROOT / f"geckolib/animations/pets/{name}.animation.json", anim)):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    atlas(ROOT / f"textures/entity/pets/{name}.png", palette)
    print(f"{name}: {len(bones)} bones, {sum(len(b.get('cubes', [])) for b in bones)} cubes")


def spirit():
    # Mystical Astral Floating Spirit Wisp
    # Teardrop body, curved ethereal horns, runic talisman face, floating wisps and halo
    bones = [
        bone("root", parent=None),
        bone("body", [
            cube([-3, 3, -2], [6, 6, 4], 0),
            cube([-2.5, 8.5, -1.5], [5, 1.5, 3], 1),
            cube([-2, 1, -1.5], [4, 2, 3], 1),
            cube([-1, -0.5, -1], [2, 1.5, 2], 2),
            cube([-.5, -1.5, -.5], [1, 1, 1], 2)
        ], pivot=[0, 5, 0]),
        bone("face", [
            cube([-2, 5.8, -2.15], [1.2, 1.6, .35], 3),
            cube([0.8, 5.8, -2.15], [1.2, 1.6, .35], 3),
            cube([-.6, 7.8, -2.18], [1.2, 1.2, .35], 2, [0, 0, 45]),
            cube([-.5, 4.5, -2.15], [1, .5, .3], 3)
        ], parent="body"),
        bone("tail", [
            cube([-1.2, -1.2, 0], [2.4, 2, 2], 1),
            cube([-0.8, -2.8, 0.5], [1.6, 2, 1.6], 2),
            cube([-0.4, -4.2, 1.0], [0.8, 1.8, 0.8], 3)
        ], parent="body", pivot=[0, 0, 0])
    ]
    for side in (-1, 1):
        bones.append(bone(f"horn_{side}", [
            cube([side * 2 - .4, 9, -.6], [.8, 3.2, 1], 2, [-15, 0, side * -20]),
            cube([side * 2.8 - .35, 11.5, -.2], [.7, 2.5, .8], 3, [-25, 0, side * -28])
        ], parent="body", pivot=[side * 2, 9, 0]))
        bones.append(bone(f"ribbon_{side}", [
            cube([side * 3 - .5, 2, -.5], [1, 5, 1], 1, [0, 0, side * -22]),
            cube([side * 4.2 - .4, -0.5, -.35], [.8, 3.5, .8], 2)
        ], parent="body", pivot=[side * 3, 5, 0]))

    # Orbiting will-o'-the-wisps (distinct to Spirit)
    bones.append(bone("wisp_0", [cube([-0.6, -0.6, -0.6], [1.2, 1.2, 1.2], 3)], parent="body", pivot=[0, 5, 0]))
    bones.append(bone("wisp_1", [cube([-0.6, -0.6, -0.6], [1.2, 1.2, 1.2], 2)], parent="body", pivot=[0, 5, 0]))

    # Astral Halo
    bones.append(bone("halo", [
        cube([-4, 12, -.4], [8, .6, .8], 3),
        cube([-4, 10.5, -.4], [.8, 1.5, .8], 3),
        cube([3.2, 10.5, -.4], [.8, 1.5, .8], 3)
    ], parent="body", pivot=[0, 12, 0]))

    # Colors: Celestial Cyan, Deep Ether, Bright Astral White, Glowing Azure
    save("spirit", bones, [(85, 215, 240), (28, 90, 135), (215, 255, 255), (0, 245, 255), (150, 240, 255)], {
        "body": {"position": [0, "math.sin(query.anim_time * 180) * 0.5", 0],
                 "rotation": [0, 0, "math.sin(query.anim_time * 180) * 2.5"]},
        "tail": {"rotation": ["math.sin(query.anim_time * 180 - 45) * 8", 0, "math.sin(query.anim_time * 180) * 6"]},
        "ribbon_-1": {"rotation": ["math.sin(query.anim_time * 180) * 10", 0, 0]},
        "ribbon_1": {"rotation": ["math.sin(query.anim_time * 180 + 70) * 10", 0, 0]},
        "wisp_0": {"position": ["math.sin(query.anim_time * 150) * 4.5", "math.cos(query.anim_time * 120) * 1.2", "math.cos(query.anim_time * 150) * 4.5"]},
        "wisp_1": {"position": ["-math.sin(query.anim_time * 150 + 90) * 4.8", "math.sin(query.anim_time * 140) * 1.0", "-math.cos(query.anim_time * 150 + 90) * 4.8"]},
        "halo": {"rotation": [0, "query.anim_time * 45", 0]}
    })


def mascot():
    # Official NV Cyber-Drone / Mecha-Companion (COMPLETELY UNIQUE SILHOUETTE)
    # Faceted mecha chassis, angular visor, robotic ears, dual thruster pods, segmented cyber-tail, floating energy core
    bones = [
        bone("root", parent=None),
        # Faceted robotic main chassis
        bone("body", [
            cube([-3.5, 3.5, -2.5], [7, 6, 5], 0),      # Obsidian chassis
            cube([-3, 2, -2], [6, 2, 4], 1),           # Lower chassis belly
            cube([-2.5, 9, -2], [5, 1.5, 4], 1),       # Upper chassis cowl
            cube([-2, 3, -2.7], [4, 1.2, .4], 2),       # Front vent grille
            cube([-1.5, 4.4, -2.7], [3, 0.8, .4], 2),   # Secondary vent
            cube([-2, 4, 2.5], [4, 4.5, 0.6], 1)       # Back battery capacitor
        ], pivot=[0, 6, 0]),
        # Curved Cybernetic Visor (Signature neon chevron / eye)
        bone("visor", [
            cube([-3.2, 5.8, -2.85], [6.4, 2.5, .55], 0), # Visor housing
            cube([-3.0, 6.0, -3.0], [6.0, 2.1, .35], 3),  # Cyan glowing visor
            cube([-1.2, 6.3, -3.15], [2.4, 1.5, .3], 4),  # Bright core chevron
            cube([-2.5, 6.5, -3.1], [0.8, 1.1, .25], 2),  # Left auxiliary optic
            cube([1.7, 6.5, -3.1], [0.8, 1.1, .25], 2)    # Right auxiliary optic
        ], parent="body"),
        # Sleek Mecha Ears / Sensor Antennae
        bone("ear_left", [
            cube([2.2, 9.8, -1.2], [1.2, 4, 2.2], 0, [0, 0, 20]),
            cube([2.4, 13.5, -1.0], [0.8, 1.5, 1.8], 2, [0, 0, 20]),
            cube([2.6, 14.8, -0.8], [0.4, 1.2, 1.4], 3, [0, 0, 20])
        ], parent="body", pivot=[2.5, 10, 0]),
        bone("ear_right", [
            cube([-3.4, 9.8, -1.2], [1.2, 4, 2.2], 0, [0, 0, -20]),
            cube([-3.2, 13.5, -1.0], [0.8, 1.5, 1.8], 2, [0, 0, -20]),
            cube([-3.0, 14.8, -0.8], [0.4, 1.2, 1.4], 3, [0, 0, -20])
        ], parent="body", pivot=[-2.5, 10, 0]),
        # Left and Right Floating Cyber-Thruster Pods (Unique to Mascot)
        bone("thruster_left", [
            cube([4.2, 4, -1.5], [2.2, 4.5, 3.5], 1),   # Outer thruster pod
            cube([4.5, 2.8, -1.0], [1.6, 1.4, 2.5], 2), # Thruster nozzle cowling
            cube([4.7, 1.6, -0.7], [1.2, 1.4, 1.9], 3), # Glowing plasma jet
            cube([4.8, 8.2, -1.2], [1.0, 1.2, 2.8], 2)  # Vectoring winglet
        ], parent="body", pivot=[4.5, 6, 0]),
        bone("thruster_right", [
            cube([-6.4, 4, -1.5], [2.2, 4.5, 3.5], 1),  # Outer thruster pod
            cube([-6.1, 2.8, -1.0], [1.6, 1.4, 2.5], 2),# Thruster nozzle cowling
            cube([-5.9, 1.6, -0.7], [1.2, 1.4, 1.9], 3),# Glowing plasma jet
            cube([-5.8, 8.2, -1.2], [1.0, 1.2, 2.8], 2) # Vectoring winglet
        ], parent="body", pivot=[-4.5, 6, 0]),
        # Multi-jointed robotic cyber-tail with energy nodes
        bone("tail_0", [cube([-0.8, 4, 2.5], [1.6, 1.6, 2.5], 1)], parent="body", pivot=[0, 4.8, 2.5]),
        bone("tail_1", [cube([-0.6, 4.2, 4.8], [1.2, 1.2, 2.5], 2)], parent="tail_0", pivot=[0, 4.8, 4.8]),
        bone("tail_2", [
            cube([-0.4, 4.4, 7.1], [0.8, 0.8, 2.2], 1),
            cube([-1.2, 4.2, 8.8], [2.4, 1.2, 1.5], 3)  # Glowing energy emitter tip
        ], parent="tail_1", pivot=[0, 4.8, 7.1]),
        # Floating Holographic Data Crown / Levitating Energy Core (NO COLLISION with thrusters or body)
        bone("energy_core", [
            cube([-1.0, 15.5, -1.0], [2.0, 2.0, 2.0], 4, [0, 45, 0]), # Central radiant power crystal
            cube([-2.8, 15.8, -0.4], [0.8, 1.4, 0.8], 2),             # Orbiting node West
            cube([2.0, 15.8, -0.4], [0.8, 1.4, 0.8], 2),              # Orbiting node East
            cube([-0.4, 15.8, -2.8], [0.8, 1.4, 0.8], 3),             # Orbiting node North
            cube([-0.4, 15.8, 2.0], [0.8, 1.4, 0.8], 3),              # Orbiting node South
            cube([-2.2, 16.2, -2.2], [4.4, 0.4, 4.4], 3)              # Hovering data halo frame
        ], parent="body", pivot=[0, 16.5, 0])
    ]

    # Colors: 0: Obsidian (24, 26, 34), 1: Slate Cyber (45, 52, 68), 2: Launcher Violet (123, 75, 255), 3: Electric Cyan (0, 240, 255), 4: Neon Core (235, 220, 255)
    save("mascot", bones, [(24, 26, 34), (45, 52, 68), (123, 75, 255), (0, 240, 255), (235, 220, 255)], {
        "body": {"position": [0, "math.sin(query.anim_time * 180) * 0.4", 0]},
        "ear_left": {"rotation": [0, 0, "math.sin(query.anim_time * 180) * 4"]},
        "ear_right": {"rotation": [0, 0, "-math.sin(query.anim_time * 180) * 4"]},
        "thruster_left": {"rotation": ["math.sin(query.anim_time * 180) * 6", 0, "math.sin(query.anim_time * 180) * 4"]},
        "thruster_right": {"rotation": ["math.sin(query.anim_time * 180) * 6", 0, "-math.sin(query.anim_time * 180) * 4"]},
        "tail_0": {"rotation": [0, "math.sin(query.anim_time * 180) * 8", 0]},
        "tail_1": {"rotation": [0, "math.sin(query.anim_time * 180 - 30) * 12", 0]},
        "tail_2": {"rotation": [0, "math.sin(query.anim_time * 180 - 60) * 16", 0]},
        "energy_core": {
            "position": [0, "math.sin(query.anim_time * 180 + 30) * 0.3", 0],
            "rotation": [0, "query.anim_time * 90", 0]
        }
    })


def dragon():
    # Celestial Astral Dragon Companion
    # Facing forward (-Z), noble horned head, glowing eyes, stepped wings, spinal crest, spade tail
    bones = [
        bone("root", parent=None),
        bone("body", [
            cube([-2.5, 3, -2], [5, 5, 7], 0),       # Dragon torso
            cube([-1.8, 3, -2.15], [3.6, 4, .5], 1),  # Chest scales
            cube([-1.8, 2.5, 0], [3.6, 1, 5], 1)      # Belly scales
        ], pivot=[0, 6, 1]),
        # Head facing FORWARD (-Z direction)
        bone("head", [
            cube([-3, 7, -5], [6, 4, 5], 0),          # Cranium
            cube([-2.2, 7, -7.5], [4.4, 2.2, 3], 1),  # Snout
            cube([-1.8, 8.8, -7.6], [0.6, 0.5, 0.4], 3), # Left glowing nostril
            cube([1.2, 8.8, -7.6], [0.6, 0.5, 0.4], 3),  # Right glowing nostril
            cube([-3.15, 8.5, -4.5], [0.35, 1.4, 1.6], 3), # Glowing left dragon eye
            cube([2.8, 8.5, -4.5], [0.35, 1.4, 1.6], 3),  # Glowing right dragon eye
            cube([-1.5, 6.2, -6.8], [3, 1, 3], 2)     # Lower jaw
        ], parent="body", pivot=[0, 8, -2]),
        bone("crest", [
            cube([-0.5, 10.5, -4.5], [1, 2.2, 3], 2, [-18, 0, 0])
        ], parent="head", pivot=[0, 10.5, -3])
    ]
    for side in (-1, 1):
        # Swept Astral Horns
        bones.append(bone(f"horn_{side}", [
            cube([side * 2.2 - .45, 10.5, -2.5], [.9, 3.5, 1.2], 2, [-24, 0, -side * 18]),
            cube([side * 2.8 - .35, 13.2, -1.5], [.7, 2.2, .8], 3, [-32, 0, -side * 22])
        ], "head", [side * 2.2, 10.5, -2.5]))
        # Folded Talons
        bones.append(bone(f"leg_{side}", [
            cube([side * 2 - .75, 1.5, 1], [1.5, 3, 2], 0),
            cube([side * 2 - 1, 0.8, -.5], [2, 1, 3], 2)
        ], "body", [side * 2, 4, 2]))
        # Swept Astral Bat Wings
        wing = []
        for i in range(4):
            x = 2.5 + i * 1.8
            origin_x = x if side > 0 else -x - 1.8
            wing.append(cube([origin_x, 6.7, -1 + i * .7], [1.8, .22, 5.5 - i * .7], 4 if i % 2 else 1))
            wing.append(cube([origin_x, 6.6, -1 + i * .7], [1.8, .45, .4], 2))
        wing.append(cube([2.5 if side > 0 else -9.7, 6.8, 4.2], [7.2, .35, .4], 3))
        bones.append(bone(f"wing_{side}", wing, "body", [side * 2.5, 7, 0], [0, 0, side * 12]))

    # Long articulated tail with dorsal spines and diamond fin
    for i in range(3):
        cubes = [
            cube([-1.5 + i * .35, 4 + i * .2, 4.5 + i * 2.3], [3 - i * .7, 2 - i * .4, 3], 0),
            cube([-.35, 6 + i * .2, 5.5 + i * 2.3], [.7, 1.2, 1], 2)
        ]
        if i == 2:
            # Astral diamond tail spade/fin
            cubes.append(cube([-1.8, 4.5, 10.5], [3.6, 0.4, 2.5], 3, [0, 0, 45]))
        bones.append(bone(f"tail_{i}", cubes, "body" if i == 0 else f"tail_{i-1}", [0, 5, 4.5 + i * 2.3]))

    # Dorsal Spines along torso
    bones.append(bone("spines", [cube([-.45, 8, z], [.9, 1.6, 1], 2, [-20, 0, 0]) for z in (0, 2, 4)], "body"))

    # Colors: Astral Navy, Sapphire Scales, Obsidian Spines, Radiant Gold Eyes/Tips, Starlight Cyan Membrane
    save("dragon", bones, [(38, 54, 96), (72, 120, 172), (20, 26, 46), (255, 210, 75), (85, 175, 225)], {
        "body": {"position": [0, "math.sin(query.anim_time * 180) * 0.35", 0]},
        "wing_-1": {"rotation": [0, 0, "math.sin(query.anim_time * 360) * -24"]},
        "wing_1": {"rotation": [0, 0, "math.sin(query.anim_time * 360) * 24"]},
        "head": {"rotation": ["math.sin(query.anim_time * 90) * 4", 0, 0]},
        **{f"tail_{i}": {"rotation": [0, f"math.sin(query.anim_time * 180 - {i * 35}) * 8", 0]} for i in range(3)}
    })


def moth():
    # Celestial Moonlit Silk-Moth
    # Fluffy thorax, glowing silk abdomen, feathery antennae, 4 layered lunar wings with glowing eye-spots
    bones = [
        bone("root", parent=None),
        bone("body", [
            cube([-1.2, 3, -1], [2.4, 5, 2], 0),       # Furry thorax
            cube([-1.6, 7, -1.4], [3.2, 2.4, 2.8], 1), # Fluffy head
            cube([-1.7, 7.8, -1.65], [1.1, 0.9, .45], 2), # Left glowing compound eye
            cube([0.6, 7.8, -1.65], [1.1, 0.9, .45], 2),  # Right glowing compound eye
            cube([-.8, 1.8, -.8], [1.6, 2.2, 1.6], 3),  # Glowing silk abdomen
            cube([-.5, 0.6, -.5], [1, 1.2, 1], 2)       # Abdomen tip
        ], pivot=[0, 6, 0])
    ]
    for side in (-1, 1):
        # Curved feathery antennae
        bones.append(bone(f"antenna_{side}", [
            cube([side * .8 - .15, 9, -.4], [.3, 3.2, .3], 1, [0, 0, -side * 22]),
            cube([side * 1.8 - .4, 11.8, -.6], [.8, 1.2, .8], 2, [0, 0, -side * 15]),
            cube([side * 2.5 - .35, 12.8, -.5], [.6, 1.4, .6], 3)
        ], "body", [side * .8, 9, 0]))

        # Four stepped lunar wings with glowing eye-spots
        pieces = []
        for i, (width, bottom, height) in enumerate(((2.2, 3, 7.5), (2.2, 2, 9.5), (2.2, 3, 8.5), (1.6, 5, 5.5))):
            x = 1 + i * 2.2
            ox = x if side > 0 else -x - width
            pieces.append(cube([ox, bottom, -.12], [width, height, .24], 4 if i % 2 else 0))
            pieces.append(cube([ox, bottom, -.22], [width, .5, .44], 1))
            pieces.append(cube([ox, bottom + height - .5, -.22], [width, .5, .44], 1))
        # Radiant gold & cyan moon eye-spots on wings
        ox = 4.2 if side > 0 else -6.4
        pieces.extend([
            cube([ox, 5.2, -.28], [2.2, 2.8, .56], 2),
            cube([ox + .5, 5.8, -.33], [1.2, 1.6, .66], 3)
        ])
        bones.append(bone(f"wing_{side}", pieces, "body", [side, 6, 0], [0, side * -20, 0]))

    # Colors: Velvet Twilight, Moonlit Cream, Radiant Lunar Gold, Glowing Jade Dust, Silk Shimmer
    save("moth", bones, [(85, 62, 125), (230, 224, 210), (255, 205, 75), (95, 245, 200), (135, 95, 185)], {
        "body": {"position": [0, "math.sin(query.anim_time * 180) * 0.4", 0]},
        "wing_-1": {"rotation": [0, "math.sin(query.anim_time * 720) * -34", 0]},
        "wing_1": {"rotation": [0, "math.sin(query.anim_time * 720) * 34", 0]},
        "antenna_-1": {"rotation": ["math.sin(query.anim_time * 180) * 6", 0, 0]},
        "antenna_1": {"rotation": ["math.sin(query.anim_time * 180 + 45) * 6", 0, 0]}
    })


if __name__ == "__main__":
    spirit()
    dragon()
    moth()
    mascot()
