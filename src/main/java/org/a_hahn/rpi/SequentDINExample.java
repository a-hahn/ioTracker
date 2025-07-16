package org.a_hahn.rpi;

import org.a_hahn.rpi.tools.CpuSerialReader;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SequentDINExample {

    public static void main(String[] args) throws Exception {

        int[] i2cAddresses = new int[] {0x27, 0x26, 0x25, 0x24, 0x23, 0x22, 0x21, 0x20};
        final int DEFAULT_I2C_BUS = 1;

        Set<Integer> lastContacts = new TreeSet<>();
        int loopCnt = 10;

        try {

            System.out.println("Device Id: " + CpuSerialReader.getCpuSerial());

            ArrayList<SequentDINBoardPi4J> boards = new ArrayList<>();
            for (Integer i : i2cAddresses) {
                try {
                    boards.add(new SequentDINBoardPi4J(DEFAULT_I2C_BUS, i));
                    System.out.println("Board " + i + " found");
                } catch (Exception e) {
                    // System.out.println("Board " + i + " not found");
                }
            }

            // Continuous monitoring
            System.out.println("\nMonitoring channels (press Ctrl+C to stop):");
            while (true) {
                Set<Integer> activeContacts = new TreeSet<>();
                boards.forEach(brd -> activeContacts.addAll(brd.getActive()));

                // Find added elements (in newSet but not in oldSet)
                Set<Integer> added = new TreeSet<>(activeContacts);
                added.removeAll(lastContacts);
                if (! added.isEmpty()) {
                    System.out.println("Off->On inputs: " + added);
                }

                // Find removed elements (in oldSet but not in newSet)
                Set<Integer> removed = new TreeSet<>(lastContacts);
                removed.removeAll(activeContacts);
                if (! removed.isEmpty()) {
                    System.out.println("On->Off: " + removed);
                }

                lastContacts = activeContacts;

                if (loopCnt-- < 0) {
                    loopCnt = 10;
                    System.out.println("Active inputs: " + activeContacts);
                }

                Thread.sleep(500); // Update every 500ms
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}