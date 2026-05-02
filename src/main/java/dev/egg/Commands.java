package dev.egg;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.*;

import static dev.egg.SubLevelWarper.WarpSubLevel;

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
                .then(net.minecraft.commands.Commands.argument("position", BlockPosArgument.blockPos())
                .executes(DimensionCommand::execute))))));
        }
        private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

            ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
            BlockPos position = BlockPosArgument.getBlockPos(ctx, "position");

            CommandSourceStack source = ctx.getSource();
            ServerSubLevel sublevel = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");

            int warpCount = WarpSubLevel(sublevel, dimension, position);

            source.sendSuccess(()->Component.literal("Teleported " + warpCount + " sublevels to " + dimension.dimension().location().getPath()), false);
            return Command.SINGLE_SUCCESS;
        }
    }
}
