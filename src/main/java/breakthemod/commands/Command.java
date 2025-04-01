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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import breakthemod.utils.config;

public abstract class Command{
    public abstract void register();


    private static String getConnectedServerAddress() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        ServerInfo serverInfo = client.getCurrentServerEntry();
        if (serverInfo == null) return null;

        String serverAddress = serverInfo.address;

        if (serverAddress.contains(":")) {
            serverAddress = serverAddress.split(":")[0];
        }
        return serverAddress;
    }

    public static Boolean getEnabledOnOtherServers() {
        String serverAddress = getConnectedServerAddress();

        if (serverAddress == null) {
            return true;
        }

        String[] addressParts = serverAddress.split("\\.");

        if (addressParts.length >= 3
                && addressParts[addressParts.length - 2].equals("earthmc")
                && addressParts[addressParts.length - 1].equals("net")) {
            return true;
        }

        return config.getInstance().isEnabledOnOtherServers();
    }
}