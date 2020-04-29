package org.imagine.editor.api;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.openide.util.Exceptions;

/**
 * Some debug stuff.
 *
 * @author Tim Boudreau
 */
public final class ContextLog {

    private static final ZonedDateTime INIT_TIME = ZonedDateTime.now();
    private static final Map<String, ContextLog> logs = new ConcurrentHashMap<>();
    private final String name;
    private FileChannel channel;

    static {
        Thread t = new Thread((Runnable) () -> {
            for (ContextLog log : logs.values()) {
                log.close();
            }
        });
        t.setName("Context log flush");
        t.setDaemon(true);
        Runtime.getRuntime().addShutdownHook(t);
    }

    public static ContextLog get(String name) {
        ContextLog log = logs.get(name);
        if (log == null) {
            log = new ContextLog(name);
            logs.put(name, log);
        }
        return log;
    }

    private ContextLog(String name) {
        this.name = name;
        Path dir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path file = dir.resolve(name + "-" + startTime());
        try {
            channel = FileChannel.open(file, StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE_NEW);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String startTime() {
        return INIT_TIME.getYear() + "-"
                + INIT_TIME.getMonthValue() + "-"
                + INIT_TIME.getDayOfMonth() + "-" + INIT_TIME.getHour()
                + "." + INIT_TIME.getMinute() + "." + INIT_TIME.getSecond();

    }

    private void close() {
        if (channel == null) {
            return;
        }
        try {
            channel.force(true);
            channel.close();
            channel = null;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

    public static void log(String s, String log) {
        get(s).log(log);
    }

    public static void log(String s, Supplier<String> log) {
        get(s).log(log);
    }

    public void log(Supplier<String> msg) {
        log(msg.get());
    }

    public void log(String msg) {
        Duration dur = Duration.ofMillis(System.currentTimeMillis() - INIT_TIME.toInstant().toEpochMilli());
        log(dur, msg);
    }

    private void log(Duration dur, String msg) {
        String fmt = format(dur, true);
        String output = fmt + " " + name + ": " + msg;
        System.out.println("CL:" + output);
        if (channel != null) {
            ByteBuffer buf = ByteBuffer.wrap((output + "\n").getBytes(UTF_8));
            try {
                channel.write(buf);
            } catch (IOException ex) {
                // do nothing
            }
        }
    }

    private static final NumberFormat TWO_DIGITS = new DecimalFormat("00");
    private static final NumberFormat THREE_DIGITS = new DecimalFormat("000");
    private static final NumberFormat MANY_DIGITS = new DecimalFormat("######00");

    /**
     * Format a duration to clock format of
     * <code>days:hours:minutes:seconds.millis</code> omitting any leading
     * fields which are zeros and preceeded only by fields which are zeros, so,
     * for example, one minute, 50 seconds and 3 millieconds would be 01:50.003.
     *
     * @param dur A duration
     * @return
     */
    private static String format(Duration dur) {
        return format(dur, false);
    }

    /**
     * Format a duration to clock format of
     * <code>days:hours:minutes:seconds.millis</code> omitting any leading
     * fields which are zeros and preceeded only by fields which are zeros, so,
     * for example, one minute, 50 seconds and 3 millieconds would be 01:50.003
     * if <code>includeAllFields</code> is false and 00:00:01:50.003 if
     * <code>includeAllFields</code> is true.
     *
     * @param dur A duration
     * @param includeAllFields Include leading fields which are all zeros to
     * generate a semi-fixed-length (with a caveat for > 99 days).
     * @return A string representation of a duration
     */
    private static String format(Duration dur, boolean includeAllFields) {
        long days = dur.toDays();
        long hours = dur.toHours() % 24;
        long minutes = dur.toMinutes() % 60;
        long seconds = 0;
        long millis = 0;
        try {
            seconds = (dur.toMillis() / 1_000) % 60;
            millis = dur.toMillis() % 1_000;
        } catch (Exception e) {
            seconds = 0;
        }

        StringBuilder sb = new StringBuilder();
        appendComponent(sb, days, ':', MANY_DIGITS, includeAllFields ? ChronoUnit.MILLIS : ChronoUnit.DAYS);
        appendComponent(sb, hours, ':', TWO_DIGITS, includeAllFields ? ChronoUnit.MILLIS : ChronoUnit.HOURS);
        appendComponent(sb, minutes, ':', TWO_DIGITS, includeAllFields ? ChronoUnit.MILLIS : ChronoUnit.MINUTES);
        appendComponent(sb, seconds, ':', TWO_DIGITS, ChronoUnit.SECONDS);
        appendComponent(sb, millis, '.', THREE_DIGITS, ChronoUnit.MILLIS);
        return sb.toString();
    }

    private static void appendComponent(StringBuilder sb, long val, char delim, NumberFormat fmt, ChronoUnit unit) {
        boolean use = ChronoUnit.SECONDS == unit || ChronoUnit.MILLIS == unit || val > 0 || sb.length() > 0;
        if (use) {
            if (sb.length() != 0) {
                sb.append(delim);
            }
            sb.append(fmt.format(val));
        }
    }

}
