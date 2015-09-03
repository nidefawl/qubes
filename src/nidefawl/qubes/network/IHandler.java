package nidefawl.qubes.network;

import nidefawl.qubes.network.packet.*;

public interface IHandler {

    public boolean isServerSide();

    public void handleHandshake(PacketHandshake packetHandshake);

    public void update();

    public void handlePing(PacketPing p);

	public String getHandlerName();

	public void handleDisconnect(PacketDisconnect packetDisconnect);

	/**
	 * Called post disconnect and post cleanup
	 */
    public void onFinish();
}
