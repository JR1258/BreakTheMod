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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import breakthemod.utils.config;
import net.minecraft.text.Text;

public abstract class Command{
    public abstract void register();


    private static String getConnectedServerAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null) return null;

        return serverInfo.address.split(",")[0];
    }

    public static Boolean getEnabledOnOtherServers() {
        String serverAddress = getConnectedServerAddress();

        if (serverAddress == null) {return true;}

        if (serverAddress.toLowerCase().endsWith("earthmc.net")) return true;

        return config.getInstance().isEnabledOnOtherServers();
    }



    public void sendMessage(MinecraftClient client, Text message) {
        client.execute(() -> {
            if (client.player != null) {
                Text prefix = Prefix.getPrefix();
                Text chatMessage = Text.literal("").append(prefix).append(message);
                client.player.sendMessage(chatMessage, false);
            }
        });
    }
}