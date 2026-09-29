package dev.gafipro.gafishader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DurationParser {
    public static final long FOREVER = Long.MAX_VALUE;
    private static final Pattern TOKEN = Pattern.compile("(\\d+(?:\\.\\d+)?)([dhmst])", Pattern.CASE_INSENSITIVE);
    private DurationParser() {}

    public static long parseTicks(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("A duração não pode estar vazia.");
        String value = raw.trim().toLowerCase(Locale.ROOT);
        if (value.equals("forever") || value.equals("infinite") || value.equals("infinity") || value.equals("inf")) return FOREVER;

        Matcher matcher = TOKEN.matcher(value);
        int end = 0;
        double totalTicks = 0;
        while (matcher.find()) {
            if (matcher.start() != end) throw new IllegalArgumentException("Duração inválida: " + raw);
            double amount = Double.parseDouble(matcher.group(1));
            char unit = matcher.group(2).charAt(0);
            double multiplier = switch (unit) {
                case 'd' -> 24000.0;
                case 'h' -> 72000.0;
                case 'm' -> 1200.0;
                case 's' -> 20.0;
                case 't' -> 1.0;
                default -> throw new IllegalArgumentException("Unidade desconhecida: " + unit);
            };
            totalTicks += amount * multiplier;
            if (totalTicks >= FOREVER - 1) return FOREVER;
            end = matcher.end();
        }
        if (end != value.length()) throw new IllegalArgumentException("Duração inválida: " + raw);
        long ticks = Math.round(totalTicks);
        if (ticks <= 0) throw new IllegalArgumentException("A duração tem de ser maior que zero.");
        return ticks;
    }

    public static String describe(long ticks) {
        if (ticks == FOREVER) return "forever";
        if (ticks % 24000 == 0) return (ticks / 24000) + "d";
        if (ticks % 1200 == 0) return (ticks / 1200) + "m";
        if (ticks % 20 == 0) return (ticks / 20) + "s";
        return ticks + "t";
    }
}
