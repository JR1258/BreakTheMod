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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class nationLocation {
    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    public static void register(){
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> command = LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("nationLocation")
                    .then(RequiredArgumentBuilder
                            .<FabricClientCommandSource, String>argument("nation", StringArgumentType.string())
                            .executes(context -> {
                                handleNationLocation(StringArgumentType.getString(context, "nation"), MinecraftClient.getInstance());
                                return 0;
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

    // A simple helper class to hold player data including coordinates.
    public static class PlayerData {
        public final String name;
        public final int x;
        public final int y;
        public final int z;

        public PlayerData(String name, int x, int y, int z) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public String toString() {
            return "PlayerData{name='" + name + "', x=" + x + ", y=" + y + ", z=" + z + '}';
        }
    }

    public static void handleNationLocation(String nation, @NotNull MinecraftClient client){
        CompletableFuture.runAsync(() -> {
            try {
                fetch FetchInstance = new fetch();
                String MapApi = FetchInstance.GetRequest("https://map.earthmc.net/tiles/players.json");
                JsonObject parsedAPI = JsonParser.parseString(MapApi).getAsJsonObject();
                JsonArray playersArray = parsedAPI.getAsJsonArray("players");

                JsonObject payload = new JsonObject();
                JsonArray queryArray = new JsonArray();
                JsonObject template = new JsonObject();
                queryArray.add(nation);
                template.addProperty("residents", true);
                payload.add("template", template);
                payload.add("query", queryArray);

                ArrayList<PlayerData> players = new ArrayList<>();
                for (JsonElement element : playersArray) {
                    JsonObject playerObj = element.getAsJsonObject();
                    String name = playerObj.get("name").getAsString();
                    int x = playerObj.get("x").getAsInt();
                    int y = playerObj.get("y").getAsInt();
                    int z = playerObj.get("z").getAsInt();
                    players.add(new PlayerData(name, x, y, z));
                }

                String response = new fetch().PostRequest("https://api.earthmc.net/v3/aurora/nations", payload.toString());
                JsonArray responseArray = JsonParser.parseString(response).getAsJsonArray();
                JsonObject parsedResponse = responseArray.get(0).getAsJsonObject();

                ArrayList<PlayerData> nationResidents = new ArrayList<>();
                JsonArray residentsArray = parsedResponse.get("residents").getAsJsonArray();
                for (JsonElement residentElement : residentsArray) {
                    JsonObject residentObj = residentElement.getAsJsonObject();
                    String residentName = residentObj.get("name").getAsString();
                    for (PlayerData player : players) {
                        if (player.name.equalsIgnoreCase(residentName)) {
                            nationResidents.add(player);
                        }
                    }
                }

                StringBuilder sb = new StringBuilder("Nation residents: ");
                for (PlayerData pd : nationResidents) {
                    sb.append(pd.name)
                            .append(" (x=")
                            .append(pd.x)
                            .append(", y=")
                            .append(pd.y)
                            .append(", z=")
                            .append(pd.z)
                            .append("), ");
                }

                if (!nationResidents.isEmpty()) {
                    sb.setLength(sb.length() - 2);
                }

                client.execute(() -> sendMessage(client, Text.literal(sb.toString())
                        .setStyle(Style.EMPTY.withColor(Formatting.GREEN))));
            } catch (Exception e){
                e.printStackTrace();
                client.execute(() -> sendMessage(client, Text.literal("Command exited with an exception.")
                        .setStyle(Style.EMPTY.withColor(Formatting.RED))));
                LOGGER.error("Command exited with an exception: " + e.getMessage());
            }
        });
    }
}