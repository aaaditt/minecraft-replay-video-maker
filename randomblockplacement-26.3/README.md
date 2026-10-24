# Random Blocks (Fabric, Minecraft 26.3)

A client-side mod that places blocks from your **hotbar and inventory** at random, using
percentages you choose, or as a **gradient** that fades from one block to the next as you build.
It's inspired by MEGATREX4's RandomBlockPlacement (which only covers the hotbar), rewritten for 26.3.

It only uses normal client actions (selecting a slot, swapping items in the inventory), so it works in
**survival** and on **multiplayer servers that don't have the mod**.

## Using it

| Key (default) | What it does |
|---|---|
| `U` | Open the palette screen |
| `Y` | Turn placement on or off (opens the palette if nothing is selected yet) |
| unbound | Reset the gradient start point |

1. Press `U`. Every block in your hotbar and inventory is listed.
2. Tick the blocks you want and drag each slider to set its percentage. Any total works: it gets
   scaled to 100%, and the screen shows the real share next to each block.
3. Choose a **Mode**:
   - **Random**: each block you place is picked at random using your percentages.
   - **Gradient**: the blocks are laid out in list order (use ▲/▼ to reorder). Each block's
     percentage sets how wide its band is. The first block you place marks the start of the gradient.
     - **Axis**: `AUTO` follows the direction you're looking when you place that first block.
       `X`/`Y`/`Z` fix it to one world axis.
     - **Length**: how many blocks the whole gradient covers.
     - **Blend**: how much neighbouring bands mix at their edges.
4. Press **Place** and build as normal. Blocks are pulled from your inventory into your hand
   automatically. When a block runs out it's skipped, and when all of them run out the mod turns itself off.

The mod only steps in while you're holding a palette block or nothing, so doors, chests and tools work
normally. Your palette is saved in `config/randomblocks-palette.json`.

## Installing

Put `randomblocks-<version>.jar` and [Fabric API](https://modrinth.com/mod/fabric-api) `0.165.0+26.3`
(or newer) in your `mods` folder, with Fabric Loader 0.19.5 or newer.

## Building

You need JDK 25.

```sh
./gradlew build              # jar ends up in build/libs/
./gradlew test               # unit tests for the selection maths
./gradlew runClientGameTest  # plays the mod in survival, singleplayer and on a dedicated server
```

The client game test makes a survival world, puts stone, andesite and cobblestone in the main inventory
(not the hotbar) and goes through the palette screen. It then places a 60-block random line and a 30-block
gradient line, checks the result on the server, and repeats both on a dedicated server that doesn't have the mod.
