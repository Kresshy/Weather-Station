package com.kresshy.weatherstation.bluetooth;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for Bluetooth frame synchronization logic. This tests the logic extracted from
 * BluetoothConnection.ConnectedRunnable.
 */
public class BluetoothFrameSyncTest {

    private String endMarker = "_end";

    @Test
    public void frameSync_WithLeadingJunk_ExtractsCorrectFrames() {
        StringBuilder curMsg = new StringBuilder();
        List<String> receivedFrames = new ArrayList<>();

        // Simulate incoming data with junk and multiple frames
        String incomingData = "31\n30\nstart_5.5 22.2_endWS_{\"temp\":25}_endjunk";
        curMsg.append(incomingData);

        processBuffer(curMsg, receivedFrames);

        assertEquals(2, receivedFrames.size());
        assertEquals("start_5.5 22.2_end", receivedFrames.get(0));
        assertEquals("WS_{\"temp\":25}_end", receivedFrames.get(1));
        assertEquals("junk", curMsg.toString());
    }

    @Test
    public void frameSync_PartialFrames_WaitUntilComplete() {
        StringBuilder curMsg = new StringBuilder();
        List<String> receivedFrames = new ArrayList<>();

        curMsg.append("junk_start_1.0");
        processBuffer(curMsg, receivedFrames);
        assertEquals(0, receivedFrames.size());

        curMsg.append(" 20.0_en");
        processBuffer(curMsg, receivedFrames);
        assertEquals(0, receivedFrames.size());

        curMsg.append("d");
        processBuffer(curMsg, receivedFrames);
        assertEquals(1, receivedFrames.size());
        assertEquals("start_1.0 20.0_end", receivedFrames.get(0));
        assertEquals("", curMsg.toString());
    }

    /** Logic extracted from BluetoothConnection.ConnectedRunnable.run() */
    private void processBuffer(StringBuilder curMsg, List<String> output) {
        int endIdx = curMsg.indexOf(endMarker);
        while (endIdx != -1) {
            int startWS = curMsg.lastIndexOf("WS_", endIdx);
            int startLegacy = curMsg.lastIndexOf("start_", endIdx);
            int startIdx = Math.max(startWS, startLegacy);

            if (startIdx != -1) {
                String fullMessage = curMsg.substring(startIdx, endIdx + endMarker.length());
                output.add(fullMessage);
                curMsg.delete(0, endIdx + endMarker.length());
            } else {
                curMsg.delete(0, endIdx + endMarker.length());
            }
            endIdx = curMsg.indexOf(endMarker);
        }
    }
}
