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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class config {
    // Static fields for widget settings
    public static WidgetPosition widgetPosition = WidgetPosition.TOP_LEFT;
    public static int customX = 0;
    public static int customY = 0;
    public static boolean radarEnabled = true;

    // Config file and Gson instance for saving/loading settings
    private static final File configFile = new File(MinecraftClient.getInstance().runDirectory, "config/breakthemod_config.json");
    private static final Gson gson = new Gson();

    // Method to create the config screen
    public Screen widget(Screen parent) {
        // Load the saved configuration before creating the screen
        loadConfig();

        // Create the configuration builder
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("BreakTheMod"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        ConfigCategory general = builder.getOrCreateCategory(Text.literal("BreakTheMod config"));

        // Add a dropdown for predefined positions
        general.addEntry(entryBuilder.startEnumSelector(
                        Text.literal("Widget Position"),
                        WidgetPosition.class,
                        widgetPosition // Default value from static variable
                )
                .setSaveConsumer(position -> {
                    setWidgetPosition(position);
                    saveConfig();
                })
                .build());

        // Add sliders for custom X and Y positions
        general.addEntry(entryBuilder.startIntField(
                        Text.literal("Custom X Position"),
                        customX // Default value from static variable
                )
                .setSaveConsumer(x -> {
                    setCustomX(x);
                    saveConfig();
                })
                .setMin(0)
                .setMax(MinecraftClient.getInstance().currentScreen.width) // Max value for screen width (adjust if needed)
                .build());

        general.addEntry(entryBuilder.startIntField(
                        Text.literal("Custom Y Position"),
                        customY // Default value from static variable
                )
                .setSaveConsumer(y -> {
                    setCustomY(y);
                    saveConfig();
                })
                .setMin(0)
                .setMax(MinecraftClient.getInstance().currentScreen.height) // Max value for screen height (adjust if needed)
                .build()

        );

        general.addEntry(entryBuilder.startBooleanToggle(
                Text.literal("Player radar"),
                radarEnabled)
                .setSaveConsumer(enabled -> {
                    radarEnabled = enabled;
                    saveConfig();
                })
                .build()
        );

      ;

        return builder.build();
    }

    // Enum for predefined widget positions
    public enum WidgetPosition {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_RIGHT,
        BOTTOM_LEFT,
        CUSTOM
    }

    // getters and setters
    public WidgetPosition getWidgetPosition() {return widgetPosition;}

    public void setWidgetPosition(WidgetPosition position) {widgetPosition = position;}

    public int getCustomX() {return customX;}

    public void setCustomX(int x) {customX = x;}

    public int getCustomY() {return customY;}

    public void setCustomY(int y) {customY = y;}

    public boolean getRadarEnabled(){return radarEnabled;}
    // Save configuration to file
    public void saveConfig() {
        JsonObject configJson = new JsonObject();

        configJson.addProperty("widgetPosition", widgetPosition.name());

        configJson.addProperty("customX", customX);
        configJson.addProperty("customY", customY);
        configJson.addProperty("radarEnabled",radarEnabled);

        try (FileWriter writer = new FileWriter(configFile)) {
            gson.toJson(configJson, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Load configuration from file
    public void loadConfig() {
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                JsonObject configJson = gson.fromJson(reader, JsonObject.class);
                String position = configJson.get("widgetPosition").getAsString();
                widgetPosition = WidgetPosition.valueOf(position);
                customX = configJson.get("customX").getAsInt();
                customY = configJson.get("customY").getAsInt();
                radarEnabled = configJson.get("radarEnabled").getAsBoolean();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
