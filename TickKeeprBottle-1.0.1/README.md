# TickKeepr: Bottled Time

Credits real-world offline time into **Time in a Bottle** (haoict's
1.16.5 port, mod id `tiab`) the moment you log back in.

## Correction from the first version of this add-on

I initially built this around a brand-new item of my own, on the belief
that Time in a Bottle had never been ported past 1.12.2. That was wrong -
[haoict's fork](https://www.curseforge.com/minecraft/mc-mods/time-in-a-bottle-standalone)
has a real 1.16.5 Forge build (`time-in-a-bottle-1.1.0-mc1.16.jar`). This
version throws out my standalone item entirely and integrates with that
mod's actual item instead, which is obviously what's useful if you're
already using it.

## How it works

tiab's own item already does the "fill up while carried" half of this
correctly - it's checked directly against its source, not assumed. What it
can't do is know how long the world was closed, since nothing ticks while
the game isn't running. This mod is just that missing half:

1. **On logout**, the current real-world time gets stamped onto the player
   (via Forge's per-entity persistent NBT, not onto the bottle itself - see
   below for why).
2. **On login**, that stamp is compared against the new current time to get
   an elapsed real-time gap, converted to ticks, and reduced by a lazy tax
   - same idea as TickKeepr's block catch-up.
3. Every `tiab:timeinabottle` stack currently in your inventory gets that
   many ticks added to its `timeData.storedTime` NBT - the exact field
   tiab's own item reads from and writes to. From the item's perspective,
   that time just looks like time that accrued normally.
4. Spending it is entirely tiab's own business - right-click a tickable
   block with it, same as always. This mod doesn't touch that part at all.

Checking against the player rather than the bottle means a bottle has to
actually be in your inventory at login to get credited - one left behind
in a chest the whole time you were away doesn't retroactively fill up.

## Verifying tiab's actual data format

Rather than guess, I cloned github.com/haoict/time-in-a-bottle and checked
out `v1.1.0` specifically - the exact tag the published 1.16.5 jar builds
from, not just the branch tip, in case anything had drifted since. That
confirmed, directly from source:

- Mod id `tiab`, item registered as `timeinabottle`
- Stored time lives in a child compound tag `timeData`, key `storedTime`,
  as a plain `int` count of ticks (20/sec, same convention as everywhere
  else in TickKeepr)
- Its own "spend on a block" logic is a genuinely different mechanic than
  I'd guessed - right-clicking spawns a temporary acceleration effect near
  that block rather than an instant catch-up, doubling in strength (and
  cost) with repeated clicks within a 30-second window. That's entirely
  self-contained in tiab's own code, so it's not something this mod needs
  to reimplement or even understand beyond "leave it alone."

This mod only ever touches the bottle's NBT directly through those string
constants (see `TiabBridge.java`) - it doesn't compile against tiab's jar
at all, so it builds standalone and simply does nothing if tiab isn't
installed, rather than requiring it.

## Building

The Gradle wrapper is included, so you don't need Gradle installed
separately - just a JDK on your PATH (any reasonably recent one, e.g. 17,
works to *run* Gradle; it'll fetch the actual Java 8 toolchain this mod
compiles against automatically):

```
./gradlew build
```

(`gradlew.bat` on Windows.) No TickKeepr or tiab jar needed on the
classpath - drop the output jar in your `mods` folder alongside tiab
(order doesn't matter). Same caveat as the other two: I verified
`Entity#getPersistentData()` and the player login/logout events directly
against Forge's 1.16.5 source, but haven't run an actual `gradlew build`
myself.

## Config

Generated at `config/tickkeeprbottle-common.toml`:

| Option | Default | What it does |
|---|---|---|
| `enabled` | `true` | Master on/off switch |
| `maxStoredTicks` | `2,592,000` (30 in-game days) | This mod's own cap on how much it'll credit in one go |
| `offlineLazyTaxPercent` | `15.0` | % of offline time discarded before crediting |

Note tiab has its own separate "Max Stored Time" cap in its own config
(defaults to roughly 360 in-game days, effectively no real limit) - the
two caps are independent since this mod doesn't read tiab's config.

## On "implement as many mechanics as you can"

I went back and checked OceanBlock's other item-carried mechanics again
with this correction in mind. Nothing else turned up that fits the same
shape (accrues while carried, matters whether you're online or not) -
Jars are an instant combination with no time component, and everything
else time-based I found (smeltery, sluice) is a block, which TickKeepr
already handles. If tiab has more surface area you want covered - the FE
variant, or the Time Charger block, both of which exist in its source but
are off by default - let me know and I'll look at those specifically
rather than guess at what's actually enabled in your pack.
