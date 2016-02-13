package nidefawl.qubes.server.compress;

import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;

@SideOnly(value = Side.SERVER)
public interface ICompressTask {

    public int fill(byte[] tmpBuffer);

    public void finish(byte[] compressed);

    public boolean isValid();

}
