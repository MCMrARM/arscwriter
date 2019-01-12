package io.mrarm.arsc;

import java.io.IOException;
import java.io.OutputStream;

import io.mrarm.arsc.chunks.ResChunk;

public class ArscWriter {

    private ResChunk.RootChunk rootChunk;

    public ArscWriter(ResChunk.RootChunk root) {
        rootChunk = root;
    }

    public void write(OutputStream outputStream) throws IOException {
        ResChunk.Writer rootChunkWriter = rootChunk.createWriter();
        DataWritePreparer preparer = new DataWritePreparer();
        rootChunkWriter.prepare(preparer);
        DataWriter writer = new DataWriter(outputStream);
        rootChunkWriter.write(writer);
    }

}
