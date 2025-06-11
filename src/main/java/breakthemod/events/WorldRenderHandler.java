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
package breakthemod.events;

import breakthemod.utils.PlayerNameTagRenderer;
import breakthemod.utils.config;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;

public class WorldRenderHandler {
    
    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderHandler::onWorldRender);
    }
    
    private static void onWorldRender(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        config Config = config.getInstance();
        
        // Only render if towny HUD is enabled and we're on the right server
        if (!Config.isTowny() || client.world == null || client.player == null) {
            return;
        }
        
        // Check if we're on EarthMC (optional - remove this check if you want it on all servers)
        if (client.getCurrentServerEntry() != null) {
            String serverAddress = client.getCurrentServerEntry().address;
            if (!serverAddress.contains("earthmc")) {
                return;
            }
        }
        
        PlayerNameTagRenderer.renderPlayerNameTags(
            context.matrixStack(),
            context.consumers(),
            context.camera()
        );
    }
}