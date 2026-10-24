# BetterBundleHUD Continued

A client-side **Fabric mod for Minecraft Java 26.3** that displays the contents of your bundles beside the inventory.

This is an independent continuation of [BetterBundleHUD by zeroowo24](https://modrinth.com/project/3TR1DyhT), with the [original source history](https://github.com/zeroowo24/BetterBundleHUDMod) and CC0-1.0 license preserved.

## Features

- See items from all bundles in your main inventory and hotbar in one panel.
- Combine identical items from different bundles into a single entry with a total count.
- Added category assignments for all 152 item IDs added between Java 26.1 and 26.3, including every color variant.
- Vanilla-style inventory frame, slots, buttons, and scrollbar that reference Minecraft assets directly for resource-pack compatibility.
- Works client-side in single-player and multiplayer. No server installation is required.

## Requirements

- Minecraft Java Edition **26.3**
- Fabric Loader **0.20.1 or newer**
- **Fabric API** for Minecraft 26.3 (0.165.0+26.3 or a compatible later build)
- Java **25 or newer**

Install the mod JAR and Fabric API in your Minecraft instance's `mods` folder, then restart the game. Install only one version of BetterBundleHUD: this continuation and the original share the mod ID `better-bundle`.

## Using the panel

Open the inventory and close the vanilla recipe book using its green book button. The recipe book and bundle panel share the space beside the inventory. Opening the recipe book temporarily hides the bundle panel. The bundle icon at the right edge of the inventory toggles the panel independently.

Select a category tab to filter items, and scroll over the item grid to see more entries.

| Action on a HUD item | Result confirmed in-game |
| --- | --- |
| Shift-click | Collect all matching items that fit in available storage |
| Left-click | Collect one bundle entry |

Displayed totals may exceed normal stack limits; withdrawals obey normal inventory and stack limits. Items that do not fit remain in their bundles. Escape cancels the remaining transfer work and leaves already collected items in inventory.

[Source code and README](https://github.com/Lulukai/BetterBundleHUDMod) · [Report a bug](https://github.com/Lulukai/BetterBundleHUDMod/issues) · [Full category list](https://github.com/Lulukai/BetterBundleHUDMod/blob/main/docs/item-categories-26.3.md)

## Attribution

Original author: **zeroowo24**. Continuation maintained by **Lulukai**.

This continuation was developed with assistance from GPT.

The original repository's CC0-1.0 license is retained. Minecraft textures are referenced from the installed game and are not redistributed. This is an unofficial project, not affiliated with Mojang or Microsoft.

## Build from source

Install a Java 25 JDK and set `JAVA_HOME` to its location. The included wrapper downloads and verifies Gradle 9.8.0.

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```sh
bash ./gradlew build
```

The installable JAR is written to `build/libs/`. The `-sources.jar` contains source code for developers. GitHub Actions builds the project and uploads both artifacts on pushes and pull requests.
