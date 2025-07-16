package org.a_hahn.rpi2;

import com.pi4j.Pi4J;
import com.pi4j.io.i2c.I2C;

public class SequentDigitalReader {
    public static void main(String[] args) {
        var pi4j = Pi4J.newAutoContext();
        var config = I2C.newConfigBuilder(pi4j)
                .id("sequent-inputs")
                .bus(1)  // Use bus 1 (for Raspberry Pi)
                .device(0x27)
                .build();

        try (var i2c = pi4j.create(config)) {
            i2c.write(0x00);  // Your command byte
            byte[] data = new byte[2];
            i2c.read(data, 0, 2);
            System.out.printf("Data: 0x%02X 0x%02X\n", data[1], data[0]);
        } finally {
            pi4j.shutdown();
        }
    }
}