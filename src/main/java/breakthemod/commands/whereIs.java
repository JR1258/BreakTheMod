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

package breakthemod.commands;

import breakthemod.utils.Prefix;
import breakthemod.utils.fetch;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

public class whereIs {
    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> command = LiteralArgumentBuilder
                .<FabricClientCommandSource>literal("whereIs")
                .then(RequiredArgumentBuilder
                    .<FabricClientCommandSource, String>argument("username", StringArgumentType.string())
                    .executes(context -> {
                    String username = StringArgumentType.getString(context, "username");
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) {
                        LOGGER.error("Player instance is null, cannot send feedback.");
                        return 0;
                    }

                    CompletableFuture.runAsync(() -> {
                        try {
                            fetch Fetch = new fetch();
                        
                            if (client == null || client.world == null) {
                                throw new IllegalStateException("Minecraft client or world is null.");
                            }

                            JsonObject response = JsonParser.parseString(Fetch.GetRequest("https://map.earthmc.net/tiles/players.json")).getAsJsonObject();

                            boolean found = false;
                            for (JsonElement User : response.get("players").getAsJsonArray()) {
                                JsonObject user = User.getAsJsonObject();
                                if (user.get("name").getAsString().equalsIgnoreCase(username)) {
                                    found = true;
                                    String apiUrl = "https://api.earthmc.net/v3/aurora/location";
                                    JsonObject payload = new JsonObject();
                                    JsonArray queryArray = new JsonArray();
                                    JsonArray coordinatesArray = new JsonArray();

                                    coordinatesArray.add(user.get("x"));
                                    coordinatesArray.add(user.get("z"));
                                    queryArray.add(coordinatesArray);
                                    
                                    payload.add("query", queryArray);

                                    JsonArray locationData = JsonParser.parseString(Fetch.Fetch(apiUrl, payload.toString())).getAsJsonArray();
                                    if (locationData != null && locationData.size() == 1 && locationData.get(0).isJsonObject()) {
                                        JsonObject data = locationData.get(0).getAsJsonObject();

                                        if (data.get("isWilderness").getAsBoolean()) {
                                            client.execute(() -> {
                                                sendMessage(client, Text.literal(username + "at coords x: " 
                                                    + user.get("x").getAsString() + ", z: " + user.get("z") + ", is in wilderness").setStyle(Style.EMPTY.withColor(Formatting.AQUA)));
                                            });
                                        }  else {
                                            JsonObject town = data.has("town") ? data.get("town").getAsJsonObject() : new JsonObject();
                                            String townName = town.has("name") ? town.get("name").getAsString() : "Unknown";
                                            client.execute(()->{
                                                sendMessage(client, Text.literal(username + " at coords x: " 
                                                + user.get("x").getAsString() + ", z: " + user.get("z") + ", is in town:" + townName).setStyle(Style.EMPTY.withColor(Formatting.AQUA)));
                                            });
                                            
                                        }
                                    break;
                                }
                            }
                            }
                            if (!found) {
                                client.execute(() -> {
                                    sendMessage(client, Text.literal(username + " is either offline or not showing up on the map"));
                                });
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                            client.execute(() -> sendMessage(client, Text.literal("Command has exited with an exception").setStyle(Style.EMPTY.withColor(Formatting.RED))));
                            LOGGER.error("Command has exited with an exception: " + e.getMessage());
                        }
                    });

                    return 1;
                })
            );

            dispatcher.register(command);  
        });
    }

    private static void sendMessage(MinecraftClient client, Text message) {
        client.execute(() -> {
            if (client.player != null) {
                Text prefix = Prefix.getPrefix();
                Text chatMessage = Text.literal("").append(prefix).append(message);
                client.player.sendMessage(chatMessage, false);
            }
        });
    }
      

}