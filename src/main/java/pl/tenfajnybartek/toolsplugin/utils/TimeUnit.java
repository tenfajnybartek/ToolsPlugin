package pl.tenfajnybartek.toolsplugin.utils;

public enum TimeUnit {
    SECONDS("s", 1L),
    MINUTES("m", 60L),
    HOURS("h", 60L * 60L),
    DAYS("d", 24L * 60L * 60L),
    WEEKS("w", 7L * 24L * 60L * 60L),
    YEARS("y", 365L * 24L * 60L * 60L);

    private final String shortcut;
    private final long seconds;

    TimeUnit(String shortcut, long seconds) {
        this.shortcut = shortcut;
        this.seconds = seconds;
    }

    public String getShortcut() {
        return shortcut;
    }

    public long getSeconds() {
        return seconds;
    }
}
