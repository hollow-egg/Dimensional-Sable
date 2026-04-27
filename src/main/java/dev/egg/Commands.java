package dev.egg;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;

import static dev.egg.SubLevelConnectionManager.GetConnectedSubLevels;
import static dev.ryanhcode.sable.api.command.SableCommandHelper.requireNotNull;

public class Commands {
    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event){
        DimensionCommand.register(event.getDispatcher());
    }
    public static class DimensionCommand {
        public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
            dispatcher.register(net.minecraft.commands.Commands.literal("sable")
                    .then(net.minecraft.commands.Commands.literal("dimension_set")
                            .then(net.minecraft.commands.Commands.argument("sub_level", SubLevelArgumentType.subLevels())
                                .then(net.minecraft.commands.Commands.argument("dimension", DimensionArgument.dimension())
                                    .executes(DimensionCommand::execute)))));
        }
        private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

            ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");

            CommandSourceStack source = ctx.getSource();
            final ServerSubLevelContainer sourcePlotContainer = SableCommandHelper.requireSubLevelContainer(source);
            final ServerSubLevelContainer destinationPlotContainer = requireNotNull(Objects.requireNonNull(SubLevelContainer.getContainer(dimension)), new SimpleCommandExceptionType(Component.literal("Invalid Dimension!")));

            if (sourcePlotContainer.equals(destinationPlotContainer)) //same dimension
                return Command.SINGLE_SUCCESS;

            ServerSubLevel sublevel = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");
            HashMap<UUID, Vector<RopePhysicsObject>> compoundSubLevel = GetConnectedSubLevels((sublevel).getUniqueId());

            WarpSubLevels(compoundSubLevel, sourcePlotContainer, destinationPlotContainer);

            source.sendSuccess(()->Component.literal("Teleported " + compoundSubLevel.size() + " to " + dimension.dimension().toString()), false);
            return Command.SINGLE_SUCCESS;
        }

        //this function is *basically* the same as the clone command from sable
        private static void WarpSubLevels(HashMap<UUID,Vector<RopePhysicsObject>> compoundSubLevel, ServerSubLevelContainer sourceContainer, ServerSubLevelContainer destinationContainer) {
            //map old ids to new ids for ropes later
            HashMap<UUID,UUID> oldToNewidMap = new HashMap<>();
            Vector<ServerSubLevel> oldSublevels = new Vector<>();

            for (UUID subLevelid : compoundSubLevel.keySet()) {
                ServerSubLevel subLevel = Objects.requireNonNull((ServerSubLevel) sourceContainer.getSubLevel(subLevelid));
                oldSublevels.add(subLevel);

                //save sublevel data
                final CompoundTag tag = subLevel.getPlot().save();
                //allocate and copy data to plot in other dimension
                final ServerSubLevel copy = (ServerSubLevel) destinationContainer.allocateNewSubLevel(subLevel.logicalPose());
                final ServerLevelPlot plot = copy.getPlot();
                plot.load(tag);

                copy.updateLastPose();
                //set name if any
                if (subLevel.getName() != null)
                    copy.setName(subLevel.getName());

                //save ids to map
                oldToNewidMap.put(subLevelid, copy.getUniqueId());
            }

            //copy ropes
            for (UUID oldSubLevelId : new ArrayList<>(compoundSubLevel.keySet())) {

                Vector<RopePhysicsObject> originalRopes = compoundSubLevel.get(oldSubLevelId);
                if (originalRopes == null || originalRopes.isEmpty()) continue;

                UUID mappedSubLevelId = oldToNewidMap.get(oldSubLevelId);

                List<RopePhysicsObject> ropesSnapshot = new ArrayList<>(originalRopes);

                for (RopePhysicsObject rope : ropesSnapshot) {

                    RopePhysicsObjectAccessor accessor = (RopePhysicsObjectAccessor) rope;

                    UUID oldStartId = accessor.dimensionalSable$startLevel();
                    UUID oldEndId = accessor.dimensionalSable$endLevel();

                    ServerSubLevel oldStart = (ServerSubLevel) sourceContainer.getSubLevel(oldStartId);
                    ServerSubLevel oldEnd = (ServerSubLevel) sourceContainer.getSubLevel(oldEndId);

                    if (oldStart == null || oldEnd == null) continue;

                    UUID newStartId = oldToNewidMap.get(oldStart.getUniqueId());
                    UUID newEndId = oldToNewidMap.get(oldEnd.getUniqueId());

                    ServerSubLevel newStart = (ServerSubLevel) destinationContainer.getSubLevel(newStartId);
                    ServerSubLevel newEnd = (ServerSubLevel) destinationContainer.getSubLevel(newEndId);

                    if (newStart == null || newEnd == null) continue;

                    RopePhysicsObject newRope = new RopePhysicsObject(rope.getPoints(), rope.getCollisionRadius());
                    destinationContainer.physicsSystem().addObject(newRope);

                    newRope.setAttachment(
                            RopeHandle.AttachmentPoint.START,
                            rope.getPoints().getFirst(),
                            newStart
                    );

                    newRope.setAttachment(
                            RopeHandle.AttachmentPoint.END,
                            rope.getPoints().getLast(),
                            newEnd
                    );
                }
            }

            //delete old sublevels
            for (ServerSubLevel subLevel : oldSublevels) {
                sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            }
        }
    }
}
