<p align="center"><img src="https://github.com/user-attachments/assets/dee820d1-99f8-4836-a6a5-f2a1b7a05081" alt="Logo" width="200"></p>
<h1 align="center">Dimensional Sable<br>
<div align="center">
    <a href="https://modrinth.com/mod/dimensional-sable">
        <img src="https://img.shields.io/modrinth/dt/dimensional-sable?logo=modrinth&amp;label=&amp;suffix=%20&amp;style=flat&amp;color=242629&amp;labelColor=5CA424&amp;logoColor=1C1C1C" alt="Modrinth Download"/>
    </a>
</div>
</h1>

An addon to the Sable Mod that allows for teleporting sublevels across dimensions!

The command to warp a sublevel is **/sable dimension_set** ...

Allows for teleporting just a sublevel:
<img width="854" height="480" alt="2026-05-15_17 11 46" src="https://github.com/user-attachments/assets/e0e6f23d-c0d1-4636-80fa-6bcbff267a28" />

Or teleport a sublevel and all connected sublevels (connections being ropes, springs, docking connectors etc). This is the default option
<img width="854" height="480" alt="2026-05-15_17 12 52" src="https://github.com/user-attachments/assets/aac06761-185e-4a8d-9d9b-cf18e78222c1" />

Currently, these block/entities are known to have broken behavior after a teleportation:
- *Moving* Mechanical Piston (and sticky ofc)

Future Goals:
- Make create contraptions properly teleport instead of disassembling
- Destroy any blocks that are connecting the sublevel to the ground (or fail instead)

If you want to contribute, hit me up on discord. Username is hollow_egg.
If you just want to steal the (very bad) code, you can do that too ^-^

NOTICE:
An official api for this very thing is going to be made by the Simulated Team. So once that comes out, GO USE THAT. I will probably archive this repo when it does.

[EVERYTHIGN BELOW IS INFO FOR DEVELOPERS]

The function to teleport a sublevel is **WarpSubLevel(subLevel, dimension, position, warpConnected?)**

Some block entities require a "fix" after being teleported. This is any case where the nbt holds information about a position, sublevel, or running state (for contraptions). This mod currently includes fixes for:
- Vanilla
- Create
- Create Aeronautics

If you find or develop a mod that has block entities that match this description, feel free to let me know on discord: hollow_egg, and I will implement fixes for those block entities. If you would like to implement those fixes yourself (such as the case of a modpack developer), just follow this guide:

- For data (at the root) containing a position that may change due to teleportation:
-     BlockEntityRegistry.PublishPosFixer( "MODID", "BLOCK_ENTITY_ID", Set.of("Data_Source", ...))
- Same conditions as above, but affecting multiple block entities with the same data:
-     BlockEntityRegistry.PublishCompoundPosFixer("MODID", Set.of("BLOCK_ENTITY_ID", ...), Set.of("Data_Source", ...))
- For data (at the root) containting a sublevel UUID:
-     BlockEntityRegistry.PublishUUIDFixer("MODID", "BLOCK_ENTITY_ID", Set.of("Data_Source", ...))
- For data (at the root) containing a "connection" (just a single sublevel tag and position tag, quick for things like the docking connector):
-     BlockEntityRegistry.PublishConnectionFixer("MODID", "BLOCK_ENTITY_ID", "SubLevel_Data_Source", "Position_Data_Source")
-   For data everywhere else, or special conditions: Create a new BlockEntityAccessor (I would use SpringBlockEntity as an example), and publish it with PublishCustomFixer

Examples are available in DimensionalSable.java

