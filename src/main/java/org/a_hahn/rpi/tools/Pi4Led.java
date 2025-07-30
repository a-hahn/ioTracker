package org.a_hahn.rpi.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Pi4Led {
    private final Logger log = LoggerFactory.getLogger(Pi4Led.class);

    private static final Path LED0_BRIGHTNESS = Paths.get("/sys/class/leds/led0/brightness");
    private static final Path LED0_TRIGGER = Paths.get("/sys/class/leds/led0/trigger");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Future<?> currentTask;
    private volatile boolean running;

    private boolean initialized = false;

    public enum LedMode {ON, OFF, SHORT, FAST, PANIC};
    private volatile LedMode ledMode = LedMode.OFF;

    public Pi4Led() {
        initialize();
    }

    private void initialize() {
        try {
            // Check if LED paths exist
            if (!Files.exists(LED0_BRIGHTNESS)) {
                log.error("LED brightness control not found at: " + LED0_BRIGHTNESS);
                return;
            }

            // Check if we can write to the brightness file
            if (!Files.isWritable(LED0_BRIGHTNESS)) {
                log.error("No write permission to LED brightness file.");
                log.error("Solutions:");
                log.error("1. Run with: sudo java " + this.getClass().getSimpleName());
                log.error("2. Set permissions: sudo chmod 666 /sys/class/leds/led0/brightness");
                log.error("3. Create udev rule for permanent solution");
                return;
            }

            // Try to set trigger to 'none' to enable manual control
            if (Files.exists(LED0_TRIGGER)) {
                try {
                    Files.write(LED0_TRIGGER, "none".getBytes(),
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING);
                } catch (IOException e) {
                    log.error("Could not set LED trigger (may need sudo): ");
                    log.error("Set proper access permissions (+w) for " + LED0_TRIGGER);
                }
            }

            initialized = true;
            log.info("LED controller initialized successfully");

        } catch (Exception e) {
            log.error("Failed to initialize LED controller: " + e.getMessage());
        }
    }

    public void setMode(LedMode mode) {
        if (!initialized) {
            log.error("LED controller not initialized");
            return;
        }
        this.ledMode = mode;
        log.info("Set LED0 " + mode);
        restartPattern();
    }

    private void restartPattern() {
        stopPattern();

        if (ledMode == LedMode.OFF) {
            setLED(false);
            return;
        }

        running = true;
        currentTask = executor.submit(this::runPattern);
    }

    private void stopPattern() {
        running = false;
        if (currentTask != null) {
            currentTask.cancel(true);
        }
    }

    public void shutdown() {
        stopPattern();
        executor.shutdown();
    }

    private void runPattern() {
        while(true){
            switch (ledMode) {
                case ON:
                    setLED(true);
                    sleep(1000);
                    break;
                case SHORT:
                    blink(10, 100, 100);
                    break;
                case FAST:
                    blink(20, 50, 50);
                    break;
                case PANIC:
                    blink(10, 100, 100);
                    blink(5, 300, 300);
                    break;
                case OFF:
                default:
                    running = false;
            }
        }
    }

    private boolean setLED(boolean on) {
        try {
            String value = on ? "1" : "0";
            Files.write(LED0_BRIGHTNESS, value.getBytes(),
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);

            log.trace("LED0 " + (on ? "ON" : "OFF"));
            return true;

        } catch (IOException e) {
            log.error("Error controlling LED0");
            log.error("Set proper access permissions (+w) for " + LED0_BRIGHTNESS  + " and " + LED0_TRIGGER);
            return false;
        }
    }

    private void blink(int times, int onTimeMs, int offTimeMs) {
        for (int i = 0; i < times; i++) {
            if (setLED(true)) {
                sleep(onTimeMs);
                setLED(false);
                if (i < times - 1) { // Don't sleep after the last blink
                    sleep(offTimeMs);
                }
            } else {
                break; // Stop if we can't control the LED
            }
        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            log.info("Interrupted");
            //Thread.currentThread().interrupt();
        }
    }

}