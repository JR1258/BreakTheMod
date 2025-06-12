package breakthemod.utils;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigTest {
    @Test
    public void testGetBooleanParsesTowny() {
        JsonObject obj = new JsonObject();
        obj.addProperty("towny", true);
        assertTrue(config.getBoolean(obj, "towny", false));
    }

    @Test
    public void testGetBooleanUsesDefaultWhenMissing() {
        JsonObject obj = new JsonObject();
        assertFalse(config.getBoolean(obj, "towny", false));
    }
}
