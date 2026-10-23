<p align="center"><img src="https://github.com/user-attachments/assets/dee820d1-99f8-4836-a6a5-f2a1b7a05081" alt="Logo" width="200"></p>
<div align="center">
  <h1 align="center">Dimensional Sable<br>
    <a href="https://modrinth.com/mod/dimensional-sable">
        <img src="https://img.shields.io/modrinth/dt/dimensional-sable?logo=modrinth&amp;label=&amp;suffix=%20&amp;style=flat&amp;color=242629&amp;labelColor=5CA424&amp;logoColor=1C1C1C" alt="Modrinth Download"/>
    </a>
    <a href="https://www.curseforge.com/minecraft/mc-mods/dimensional-sable">  
        <img src="https://img.shields.io/curseforge/dt/1758851?logo=curseforge&amp;label=&amp;suffix=%20&amp;style=flat&amp;color=242629&amp;labelColor=5CA424&amp;logoColor=1C1C1C" alt="Curseforge Download"/>  
    </a>  
</div>
</h1>

An addon to the Sable Mod that allows for teleporting sublevels across dimensions!

The command to warp a sublevel is **/sable dim_teleport** ...

Allows for teleporting a sublevel and all connected/intersecting sublevels (connections being ropes, springs, docking connectors etc).
<img width="854" height="480" alt="2026-05-15_17 12 52" src="https://github.com/user-attachments/assets/aac06761-185e-4a8d-9d9b-cf18e78222c1" />

How it works:
The server keeps a global mask of what plots in the plotyard are occupied in any dimension. By doing this, you divide the number of sublevels you can have (which is huge anyway) by the number of dimensions. This allows sublevels to be serialized "in-place", keeping all previous positions and data. This avoids the biggest issue of the previous solution: fixing block entities. Currently, this does mean there is some undefined behavior when warping a sublevel that was made **before** installing the mod, so beware!!! (disassembling and reassembling the sublevel should be enough to fix this)

Completed:
- Block nbt stays saved and consistent (even between dimensions of different heights)
- Connections (like ropes) teleport correctly and stay connected
- Create contraptions stay running and seamlessly continue moving
- Entities are teleported with the sublevel and keep relative positions
- Entities riding vehicles stay on those vehicles between teleports

Future Goals:
- Destroy any blocks that are connecting the sublevel to the ground (or fail instead)
- Find a solution to loading sublevels that are over the world boundary of a dimension (maybe cut them in half?)

[FOR DEVELOPERS]

The function to teleport a sublevel is **SubLevelWarper::warpConnectedSubLevels(subLevel, dimension, position, orientationTransform)**
