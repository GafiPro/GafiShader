package dev.gafipro.gafishader;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class GafiShaderCommands {
    private GafiShaderCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(root("gafishader"));
            dispatcher.register(root("gafi"));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> root(String name) {
        return literal(name)
                .executes(GafiShaderCommands::help)
                .then(literal("help").executes(GafiShaderCommands::help))
                .then(literal("status").executes(GafiShaderCommands::status))
                .then(literal("reset").executes(GafiShaderCommands::resetAll))

                .then(literal("toggle").executes(GafiShaderCommands::toggle))
                .then(literal("on").executes(GafiShaderCommands::shaderOn))
                .then(literal("off").executes(GafiShaderCommands::shaderOff))
                .then(literal("settings").executes(GafiShaderCommands::settings))
                .then(literal("gui").executes(GafiShaderCommands::settings))
                .then(literal("reload").executes(GafiShaderCommands::reload))
                .then(effectCommands())

                .then(fixedTime("day", 1000))
                .then(fixedTime("alwaysday", 1000))
                .then(fixedTime("dawn", 0))
                .then(fixedTime("noon", 6000))
                .then(fixedTime("sunset", 12000))
                .then(fixedTime("night", 13000))
                .then(fixedTime("alwaysnight", 13000))
                .then(fixedTime("midnight", 18000))
                .then(freezeTime("freeze"))

                .then(durationWeather("clear", 0.0f, 0.0f))
                .then(durationWeather("alwaysclear", 0.0f, 0.0f))
                .then(durationWeather("rain", 1.0f, 0.0f))
                .then(durationWeather("alwaysrain", 1.0f, 0.0f))
                .then(durationWeather("snow", 1.0f, 0.0f))
                .then(durationWeather("alwayssnow", 1.0f, 0.0f))
                .then(durationWeather("storm", 1.0f, 1.0f))
                .then(durationWeather("thunder", 1.0f, 1.0f))

                .then(literal("aurora")
                        .executes(ctx -> aurora(ctx, "fullmoon", DurationParser.FOREVER))
                        .then(literal("force")
                                .executes(ctx -> aurora(ctx, "fullmoon", DurationParser.FOREVER))
                                .then(durationNode((ctx, d) -> aurora(ctx, "fullmoon", d))))
                        .then(literal("night")
                                .executes(ctx -> aurora(ctx, "night", DurationParser.FOREVER))
                                .then(durationNode((ctx, d) -> aurora(ctx, "night", d))))
                        .then(literal("fullmoon")
                                .executes(ctx -> aurora(ctx, "fullmoon", DurationParser.FOREVER))
                                .then(durationNode((ctx, d) -> aurora(ctx, "fullmoon", d))))
                        .then(literal("status").executes(GafiShaderCommands::auroraStatus))
                        .then(literal("release").executes(GafiShaderCommands::releaseAurora)))

                .then(complementaryCommands())
                .then(timeCommands())
                .then(weatherCommands())
                .then(presetCommands());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> effectCommands() {
        return literal("effect")
                .executes(ctx -> {
                    ctx.getSource().sendFeedback(Text.literal(ComplementaryCatalog.helpText()));
                    return 1;
                })
                .then(argument("option", StringArgumentType.word())
                        .suggests((ctx, builder) -> ComplementaryCatalog.optionSuggestions(builder))
                        .executes(ctx -> {
                            String option = StringArgumentType.getString(ctx, "option");
                            ctx.getSource().sendFeedback(Text.literal(
                                    "Shader effect " + option + ": indica um valor. Exemplos: on/off, 0, 1, 2, 3, reimagined, unbound."
                            ));
                            return 1;
                        })
                        .then(argument("value", StringArgumentType.word())
                                .suggests((ctx, builder) -> ComplementaryCatalog.genericValueSuggestions(builder))
                                .executes(GafiShaderCommands::effectSet)));
    }

    private static int effectSet(CommandContext<FabricClientCommandSource> ctx) {
        String option = StringArgumentType.getString(ctx, "option");
        String value = StringArgumentType.getString(ctx, "value");

        if (!ComplementaryCatalog.optionNames().contains(option)) {
            return fail(ctx, new IllegalArgumentException(
                    "Opção desconhecida no catálogo atual do Complementary Reimagined: " + option));
        }

        try {
            IrisBridge.setShaderPackOption(option, value);
            ctx.getSource().sendFeedback(Text.literal(
                    "Shader effect aplicado: " + option + "=" + value + "."
            ));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> timeCommands() {
        return literal("time")
                .executes(GafiShaderCommands::timeStatus)
                .then(literal("status").executes(GafiShaderCommands::timeStatus))
                .then(literal("normal").executes(GafiShaderCommands::timeNormal))
                .then(literal("unfreeze").executes(GafiShaderCommands::timeNormal))
                .then(fixedTimeSubcommand("day", 1000))
                .then(fixedTimeSubcommand("dawn", 0))
                .then(fixedTimeSubcommand("noon", 6000))
                .then(fixedTimeSubcommand("sunset", 12000))
                .then(fixedTimeSubcommand("night", 13000))
                .then(fixedTimeSubcommand("midnight", 18000))
                .then(literal("freeze")
                        .executes(GafiShaderCommands::timeFreeze)
                        .then(durationNode((ctx, d) -> timeFreeze(ctx, d))))
                .then(literal("set")
                        .then(argument("ticks", IntegerArgumentType.integer(0, 23999))
                                .executes(GafiShaderCommands::timeSet)
                                .then(durationNode((ctx, d) -> timeSet(ctx, d)))))
                .then(literal("speed")
                        .then(argument("multiplier", DoubleArgumentType.doubleArg(0.0, 100.0))
                                .executes(GafiShaderCommands::timeSpeed)
                                .then(durationNode((ctx, d) -> timeSpeed(ctx, d)))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> weatherCommands() {
        return literal("weather")
                .executes(GafiShaderCommands::weatherStatus)
                .then(literal("status").executes(GafiShaderCommands::weatherStatus))
                .then(literal("normal").executes(GafiShaderCommands::weatherNormal))
                .then(literal("unfreeze").executes(GafiShaderCommands::weatherNormal))
                .then(literal("freeze")
                        .executes(GafiShaderCommands::weatherFreeze)
                        .then(durationNode((ctx, d) -> weatherFreeze(ctx, d))))
                .then(durationWeather("clear", 0.0f, 0.0f))
                .then(durationWeather("rain", 1.0f, 0.0f))
                .then(durationWeather("snow", 1.0f, 0.0f))
                .then(durationWeather("storm", 1.0f, 1.0f))
                .then(durationWeather("thunder", 1.0f, 1.0f))
                .then(literal("intensity")
                        .then(argument("rain", DoubleArgumentType.doubleArg(0.0, 1.0))
                                .then(argument("thunder", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(GafiShaderCommands::weatherIntensity)
                                        .then(durationNode((ctx, d) -> weatherIntensity(ctx, d))))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> complementaryCommands() {
        return literal("complementary")
                .executes(ctx -> complementary(ctx, null))
                .then(literal("list").executes(ctx -> complementary(ctx, null)))
                .then(literal("settings").executes(GafiShaderCommands::settings))
                .then(literal("group")
                        .then(argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> CommandSource.suggestMatching(new String[]{
                                        "atmosphere", "clouds", "fog", "sunmoon", "weather", "water",
                                        "materials", "camera", "color", "dimensions"
                                }, builder))
                                .executes(ctx -> complementary(ctx, StringArgumentType.getString(ctx, "name")))));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> presetCommands() {
        return literal("preset")
                .then(preset("sunny"))
                .then(preset("night"))
                .then(preset("sunset"))
                .then(preset("storm"))
                .then(preset("aurora"));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> preset(String name) {
        return literal(name)
                .executes(ctx -> preset(ctx, name, DurationParser.FOREVER))
                .then(durationNode((ctx, d) -> preset(ctx, name, d)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> fixedTime(String name, int ticks) {
        return literal(name)
                .executes(ctx -> executeFixedTime(ctx, ticks, DurationParser.FOREVER))
                .then(durationNode((ctx, d) -> executeFixedTime(ctx, ticks, d)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> fixedTimeSubcommand(String name, int ticks) {
        return fixedTime(name, ticks);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> freezeTime(String name) {
        return literal(name)
                .executes(GafiShaderCommands::timeFreeze)
                .then(durationNode((ctx, d) -> timeFreeze(ctx, d)));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> durationWeather(String name, float rain, float thunder) {
        return literal(name)
                .executes(ctx -> executeWeather(ctx, rain, thunder, DurationParser.FOREVER))
                .then(durationNode((ctx, d) -> executeWeather(ctx, rain, thunder, d)));
    }

    private static ArgumentBuilder<FabricClientCommandSource, ?> durationNode(DurationHandler handler) {
        return argument("duration", StringArgumentType.word())
                .suggests(GafiShaderCommands::durationSuggestions)
                .executes(ctx -> {
                    try {
                        return handler.run(ctx, DurationParser.parseTicks(StringArgumentType.getString(ctx, "duration")));
                    } catch (Exception e) {
                        return fail(ctx, e);
                    }
                });
    }

    @FunctionalInterface
    private interface DurationHandler {
        int run(CommandContext<FabricClientCommandSource> ctx, long duration);
    }

    private static int executeFixedTime(CommandContext<FabricClientCommandSource> ctx, long time, long duration) {
        try {
            GafiShaderController.setFixedTime(time, duration);
            ctx.getSource().sendFeedback(Text.literal("Tempo definido para " + Math.floorMod(time, 24000L)
                    + " durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int executeWeather(CommandContext<FabricClientCommandSource> ctx, float rain, float thunder, long duration) {
        try {
            GafiShaderController.setWeather(rain, thunder, duration);
            ctx.getSource().sendFeedback(Text.literal("Clima: chuva=" + rain + " trovão=" + thunder
                    + " durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int timeFreeze(CommandContext<FabricClientCommandSource> ctx) {
        return timeFreeze(ctx, durationOrForever(ctx));
    }

    private static int timeFreeze(CommandContext<FabricClientCommandSource> ctx, long duration) {
        try {
            GafiShaderController.freezeTime(duration);
            ctx.getSource().sendFeedback(Text.literal("Tempo congelado durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int timeSet(CommandContext<FabricClientCommandSource> ctx) {
        return timeSet(ctx, durationOrForever(ctx));
    }

    private static int timeSet(CommandContext<FabricClientCommandSource> ctx, long duration) {
        try {
            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
            GafiShaderController.setFixedTime(ticks, duration);
            ctx.getSource().sendFeedback(Text.literal("Tempo definido para " + ticks + " durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int timeSpeed(CommandContext<FabricClientCommandSource> ctx) {
        return timeSpeed(ctx, durationOrForever(ctx));
    }

    private static int timeSpeed(CommandContext<FabricClientCommandSource> ctx, long duration) {
        try {
            double speed = DoubleArgumentType.getDouble(ctx, "multiplier");
            GafiShaderController.setTimeSpeed(speed, duration);
            ctx.getSource().sendFeedback(Text.literal("Velocidade do tempo: " + speed + "x durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int weatherFreeze(CommandContext<FabricClientCommandSource> ctx) {
        return weatherFreeze(ctx, durationOrForever(ctx));
    }

    private static int weatherFreeze(CommandContext<FabricClientCommandSource> ctx, long duration) {
        try {
            GafiShaderController.freezeWeather(duration);
            ctx.getSource().sendFeedback(Text.literal("Clima congelado durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int weatherIntensity(CommandContext<FabricClientCommandSource> ctx) {
        return weatherIntensity(ctx, durationOrForever(ctx));
    }

    private static int weatherIntensity(CommandContext<FabricClientCommandSource> ctx, long duration) {
        try {
            float rain = (float) DoubleArgumentType.getDouble(ctx, "rain");
            float thunder = (float) DoubleArgumentType.getDouble(ctx, "thunder");
            GafiShaderController.setWeather(rain, thunder, duration);
            ctx.getSource().sendFeedback(Text.literal("Intensidade: chuva=" + rain + " trovão=" + thunder
                    + " durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int aurora(CommandContext<FabricClientCommandSource> ctx, String mode, long duration) {
        try {
            if (ctx.getSource().getClient().world == null) throw new IllegalStateException("Não estás num mundo.");
            long target = mode.equals("night")
                    ? 13000L
                    : GafiShaderController.resolveFullMoonNight(ctx.getSource().getClient().world);
            GafiShaderController.setFixedTime(target, duration);
            GafiShaderController.setWeather(0.0f, 0.0f, duration);
            ctx.getSource().sendFeedback(Text.literal("Setup de aurora aplicado durante "
                    + DurationParser.describe(duration) + ". O shader continua a respeitar o seu próprio Aurora Condition."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int auroraStatus(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("Aurora: usa /gafishader complementary group atmosphere para veres AURORA_STYLE_DEFINE e AURORA_CONDITION."));
        return 1;
    }

    private static int releaseAurora(CommandContext<FabricClientCommandSource> ctx) {
        GafiShaderController.clearTime();
        GafiShaderController.clearWeather();
        ctx.getSource().sendFeedback(Text.literal("Setup de aurora libertado."));
        return 1;
    }

    private static int preset(CommandContext<FabricClientCommandSource> ctx, String name, long duration) {
        try {
            if (ctx.getSource().getClient().world == null) throw new IllegalStateException("Não estás num mundo.");
            switch (name) {
                case "sunny" -> {
                    GafiShaderController.setFixedTime(1000, duration);
                    GafiShaderController.setWeather(0.0f, 0.0f, duration);
                }
                case "night" -> {
                    GafiShaderController.setFixedTime(13000, duration);
                    GafiShaderController.setWeather(0.0f, 0.0f, duration);
                }
                case "sunset" -> {
                    GafiShaderController.setFixedTime(GafiShaderController.resolveNextSunset(ctx.getSource().getClient().world), duration);
                    GafiShaderController.setWeather(0.0f, 0.0f, duration);
                }
                case "storm" -> {
                    GafiShaderController.setFixedTime(13000, duration);
                    GafiShaderController.setWeather(1.0f, 1.0f, duration);
                }
                case "aurora" -> {
                    GafiShaderController.setFixedTime(GafiShaderController.resolveFullMoonNight(ctx.getSource().getClient().world), duration);
                    GafiShaderController.setWeather(0.0f, 0.0f, duration);
                }
                default -> throw new IllegalStateException("Preset desconhecido.");
            }
            ctx.getSource().sendFeedback(Text.literal("Preset " + name + " aplicado durante " + DurationParser.describe(duration) + "."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int help(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal(
                "GafiShader — client-side\n" +
                "Shader: toggle | on | off | settings | reload | status\n" +
                "Tempo: day | dawn | noon | sunset | night | midnight | freeze | time ...\n" +
                "Clima: clear | rain | snow | storm | thunder | weather ...\n" +
                "Efeitos: aurora ... | complementary ...\n" +
                "Presets: sunny | night | sunset | storm | aurora\n" +
                "Durações: 30s, 1m, 5m, 1h, 1d, 200t, 1h30m ou forever."
        ));
        return 1;
    }

    private static int status(CommandContext<FabricClientCommandSource> ctx) {
        Optional<Boolean> enabled = IrisBridge.shadersEnabled();
        String iris = !IrisBridge.isInstalled()
                ? "Iris=ausente"
                : "Iris=" + enabled.map(v -> v ? "on" : "off").orElse("desconhecido")
                + ", pack=" + (IrisBridge.shaderPackInUse() ? "ativo" : "inativo");

        ctx.getSource().sendFeedback(Text.literal(
                "GafiShader | " + iris
                        + " | world=" + GafiShaderController.worldStatus()
                        + " | timeOverride=" + GafiShaderController.timeStatus()
                        + " | weatherOverride=" + GafiShaderController.weatherStatus()
        ));
        return 1;
    }

    private static int resetAll(CommandContext<FabricClientCommandSource> ctx) {
        GafiShaderController.clearAll();
        ctx.getSource().sendFeedback(Text.literal("Todos os overrides client-side foram libertados e o estado anterior foi restaurado."));
        return 1;
    }

    private static int toggle(CommandContext<FabricClientCommandSource> ctx) {
        try {
            IrisBridge.toggleShaders();
            ctx.getSource().sendFeedback(Text.literal("Shaders alternados."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int shaderOn(CommandContext<FabricClientCommandSource> ctx) {
        try {
            IrisBridge.setShadersEnabled(true);
            ctx.getSource().sendFeedback(Text.literal("Shaders ligados."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int shaderOff(CommandContext<FabricClientCommandSource> ctx) {
        try {
            IrisBridge.setShadersEnabled(false);
            ctx.getSource().sendFeedback(Text.literal("Shaders desligados."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int settings(CommandContext<FabricClientCommandSource> ctx) {
        try {
            IrisBridge.openSettings();
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int reload(CommandContext<FabricClientCommandSource> ctx) {
        try {
            IrisBridge.reloadShaders();
            ctx.getSource().sendFeedback(Text.literal("Pedido de aplicação/reload enviado ao Iris."));
            return 1;
        } catch (Exception e) {
            return fail(ctx, e);
        }
    }

    private static int timeStatus(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("Tempo: " + GafiShaderController.timeStatus()));
        return 1;
    }

    private static int timeNormal(CommandContext<FabricClientCommandSource> ctx) {
        GafiShaderController.clearTime();
        ctx.getSource().sendFeedback(Text.literal("Override de tempo libertado."));
        return 1;
    }

    private static int weatherStatus(CommandContext<FabricClientCommandSource> ctx) {
        ctx.getSource().sendFeedback(Text.literal("Clima: " + GafiShaderController.weatherStatus()));
        return 1;
    }

    private static int weatherNormal(CommandContext<FabricClientCommandSource> ctx) {
        GafiShaderController.clearWeather();
        ctx.getSource().sendFeedback(Text.literal("Override de clima libertado."));
        return 1;
    }

    private static int complementary(CommandContext<FabricClientCommandSource> ctx, String group) {
        String text = group == null ? ComplementaryCatalog.helpText() : ComplementaryCatalog.groupText(group).getString();
        ctx.getSource().sendFeedback(Text.literal(text));
        return 1;
    }

    private static int fail(CommandContext<FabricClientCommandSource> ctx, Exception e) {
        ctx.getSource().sendError(Text.literal("GafiShader: " + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage())));
        return 0;
    }

    private static long durationOrForever(CommandContext<FabricClientCommandSource> ctx) {
        boolean hasDuration = ctx.getNodes().stream().anyMatch(node -> node.getNode().getName().equals("duration"));
        return hasDuration ? DurationParser.parseTicks(StringArgumentType.getString(ctx, "duration")) : DurationParser.FOREVER;
    }

    private static CompletableFuture<Suggestions> durationSuggestions(CommandContext<FabricClientCommandSource> ctx, SuggestionsBuilder builder) {
        return CommandSource.suggestMatching(new String[]{
                "30s", "1m", "5m", "10m", "30m", "1h", "1d", "200t", "1h30m", "forever"
        }, builder);
    }
}
