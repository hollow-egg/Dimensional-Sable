package dev.egg.dimensionalsable;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ryanhcode.sable.api.command.SubLevelArgumentType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class Commands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(net.minecraft.commands.Commands.literal("sable")
            .then(net.minecraft.commands.Commands.literal("dim_teleport")
            .then(net.minecraft.commands.Commands.argument("sub_level", SubLevelArgumentType.subLevels())
            .then(net.minecraft.commands.Commands.argument("dimension", DimensionArgument.dimension())
                    .executes(Commands::executeNoPosition)
            .then(net.minecraft.commands.Commands.argument("position", Vec3Argument.vec3())
                .executes(Commands::execute))))));
    }
    private static int executeNoPosition(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");

        CommandSourceStack source = ctx.getSource();
        ServerSubLevel sublevel = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");

        int warpCount = SubLevelWarper.warpConnectedSubLevels(sublevel, dimension, sublevel.logicalPose().position());

        source.sendSuccess(()->Component.literal("Warped " + warpCount + " sublevels to " + dimension.dimension().location().getPath()), true);
        return Command.SINGLE_SUCCESS;
    }
    private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {

        ServerLevel dimension = DimensionArgument.getDimension(ctx, "dimension");
        Vec3 position = Vec3Argument.getVec3(ctx, "position");

        CommandSourceStack source = ctx.getSource();
        ServerSubLevel sublevel = SubLevelArgumentType.getSingleSubLevel(ctx, "sub_level");

        int warpCount = SubLevelWarper.warpConnectedSubLevels(sublevel, dimension, new Vector3d(position.x, position.y, position.z));

        source.sendSuccess(()->Component.literal("Warped " + warpCount + " sublevels to " + dimension.dimension().location().getPath()), false);
        return Command.SINGLE_SUCCESS;
    }
}
