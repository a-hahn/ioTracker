package org.a_hahn.rpi;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.i2c.I2C;
import com.pi4j.io.i2c.I2CConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SequentDINBoardPi4J {

    private Context pi4j;
    private I2C i2c;
    private int offset;
    private ArrayList<Boolean> channelState ;

    final Logger log = LoggerFactory.getLogger(SequentDINBoardPi4J.class);

    public SequentDINBoardPi4J(int bus, int i2cAddress) throws Exception {
        //int[] i2cAddresses = new int[]{0x27, 0x26, 0x25, 0x24, 0x23, 0x22, 0x21, 0x20};
        offset = -(i2cAddress - 0x27);  // 0-7
        pi4j = Pi4J.newAutoContext();

        I2CConfig config = I2C.newConfigBuilder(pi4j)
                .id("sequent-din16-" + offset + " [" + i2cAddress + "]")
                .bus(bus)
                .device(i2cAddress)
                .build();

        i2c = pi4j.create(config);
        int res = i2c.read(); // check availability
        if (res >= 0) {
            log.info("DIN Board " + offset + " [" + i2cAddress + "] initialized");
        } else {
            log.error("DIN Board " + offset + " [" + i2cAddress + "]. Could not initialize board");
        }
    }

    // Read all 16 channels (2 bytes)
    public int updateChannelState() {
        ArrayList<Boolean> allChannels = new ArrayList<Boolean>(16);
        byte[] buffer = new byte[2];
        int readCount = 0;

        readCount = i2c.readRegister(0x00, buffer, 0, 2); // Register 0x00 for all channels

        // Combine bytes to get 16-bit value
        int bitAllChannels = ((buffer[1] & 0xFF) << 8) | (buffer[0] & 0xFF);
        for (int i = 0; i <= 15; i++) {
            allChannels.add((bitAllChannels & (1 << (15 - i))) != 0);
        }
        channelState = allChannels;
        if (readCount >= 0) {
            log.info("DIN Board " + offset + " updated " + readCount + " contacts");
        } else {
            log.error("DIN Board " + offset + ". Could not update contacts");
        }
        return readCount;
    }

    // Contact range 1-16 as printed on physical board !
    public ArrayList<Integer> getActive(boolean isActive) {
        return IntStream.range(0, channelState.size())
                .filter(i -> channelState.get(i) ^ isActive)
                .map(this::channelToContact)
                .boxed()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    // Transform channel (0..15) to contact (1..128)
    private Integer channelToContact(int channel) {
        return channel + 1 + 16 * offset;
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
