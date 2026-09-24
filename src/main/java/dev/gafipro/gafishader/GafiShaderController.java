package dev.gafipro.gafishader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

public final class GafiShaderController {
    private static TimeOverride timeOverride;
    private static WeatherOverride weatherOverride;
    private GafiShaderController() {}

    public static void reset() { timeOverride = null; weatherOverride = null; }

    public static void clearAll() {
        clearTime();
        clearWeather();
    }

    public static void tick(ClientWorld world) {
        if (timeOverride != null && timeOverride.tick(world)) {
            timeOverride.restore(world);
            timeOverride = null;
        }
        if (weatherOverride != null && weatherOverride.tick(world)) {
            weatherOverride.restore(world);
            weatherOverride = null;
        }
    }

    public static void setFixedTime(long timeOfDay, long durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) throw new IllegalStateException("Não estás num mundo.");
        timeOverride = TimeOverride.fixed(client.world.getTimeOfDay(), timeOfDay, durationTicks);
        applyImmediately();
    }

    public static void freezeTime(long durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) throw new IllegalStateException("Não estás num mundo.");
        long current = client.world.getTimeOfDay();
        timeOverride = TimeOverride.fixed(current, current, durationTicks);
        applyImmediately();
    }

    public static void setTimeSpeed(double multiplier, long durationTicks) {
        if (multiplier < 0) throw new IllegalArgumentException("A velocidade do tempo não pode ser negativa.");
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) throw new IllegalStateException("Não estás num mundo.");
        long current = client.world.getTimeOfDay();
        timeOverride = TimeOverride.speed(current, current, multiplier, durationTicks);
        applyImmediately();
    }

    public static void clearTime() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (timeOverride != null && client.world != null) timeOverride.restore(client.world);
        timeOverride = null;
    }
    public static void setWeather(float rain, float thunder, long durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) throw new IllegalStateException("Não estás num mundo.");
        weatherOverride = new WeatherOverride(
                client.world.getRainGradient(1.0f),
                client.world.getThunderGradient(1.0f),
                rain,
                thunder,
                durationTicks
        );
        applyImmediately();
    }

    public static void freezeWeather(long durationTicks) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) throw new IllegalStateException("Não estás num mundo.");
        float rain = client.world.getRainGradient(1.0f);
        float thunder = client.world.getThunderGradient(1.0f);
        weatherOverride = new WeatherOverride(rain, thunder, rain, thunder, durationTicks);
        applyImmediately();
    }

    public static void clearWeather() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (weatherOverride != null && client.world != null) weatherOverride.restore(client.world);
        weatherOverride = null;
    }

    public static String timeStatus() { return timeOverride == null ? "normal" : timeOverride.describe(); }
    public static String weatherStatus() { return weatherOverride == null ? "normal" : weatherOverride.describe(); }

    public static long resolveFullMoonNight(ClientWorld world) {
        long current = world.getTimeOfDay();
        long currentDay = Math.floorDiv(current, 24000L);
        long daysUntilFullMoon = Math.floorMod(-currentDay, 8L);
        if (daysUntilFullMoon == 0 && Math.floorMod(current, 24000L) > 13000L) daysUntilFullMoon = 8L;
        return (currentDay + daysUntilFullMoon) * 24000L + 13000L;
    }

    public static long resolveNextSunset(ClientWorld world) {
        long current = world.getTimeOfDay();
        long day = Math.floorDiv(current, 24000L);
        if (Math.floorMod(current, 24000L) >= 12000L) day++;
        return day * 24000L + 12000L;
    }

    public static String worldStatus() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return "sem mundo";
        ClientWorld world = client.world;
        return "time=" + Math.floorMod(world.getTimeOfDay(), 24000L)
                + " rain=" + String.format(java.util.Locale.ROOT, "%.2f", world.getRainGradient(1.0f))
                + " thunder=" + String.format(java.util.Locale.ROOT, "%.2f", world.getThunderGradient(1.0f));
    }

    private static void applyImmediately() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;
        if (timeOverride != null) timeOverride.apply(client.world);
        if (weatherOverride != null) weatherOverride.apply(client.world);
    }

    private static float clamp01(float value) { return Math.max(0.0f, Math.min(1.0f, value)); }

    private static final class TimeOverride {
        private final Mode mode;
        private final long duration;
        private long remaining;
        private final long restoreTime;
        private final long fixedTime;
        private final double speed;
        private double simulationTime;

        private TimeOverride(Mode mode, long duration, long restoreTime, long fixedTime, double speed) {
            this.mode = mode; this.duration = duration; this.remaining = duration;
            this.restoreTime = restoreTime; this.fixedTime = fixedTime; this.speed = speed;
            this.simulationTime = fixedTime;
        }

        static TimeOverride fixed(long restoreTime, long time, long duration) {
            return new TimeOverride(Mode.FIXED, duration, restoreTime, time, 0);
        }

        static TimeOverride speed(long restoreTime, long start, double speed, long duration) {
            return new TimeOverride(Mode.SPEED, duration, restoreTime, start, speed);
        }

        boolean tick(ClientWorld world) {
            apply(world);
            if (duration != DurationParser.FOREVER) { remaining--; return remaining <= 0; }
            return false;
        }

        void apply(ClientWorld world) {
            if (mode == Mode.FIXED) world.setTimeOfDay(fixedTime);
            else {
                simulationTime += speed;
                world.setTimeOfDay(Math.round(simulationTime));
            }
        }

        void restore(ClientWorld world) {
            world.setTimeOfDay(restoreTime);
        }

        String describe() {
            return mode == Mode.FIXED
                    ? "fixed=" + Math.floorMod(fixedTime, 24000L) + " for " + DurationParser.describe(duration)
                    : "speed=" + speed + "x for " + DurationParser.describe(duration);
        }
    }

    private enum Mode { FIXED, SPEED }

    private static final class WeatherOverride {
        private final float restoreRain;
        private final float restoreThunder;
        private final float rain;
        private final float thunder;
        private final long duration;
        private long remaining;

        private WeatherOverride(float rain, float thunder, long duration) {
            this(0.0f, 0.0f, rain, thunder, duration);
        }

        private WeatherOverride(float restoreRain, float restoreThunder, float rain, float thunder, long duration) {
            this.restoreRain = clamp01(restoreRain);
            this.restoreThunder = clamp01(restoreThunder);
            this.rain = clamp01(rain);
            this.thunder = clamp01(thunder);
            this.duration = duration;
            this.remaining = duration;
        }

        boolean tick(ClientWorld world) {
            apply(world);
            if (duration != DurationParser.FOREVER) { remaining--; return remaining <= 0; }
            return false;
        }

        void apply(ClientWorld world) {
            world.setRainGradient(rain);
            world.setThunderGradient(thunder);
        }

        void restore(ClientWorld world) {
            world.setRainGradient(restoreRain);
            world.setThunderGradient(restoreThunder);
        }

        String describe() {
            return "rain=" + rain + " thunder=" + thunder + " for " + DurationParser.describe(duration);
        }
    }
}
