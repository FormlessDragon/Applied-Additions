# Applied Additions

An addon mod of Applied Energistics 2 Supergiant.

## Requirements

Cleanroom is required for the necessity.

[Cleanroom](https://github.com/CleanroomMC/Cleanroom) is a 1.12.2 Forge fork, providing newer toolchain, new APIs and 99% compatibility.

Need Java25.

## Infinity Cells

The mod itself adds only two recipe-less cells: `<ae2additions:infinity_cobblestone_cell>`
and `<ae2additions:infinity_water_cell>`. Both use the same cell implementation. Pack authors can register additional
cells from a CraftTweaker `.zs` script; a cell may contain one key or many keys.

```zenscript
import mods.ae2additions.InfinityCell;
import mods.ae2additions.AEKeyHelper;
import mods.ae2additions.KeyList;

// A single-key cell is represented by a one-element collection.
InfinityCell.register("infinity_iron", [<minecraft:iron_ingot>]);

// Mixed item and fluid keys.
InfinityCell.register("infinity_iron_redstone_water",
    [<minecraft:iron_ingot>, <minecraft:redstone>],
    [<liquid:water>]);

// Item NBT is part of the AE key (1.12.2 uses the legacy NBT form).
InfinityCell.register("infinity_potion",
    AEKeyHelper.item("minecraft:potion", "{Potion:\"minecraft:long_night_vision\"}"));

// KeyList is useful when keys are assembled incrementally.
InfinityCell.register("infinities_cell",
    KeyList.create().add(<minecraft:diamond>).add(<liquid:water>));
```

`AEKeyHelper.of(...)` accepts an AE2 key supplied by another mod, so addon keys (energy, gases, pigments, slurries, and
so on) can be passed through
`Java.loadClass()`. Registration runs during CraftTweaker pre-initialization and does not create a recipe.

## Integrated
* [GlodBlock/ExtendedAE](https://github.com/GlodBlock/ExtendedAE): integrated Quantum Computer behavior with partial code reuse under the LGPL-3.0 License.
* [pedroksl/AdvancedAE](https://github.com/pedroksl/AdvancedAE): integrated Wireless Hub, Wireless Connector, Assembler Matrix behavior with partial code reuse under the LGPL-3.0 License;

## Credits

Thanks to Team Applied Energistics, AlgorithmX2, the upstream AE2 contributors, and everyone involved in the original project:
[AppliedEnergistics/Applied-Energistics-2](https://github.com/AppliedEnergistics/Applied-Energistics-2).
