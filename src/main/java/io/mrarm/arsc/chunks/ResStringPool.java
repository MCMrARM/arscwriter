package io.mrarm.arsc.chunks;

import java.io.IOException;
import java.util.List;

import io.mrarm.arsc.DataWriter;

public class ResStringPool extends ResChunk {

    public static final int FLAG_SORTED = 1;
    public static final int FLAG_UTF8 = 256;

    public List<Integer> stringOffsets;
    public byte[] stringData;
    public int flags;

    @Override
    public int getType() {
        return TYPE_STRING_POOL;
    }

    public Writer createWriter() {
        return new Writer(this);
    }


    public static class Writer extends ResChunk.Writer<ResStringPool> {

        public Writer(ResStringPool chunk) {
            super(chunk);
        }


        @Override
        public void writeHeader(DataWriter writer) throws IOException {
            super.writeHeader(writer);
            writer.writeInt(chunk.stringOffsets.size());
            writer.writeInt(0); // styleCount
            writer.writeInt(chunk.flags);
            writer.writeInt(getHeaderSize() + chunk.stringOffsets.size() * 4); // stringsStart
            writer.writeInt(0); // stylesStart
        }

        @Override
        public int getHeaderSize() {
            return super.getHeaderSize() + 4 * 5;
        }

        @Override
        public void writeBody(DataWriter writer) throws IOException {
            for (int offset : chunk.stringOffsets)
                writer.writeInt(offset);
            writer.write(chunk.stringData);
            // pad to 4 bytes
            if ((chunk.stringData.length % 4) != 0) {
                for (int i = 4 - (chunk.stringData.length % 4); i > 0; --i)
                    writer.writeByte(0);
            }
        }

        @Override
        public int calculateBodySize() {
            return chunk.stringOffsets.size() * 4 + (chunk.stringData.length + 3) / 4 * 4;
        }
    }

}
