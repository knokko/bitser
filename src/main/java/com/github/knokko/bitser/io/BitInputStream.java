package com.github.knokko.bitser.io;

import java.io.IOException;
import java.io.InputStream;

public class BitInputStream {

    private final InputStream byteStream;
    private int currentByte;
    private int boolIndex = 8;

    public BitInputStream(InputStream byteStream) {
        this.byteStream = byteStream;
    }

    public boolean read() throws IOException {
        if (boolIndex == 8) {
            currentByte = byteStream.read();
            if (currentByte == -1) throw new IOException("End of stream reached");
            boolIndex = 0;
        }

        return (currentByte & (1 << boolIndex++)) != 0;
    }

    public void close() throws IOException {
        byteStream.close();
    }
}
