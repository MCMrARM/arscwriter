package io.mrarm.arsc;

public class DataWritePreparer {

    private StringPoolBuilder globalStringPool;

    public void setGlobalStringPool(StringPoolBuilder pool) {
        this.globalStringPool = pool;
    }

    public int appendGlobalString(String text) {
        return globalStringPool.appendString(text);
    }

}
