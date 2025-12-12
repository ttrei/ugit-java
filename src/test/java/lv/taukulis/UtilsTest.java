package lv.taukulis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UtilsTest {

    @Test
    void testBytesToHex() {
        byte[] bytes = new byte[]{0x00, 0x01, 0x0A, 0x0F, 0x10, (byte) 0xFF};
        String expected = "00010a0f10ff";
        assertEquals(expected, Utils.bytesToHex(bytes));
    }

    @Test
    void testBytesToHexEmpty() {
        byte[] bytes = new byte[]{};
        assertEquals("", Utils.bytesToHex(bytes));
    }

    @Test
    void testBytesToHexSingleByte() {
        byte[] bytes = new byte[]{0x7F};
        assertEquals("7f", Utils.bytesToHex(bytes));
    }
}
