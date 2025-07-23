package org.a_hahn.rpi;

import org.a_hahn.rpi.tools.CpuSerialReader;
import org.a_hahn.rpi.tools.HttpClientPost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

public class SequentDINExample {

    public static void main(String[] args) throws Exception {

        final Logger log = LoggerFactory.getLogger(SequentDINExample.class);

        final String baseUrl = "https://zmx.oops.de/VRServer/api/v1/device/din";
        //final String baseUrl = "http://192.168.1.125:8080/VRServer/api/v1/device/din";
        int[] i2cAddresses = new int[]{0x27, 0x26, 0x25, 0x24, 0x23, 0x22, 0x21, 0x20};
        final int DEFAULT_I2C_BUS = 1;

        Set<Integer> lastContacts = new TreeSet<>();
        int loopCnt = 10;

        final String deviceId = CpuSerialReader.getCpuSerial();
        log.info("Device Id: " + deviceId);

        try {
            ArrayList<SequentDINBoardPi4J> boards = new ArrayList<>();
            for (Integer i : i2cAddresses) {
                try {
                    boards.add(new SequentDINBoardPi4J(DEFAULT_I2C_BUS, i));
                    log.info("Board " + i + " found");
                } catch (Exception e) {
                    // log.info(Board " + i + " not found");
                }
            }

            // Continuous monitoring
            log.info("\nMonitoring channels (press Ctrl+C to stop):");
            while (true) {
                Set<Integer> activeContacts = new TreeSet<>();
                boards.forEach(brd -> activeContacts.addAll(brd.getActive()));

                // Find added elements (in newSet but not in oldSet)
                Set<Integer> added = new TreeSet<>(activeContacts);
                added.removeAll(lastContacts);
                if (!added.isEmpty()) {
                    log.info("Off->On inputs: " + added);
                    HttpClientPost.sendPostRequest(baseUrl, deviceId, false, getArray(added));
                }

                // Find removed elements (in oldSet but not in newSet)
                Set<Integer> removed = new TreeSet<>(lastContacts);
                removed.removeAll(activeContacts);
                if (!removed.isEmpty()) {
                    log.info("On->Off: " + removed);
                    HttpClientPost.sendPostRequest(baseUrl, deviceId, true, getArray(removed));
                }

                lastContacts = activeContacts;

                if (loopCnt-- < 0) {
                    loopCnt = 10;
                    log.info("Active inputs: " + activeContacts);
                    HttpClientPost.sendPostRequest(baseUrl, deviceId, false, getArray(activeContacts));
                }

                Thread.sleep(500); // Update every 500ms
            }

        } catch (Exception e) {
            log.error("Error: ",e);
        }

    }

    private static int[] getArray(Set<Integer> activeContacts) {
        return activeContacts.stream().mapToInt(Integer::intValue).toArray();
    }

}