package io.mrarm.arsc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import io.mrarm.arsc.chunks.ResStringPool;

public class StringPoolBuilder {

    private static final Charset UTF8Charset = Charset.forName("UTF-8");
    private static final Charset UTF16Charset = Charset.forName("UTF-16LE");

    private final ByteArrayOutputStream data = new ByteArrayOutputStream();
    private final Charset charset;
    private final boolean isUtf8;

    private final List<Integer> offsets = new ArrayList<>();

    public StringPoolBuilder(boolean utf8) {
        isUtf8 = utf8;
        if (utf8)
            charset = UTF8Charset;
        else
            charset = UTF16Charset;
    }

    private void appendLengthUTF8(int length) {
        if (length >= 0x80) {
            data.write(((length >> 8) & 0x7F) | 0x80);
            data.write(length & 0xFF);
        } else {
            data.write(length);
        }
    }

    private void appendLengthUTF16(int length) {
        length /= 2;
        if (length >= 0x8000) {
            int lo = length & 0xFFFF;
            int hi = ((length >> 16) & 0x7FFF) | 0x8000;
            data.write(hi & 0xFF);
            data.write((hi >> 8) & 0xFF);
            data.write(lo & 0xFF);
            data.write((lo >> 8) & 0xFF);
        } else {
            data.write(length & 0xFF);
            data.write((length >> 8) & 0xFF);
        }
    }

    public int appendString(String str) {
        int ret = offsets.size();
        offsets.add(data.size());
        byte[] val = str.getBytes(charset);
        try {
            if (isUtf8) {
                appendLengthUTF8(str.getBytes(UTF16Charset).length / 2);
                appendLengthUTF8(val.length);
            } else {
                appendLengthUTF16(val.length);
            }
            data.write(val);
            data.write(0);
            if (!isUtf8)
                data.write(0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ret;
    }

    public ResStringPool build() {
        ResStringPool res = new ResStringPool();
        res.stringOffsets = offsets;
        res.stringData = data.toByteArray();
        if (isUtf8)
            res.flags |= ResStringPool.FLAG_UTF8;
        return res;
    }

}
