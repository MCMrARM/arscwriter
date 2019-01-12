package io.mrarm.arsc;

public abstract class BaseFragmentWriter implements FragmentWriter {

    private int cachedTotalSize = -1;

    @Override
    public void prepare(DataWritePreparer preparer) {
        // Stub as many classes don't need to implement this
    }

    public abstract int calculateTotalSize();

    private void invalidateTotalSize() {
        cachedTotalSize = -1;
    }

    @Override
    public int getTotalSize() {
        if (cachedTotalSize == -1) {
            cachedTotalSize = calculateTotalSize();
            if (cachedTotalSize % 4 != 0)
                System.out.println("error: invalid total size: " + this);
        }
        return cachedTotalSize;
    }
}
