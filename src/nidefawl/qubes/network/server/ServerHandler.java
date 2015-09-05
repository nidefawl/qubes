package nidefawl.qubes.network.server;

import java.net.InetSocketAddress;

import nidefawl.qubes.chunk.Chunk;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.logging.ErrorHandler;
import nidefawl.qubes.network.Connection;
import nidefawl.qubes.network.Handler;
import nidefawl.qubes.network.packet.*;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.server.PlayerManager;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldServer;

public class ServerHandler extends Handler {

	int state = STATE_HANDSHAKE;
	public Connection conn;
	private final InetSocketAddress addr;
	private final String name;
	private long time;
	private GameServer server;
	private NetworkServer netServer;
    private Player player;

	public ServerHandler(GameServer server, NetworkServer netServer, final Connection c) {
		this.conn = c;
		this.addr = this.conn.getAddr();
		this.name = this.addr.getHostString();
		this.time = System.currentTimeMillis();
		this.server = server;
		this.netServer = netServer;
	}

	@Override
	public boolean isServerSide() {
		return true;
	}

	@Override
	public void handleHandshake(final PacketHandshake packetHandshake) {
		if (this.state != STATE_HANDSHAKE) {
			this.conn.disconnect(Connection.LOCAL, "Invalid packet");
			return;
		}
		if (packetHandshake.version != this.netServer.netVersion) {
			this.conn.disconnect(Connection.LOCAL, "Invalid version. Server is on version "+this.netServer.netVersion);
			return;
		}
		this.state = STATE_AUTH;
		sendPacket(new PacketHandshake(this.netServer.netVersion));
		this.time = System.currentTimeMillis();
	}

	@Override
	public void update() {
		if (state != STATE_CONNECTED) {
			if (System.currentTimeMillis() - time > netServer.packetTimeout) {
				this.conn.disconnect(Connection.LOCAL, "Packet timed out");
				return;
			}
		} else {
			if (System.currentTimeMillis() - time > netServer.pingDelay) {
				this.time = System.currentTimeMillis();
	            sendPacket(new PacketPing(System.nanoTime()));
			}
		}
	}

    @Override
    public void handlePing(PacketPing p) {
        
    }


	@Override
	public String getHandlerName() {
		return this.name;
	}

	@Override
	public void handleDisconnect(PacketDisconnect packetDisconnect) {
		System.out.println("client send disconnect");
		this.conn.disconnect(Connection.REMOTE, "Client sent disconnect: "+packetDisconnect.message);
	}

    @Override
    public void onFinish() {
        System.out.println("Lost connection: "+getHandlerName());
    }

    @Override
    public void onDisconnect(int from, String reason) {
        try {
            if (this.player != null) {
                PlayerManager mgr = this.server.getPlayerManager();
                mgr.removePlayer(this.player);
            }
        } catch (Exception e) {
            ErrorHandler.setException(new GameError("Failed removing player", e));
        }
    }

    @Override
    public void handleAuth(PacketAuth packetAuth) {
        if (this.state != STATE_AUTH) {
            this.conn.disconnect(Connection.LOCAL, "Invalid packet");
            return;
        }
        this.time = System.currentTimeMillis();
        this.state = STATE_CONNECTED;
        try {
            PlayerManager mgr = this.server.getPlayerManager();
            this.player = mgr.addPlayer(packetAuth.name);
            this.player.netHandler = this;
        } catch (Exception e) {
            ErrorHandler.setException(new GameError("Failed adding player", e));
        }
        WorldServer world = (WorldServer) this.player.world;
        int flags = 0;
        if (this.player.flying) {
            flags |= 1;
        }
        sendPacket(new PacketSSpawnInWorld(world.getId(), this.player.pos, flags, world.getUUID(), world.getSeed(), world.getTime()));
    }

    @Override
    public void handleMovement(PacketCMovement packetMovement) {
        this.player.pos = packetMovement.pos;
        boolean hitground = (packetMovement.flags & 0x1) != 0;
        boolean fly = (packetMovement.flags & 0x2) != 0;
        this.player.hitGround = hitground;
        this.player.flying = fly;
    }


    public void sendPacket(Packet packet) {
        this.conn.sendPacket(packet);
    }

    public void handleSetBlock(PacketCSetBlock p) {
        if (p.y < 0 || p.y >= this.player.world.worldHeight)
            return;
        this.player.world.setType(p.x, p.y, p.z, p.type, Flags.MARK);
        
    }
    public void handleSetBlocks(PacketCSetBlocks p1) {

        int w = p1.x2-p1.x+1;
        int h = p1.y2-p1.y+1;
        int l = p1.z2-p1.z+1;
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                for (int z = 0; z < l; z++) {
                    int blockX = p1.x+x;
                    int blockY = p1.y+y;
                    int blockZ = p1.z+z;
                    this.player.world.setType(blockX, blockY, blockZ, p1.type, Flags.MARK);
                }
            }
            
        }
    }

    @Override
    public boolean isValidWorld(AbstractPacketWorldRef packet) {
        return packet.getWorldId() == this.player.world.getId();
    }
}
