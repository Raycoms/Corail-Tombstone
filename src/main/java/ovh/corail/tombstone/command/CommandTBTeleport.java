package ovh.corail.tombstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.BlockPosArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import ovh.corail.tombstone.helper.EntityHelper;
import ovh.corail.tombstone.helper.Helper;
import ovh.corail.tombstone.helper.LangKey;
import ovh.corail.tombstone.helper.Location;
import ovh.corail.tombstone.helper.SpawnHelper;
import ovh.corail.tombstone.helper.StyleType;

public class CommandTBTeleport extends TombstoneCommand {

    public CommandTBTeleport(CommandDispatcher<CommandSource> commandDispatcher) {
        super(commandDispatcher);
    }

    @Override
    public String getName() {
        return "tbteleport";
    }

    @Override
    LiteralArgumentBuilder<CommandSource> getBuilder(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(c -> showUsage(c.getSource()));
        builder.then(Commands.argument("source", EntityArgument.entity())
                .executes(c -> showUsage(c.getSource()))
                .then(Commands.argument(TARGET_PARAM, EntityArgument.entity())
                        .executes(c -> {
                            Entity target = EntityArgument.getEntity(c, TARGET_PARAM);
                            BlockPos targetPos = target.getPosition();
                            return teleport(c.getSource(), EntityArgument.getEntity(c, "source"), targetPos.getX(), targetPos.getY(), targetPos.getZ(), Helper.getDimensionId(target));
                        })
                )
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(c -> {
                            Entity source = EntityArgument.getEntity(c, "source");
                            BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
                            return teleport(c.getSource(), source, pos.getX(), pos.getY(), pos.getZ(), Helper.getDimensionId(source));
                        })
                        .then(Commands.argument("dim", IntegerArgumentType.integer()).suggests(SUGGESTION_DIM_IDS)
                                .executes(c -> {
                                    BlockPos pos = BlockPosArgument.getBlockPos(c, "pos");
                                    return teleport(c.getSource(), EntityArgument.getEntity(c, "source"), pos.getX(), pos.getY(), pos.getZ(), IntegerArgumentType.getInteger(c, "dim"));
                                })
                        ))
        );
        return builder;
    }

    private int teleport(CommandSource sender, Entity source, int x, int y, int z, int dim) {
        checkAlive(source);
        checkNotSpectator(source);

        Location sourceLoc = new Location(source);
        Location destLoc = new Location(x, y, z, dim);

        DimensionType dimensionType = getOrThrowDimensionType(dim);
        // checks same location
        if (sourceLoc.equals(destLoc)) {
            throw LangKey.MESSAGE_SAME_LOCATION.asCommandException();
        }
        // finds spawn position and teleports
        ServerWorld targetWorld = sender.getServer().getWorld(dimensionType);
        checkValidPos(targetWorld, destLoc.getPos());
        destLoc = new SpawnHelper(targetWorld, Helper.getCloserValidPos(targetWorld, destLoc.getPos())).findSpawnPlace();
        if (destLoc.isOrigin()) {
            throw LangKey.MESSAGE_NO_SPAWN.asCommandException();
        }

        Entity newEntity = Helper.teleportEntity(source, destLoc);
        if (EntityHelper.isValidPlayer(newEntity)) {
            newEntity.sendMessage(LangKey.MESSAGE_TELEPORT_SUCCESS.getTranslationWithStyle(StyleType.MESSAGE_SPELL));
        }
        sendMessage(sender, LangKey.MESSAGE_TELEPORT_TARGET_TO_LOCATION.getTranslation(newEntity.getName(), LangKey.MESSAGE_HERE.getTranslation(), destLoc.x, destLoc.y, destLoc.z, destLoc.dim), false);
        return 1;
    }
}
