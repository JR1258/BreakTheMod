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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.text.Style;
import net.minecraft.text.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.concurrent.CompletableFuture;

public class locate extends  Command {
    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    @Override
    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("locate")
                    .then(RequiredArgumentBuilder
                            .<FabricClientCommandSource, String>argument("name", StringArgumentType.word())
                            .then(RequiredArgumentBuilder
                                    .<FabricClientCommandSource, String>argument("type", StringArgumentType.word())
                                    .executes(context -> {
                                        if (!getEnabledOnOtherServers()) return 0;

                                        String name = StringArgumentType.getString(context, "name");
                                        String type = StringArgumentType.getString(context, "type").toLowerCase();

                                        MinecraftClient client = MinecraftClient.getInstance();

                                        if (client.player == null) {
                                            LOGGER.error("Player instance is null, cannot send feedback.");
                                            return 0;
                                        }

                                        CompletableFuture.runAsync(() -> {
                                            try {
                                                fetch FetchInstance = new fetch();
                                                JsonObject payload = new JsonObject();
                                                JsonArray queryArray = new JsonArray();
                                                queryArray.add(name);
                                                payload.add("query", queryArray);
                                                JsonObject template = new JsonObject();
                                                template.addProperty("coordinates", true);
                                                payload.add("template", template);

                                                String apiUrl = "";
                                                if ("town".equals(type)) {
                                                    apiUrl = "https://api.earthmc.net/v3/aurora/towns";
                                                } else if ("nation".equals(type)) {
                                                    apiUrl = "https://api.earthmc.net/v3/aurora/nations";
                                                } else {
                                                    client.execute(() -> {
                                                        sendMessage(client, Text.literal("Invalid type! Use 'town' or 'nation'.")
                                                                .setStyle(Style.EMPTY.withColor(Formatting.RED)));
                                                    });
                                                    return;
                                                }

                                                JsonArray response = JsonParser.parseString(FetchInstance.PostRequest(apiUrl, payload.toString())).getAsJsonArray();

                                                if (response.size() > 0) {
                                                    JsonObject coordinates = response.get(0).getAsJsonObject()
                                                            .get("coordinates").getAsJsonObject()
                                                            .get("spawn").getAsJsonObject();
                                                    int x = coordinates.get("x").getAsInt();
                                                    int z = coordinates.get("z").getAsInt();

                                                    String hyperlink = String.format(
                                                            "https://map.earthmc.net/?world=minecraft_overworld&zoom=3&x=%d&z=%d",
                                                            x, z
                                                    );
                                                    Text message = Text.literal(String.format("%s is located at X: %d, Z: %d. ", name, x, z))
                                                            .append(Text.literal("Click Here")
                                                                    .formatted(Formatting.AQUA)
                                                                    .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, hyperlink))));

                                                    client.execute(() -> sendMessage(client, message));
                                                } else {
                                                    client.execute(() -> sendMessage(client, Text.literal("Location not found.")
                                                            .setStyle(Style.EMPTY.withColor(Formatting.RED))));
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                client.execute(() -> sendMessage(client, Text.literal("Command exited with an exception.")
                                                        .setStyle(Style.EMPTY.withColor(Formatting.RED))));
                                                LOGGER.error("Command exited with an exception: " + e.getMessage());
                                            }
                                        });

                                        return 1;
                                    })
                            )
                    )
            );
        });
    }

   
}
