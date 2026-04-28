package dev.egg;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.command.SableCommandHelper;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;

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

            //if (sourcePlotContainer.equals(destinationPlotContainer)) //same dimension
                //return Command.SINGLE_SUCCESS;

            ServerSubLevel sublevel = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");
            Collection<SubLevel> compoundSubLevel = SubLevelHelper.getConnectedChain(sublevel);

            WarpSubLevels(compoundSubLevel, sourcePlotContainer, destinationPlotContainer);

            source.sendSuccess(()->Component.literal("Teleported " + compoundSubLevel.size() + " sublevels to " + dimension.dimension().location().getPath()), false);
            return Command.SINGLE_SUCCESS;
        }

        //this function is *basically* the same as the clone command from sable
        private static void WarpSubLevels(Collection<SubLevel> compoundSubLevel, ServerSubLevelContainer sourceContainer, ServerSubLevelContainer destinationContainer) {
            //map old ids to new ids for ropes later
            Vector<ServerSubLevel> oldSublevels = new Vector<>();

            for (SubLevel subLevel : compoundSubLevel) {

                ServerSubLevel serverSubLevel = (ServerSubLevel) subLevel;
                oldSublevels.add(serverSubLevel);

                //save sublevel data
                CompoundTag tag = SubLevelTemplate.save(serverSubLevel.getPlot());
                //allocate
                Pose3d pose = new Pose3d();
                pose.position().set(0,70.5,0); //temp so it's easier to find
                ServerSubLevel copy = (ServerSubLevel) destinationContainer.allocateNewSubLevel(pose);
                //copy data to plot in other dimension
                SubLevelTemplate.load(copy.getPlot(), tag);

                if (serverSubLevel.getName() != null)
                    copy.setName(serverSubLevel.getName());
            }

            //delete old sublevels
            for (ServerSubLevel subLevel : oldSublevels) {
                sourceContainer.removeSubLevel(subLevel, SubLevelRemovalReason.REMOVED);
            }
        }
    }
}
