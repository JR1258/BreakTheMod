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

package breakthemod.commands.economy;
import breakthemod.commands.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;


public class calculateStacks extends Command {
    @Override
    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> command = LiteralArgumentBuilder
                    .<FabricClientCommandSource>literal("calculateStacks")
                    .then(RequiredArgumentBuilder
                            .<FabricClientCommandSource, Integer>argument("blocks", IntegerArgumentType.integer())
                            .executes(context -> {
                                MinecraftClient client = MinecraftClient.getInstance();
                                if (!getEnabledOnOtherServers()) return 0;
                                int blocks = IntegerArgumentType.getInteger(context, "blocks");
                                int fullStacks = blocks / 64;
                                int remainder = blocks % 64;

                                String message = blocks + " blocks are " + fullStacks + " stacks and " + remainder + " blocks";

                                client.execute(()->sendMessage(client,Text.literal(message)));
                                return 0;
                            })
                    );

            dispatcher.register(command);
        });
    }
}