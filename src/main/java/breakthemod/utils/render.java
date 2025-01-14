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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class render {

    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");
    private static final List<String> playerList = new ArrayList<>();
    private static final int MARGIN = 10;

    private static int customX = 10;
    private static int customY = 10;
    private static WidgetPosition widgetPosition = WidgetPosition.TOP_LEFT;

    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 1000; // Update every 1 second

    public enum WidgetPosition {
        TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM, TOP_LEFT
    }

    public static void setCustomPosition(int x, int y) {
        customX = x;
        customY = y;
        widgetPosition = WidgetPosition.CUSTOM;
    }

    public static void setWidgetPosition(WidgetPosition position) {
        widgetPosition = position;
    }

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
        if (yaw >= 337.5 || yaw < 22.5) return "South";
        if (yaw >= 22.5 && yaw < 67.5) return "Southwest";
        if (yaw >= 67.5 && yaw < 112.5) return "West";
        if (yaw >= 112.5 && yaw < 157.5) return "Northwest";
        if (yaw >= 157.5 && yaw < 202.5) return "North";
        if (yaw >= 202.5 && yaw < 247.5) return "Northeast";
        if (yaw >= 247.5 && yaw < 292.5) return "East";
        if (yaw >= 292.5 && yaw < 337.5) return "Southeast";
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
        return player.getWorld().getRegistryKey().getValue().equals(new Identifier("minecraft", "nether"));
    }

    private static boolean isInVehicle(PlayerEntity player) {
        return player.getVehicle() != null;
    }

    private static boolean isPlayerUnderBlock(MinecraftClient client, BlockPos playerBlockPos) {
        for (int y = playerBlockPos.getY() + 1; y <= client.world.getTopY(); y++) {
            BlockPos checkPos = new BlockPos(playerBlockPos.getX(), y, playerBlockPos.getZ());
            BlockState blockStateAbove = client.world.getBlockState(checkPos);
            if (!blockStateAbove.isAir()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Renders an overlay using DrawContext.
     */
    public static void renderOverlay(DrawContext drawContext, MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        // Update the player list every second
        updateNearbyPlayers(client);

        TextRenderer textRenderer = client.textRenderer;

        // Determine position
        int x = switch (widgetPosition) {
            case TOP_RIGHT, BOTTOM_RIGHT -> client.getWindow().getScaledWidth() - 210;
            case CUSTOM -> customX;
            default -> 10;
        };

        int y = switch (widgetPosition) {
            case BOTTOM_LEFT -> client.getWindow().getScaledHeight() - (20 + playerList.size() * 15 + 10); // Adjusted for new spacing
            case BOTTOM_RIGHT -> client.getWindow().getScaledHeight() - (20 + playerList.size() * 15 + 10); // Adjusted for new spacing
            case CUSTOM -> customY;
            default -> 10;
        };

        // Adjusted spacing between entries
        int entryHeight = 15; // Increased from 10 to 15 for more spacing
        int width = playerList.stream().mapToInt(String::length).max().orElse(20) * 6 + 2 * MARGIN;
        int height = Math.max(20 + playerList.size() * entryHeight, 40);

        // Render the box
        drawContext.fill(x, y, x + width, y + height, 0x80000000); // Semi-transparent black

        // Render the text
        int textY = y + 5;
        synchronized (playerList) {
            for (String line : playerList) {
                drawContext.drawTextWithShadow(textRenderer, line, x + MARGIN, textY, 0xFFFFFF);
                textY += entryHeight; // Use the adjusted entryHeight for spacing
            }
        }
    }

}
