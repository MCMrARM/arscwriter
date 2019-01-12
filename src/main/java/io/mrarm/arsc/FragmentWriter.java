package io.mrarm.arsc;

import java.io.IOException;

public interface FragmentWriter {

    void prepare(DataWritePreparer preparer);

    void write(DataWriter writer) throws IOException;

    int getTotalSize();

}
