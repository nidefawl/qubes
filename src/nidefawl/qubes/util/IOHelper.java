package nidefawl.qubes.util;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import nidefawl.qubes.network.StreamIO;

public class IOHelper {
    public static void copyTo(StreamIO input, StreamIO output) {
        try {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            input.write(out);
            ByteArrayDataInput in = ByteStreams.newDataInput(out.toByteArray());
            output.read(in);
        } catch (Exception e) {
            throw new GameError(e);
        }
    }
}
