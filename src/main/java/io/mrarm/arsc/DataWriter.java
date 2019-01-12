package io.mrarm.arsc;

import java.io.IOException;
import java.io.OutputStream;

public class DataWriter {

    private OutputStream output;

    DataWriter(OutputStream output) {
        this.output = output;
    }

    public void write(byte[] data) throws IOException {
        output.write(data);
    }

    public void writeByte(int v) throws IOException {
        output.write(v & 0xff);
    }

    public void writeShort(int v) throws IOException {
        output.write(v & 0xff);
        output.write((v >>> 8) & 0xff);
    }

    public void writeInt(int v) throws IOException {
        output.write(v & 0xff);
        output.write((v >>> 8) & 0xff);
        output.write((v >>> 16) & 0xff);
        output.write((v >>> 24) & 0xff);
    }


}
