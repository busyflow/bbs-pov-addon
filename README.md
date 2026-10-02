# BBS-POV

**BBS-POV** is a **BBS FS 2.5.2** add-on that lets you record, customize, and animate the Minecraft hotbar, hands, GUIs, and other first-person elements for your BBS films and replays.

## Features

### POV Editor
The main first-person editor inside BBS.

It is split into separate tabs so each part of the POV setup stays organized:

- **Hotbar**
- **Hand**
- **Bodypart**
- **Actions**

You can move, rotate, animate, and edit first-person elements directly from this editor.

### POV Camera Mode
A camera mode made specifically for first-person shots.

It follows the player's head position and rotation so recorded POV scenes feel natural and stay connected to the replay.

### POV Camera Clip
A dedicated film clip that can be placed on the BBS timeline to play recorded first-person POV animation inside a scene.

### Replay POV Edit Mode
Lets you open a recorded replay and directly edit its POV keyframes from the timeline.

This makes it possible to fix or adjust recorded first-person animation without recording the whole scene again.

### Bodypart Support
Lets you attach and animate custom model limbs and body parts in first person.

This is useful when you want more than the normal vanilla player arms and need custom models or extra animated parts in the POV view.

### POV Settings
BBS-POV adds its own section inside the normal BBS Settings.

Available options include:

- Action baking toggle
- Custom cursor texture
- Cursor cropping
- Cursor scale

### F4 Recording
Supports clean F4 video recording while keeping the fake POV GUI and hotbar visible correctly.

The vanilla Minecraft UI is kept out of the way so the recorded result matches the POV setup instead of showing duplicate UI elements.

### Replay Recording Keyframe Baking — Right Alt
While recording a replay, press **Right Alt** to bake live POV behavior into editable keyframes.

This can include things such as:

- Player movement-related POV changes
- Hand actions
- Hotbar state
- GUI interactions

The recorded result can then be edited later in the POV Editor.

### First-Person Playback — Right Ctrl
Press **Right Ctrl** to instantly preview the recorded POV animation and camera movement from the player's first-person view.

This is useful for quickly checking how the final POV shot feels without going through a full render.

## Basic Workflow

1. Record or open a replay in BBS.
2. Record POV data while using the hotbar, hands, and GUIs.
3. Use **Right Alt** when recording to bake POV behavior into editable keyframes.
4. Open the **POV Editor**.
5. Edit the **Hotbar**, **Hand**, **Bodypart**, and **Actions** tabs separately.
6. Use **POV Camera Mode** or a **POV Camera Clip** for the first-person shot.
7. Press **Right Ctrl** to preview the POV playback.
8. Use **F4** when you are ready to record the final result.

## Requirements

- **Minecraft:** 1.20.1
- **BBS FS:** 2.5.2
- **Fabric**
- **Fabric API**
- **Java 17+**

## Building From Source

Place `bbs-2.5.2-1.20.1.jar` inside the project's `libs/` folder.

Then run:

```bash
./gradlew build
```

The built add-on is written to `build/libs/`. Version is `1.1.0-beta`.

## Docs

- [Architecture](docs/ARCHITECTURE.md)
- [Save format (frozen IDs)](docs/SAVE_FORMAT.md)
