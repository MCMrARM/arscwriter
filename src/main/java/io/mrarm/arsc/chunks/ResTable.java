package io.mrarm.arsc.chunks;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.mrarm.arsc.DataWritePreparer;
import io.mrarm.arsc.DataWriter;
import io.mrarm.arsc.FragmentWriter;
import io.mrarm.arsc.StringPoolBuilder;

public class ResTable extends ResChunk implements ResChunk.RootChunk {

    public static int makeReference(int pkgId, int typId, int id) {
        return (pkgId << 24) | (typId << 16) | id;
    }

    public static int makeReference(Package pkg, TypeSpec type, int id) {
        return makeReference(pkg.id, type.id, id);
    }

    public static int makeReference(Package pkg, TypeSpec type, EntryBase entry) {
        return makeReference(pkg.id, type.id, entry.id);
    }


    private List<Package> packages;

    public ResTable() {
        this.packages = new ArrayList<>();
    }

    public ResTable(List<Package> packages) {
        this.packages = packages;
    }

    public void addPackage(Package pkg) {
        this.packages.add(pkg);
    }

    @Override
    public ResChunk.Writer createWriter() {
        return new Writer(this);
    }

    @Override
    public int getType() {
        return TYPE_TABLE;
    }

    public static class Writer extends ResChunk.Writer<ResTable> {

        private List<Package.Writer> packageWriters = new ArrayList<>();
        private ResStringPool globalPoolBuilt;
        private ResStringPool.Writer globalPoolWriter;

        public Writer(ResTable chunk) {
            super(chunk);
            if (chunk.packages != null) {
                for (Package pkg : chunk.packages)
                    packageWriters.add(new Package.Writer(pkg));
            }
        }

        @Override
        public void prepare(DataWritePreparer preparer) {
            StringPoolBuilder globalPool = new StringPoolBuilder(true);
            preparer.setGlobalStringPool(globalPool);
            for (Package.Writer child : packageWriters)
                child.prepare(preparer);
            preparer.setGlobalStringPool(null);
            globalPoolBuilt = globalPool.build();
            globalPoolWriter = globalPoolBuilt.createWriter();
        }

        @Override
        public void writeHeader(DataWriter writer) throws IOException {
            super.writeHeader(writer);
            writer.writeInt(packageWriters.size());
        }

        @Override
        public int getHeaderSize() {
            return super.getHeaderSize() + 4;
        }

        @Override
        public void writeBody(DataWriter writer) throws IOException {
            globalPoolWriter.write(writer);
            for (Package.Writer child : packageWriters)
                child.write(writer);
        }

        @Override
        public int calculateBodySize() {
            int ret = globalPoolWriter.getTotalSize();
            for (Package.Writer pkg : packageWriters)
                ret += pkg.getTotalSize();
            return ret;
        }

    }


    public static class Package extends ResChunk {

        public int id;
        public String name;
        private List<TypeBase> entries;

        public Package(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public int getType() {
            return TYPE_TABLE_PACKAGE;
        }

        public void addType(TypeBase entry) {
            if (entries == null)
                entries = new ArrayList<>();
            entries.add(entry);
        }



        public static class Writer extends ResChunk.Writer<Package> {

            private List<TypeBase.Writer> typeWriters = new ArrayList<>();
            private Map<String, Integer> publicKeysIdx;
            private Map<String, Integer> privateKeysIdx;
            private ResStringPool typesPool;
            private ResStringPool keysPool;
            private ResStringPool.Writer typesPoolWriter;
            private ResStringPool.Writer keysPoolWriter;

            public Writer(Package chunk) {
                super(chunk);
                if (chunk.entries != null) {
                    for (TypeBase entry : chunk.entries)
                        typeWriters.add(entry.createWriter(this));
                }
            }

            private void buildKeysPool() {
                StringPoolBuilder keysBuilder = new StringPoolBuilder(true);
                Set<String> privateKeys = new HashSet<>();
                Set<String> publicKeys = new HashSet<>();
                for (TypeBase.Writer t : typeWriters)
                    t.collectKeyStrings(publicKeys, privateKeys);
                publicKeysIdx = new HashMap<>();
                privateKeysIdx = new HashMap<>();
                for (String key : publicKeys)
                    publicKeysIdx.put(key, keysBuilder.appendString(key));
                for (String key : privateKeys)
                    privateKeysIdx.put(key, keysBuilder.appendString(key));
                keysPool = keysBuilder.build();
                keysPoolWriter = keysPool.createWriter();
            }

            private void buildTypesPool() {
                StringPoolBuilder typesBuilder = new StringPoolBuilder(false);
                int maxIndex = 0;
                for (TypeBase.Writer type : typeWriters) {
                    if (type.chunk instanceof TypeSpec)
                        maxIndex = ((TypeSpec) type.chunk).id;
                }
                String[] strings = new String[maxIndex];
                for (TypeBase.Writer type : typeWriters) {
                    if (type.chunk instanceof TypeSpec)
                        strings[((TypeSpec) type.chunk).id - 1] = ((TypeSpec) type.chunk).name;
                }
                for (String str : strings)
                    typesBuilder.appendString(str);
                typesPool = typesBuilder.build();
                typesPoolWriter = typesPool.createWriter();
            }

            @Override
            public void prepare(DataWritePreparer preparer) {
                buildKeysPool();
                buildTypesPool();
                typesPoolWriter.prepare(preparer);
                keysPoolWriter.prepare(preparer);
                for (TypeBase.Writer child : typeWriters)
                    child.prepare(preparer);
            }

            @Override
            public void writeHeader(DataWriter writer) throws IOException {
                super.writeHeader(writer);
                writer.writeInt(chunk.id);
                byte[] nameBytes = chunk.name.getBytes("UTF-16LE");
                if (nameBytes.length >= 128 * 2)
                    throw new IOException("name is too long");
                writer.write(nameBytes);
                writer.write(new byte[128 * 2 - nameBytes.length]);
                writer.writeInt(getHeaderSize()); // typeStrings
                writer.writeInt(publicKeysIdx.size()); // lastPublicType
                writer.writeInt(getHeaderSize() + typesPoolWriter.getTotalSize()); // keyStrings
                writer.writeInt(publicKeysIdx.size()); // lastPublicKey
            }

            @Override
            public void writeBody(DataWriter writer) throws IOException {
                typesPoolWriter.write(writer);
                keysPoolWriter.write(writer);
                for (TypeBase.Writer child : typeWriters)
                    child.write(writer);
            }


            @Override
            public int getHeaderSize() {
                return super.getHeaderSize() + 4 + 128 * 2 + 4 * 4;
            }

            @Override
            public int calculateBodySize() {
                int ret = typesPoolWriter.getTotalSize() + keysPoolWriter.getTotalSize();
                for (TypeBase.Writer child : typeWriters)
                    ret += child.getTotalSize();
                return ret;
            }
        }

    }

    public static class Config {

        public static final String STR_ANY = "\0\0";

        public int imsi;
        public String language = STR_ANY;
        public String country = STR_ANY;
        public int screenType = 0;
        private int input = 0;
        private int screenSize = 0;
        private short sdkVersion = 0;
        private short minorVersion = 0;
        private int screenConfig = 0;
        private int screenSizeDp = 0;

        public void write(DataWriter writer) throws IOException {
            writer.writeInt(getSize());
            writer.writeInt(imsi);
            writer.writeByte(language.charAt(0));
            writer.writeByte(language.charAt(1));
            writer.writeByte(country.charAt(0));
            writer.writeByte(country.charAt(1));
            writer.writeInt(screenType);
            writer.writeInt(input);
            writer.writeInt(screenSize);
            writer.writeShort(sdkVersion);
            writer.writeShort(minorVersion);
            writer.writeInt(screenConfig);
            writer.writeInt(screenSizeDp);
        }

        public int getSize() {
            return 4 * 9;
        }

    }

    public abstract static class TypeBase extends ResChunk {

        public abstract Writer createWriter(Package.Writer packageWriter);

        public abstract static class Writer<T extends TypeBase> extends ResChunk.Writer<T> {

            public Writer(T chunk) {
                super(chunk);
            }

            public void collectKeyStrings(Set<String> publicKeys, Set<String> privateKeys) {
            }

        }

    }

    public static class TypeSpec extends TypeBase {

        public static final int SPEC_PUBLIC = 0x40000000;

        public int id;
        public String name;
        public int[] flags;

        public TypeSpec(int id, String name, int[] flags) {
            this.id = id;
            this.name = name;
            this.flags = flags;
        }


        @Override
        public TypeBase.Writer createWriter(Package.Writer packageWriter) {
            return new Writer(this);
        }

        public static class Writer extends TypeBase.Writer<TypeSpec> {

            public Writer(TypeSpec chunk) {
                super(chunk);
            }


            @Override
            public void writeHeader(DataWriter writer) throws IOException {
                super.writeHeader(writer);
                writer.writeByte(chunk.id);
                writer.writeByte(0);
                writer.writeShort(0);
                writer.writeInt(chunk.flags.length);
            }

            @Override
            public int getHeaderSize() {
                return super.getHeaderSize() + 8;
            }

            @Override
            public void writeBody(DataWriter writer) throws IOException {
                for (int flags : chunk.flags)
                    writer.writeInt(flags);
            }

            @Override
            public int calculateBodySize() {
                return chunk.flags.length * 4;
            }

        }

        @Override
        public int getType() {
            return TYPE_TABLE_TYPE_SPEC;
        }

    }

    public static class Type extends TypeBase {

        public int id;
        public Config config;
        public List<EntryBase> entries;

        public Type(int id, Config config) {
            this.id = id;
            this.config = config;
        }

        public void addEntry(EntryBase entry) {
            if (this.entries == null)
                this.entries = new ArrayList<>();
            this.entries.add(entry);
        }

        @Override
        public TypeBase.Writer createWriter(Package.Writer packageWriter) {
            return new Writer(this, packageWriter);
        }

        public static class Writer extends TypeBase.Writer<Type> {

            private List<EntryBase.Writer> entryWriters = new ArrayList<>();
            private int maxEntryId = -1;

            public Writer(Type chunk, Package.Writer packageWriter) {
                super(chunk);
                if (chunk.entries != null) {
                    for (EntryBase entry : chunk.entries)
                        entryWriters.add(entry.createWriter(packageWriter));
                }
            }

            @Override
            public void collectKeyStrings(Set<String> publicKeys, Set<String> privateKeys) {
                for (EntryBase.Writer child : entryWriters) {
                    if ((child.getEntry().flags & Entry.FLAG_PUBLIC) != 0)
                        publicKeys.add(child.getEntry().key);
                    else
                        privateKeys.add(child.getEntry().key);
                }
            }

            private void updateMaxEntryId() {
                maxEntryId = -1;
                for (EntryBase.Writer child : entryWriters) {
                    int eid = child.getEntry().id;
                    if (eid > maxEntryId)
                        maxEntryId = eid;
                }
            }

            @Override
            public void prepare(DataWritePreparer preparer) {
                super.prepare(preparer);
                updateMaxEntryId();
                for (EntryBase.Writer child : entryWriters)
                    child.prepare(preparer);
            }

            @Override
            public void writeHeader(DataWriter writer) throws IOException {
                super.writeHeader(writer);
                writer.writeByte(chunk.id);
                writer.writeByte(0);
                writer.writeShort(0);
                writer.writeInt(maxEntryId + 1);
                writer.writeInt(getHeaderSize() + (maxEntryId + 1) * 4);
                chunk.config.write(writer);
            }

            @Override
            public int getHeaderSize() {
                return super.getHeaderSize() + 12 + chunk.config.getSize();
            }

            @Override
            public void writeBody(DataWriter writer) throws IOException {
                EntryBase.Writer[] entriesById = new EntryBase.Writer[maxEntryId + 1];
                for (EntryBase.Writer entry : entryWriters)
                    entriesById[entry.getEntry().id] = entry;
                int off = 0;
                for (int i = 0; i <= maxEntryId; i++) {
                    if (entriesById[i] != null) {
                        writer.writeInt(off);
                        off += entriesById[i].getTotalSize();
                    } else {
                        writer.writeInt(-1);
                    }
                }
                for (EntryBase.Writer entry : entryWriters)
                    entry.write(writer);
            }

            @Override
            public int calculateBodySize() {
                int ret = (maxEntryId + 1) * 4;
                for (EntryBase.Writer entry : entryWriters)
                    ret += entry.getTotalSize();
                return ret;
            }

        }

        @Override
        public int getType() {
            return TYPE_TABLE_TYPE;
        }

    }

    public abstract static class EntryBase {

        public static final int FLAG_COMPLEX = 1;
        public static final int FLAG_PUBLIC = 2;

        public int id;
        public short flags;
        public String key;

        private EntryBase(int id, String key, boolean isPublic) {
            this.id = id;
            this.key = key;
            if (isPublic)
                flags |= FLAG_PUBLIC;
        }

        private EntryBase(int id, String key) {
            this(id, key, false);
        }

        public abstract Writer createWriter(Package.Writer packageWriter);


        public static class Writer<T extends EntryBase> implements FragmentWriter {

            protected T entry;
            private Package.Writer packageWriter;

            public Writer(T entry, Package.Writer packageWriter) {
                this.entry = entry;
                this.packageWriter = packageWriter;
            }

            public T getEntry() {
                return entry;
            }

            public void prepare(DataWritePreparer writer) {
            }

            public void write(DataWriter writer) throws IOException {
                writer.writeShort(getHeaderSize());
                writer.writeShort(entry.flags);
                Map<String, Integer> keyTable = (entry.flags & FLAG_PUBLIC) != 0
                        ? packageWriter.publicKeysIdx : packageWriter.privateKeysIdx;
                //noinspection ConstantConditions
                writer.writeInt(keyTable.get(entry.key));
            }

            public int getHeaderSize() {
                return 8;
            }

            @Override
            public int getTotalSize() {
                return getHeaderSize();
            }

        }

    }

    public static class Entry extends EntryBase {

        private ResValue value;

        public Entry(int id, String key, ResValue value, boolean isPublic) {
            super(id, key, isPublic);
            this.value = value;
        }

        public Entry(int id, String key, ResValue value) {
            this(id, key, value, false);
        }

        @Override
        public Writer createWriter(Package.Writer packageWriter) {
            return new Writer(this, packageWriter);
        }

        public static class Writer extends EntryBase.Writer<Entry> {

            private ResValue.Writer valueWriter;

            public Writer(Entry entry, Package.Writer packageWriter) {
                super(entry, packageWriter);
                valueWriter = entry.value.createWriter();
            }

            @Override
            public void prepare(DataWritePreparer writer) {
                valueWriter.prepare(writer);
            }

            @Override
            public void write(DataWriter writer) throws IOException {
                super.write(writer);
                valueWriter.write(writer);
            }

            @Override
            public int getTotalSize() {
                return super.getTotalSize() + valueWriter.getTotalSize();
            }
        }

    }


    public static class MapEntry extends EntryBase {

        public int parent;
        public List<Entry> value;

        public MapEntry(int id, String key, List<Entry> value, boolean isPublic) {
            super(id, key, isPublic);
            this.value = value;
            this.flags |= FLAG_COMPLEX;
        }

        public MapEntry(int id, String key, List<Entry> value) {
            this(id, key, value, false);
        }

        public MapEntry(int id, String key) {
            this(id, key, new ArrayList<Entry>(), false);
        }

        public void setParent(int parent) {
            this.parent = parent;
        }

        public Entry addValue(int name, ResValue value) {
            Entry e = new Entry(name, value);
            this.value.add(e);
            return e;
        }

        @Override
        public Writer createWriter(Package.Writer packageWriter) {
            return new Writer(this, packageWriter);
        }

        public static class Writer extends EntryBase.Writer<MapEntry> {

            private ResValue.Writer[] valueWriters;

            public Writer(MapEntry entry, Package.Writer packageWriter) {
                super(entry, packageWriter);
                valueWriters = new ResValue.Writer[entry.value.size()];
                for (int i = entry.value.size() - 1; i >= 0; --i)
                    valueWriters[i] = entry.value.get(i).value.createWriter();
            }

            @Override
            public void prepare(DataWritePreparer writer) {
                for (ResValue.Writer valueWriter : valueWriters)
                    valueWriter.prepare(writer);
            }

            @Override
            public void write(DataWriter writer) throws IOException {
                super.write(writer);
                writer.writeInt(entry.parent);
                int n = entry.value.size();
                writer.writeInt(n);
                // write body
                for (int i = 0; i < n; i++) {
                    writer.writeInt(entry.value.get(i).name);
                    valueWriters[i].write(writer);
                }
            }

            @Override
            public int getHeaderSize() {
                return super.getHeaderSize() + 8;
            }

            @Override
            public int getTotalSize() {
                int res = getHeaderSize();
                int n = entry.value.size();
                for (int i = 0; i < n; i++) {
                    res += 4;
                    res += valueWriters[i].getTotalSize();
                }
                return res;
            }
        }

        public static class Entry {

            public int name;
            public ResValue value;

            public Entry(int name, ResValue value) {
                this.name = name;
                this.value = value;
            }

        }

    }




}
