package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.OutputStream;

public class BitOutputStream {

    private final OutputStream byteStream;
    private int wipByte;
    private int boolIndex;

    public BitOutputStream(OutputStream byteStream) {
        this.byteStream = byteStream;
    }

    public void write(boolean value) throws IOException {
        if (value) wipByte |= 1 << boolIndex;
        if (++boolIndex == 8) {
            boolIndex = 0;
            byteStream.write(wipByte);
            wipByte = 0;
        }
    }

    public void finish() throws IOException {
        if (boolIndex > 0) byteStream.write(wipByte);
        byteStream.flush();
        byteStream.close();
    }
}
