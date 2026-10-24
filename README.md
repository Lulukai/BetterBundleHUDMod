# BetterBundleHUD Continued

A client-side Fabric mod for **Minecraft Java 26.3** that displays the contents of your bundles beside the inventory. An independent continuation of [zeroowo24's BetterBundleHUD](https://github.com/zeroowo24/BetterBundleHUDMod), preserving the upstream source history and CC0 license.

## Features

- One inventory panel for the contents of every bundle in your main inventory and hotbar.
- Matching items from different bundles combined into one HUD entry with their total count.
- Items with different names, enchantments, damage, or other components kept separate.
- Eight English category tabs, including **Decorations**.
- All 152 item IDs added between Minecraft 26.1 and 26.3 categorized, including every color variant. See the [complete category audit](docs/item-categories-26.3.md).
- Vanilla inventory frames, slots, buttons, and scrollbars, using Minecraft assets directly for resource-pack compatibility.
- A simple **All Items** heading; no search field or mouse-action instructions in item tooltips.
- Client-side operation in single-player and multiplayer, with no server installation required.

## Install

You need:

| Requirement | Version |
| --- | --- |
| Minecraft Java Edition | 26.3 |
| Java | 25 or newer |
| Fabric Loader | 0.20.1 or newer |
| Fabric API | 0.165.0+26.3 or a compatible later build for 26.3 |

Download the compiled mod from [Releases](https://github.com/Lulukai/BetterBundleHUDMod/releases), or build it from source. Put `better-bundle-1.5.1+26.3.jar` and Fabric API in your Minecraft instance's `mods` folder, then restart the game. Install only one version of BetterBundleHUD at a time: the original and this fork use the same mod ID, `better-bundle`.

If no release is listed yet, the [Build workflow](https://github.com/Lulukai/BetterBundleHUDMod/actions/workflows/build.yml) provides downloadable JAR artifacts after a successful run (GitHub sign-in required).

## Use

Open your inventory and close the vanilla recipe book with its green book button. The bundle panel and recipe book share the space beside the inventory, so opening the recipe book temporarily hides the bundle panel. The bundle icon at the right edge of the inventory toggles the panel independently.

Select a category tab to filter the list, and scroll over the item grid to view more entries.

The following controls were confirmed by the tester in both single-player and multiplayer:

| Action on a HUD item | Result |
| --- | --- |
| Shift-click | Collect all matching items that fit in available storage |
| Left-click | Collect one bundle entry |
| Right-click | No action |

Totals can exceed Minecraft's normal stack size; actual withdrawals still obey normal inventory and stack limits. Items that do not fit stay in their source bundles. Avoid closing or changing screens while a transfer is running; Escape cancels the remaining work and leaves already collected items in inventory.

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

## Validation

The Gradle build includes access-widener validation and 22 automated tests covering category coverage, shared UI layout, item-component grouping, and simulated withdrawal behavior. The withdrawal tests cover full inventories, partial stacks, changing bundle indexes, cancellation, and rejected transfers.

The tester confirmed grouped contents and the controls above in both single-player and multiplayer on Minecraft 26.3 with version 1.5.0. Version 1.5.1 removes the tooltip action hints and updates documentation; it does not change the interaction code. Automated tests supplement that manual test and do not simulate a live server connection.

Please report bugs through [Issues](https://github.com/Lulukai/BetterBundleHUDMod/issues) with your Minecraft version, mod version, relevant log excerpt, and reproduction steps.

## Attribution and license

Original author: **zeroowo24**. Continuation maintained by [Lulukai](https://github.com/Lulukai).

- [Original source](https://github.com/zeroowo24/BetterBundleHUDMod)
- [Original Modrinth project](https://modrinth.com/project/3TR1DyhT)
- Upstream base: commit `651220f`, version 1.2.0

The original repository's [LICENSE](LICENSE) is **CC0-1.0** and is retained unchanged. The original mod metadata and Modrinth listing said MIT; this fork follows the license file distributed with the source.

Minecraft textures are referenced from the installed game and are not redistributed by this project. This is an unofficial project, not affiliated with Mojang or Microsoft.
