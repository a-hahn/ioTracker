package org.a_hahn.rpi;

import com.sun.tools.javac.Main;
import org.a_hahn.rpi.tools.CpuSerialReader;
import org.a_hahn.rpi.tools.HttpClientPost;
import org.a_hahn.rpi.tools.Pi4Led;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class IoTracker {

    // Default URL if environment variable is not set
    private static final String DEFAULT_BASE_URL = "http://192.168.1.125:8080/VRServer/api/v1/device/din";
    private static final String ENV_VAR_NAME = "IOTRACKER_BASE_URL";

    public static void main(String[] args) throws Exception {
        final Logger log = LoggerFactory.getLogger(IoTracker.class);

        // Get base URL from environment variable or use default
        final String baseUrl = System.getenv(ENV_VAR_NAME) != null ?
                System.getenv(ENV_VAR_NAME) : DEFAULT_BASE_URL;

        int[] i2cAddresses = new int[]{0x27, 0x26, 0x25, 0x24, 0x23, 0x22, 0x21, 0x20};
        final int DEFAULT_I2C_BUS = 1;
        int sendStatus = 0;

        final Pi4Led pi4Led = new Pi4Led();
        pi4Led.setMode(Pi4Led.LedMode.SHORT);

        String version = IoTracker.class.getPackage().getImplementationVersion();
        log.info(IoTracker.class.getPackage().getImplementationTitle() + " " + (version != null ? version : "DEV"));

        final String deviceId = CpuSerialReader.getCpuSerial();
        log.info("Device Id: " + deviceId);
        log.info("Connecting to " + baseUrl);
        log.info("Using environment variable {}: {}", ENV_VAR_NAME, System.getenv(ENV_VAR_NAME) != null ? "set" : "not set (using default)");

        try {
            ArrayList<SequentDINBoardPi4J> boards = new ArrayList<>();
            for (Integer i : i2cAddresses) {
                try {
                    boards.add(new SequentDINBoardPi4J(DEFAULT_I2C_BUS, i));
                    log.info("Board " + i + " found");
                } catch (Exception e) {
                    // log.info("Board " + i + " not found");
                }
            }

            // Continuous monitoring
            log.info("\nMonitoring channels (press Ctrl+C to stop):");
            while (true) {
                Set<Integer> activeContacts = new TreeSet<>();
                Set<Integer> inactiveContacts = new TreeSet<>();
                boards.forEach(brd -> brd.updateChannelState());

                boards.forEach(brd -> activeContacts.addAll(brd.getActive(true)));
                log.info("On: " + activeContacts);
                sendStatus = HttpClientPost.sendPostRequest(baseUrl, deviceId, false, activeContacts);
                setLedMode(sendStatus, pi4Led);

                boards.forEach(brd -> inactiveContacts.addAll(brd.getActive(false)));
                log.info("Off: " + inactiveContacts);
                sendStatus = HttpClientPost.sendPostRequest(baseUrl, deviceId, true, inactiveContacts);
                setLedMode(sendStatus, pi4Led);

                TimeUnit.MILLISECONDS.sleep(5000);
            }

        } catch (Exception e) { // program terminating ...
            pi4Led.setMode(Pi4Led.LedMode.OFF);
            log.error("Error: ", e);
        }
    }

    private static void setLedMode(int sendStatus, Pi4Led pi4Led) {
        if (sendStatus < 100) {
            pi4Led.setMode(Pi4Led.LedMode.SHORT);
        } else if (sendStatus < 400) {
            pi4Led.setMode(Pi4Led.LedMode.ON);
        } else {
            pi4Led.setMode(Pi4Led.LedMode.OFF);
        }
    }
}