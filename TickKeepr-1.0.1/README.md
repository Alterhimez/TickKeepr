# TickKeepr

A Forge 1.16.5 mod for FTB OceanBlock (or any 1.16.5 pack) that simulates
offline / unloaded-chunk progress - the same idea as "The Block Keeps
Ticking," which only exists for Fabric 1.21+, not Forge 1.16.5.

## How it works

Instead of trying to re-implement every machine's internal recipe math
(furnace burn timers, Tinkers' Construct smeltery heat, FTB Sluice
processing, whatever Time in a Bottle nudges...), TickKeepr does the same
thing those blocks already do to themselves every tick: it just calls their
real `tick()` method again, many times in a row, when you come back.

1. Every tickable block entity gets a small capability attached
   (`ILastActive`) that stores one thing: the real-world timestamp it was
   last known to be loaded. This piggybacks on Forge's existing "ForgeCaps"
   NBT round-trip on `TileEntity`, so it persists automatically for *any*
   block entity from *any* mod - no per-mod code needed.
2. Whenever that block's NBT gets saved (autosave, chunk unload, or world
   shutdown), the capability stamps the current real time.
3. When a chunk loads back in, TickKeepr compares "now" against that
   stamp, converts the real-time gap into an equivalent number of game
   ticks (50ms = 1 tick), and queues that many replayed `tick()` calls.
4. Those replayed ticks are spread across many real server ticks (a
   configurable budget per tick) instead of all at once, so a base full of
   machines after a multi-day absence doesn't freeze the server the moment
   the world loads.

Because step 1-4 only touch `ITickableTileEntity` and Forge's own
capability system, this works on vanilla furnaces *and* Tinkers'
smelteries, FTB Sluice, and anything else that ticks - without the mod ever
importing or depending on those other mods' code. That also means it
doesn't need to be rebuilt every time one of those mods updates.

This is deliberately not pixel-perfect: a `lazyTaxPercent` config option
(default 15%) shaves a slice off every catch-up on purpose, and there's a
hard per-block tick cap, both in keeping with "roughly right is fine."

## Building

The Gradle wrapper is included, so you don't need Gradle installed
separately - just a JDK on your PATH (any reasonably recent one, e.g. 17,
works to *run* Gradle; it'll fetch the actual Java 8 toolchain this mod
compiles against automatically):

```
./gradlew build
```

(`gradlew.bat` on Windows.) The output jar will be in `build/libs/`. Drop
it in your `mods` folder alongside Forge 1.16.5.

**I can't compile or run this in the sandbox I wrote it in** (no access to
Mojang's/Forge's download servers from here), so treat this as a solid
starting point that's right in design and API usage, but hasn't been
proven by an actual `gradlew build`. The one thing most likely to need a
tweak: this project targets 1.16.5 with **official (Mojang) mappings**,
since that's what current Forge 1.16.5 tooling is built against (confirmed
directly against Forge's own maintained 1.16.x source while writing this).
If your local setup somehow resolves to the older MCP mapping set instead,
the fix is mechanical: rename `TileEntity#load`/`save` back to
`read`/`write` in `LastActiveCapability`/`CapabilityHandler`'s call sites -
nothing else changes.

Also double check `forge_version` in `gradle.properties` against
[the Forge files site](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.16.5.html)
and bump it to whatever the current build is; I couldn't confirm the exact
latest patch number from here.

## Config

Generated at `config/tickkeeper-common.toml` after the first run:

| Option | Default | What it does |
|---|---|---|
| `enabled` | `true` | Master on/off switch |
| `maxCatchUpTicks` | `1,728,000` (24h) | Hard cap per block, however long it was actually offline |
| `lazyTaxPercent` | `15.0` | % of the calculated catch-up discarded before simulating |
| `minTicksToSimulate` | `20` (1s) | Skip trivial gaps |
| `serverTickBudget` | `20,000` | Total replayed ticks allowed per real server tick |
| `blockEntityBlacklist` | `[]` | Registry names (`"modid:block"`) to never fast-forward |

## Things worth knowing / testing for yourself

- **Multiblocks** (like a Tinkers smeltery): as long as the controller
  block entity's own `tick()` fully drives the structure's behavior (which
  is the normal design for these), replay works fine. If a mod's
  multiblock relies on a *neighboring* block also ticking in sync, replay
  may drift more than usual - the lazy tax already assumes some of this.
- **Performance with a lot of machines catching up at once**: tune
  `serverTickBudget` down if you see a hitch right after loading a busy
  base; tune it up if machines feel slow to "wake up."
- **A specific modded machine behaving oddly under rapid replay**: add its
  registry name to `blockEntityBlacklist` rather than fighting it.
- I picked a chunk-load hook plus a capability read, both confirmed against
  Forge's own source while building this, but I'd still recommend testing
  against a disposable copy of your world first, the way you would with any
  new mod that touches machine state.
