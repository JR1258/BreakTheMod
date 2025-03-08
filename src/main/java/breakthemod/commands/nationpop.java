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
import breakthemod.utils.fetch;
import breakthemod.utils.Prefix;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;


public class nationpop {
    private static final Logger LOGGER = LoggerFactory.getLogger("breakthemod");

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<FabricClientCommandSource>literal("nationpop")
                            .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                    .executes(context -> {
                                        String name = StringArgumentType.getString(context, "name");
                                        return handleNationPop(name, MinecraftClient.getInstance());

                                    })
                            )
            );
        });
    }

    public static int handleNationPop(String name, @NotNull MinecraftClient client){
        CompletableFuture.runAsync(() -> {
           try {

               JsonObject payload = new JsonObject();
               JsonArray queryArray = new JsonArray();
               queryArray.add(name);
               payload.add("query", queryArray);
               String response = new fetch().PostRequest("https://api.earthmc.net/v3/aurora/nations", payload.toString());
               JsonObject parsedResponse = JsonParser.parseString(response).getAsJsonArray().get(0).getAsJsonObject();
               int numResidents =  parsedResponse.get("stats").getAsJsonObject().get("numResidents").getAsInt();
               sendMessage(client, Text.literal(name + " nation has " + numResidents + " residents and a nation bonus of " + getNationBonus(numResidents)));
           }  catch (Exception e){
               LOGGER.error("Unexpected error encountered", e);
           }
        });
        return 0;
    }

    private static void sendMessage(@NotNull MinecraftClient client, net.minecraft.text.Text message) {
        client.execute(() -> {
            if (client.player != null) {
                net.minecraft.text.Text prefix = Prefix.getPrefix();
                net.minecraft.text.Text chatMessage = Text.literal("").append(prefix).append(message);
                client.player.sendMessage(chatMessage, false);
            }
        });
    }

    public static int getNationBonus(int residentAmt) {
        if (residentAmt >= 200) return 100;
        else if (residentAmt >= 120) return 80;
        else if (residentAmt >= 80) return 60;
        else if (residentAmt >= 60) return 50;
        else if (residentAmt >= 40) return 30;
        else if (residentAmt >= 20) return 10;
        else return 0;
    }
}
