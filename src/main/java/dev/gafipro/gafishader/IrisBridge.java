package dev.gafipro.gafishader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Properties;

public final class IrisBridge {
    private static final String API_CLASS = "net.irisshaders.iris.api.v0.IrisApi";
    private static final String CONFIG_CLASS = "net.irisshaders.iris.api.v0.IrisApiConfig";
    private IrisBridge() {}

    public static boolean isInstalled() { return FabricLoader.getInstance().isModLoaded("iris"); }

    public static Optional<Boolean> shadersEnabled() {
        if (!isInstalled()) return Optional.empty();
        try {
            Object config = getConfig();
            Method method = Class.forName(CONFIG_CLASS).getMethod("areShadersEnabled");
            return Optional.of((Boolean) method.invoke(config));
        } catch (ReflectiveOperationException | LinkageError e) { return Optional.empty(); }
    }

    public static boolean shaderPackInUse() {
        if (!isInstalled()) return false;
        try {
            Object api = getApi();
            Method method = Class.forName(API_CLASS).getMethod("isShaderPackInUse");
            return (Boolean) method.invoke(api);
        } catch (ReflectiveOperationException | LinkageError e) { return false; }
    }

    public static void setShadersEnabled(boolean enabled) {
        requireIris();
        try {
            Object config = getConfig();
            Method method = Class.forName(CONFIG_CLASS).getMethod("setShadersEnabledAndApply", boolean.class);
            method.invoke(config, enabled);
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("A API pública do Iris não pôde ser usada: " + e.getClass().getSimpleName(), e);
        }
    }

    public static void toggleShaders() { setShadersEnabled(!shadersEnabled().orElse(false)); }

    public static void openSettings() {
        requireIris();
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            Object api = getApi();
            Method method = Class.forName(API_CLASS).getMethod("openMainIrisScreenObj", Object.class);
            Object screen = method.invoke(api, client.currentScreen);
            if (screen instanceof Screen irisScreen) client.setScreen(irisScreen);
            else throw new IllegalStateException("O Iris não devolveu um Screen válido.");
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException("Não foi possível abrir as opções do Iris.", e);
        }
    }

    public static void reloadShaders() {
        requireIris();
        Optional<Boolean> enabled = shadersEnabled();
        if (enabled.isEmpty()) throw new IllegalStateException("Não foi possível ler o estado dos shaders.");
        setShadersEnabled(enabled.get());
    }

    /**
     * Applies an Iris/OptiFine-compatible shader-pack option through Iris' own
     * option queue. This is intentionally reflective so GafiShader remains
     * optional with respect to Iris and does not hard-link against Iris internals.
     */
    public static void setShaderPackOption(String option, String value) {
        requireIris();
        if (option == null || option.isBlank()) throw new IllegalArgumentException("Opção vazia.");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Valor vazio.");

        try {
            Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
            Method queue = irisClass.getMethod("queueShaderPackOptionsFromProperties", Properties.class);
            Properties properties = new Properties();
            properties.setProperty(option, normalizeOptionValue(value));
            queue.invoke(null, properties);

            Optional<Boolean> enabled = shadersEnabled();
            if (enabled.isEmpty()) throw new IllegalStateException("O Iris não devolveu o estado atual dos shaders.");
            setShadersEnabled(enabled.get());
        } catch (ReflectiveOperationException | LinkageError e) {
            throw new IllegalStateException(
                    "Esta versão do Iris não expõe o mecanismo interno necessário para alterar opções do shader: "
                            + e.getClass().getSimpleName(), e);
        }
    }

    private static String normalizeOptionValue(String value) {
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "on", "true" -> "true";
            case "off", "false" -> "false";
            default -> value.trim();
        };
    }

    private static Object getApi() throws ReflectiveOperationException {
        Class<?> apiClass = Class.forName(API_CLASS);
        return apiClass.getMethod("getInstance").invoke(null);
    }

    private static Object getConfig() throws ReflectiveOperationException {
        return Class.forName(API_CLASS).getMethod("getConfig").invoke(getApi());
    }

    private static void requireIris() {
        if (!isInstalled()) throw new IllegalStateException("Iris não está instalado.");
    }
}
