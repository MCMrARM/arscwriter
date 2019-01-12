package io.mrarm.arsc.chunks;

import java.io.IOException;

import io.mrarm.arsc.DataWriter;
import io.mrarm.arsc.BaseFragmentWriter;

public abstract class ResChunk {

    public static final int TYPE_NULL = 0;
    public static final int TYPE_STRING_POOL = 1;
    public static final int TYPE_TABLE = 2;
    public static final int TYPE_XML = 3;

    public static final int TYPE_TABLE_PACKAGE = 0x200;
    public static final int TYPE_TABLE_TYPE = 0x201;
    public static final int TYPE_TABLE_TYPE_SPEC = 0x202;


    public abstract int getType();


    public static class Writer<T extends ResChunk> extends BaseFragmentWriter {

        protected T chunk;

        public Writer(T chunk) {
            this.chunk = chunk;
        }

        public void writeHeader(DataWriter writer) throws IOException {
            writer.writeShort(chunk.getType());
            writer.writeShort(getHeaderSize());
            writer.writeInt(getTotalSize());
        }

        public void writeBody(DataWriter writer) throws IOException {
        }

        @Override
        public final void write(DataWriter writer) throws IOException {
            writeHeader(writer);
            writeBody(writer);
        }


        public int getHeaderSize() {
            return 8;
        }

        public int calculateBodySize() {
            return 0;
        }

        @Override
        public int calculateTotalSize() {
            return getHeaderSize() + calculateBodySize();
        }

    }

    public interface RootChunk {

        Writer createWriter();

    }

}
