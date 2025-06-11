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

import breakthemod.utils.fetch;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public class PlayerNameTagRenderer {
    private static final Map<String, PlayerTownInfo> playerInfoCache = new ConcurrentHashMap<>();
    private static long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL = 30000; // Update every 30 seconds
    
    public static class PlayerTownInfo {
        public String town;
        public String nation;
        
        public PlayerTownInfo(String town, String nation) {
            this.town = town;
            this.nation = nation;
        }
    }
    
    public static void renderPlayerNameTags(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        // Update player info cache periodically
        updatePlayerInfoCache();
        
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || player.isInvisible()) continue;
            
            Vec3d playerPos = player.getPos();
            Vec3d cameraPos = camera.getPos();
            double distance = cameraPos.distanceTo(playerPos);
            
            if (distance > 64.0) continue; // Don't render beyond 64 blocks
            
            renderPlayerInfo(matrices, vertexConsumers, player, camera, distance);
        }
    }
    
    private static void renderPlayerInfo(MatrixStack matrices, VertexConsumerProvider vertexConsumers, 
                                       PlayerEntity player, Camera camera, double distance) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;
        
        Vec3d playerPos = player.getPos().add(0, player.getHeight() + 0.5, 0);
        Vec3d cameraPos = camera.getPos();
        
        matrices.push();
        matrices.translate(
            playerPos.x - cameraPos.x,
            playerPos.y - cameraPos.y,
            playerPos.z - cameraPos.z
        );
        
        // Face the camera
        matrices.multiply(camera.getRotation());
        matrices.scale(-0.025f, -0.025f, 0.025f);
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        String playerName = player.getName().getString();
        PlayerTownInfo info = playerInfoCache.get(playerName);
        
        float yOffset = 0;
        
        // Render town info
        if (info != null && info.town != null && !info.town.isEmpty()) {
            Text townText = Text.literal("§b" + info.town); // Light blue color
            int townWidth = textRenderer.getWidth(townText);
            textRenderer.draw(townText, -townWidth / 2f, yOffset, 0xFFFFFF, false, 
                            matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
            yOffset += 12;
        }
        
        // Render nation info
        if (info != null && info.nation != null && !info.nation.isEmpty()) {
            Text nationText = Text.literal("§e" + info.nation); // Yellow color
            int nationWidth = textRenderer.getWidth(nationText);
            textRenderer.draw(nationText, -nationWidth / 2f, yOffset, 0xFFFFFF, false,
                            matrix, vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, 0xF000F0);
        }
        
        matrices.pop();
    }
    
    private static void updatePlayerInfoCache() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime < UPDATE_INTERVAL) return;
        
        lastUpdateTime = currentTime;
        
        // Update cache using the same API as whereIs command
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;
            
            String playerName = player.getName().getString();
            CompletableFuture.runAsync(() -> fetchPlayerTownInfo(playerName));
        }
    }
    
    private static void fetchPlayerTownInfo(String playerName) {
        try {
            // Use the same API endpoints as your whereIs command
            JsonObject response = JsonParser.parseString(fetch.GetRequest("https://map.earthmc.net/tiles/players.json")).getAsJsonObject();
            
            for (JsonElement userElement : response.get("players").getAsJsonArray()) {
                JsonObject user = userElement.getAsJsonObject();
                if (user.get("name").getAsString().equalsIgnoreCase(playerName)) {
                    String apiUrl = "https://api.earthmc.net/v3/aurora/location";
                    JsonObject payload = new JsonObject();
                    JsonArray queryArray = new JsonArray();
                    JsonArray coordinatesArray = new JsonArray();
                    
                    coordinatesArray.add(user.get("x"));
                    coordinatesArray.add(user.get("z"));
                    queryArray.add(coordinatesArray);
                    payload.add("query", queryArray);
                    
                    JsonArray locationData = JsonParser.parseString(fetch.Fetch(apiUrl, payload.toString())).getAsJsonArray();
                    
                    if (locationData != null && locationData.size() == 1 && locationData.get(0).isJsonObject()) {
                        JsonObject data = locationData.get(0).getAsJsonObject();
                        
                        if (!data.get("isWilderness").getAsBoolean()) {
                            String townName = data.has("town") ? data.get("town").getAsString() : null;
                            String nationName = data.has("nation") ? data.get("nation").getAsString() : null;
                            
                            playerInfoCache.put(playerName, new PlayerTownInfo(townName, nationName));
                        } else {
                            // Clear cache for wilderness players
                            playerInfoCache.remove(playerName);
                        }
                    }
                    break;
                }
            }
        } catch (Exception e) {
            // Handle errors silently
        }
    }
    
    public static void clearCache() {
        playerInfoCache.clear();
    }
}