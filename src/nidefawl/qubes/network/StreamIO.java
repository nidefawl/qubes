/**
 * 
 */
package nidefawl.qubes.network;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface StreamIO {

    public void read(DataInput in) throws IOException;
    public void write(DataOutput out) throws IOException;
}
