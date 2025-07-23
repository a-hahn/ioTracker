package org.a_hahn.rpi.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CpuSerialReader {
    public static String getCpuSerial() {
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/cpuinfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("Serial")) {
                    String[] parts = line.split(":");
                    if (parts.length > 1) {
                        return parts[1].trim();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}