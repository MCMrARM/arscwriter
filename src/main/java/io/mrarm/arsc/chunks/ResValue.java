package io.mrarm.arsc.chunks;

import java.io.IOException;

import io.mrarm.arsc.DataWritePreparer;
import io.mrarm.arsc.DataWriter;
import io.mrarm.arsc.FragmentWriter;

public abstract class ResValue {

    public static final byte TYPE_REFERENCE = 1;
    public static final byte TYPE_STRING = 3;
    public static final byte TYPE_INT_DEC = 0x10;
    public static final byte TYPE_INT_HEX = 0x11;
    public static final byte TYPE_INT_BOOLEAN = 0x12;
    public static final byte TYPE_INT_COLOR_ARGB8 = 0x1c;


    public abstract Writer createWriter();

    public abstract static class Writer<T extends ResValue> implements FragmentWriter {

        protected T value;

        public Writer(T value) {
            this.value = value;
        }

        @Override
        public void prepare(DataWritePreparer preparer) {
        }

    }



    public static class Integer extends ResValue {

        public byte dataType;
        public int data;

        public Integer(byte dataType, int data) {
            this.dataType = dataType;
            this.data = data;
        }

        @Override
        public ResValue.Writer createWriter() {
            return new Writer(this);
        }

        public static class Writer extends ResValue.Writer<Integer> {

            public Writer(Integer value) {
                super(value);
            }

            @Override
            public void write(DataWriter writer) throws IOException {
                writer.writeShort(getTotalSize());
                writer.writeByte(0);
                writer.writeByte(value.dataType);
                writer.writeInt(value.data);
            }

            @Override
            public int getTotalSize() {
                return 8;
            }
        }

    }

    public static class Reference extends Integer {

        public Reference(int data) {
            super(TYPE_REFERENCE, data);
        }

        public Reference(int pkgId, int typId, int id) {
            this(ResTable.makeReference(pkgId, typId, id));
        }

        public Reference(ResTable.Package pkg, ResTable.TypeSpec type, int id) {
            this(ResTable.makeReference(pkg, type, id));
        }

        public Reference(ResTable.Package pkg, ResTable.TypeSpec type, ResTable.EntryBase entry) {
            this(ResTable.makeReference(pkg, type, entry));
        }

    }

    public static class Text extends ResValue {

        public String data;

        public Text(String data) {
            this.data = data;
        }

        @Override
        public ResValue.Writer createWriter() {
            return new Writer(this);
        }

        public static class Writer extends ResValue.Writer<Text> {

            private int stringId;

            public Writer(Text value) {
                super(value);
            }

            @Override
            public void prepare(DataWritePreparer preparer) {
                stringId = preparer.appendGlobalString(value.data);
            }

            @Override
            public void write(DataWriter writer) throws IOException {
                writer.writeShort(getTotalSize());
                writer.writeByte(0);
                writer.writeByte(TYPE_STRING);
                writer.writeInt(stringId);
            }

            @Override
            public int getTotalSize() {
                return 8;
            }
        }

    }


}
