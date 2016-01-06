package nidefawl.qubes.server.compress;

public interface ICompressTask {

    public int fill(byte[] tmpBuffer);

    public void finish(byte[] compressed);

    public boolean isValid();

}
