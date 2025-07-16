package org.a_hahn.rpi;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;

import java.util.ArrayList;

public class SequentDINBoardPi4J {

    private Context pi4j;
    private I2C i2c;
    private int offset;

    public SequentDINBoardPi4J(int bus, int i2cAddress) throws Exception {
        offset = -(i2cAddress - 0x27);
        pi4j = Pi4J.newAutoContext();

        I2CConfig config = I2C.newConfigBuilder(pi4j)
                .id("sequent-din16-" + i2cAddress)
                .bus(bus)
                .device(i2cAddress)
                .build();

        i2c = pi4j.create(config);
        i2c.read(); // check availability
    }

    // Read all 16 channels (2 bytes)
    private int readAllChannels() throws Exception {
        byte[] buffer = new byte[2];
        i2c.readRegister(0x00, buffer, 0, 2); // Register 0x00 for all channels

        // Combine bytes to get 16-bit value
        return ((buffer[1] & 0xFF) << 8) | (buffer[0] & 0xFF);
    }

    // Read specific channel (0-15)
    private boolean readChannel(int channel) throws Exception {
        if (channel < 0 || channel > 15) {
            throw new IllegalArgumentException("Channel must be 0-15");
        }

        int allChannels = readAllChannels();
        return (allChannels & (1 << (15 - channel))) != 0;
    }

    // Contact range 1-16 as printed on physical board !
    public ArrayList<Integer> getActive() {
        ArrayList<Integer> res = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            try {
                if (! readChannel(i)) {
                    res.add(i + 1 + 16 * offset);
                }
            } catch (Exception e) {
            }
        }
        return res;
    }

    public void close() {
        if (i2c != null) {
            i2c.close();
        }
        if (pi4j != null) {
            pi4j.shutdown();
        }
    }
}
