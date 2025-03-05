/*
 * This file is part of BreakTheMod.
 *
 * BreakTheMod is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreakTheMod is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreakTheMod. If not, see <https://www.gnu.org/licenses/>.
 */
package breakthemod.utils;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import breakthemod.utils.config.WidgetPosition;
import java.util.*;
import breakthemod.commands.nearby;

public class render {

    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");
    private static final List<String> playerList = new ArrayList<>();
    private static final int MARGIN = 10;

    private static int customX = 10;
    private static int customY = 10;
    private static WidgetPosition widgetPosition = WidgetPosition.TOP_LEFT;

    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every 1 second

    /**
     * Updates the nearby player information asynchronously.
     */
    public static void updateNearbyPlayers(MinecraftClient client) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) {
            return; // Avoid updating too frequently
        }

        lastUpdateTime = currentTime; // Update the last update time

        Set<String> playerInfoList = new HashSet<>();

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity otherPlayer && entity != client.player) {
                if (shouldSkipPlayer(otherPlayer, client)) {
                    continue;
                }

                Vec3d otherPlayerPos = otherPlayer.getPos();
                BlockPos playerBlockPos = new BlockPos(
                        (int) Math.floor(otherPlayerPos.getX()),
                        (int) Math.floor(otherPlayerPos.getY()),
                        (int) Math.floor(otherPlayerPos.getZ())
                );

                if (!isPlayerUnderBlock(client, playerBlockPos)) {
                    double distance = client.player.getPos().distanceTo(otherPlayerPos);
                    String otherPlayerName = otherPlayer.getName().getString();
                    int x = (int) otherPlayerPos.getX();
                    int z = (int) otherPlayerPos.getZ();
                    String direction = getDirectionFromYaw(otherPlayer.getYaw());

                    playerInfoList.add(String.format(
                            "- %s (%d, %d) direction: %s, distance: %.1f blocks",
                            otherPlayerName, x, z, direction, distance
                    ));
                }
            }
        }

        synchronized (playerList) {
            playerList.clear();
            if (playerInfoList.isEmpty()) {
                playerList.add("No players nearby");
            } else {
                playerList.addAll(playerInfoList);
            }
        }
    }

    private static String getDirectionFromYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360; // Normalize yaw to [0, 360)
        if (yaw >= 337.5 || yaw < 22.5) return "S";
        if (yaw >= 22.5 && yaw < 67.5) return "SW";
        if (yaw >= 67.5 && yaw < 112.5) return "W";
        if (yaw >= 112.5 && yaw < 157.5) return "NW";
        if (yaw >= 157.5 && yaw < 202.5) return "N";
        if (yaw >= 202.5 && yaw < 247.5) return "NE";
        if (yaw >= 247.5 && yaw < 292.5) return "E";
        if (yaw >= 292.5 && yaw < 337.5) return "SE";
        return "Unknown";
    }

    private static boolean shouldSkipPlayer(PlayerEntity player, MinecraftClient client) {
        return player.isInvisible() || isPlayerInRiptideAnimation(player) || isInNether(player)
                || isInVehicle(player) || player.isSneaking();
    }

    private static boolean isPlayerInRiptideAnimation(PlayerEntity player) {
        return player.getActiveItem().getItem().toString().contains("riptide");
    }

    private static boolean isInNether(PlayerEntity player) {
        return player.getWorld().getRegistryKey() == World.NETHER;
    }

    private static boolean isInVehicle(PlayerEntity player) {
        return player.getVehicle() != null;
    }

    private static boolean isPlayerUnderBlock(MinecraftClient client, BlockPos playerBlockPos) {
        boolean isUnderAnyBlock = false;
        int topY = client.world.getTopY(Heightmap.Type.MOTION_BLOCKING, playerBlockPos.getX(), playerBlockPos.getZ());
        for (int y = playerBlockPos.getY() + 1; y <= topY; y++) {
            BlockPos checkPos = new BlockPos(playerBlockPos.getX(), y, playerBlockPos.getZ());
            BlockState blockStateAbove = client.world.getBlockState(checkPos);
            if (!blockStateAbove.isAir()) {
                isUnderAnyBlock = true;
                break;
            }
        }
        return isUnderAnyBlock;
    }

    /**
     * Renders an overlay using DrawContext.
     */
    public void renderOverlay(DrawContext drawContext, MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        config Config = new config();

        if (!Config.getRadarEnabled()) return;

        widgetPosition = Config.getWidgetPosition();
        updateNearbyPlayers(client);

        TextRenderer textRenderer = client.textRenderer;

        int entryHeight = 15;
        int width = playerList.stream()
                .mapToInt(textRenderer::getWidth)
                .max()
                .orElse(20) + 2 * MARGIN;

        int height = Math.max(20 + playerList.size() * entryHeight, 40);

        int x = 0, y = 0;
        switch (widgetPosition) {
            case TOP_RIGHT -> {
                x = client.getWindow().getScaledWidth() - width - MARGIN;
                y = MARGIN;
            }
            case BOTTOM_LEFT -> {
                x = MARGIN;
                y = client.getWindow().getScaledHeight() - height - MARGIN;
            }
            case BOTTOM_RIGHT -> {
                x = client.getWindow().getScaledWidth() - width - MARGIN;
                y = client.getWindow().getScaledHeight() - height - MARGIN;
            }
            case CUSTOM -> {
                x = customX;
                y = customY;
            }
            case TOP_LEFT -> { // Default case
                x = MARGIN;
                y = MARGIN;
            }
        }


        int textY = y + 5;
        synchronized (playerList) {
            for (String line : playerList) {
                drawContext.drawText(textRenderer, line, x + MARGIN, textY, 0xFFFFFF, false);
                textY += entryHeight;
            }
        }
    }


}
