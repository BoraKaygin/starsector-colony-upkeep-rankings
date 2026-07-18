# Colony Upkeep Rankings

Colony Upkeep Rankings is a utility mod for **Starsector 0.98a-RC8**. It adds a persistent, full-width Intel entry that ranks every operational structure across all player-owned colonies by current monthly upkeep.

## Features

- Sorts all colony industries and structures globally by live monthly upkeep.
- Shows the colony, installed AI core, and estimated Beta-core savings for every entry.
- Includes industries and structures added by other mods.
- Uses Starsector's full-width Major Event view with a scrollable table.
- Refreshes every five active campaign minutes and whenever the Intel screen opens.
- Works when added to an existing save; no new game is required.
- Has no mod dependencies.

## Installation

1. Download the latest release.
2. Extract the `Colony Upkeep Rankings` folder into the Starsector `mods` directory.
3. Enable **Colony Upkeep Rankings** in the launcher.
4. Load a save and open **Intel → Major Events** or **Intel → Colonies**.

## How the values are calculated

The ranking reads each industry's live `getUpkeep().getModifiedValue()` value. This includes current colony and industry modifiers rather than relying on static base-upkeep data.

The estimated Beta saving is 25% of current upkeep when the structure does not already have an Alpha or Beta core. Structures that already receive the upkeep reduction are marked as such.

## Building from source

Requirements:

- Starsector 0.98a-RC8
- JDK 17
- PowerShell

Run:

```powershell
./build.ps1 -StarsectorHome "D:\Starsector"
```

The installable mod folder is generated under `build/Colony Upkeep Rankings`.

## Compatibility

- Starsector 0.98a-RC8
- Existing saves supported
- Modded colony industries supported automatically

See [CHANGELOG.md](CHANGELOG.md) for version history.
