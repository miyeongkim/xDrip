package com.eveningoutpost.dexdrip.insulin.diaconnp8.packet;

import java.nio.ByteBuffer;

public class TimeInquirePacket extends DiaconnP8Packet {

    static byte MSG_TYPE = 0x4C;

    public byte[] encode(int msgSeq) {
        ByteBuffer buffer = prefixEncode(MSG_TYPE, msgSeq, MSG_CON_END);
        return suffixEncode(buffer);
    }
}
