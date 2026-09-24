package dev.gafipro.gafishader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GafiShaderClient implements ClientModInitializer {
    public static final String MOD_ID = "gafishader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        GafiShaderController.reset();
        GafiShaderCommands.register();
        ClientTickEvents.END_WORLD_TICK.register(GafiShaderController::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> GafiShaderController.reset());
        LOGGER.info("GafiShader client initialized.");
    }
}
