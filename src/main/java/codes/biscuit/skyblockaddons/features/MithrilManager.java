package codes.biscuit.skyblockaddons.features;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.core.Feature;
import codes.biscuit.skyblockaddons.utils.ColorCode;
import net.minecraft.block.Block;
import net.minecraft.block.BlockColored;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class MithrilManager {
    private static final SkyblockAddons main = SkyblockAddons.getInstance();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int lightBlueWool = EnumDyeColor.LIGHT_BLUE.getMetadata();
    private static final int smoothDiorite = BlockStone.EnumType.DIORITE_SMOOTH.getMetadata();
    private static final int obsidian = Blocks.obsidian.getMetaFromState(Blocks.obsidian.getDefaultState());
    private static final BlockPos scanRange = new BlockPos(5, 5, 5);
    private static final LinkedHashSet<BlockPos> blockPositions = new LinkedHashSet<>();
    private static final EnumFacing[] axes = {EnumFacing.EAST, EnumFacing.WEST, EnumFacing.UP, EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.NORTH};
    private static final int defaultTimeout = 5;
    private static BlockPos prevBlockPos = null;
    private static Vec3 prevPos = null;

    private static int timeout = 0;
    private static boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggleEnabled() {
        enabled = !enabled;
        EntityPlayer player = mc.thePlayer;
        if (player != null) {
            player.addChatMessage(new ChatComponentText((enabled ? ColorCode.GREEN : ColorCode.RED) + "Mithril Helper " + (enabled ? "Enabled" : "Disabled")));
            if (!enabled) {
                blockPositions.clear();
            }
        }
    }

    public static void onTick() {
        World world = mc.theWorld;
        EntityPlayer player = mc.thePlayer;
        if (world == null || player == null)
            return;

        if (timeout == 0) {
            if (enabled) {
                // switch targets
                blockPositions.removeIf(pos -> !isInRange(player, getClosestPos(world, player, pos)) || !validBlock(pos));
                //if (blockPositions.isEmpty()) {
                    scanBlocks();
                //}
                if (!blockPositions.isEmpty()) {
                    // --just get the first element--
                    // find the closest one to the previous
                    if (prevPos == null)
                        prevPos = player.getPositionVector();
                    Vec3 closest = prevPos;
                    BlockPos closestBlockPos = prevBlockPos;
                    double distance = Double.MAX_VALUE;
                    for (BlockPos pos : blockPositions) {
                        Vec3 vec = getClosestPos(world, player, pos);
                        double dist = vec.squareDistanceTo(prevPos) + .02 * (Math.random() - .3) * vec.squareDistanceTo(player.getPositionVector());
                        if (dist < distance) {
                            closest = vec;
                            closestBlockPos = pos;
                            distance = dist;
                        }
                    }
                    if (closest != player.getPositionVector()) {
                        timeout = defaultTimeout;
                        lookAt(player, closest);
                        prevPos = closest;
                        prevBlockPos = closestBlockPos;
                    }
                }
            }

        }
        else {
            timeout--;
            if (!validBlock(prevBlockPos))
                timeout = 0;
        }
    }

    public static void lookAt(EntityPlayer player, Vec3 pos) {
        if (pos != null)
            CrystalHollowsChestManager.lookAt(player, pos);
    }

    public static boolean validBlock(BlockPos pos) {
        World world = mc.theWorld;
        EntityPlayer player = mc.thePlayer;
        if (player == null || world == null)
            return false;
        return (isWool(world, pos) || isTitanium(world, pos) || isObsidian(world, pos) && main.getConfigValues().isEnabled(Feature.MITHRIL_HELPER_OBSIDIAN));
    }

    public static void scanBlocks() {
        World world = mc.theWorld;
        EntityPlayer player = mc.thePlayer;
        if (player == null || world == null)
            return;
        BlockPos start = player.getPosition().subtract(scanRange);
        BlockPos end = player.getPosition().add(scanRange);
        for (BlockPos pos : BlockPos.getAllInBox(start, end)) {
            if (pos.getY() >= 0 && pos.getY() < 256 && (isWool(world, pos) || isTitanium(world, pos) || isObsidian(world, pos) && main.getConfigValues().isEnabled(Feature.MITHRIL_HELPER_OBSIDIAN))) {
                Vec3 closestPos = getClosestPos(world, player, pos);
                if (closestPos.lengthVector() > 0.1 && isInRange(player, closestPos)) {
                    blockPositions.add(pos);
                }
            }
        }
        //main.getUtils().getLogger().info(blockPositions);
    }

    private static boolean isWool(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return Block.isEqualTo(state.getBlock(), Blocks.wool) && state.getValue(BlockColored.COLOR).getMetadata() == lightBlueWool;
    }

    private static boolean isTitanium(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return Block.isEqualTo(state.getBlock(), Blocks.stone) && state.getValue(BlockStone.VARIANT).getMetadata() == smoothDiorite;
    }

    private static boolean isObsidian(World world, BlockPos pos) {
        return Block.isEqualTo(world.getBlockState(pos).getBlock(), Blocks.obsidian);
    }

    /**
     * actually returns the direction of the block facing towards you
     * @param dist
     * @param axis
     * @return
     */
    private static EnumFacing getDirectionFacing(double dist, int axis) {
        return axes[axis * 2 + (dist >= 0 ? 1 : 0)];
    }

    private static EnumFacing getDirection(Vec3 direction) {
        double xDist = Math.abs(direction.xCoord), yDist = Math.abs(direction.yCoord), zDist = Math.abs(direction.zCoord);
        if (xDist > yDist) {
            if (xDist > zDist) {
                // X: x > (y ? z)
                return getDirectionFacing(direction.xCoord, 0);
            }
            else {
                // Z: z > x > y
                return getDirectionFacing(direction.zCoord, 2);
            }
        }
        else {
            if (yDist > zDist) {
                // Y: y > (x ? z)
                return getDirectionFacing(direction.yCoord, 1);
            }
            else {
                // Z: z > y > x
                return getDirectionFacing(direction.zCoord, 2);
            }
        }
    }

    /**
     * should work properly now
     * @param world
     * @param player
     * @param pos
     * @return
     */
    private static Vec3 getClosestPos(World world, EntityPlayer player, BlockPos pos) {
        Vec3 direction = new Vec3(pos).subtract(player.getPositionVector().addVector(0, player.getEyeHeight(), 0));
        while (direction.lengthVector() > 0.1) {
            EnumFacing facing = getDirection(direction);
            Block block;
            if ((block = world.getBlockState(pos.getImmutable().offset(facing)).getBlock()).equals(Blocks.air) || block.equals(Blocks.carpet)) {
                Vec3 vec = new Vec3(pos).addVector(.5, .5, .5);
                String str = vec.toString();
                if (facing == EnumFacing.EAST || facing == EnumFacing.WEST)
                    vec = vec.addVector(facing.getDirectionVec().getX() / 2.0, 0, 0);
                else if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH)
                    vec = vec.addVector(0, 0, facing.getDirectionVec().getZ() / 2.0);
                else
                    vec = vec.addVector(0, facing.getDirectionVec().getY() / 2.0, 0);
                //main.getUtils().getLogger().info("Vec: " + str + ", offset: " + vec);
                return vec;
            }
            //main.getUtils().getLogger().info("Failed " + pos + ", " + facing);
            if (facing == EnumFacing.EAST || facing == EnumFacing.WEST)
                direction = direction.subtract(direction.xCoord, 0, 0);
            else if (facing == EnumFacing.NORTH || facing == EnumFacing.SOUTH)
                direction = direction.subtract(0, 0, direction.zCoord);
            else
                direction = direction.subtract(0, direction.yCoord, 0);
        }
        return new Vec3(0, 0, 0);
    }

    private static boolean isInRange(EntityPlayer player, Vec3 pos) {
        return pos.distanceTo(CrystalHollowsChestManager.getPlayerEyesPos(player)) < 4.5;
    }
}
