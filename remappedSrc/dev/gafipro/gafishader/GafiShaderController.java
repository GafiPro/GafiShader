package dev.gafipro.gafishader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

public final class GafiShaderController {
    private static TimeOverride timeOverride;
    private static WeatherOverride weatherOverride;
    private GafiShaderController() {}

    public static void reset() { timeOverride = null; weatherOverride = null; }

    public static void clearAll() {
        clearTime();
        clearWeather();
    }

    public static void tick(ClientLevel world) {
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
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) throw new IllegalStateException("Não estás num mundo.");
        timeOverride = TimeOverride.fixed(client.level.getDayTime(), timeOfDay, durationTicks);
        applyImmediately();
    }

    public static void freezeTime(long durationTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) throw new IllegalStateException("Não estás num mundo.");
        long current = client.level.getDayTime();
        timeOverride = TimeOverride.fixed(current, current, durationTicks);
        applyImmediately();
    }

    public static void setTimeSpeed(double multiplier, long durationTicks) {
        if (multiplier < 0) throw new IllegalArgumentException("A velocidade do tempo não pode ser negativa.");
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) throw new IllegalStateException("Não estás num mundo.");
        long current = client.level.getDayTime();
        timeOverride = TimeOverride.speed(current, current, multiplier, durationTicks);
        applyImmediately();
    }

    public static void clearTime() {
        Minecraft client = Minecraft.getInstance();
        if (timeOverride != null && client.level != null) timeOverride.restore(client.level);
        timeOverride = null;
    }
    public static void setWeather(float rain, float thunder, long durationTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) throw new IllegalStateException("Não estás num mundo.");
        weatherOverride = new WeatherOverride(
                client.level.getRainLevel(1.0f),
                client.level.getThunderLevel(1.0f),
                rain,
                thunder,
                durationTicks
        );
        applyImmediately();
    }

    public static void freezeWeather(long durationTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) throw new IllegalStateException("Não estás num mundo.");
        float rain = client.level.getRainLevel(1.0f);
        float thunder = client.level.getThunderLevel(1.0f);
        weatherOverride = new WeatherOverride(rain, thunder, rain, thunder, durationTicks);
        applyImmediately();
    }

    public static void clearWeather() {
        Minecraft client = Minecraft.getInstance();
        if (weatherOverride != null && client.level != null) weatherOverride.restore(client.level);
        weatherOverride = null;
    }

    public static String timeStatus() { return timeOverride == null ? "normal" : timeOverride.describe(); }
    public static String weatherStatus() { return weatherOverride == null ? "normal" : weatherOverride.describe(); }

    public static long resolveFullMoonNight(ClientLevel world) {
        long current = world.getDayTime();
        long currentDay = Math.floorDiv(current, 24000L);
        long daysUntilFullMoon = Math.floorMod(-currentDay, 8L);
        if (daysUntilFullMoon == 0 && Math.floorMod(current, 24000L) > 13000L) daysUntilFullMoon = 8L;
        return (currentDay + daysUntilFullMoon) * 24000L + 13000L;
    }

    public static long resolveNextSunset(ClientLevel world) {
        long current = world.getDayTime();
        long day = Math.floorDiv(current, 24000L);
        if (Math.floorMod(current, 24000L) >= 12000L) day++;
        return day * 24000L + 12000L;
    }

    public static String worldStatus() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return "sem mundo";
        ClientLevel world = client.level;
        return "time=" + Math.floorMod(world.getDayTime(), 24000L)
                + " rain=" + String.format(java.util.Locale.ROOT, "%.2f", world.getRainLevel(1.0f))
                + " thunder=" + String.format(java.util.Locale.ROOT, "%.2f", world.getThunderLevel(1.0f));
    }

    private static void applyImmediately() {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        if (timeOverride != null) timeOverride.apply(client.level);
        if (weatherOverride != null) weatherOverride.apply(client.level);
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

        boolean tick(ClientLevel world) {
            apply(world);
            if (duration != DurationParser.FOREVER) { remaining--; return remaining <= 0; }
            return false;
        }

        void apply(ClientLevel world) {
            if (mode == Mode.FIXED) world.setTimeFromServer(world.getGameTime(), fixedTime, false);
            else {
                simulationTime += speed;
                world.setTimeFromServer(world.getGameTime(), Math.round(simulationTime), false);
            }
        }

        void restore(ClientLevel world) {
            world.setTimeFromServer(world.getGameTime(), restoreTime, true);
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

        boolean tick(ClientLevel world) {
            apply(world);
            if (duration != DurationParser.FOREVER) { remaining--; return remaining <= 0; }
            return false;
        }

        void apply(ClientLevel world) {
            world.setRainLevel(rain);
            world.setThunderLevel(thunder);
        }

        void restore(ClientLevel world) {
            world.setRainLevel(restoreRain);
            world.setThunderLevel(restoreThunder);
        }

        String describe() {
            return "rain=" + rain + " thunder=" + thunder + " for " + DurationParser.describe(duration);
        }
    }
}
