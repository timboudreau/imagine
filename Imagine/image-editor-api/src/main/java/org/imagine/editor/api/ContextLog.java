package org.imagine.editor.api;

import com.mastfrog.util.strings.Strings;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.openide.util.Exceptions;

/**
 * Some simple debug logging with zero overhead when off.
 *
 * @author Tim Boudreau
 */
public abstract class ContextLog {

    private static final ZonedDateTime INIT_TIME = ZonedDateTime.now();
    private static final Map<String, ContextLog> logs = new ConcurrentHashMap<>();
    private static final String SYSPROP_FILE = "context.logs.file"; // NOI18N
    private static final String SYSPROP = "context.logs"; // NOI18N
    private static final Set<String> fileLogs = new HashSet<>();
    private static final Set<String> consoleOnly = new HashSet<>();
    private static final boolean empty;

    static {
        String fileLogProp = System.getProperty(SYSPROP_FILE);

        if (fileLogProp != null) {
            for (CharSequence seq : Strings.splitUniqueNoEmpty(',', fileLogProp)) {
                fileLogs.add(seq.toString());
            }
        }
        String consoleOnlyProp = System.getProperty(SYSPROP);
        if (consoleOnlyProp != null) {
            for (CharSequence seq : Strings.splitUniqueNoEmpty(',', consoleOnlyProp)) {
                consoleOnly.add(seq.toString());
            }
        }
        empty = fileLogs.isEmpty() && consoleOnly.isEmpty();
        if (!fileLogs.isEmpty()) {
            Thread t = new Thread((Runnable) () -> {
                for (Map.Entry<String, ContextLog> e : logs.entrySet()) {
                    ContextLog log = e.getValue();
                    log.log("Close " + e.getKey());
                    log.close();
                }
                logs.clear();
            });
            t.setName("Context log flush");
            t.setDaemon(true);
            Runtime.getRuntime().addShutdownHook(t);
        }
    }

    ContextLog() {
        // do nothing
    }

    public static ContextLog get(String name) {
        if (empty) {
            return new NoOpLog();
        }
        ContextLog log = logs.get(name);
        if (log == null) {
            if (fileLogs.contains(name)) {
                log = new RealLog(name);
            } else if (consoleOnly.contains(name)) {
                log = new ConsoleLog(name);
            } else {
                log = new NoOpLog();
            }
            logs.put(name, log);
        }
        return log;
    }

    private static final class NoOpLog extends ContextLog {

        @Override
        public void log(Supplier<String> msg) {
            // do nothing
        }

        @Override
        public void log(String msg) {
            // do nothing
        }
    }

    private static final class ConsoleLog extends ContextLog {

        private final String name;

        ConsoleLog(String name) {
            this.name = name;
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
        }
    }

    private static final class RealLog extends ContextLog {

        private final String name;
        private FileChannel channel;

        private RealLog(String name) {
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

        void close() {
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
    }

    public abstract void log(Supplier<String> msg);

    public abstract void log(String msg);

    private static String startTime() {
        return INIT_TIME.getYear() + "-"
                + INIT_TIME.getMonthValue() + "-"
                + INIT_TIME.getDayOfMonth() + "-" + INIT_TIME.getHour()
                + "." + INIT_TIME.getMinute() + "." + INIT_TIME.getSecond();

    }

    void close() {
    }

    public static void log(String s, String log) {
        get(s).log(log);
    }

    public static void log(String s, Supplier<String> log) {
        get(s).log(log);
    }

    private static final NumberFormat TWO_DIGITS = new DecimalFormat("00");
    private static final NumberFormat THREE_DIGITS = new DecimalFormat("000");
    private static final NumberFormat MANY_DIGITS = new DecimalFormat("######00");

    // borrowed from Mastfrog TimeUtil
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
