# Changelog

## 1.5.1+26.3

- Remove mouse-action descriptions from HUD item tooltips; retain vanilla item details and combined totals.
- Update the README with installation, building, attribution, and the controls confirmed in single-player and multiplayer.
- Preserve the interaction behavior of version 1.5.0.

## 1.5.0+26.3

- Replace search with a static All Items/category heading and remove the pinyin4j dependency.
- Combine component-identical items from multiple bundles into one HUD entry with their total count.
- Tester-confirmed behavior: left-click collects one bundle entry; right-click does nothing.
- Shift-click withdraws all matching entries, merging partial destination stacks and respecting storage limits.
- Use fresh source indexes and vanilla client click prediction for grouped withdrawals, with cancellation on menu changes and staging-slot corrections.
- Add grouping and transfer regression tests, including full inventories, component differences, leftovers, cancellation, and changed bundle indexes.

## 1.4.1+26.3

- Categorize all 152 item IDs introduced after Minecraft Java 26.1 through 26.3, including every color variant.
- Rename Partial Blocks to Decorations.
- Reduce the search field from 122 x 14 to 108 x 12 GUI pixels; drawing and hit testing use the same bounds.
- Add exhaustive category-coverage tests against the official game item-asset difference.

## 1.4.0+26.3

- Translate category names and tooltips to English.
- Use vanilla inventory frames, slots, buttons, and scrollbars.
- Align panel height with the inventory and share drawing/input layout.
- Preserve the original recipe-book behavior.
