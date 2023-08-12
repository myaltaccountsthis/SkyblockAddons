package codes.biscuit.skyblockaddons.features;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.core.Feature;
import codes.biscuit.skyblockaddons.utils.ColorCode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.EntityCrit2FX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

public class CrystalHollowsChestManager {
    private static final SkyblockAddons main = SkyblockAddons.getInstance();
    private static final int defaultChestSamePositionTimeout = 5;
    private static final int defaultChestSameBlockPosTimeout = 100; // this should last all 5, when chat message "You have successfully picked the lock on this chest!"

    public static final float rotationIncrement = 30f;

    private static int chestSamePositionTimeout = 0;
    private static int chestSameBlockPosTimeout = 0;
    private static Vec3 chestPrevPosition = new Vec3(0, 0, 0);
    private static BlockPos chestPrevBlockPos = new BlockPos(0, 0, 0);
    private static int chestConsecutive = 0;

    private static boolean enabled = false;

    public static float dYaw = 0;
    public static float dPitch = 0;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggleEnabled() {
        enabled = !enabled;
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player != null) {
            player.addChatMessage(new ChatComponentText((enabled ? ColorCode.GREEN : ColorCode.RED) + "Crystal Hollows Chest Helper " + (enabled ? "Enabled" : "Disabled")));
        }
    }

    /**
     * Use this method when chest is finished picking, via chat message
     */
    public static void resetTimeout() {
        chestSameBlockPosTimeout = 0;
    }

    public static void onTick() {
        chestSamePositionTimeout = chestSamePositionTimeout > 0 ? chestSamePositionTimeout - 1 : 0;
        chestSameBlockPosTimeout = chestSameBlockPosTimeout > 0 ? chestSameBlockPosTimeout - 1 : 0;
        if (chestSamePositionTimeout > 0) {
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            if (player != null) {
                lookAt(player, chestPrevPosition);
            }
        }
    }

    public static void onCritParticle(EntityCrit2FX particle) {
        Vec3 pos = getEntityPos(particle);
        BlockPos blockPos = new BlockPos(pos);
        double dist = pos.squareDistanceTo(chestPrevPosition);
        if (dist <= .005 && blockPos.equals(chestPrevBlockPos)) { // between .05^2 and .1^2
            chestSamePositionTimeout = defaultChestSamePositionTimeout;
            chestSameBlockPosTimeout = defaultChestSameBlockPosTimeout;
            chestConsecutive++;
            if (chestConsecutive >= 2) {
                lookAt(Minecraft.getMinecraft().thePlayer, chestPrevPosition);
                if (main.getConfigValues().isEnabled(Feature.CRYSTAL_HOLLOWS_CHEST_DEBUG)) {
                    EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                    if (player != null) {
                        player.addChatMessage(new ChatComponentText("Crystal Hollows Chest Helper: Performed LookAt"));
                    }
                }
            }
        }
        else {
            if (chestSamePositionTimeout == 0) {
                if (blockPos.equals(chestPrevBlockPos) || chestSameBlockPosTimeout == 0) {
                    chestConsecutive = 0;
                    chestSamePositionTimeout = defaultChestSamePositionTimeout;
                    chestSameBlockPosTimeout = defaultChestSameBlockPosTimeout;
                    chestConsecutive++;
                    /*
                    only these two extra lines when changing particles
                    in the case that multiple stages are at the same position, it will take the one with the same block pos ofc,
                    so it doesn't matter whether the particle stays at the user's crosshair or moves to a different location within the same block
                    if the chest times out, then the timeout will just run out and yea
                     */
                    chestPrevPosition = pos;
                    chestPrevBlockPos = blockPos;
                    if (main.getConfigValues().isEnabled(Feature.CRYSTAL_HOLLOWS_CHEST_DEBUG)) {
                        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
                        if (player != null) {
                            player.addChatMessage(new ChatComponentText("Crystal Hollows Chest Helper: D:" + String.format("%.3f", Math.sqrt(dist)) + ", CCS:" + chestConsecutive));
                        }
                    }
                    lookAt(Minecraft.getMinecraft().thePlayer, chestPrevPosition);
                }
            }
        }
    }

    public static void lookAt(EntityPlayer player, Vec3 lookPos) {
        Vec3 direction = lookPos.subtract(getPlayerEyesPos(player)).normalize();
        float yaw = getYaw(direction);
        float pitch = getPitch(direction);
        dYaw = yaw - player.rotationYaw;
        dPitch = pitch - player.rotationPitch;
        //player.rotationYaw = yaw;
        //player.rotationPitch = pitch;
    }

    private static final float turnMultiplier = 4f;

    public static void updateRotation() {
        if (dYaw == 0f && dPitch == 0f) return;

        EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;

        dYaw = MathHelper.wrapAngleTo180_float(dYaw);

        float yaw = player.rotationYaw;
        float toRotateYaw = dYaw / turnMultiplier;
        if (toRotateYaw < 0) toRotateYaw--; else toRotateYaw++;
        if (Math.abs(toRotateYaw) >= Math.abs(dYaw)) {
            toRotateYaw = dYaw;
        }
        yaw += toRotateYaw;
        dYaw -= toRotateYaw;
        if (MathHelper.epsilonEquals(dYaw, 0f))
            dYaw = 0f;

        while (dPitch < -180f)
            dPitch += 360f;
        while (dPitch > 180f)
            dPitch -= 360f;

        float pitch = player.rotationPitch;
        float toRotatePitch = dPitch / turnMultiplier;
        if (toRotatePitch < 0) toRotatePitch--; else toRotatePitch++;
        if (Math.abs(toRotatePitch) > Math.abs(dPitch)) {
            toRotatePitch = dPitch;
        }
        pitch += toRotatePitch;
        dPitch -= toRotatePitch;

        player.rotationYaw = yaw;
        player.rotationPitch = pitch;
    }

    public static float getYaw(Vec3 direction) {
        return MathHelper.wrapAngleTo180_float((float) MathHelper.atan2(direction.zCoord, direction.xCoord) / (float) Math.PI * 180.0f - 90.0f);
    }

    public static float getPitch(Vec3 direction) {
        return -(float) MathHelper.atan2(direction.yCoord, MathHelper.sqrt_double(direction.xCoord * direction.xCoord + direction.zCoord * direction.zCoord)) / (float) Math.PI * 180.0f;
    }

    public static Vec3 getEntityPos(Entity e) {
        return new Vec3(e.posX, e.posY, e.posZ);
    }

    public static Vec3 getPlayerEyesPos(EntityPlayer player) {
        return getEntityPos(player).add(new Vec3(0, player.getEyeHeight(), 0));
    }
}
