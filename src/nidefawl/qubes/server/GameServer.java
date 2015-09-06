package nidefawl.qubes.server;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import nidefawl.qubes.config.InvalidConfigException;
import nidefawl.qubes.config.ServerConfig;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.logging.IErrorHandler;
import nidefawl.qubes.network.server.NetworkServer;
import nidefawl.qubes.server.commands.CommandHandler;
import nidefawl.qubes.server.commands.PreparedCommand;
import nidefawl.qubes.server.compress.CompressThread;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.WorldSettings;

public class GameServer implements Runnable, IErrorHandler {
	final ServerConfig config = new ServerConfig();
    final CommandHandler commands = new CommandHandler();
    final PlayerManager playerManager = new PlayerManager(this);
	Thread mainThread;
	Thread handshakeThread;
	NetworkServer networkServer;
	private boolean running;
	private boolean finished;
    private WorldServer[] worlds;
    private HashMap<UUID, WorldServer> worldsMap = new HashMap<>();
    static final long TICK_LEN_MS = 50;
    long lastTick = System.currentTimeMillis();
    private GameError reportedException;
    private int nextWorldID = 0;
    public ConcurrentLinkedQueue<PreparedCommand> commandQueue = new ConcurrentLinkedQueue<>();

	public GameServer() {

	}

	public void startServer() {
		this.mainThread = new Thread(this);
		this.mainThread.setPriority(Thread.MAX_PRIORITY);
		this.mainThread.start();
	}

	@Override
	public void run() {
		try {
			this.running = true;
			load();
			CompressThread.startNewThread(this);
			networkServer.startListener();
			System.out.println("server is running");
			while (this.running) {
				loop();
				if (reportedException != null) {
				    throw reportedException; // MADNESS
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("server ended");
			this.finished = true;
		}
	}

	private void load() {
        loadConfig();
        try {
            this.networkServer = new NetworkServer(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.playerManager.init();
        File worldFolder = WorkingEnv.getWorldsFolder();
        worldFolder.mkdirs();
        File[] worldList = worldFolder.listFiles(new FileFilter() {
            
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && new File(pathname, "world.yml").exists();
            }
        });
        ArrayList<WorldServer> worldsLoaded = new ArrayList<>();
        for (int i = 0; worldList != null && i < worldList.length; i++) {
            File worldDirectory = worldList[i];
            try {
                WorldSettings settings = new WorldSettings(worldDirectory);
                settings.load(new File(worldDirectory, "world.yml"));
                settings.setId(getNextWorldID());
                WorldServer world = new WorldServer(settings, this);
                worldsLoaded.add(world);
            } catch (InvalidConfigException e) {
                e.printStackTrace();
            }
        }
        if (worldsLoaded.isEmpty()) {
            String name = "world";
            File f = new File(worldFolder, name);
            f.mkdirs();
            WorldSettings settings = new WorldSettings(f);
            File fConfig = new File(f, "world.yml");
            try {
                settings.write(fConfig);
            } catch (InvalidConfigException e) {
                e.printStackTrace();
            }
            WorldServer world = new WorldServer(settings, this);
            settings.setId(getNextWorldID());
            worldsLoaded.add(world);
        }
        this.worlds = worldsLoaded.toArray(new WorldServer[worldsLoaded.size()]);
        for (int i = 0; i < this.worlds.length; i++) {
            this.worlds[i].onLoad();
            this.worldsMap.put(this.worlds[i].getUUID(), this.worlds[i]);
        }
    }

    private int getNextWorldID() {
        return this.nextWorldID++;
    }

    protected void loop() throws Exception {
        long start = System.currentTimeMillis();
        this.networkServer.update();
        long passed = System.currentTimeMillis()-lastTick;
        if (passed >= TICK_LEN_MS) {
            updateTick();
            lastTick = System.currentTimeMillis();
        }
        long start2 = System.currentTimeMillis();
        PreparedCommand run;
        while ((run = commandQueue.poll()) != null) {

            run.run();
            long passed2 = System.currentTimeMillis()-start2;
            if (passed2 > TICK_LEN_MS/5) {
                break;
            }
        }
            
        passed = System.currentTimeMillis()-start;
        if (passed < 10) {
            long lSleep = 10-passed;
            if (lSleep > 0)
                Thread.sleep(lSleep);
        }
	}

	private void updateTick() {
	    for (int i = 0; i < this.worlds.length; i++) {
	        try {
	            this.worlds[i].tickUpdate();
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
	    }
    }

    public boolean isRunning() {
		return this.running;
	}
    public void stopServer() {
        this.running = false;
    }

	public void halt() {
		if (this.running) {
			this.running = false;
			System.out.println("Shutting down server...");
            save(true);
			if (this.worlds != null) {
	            for (int i = 0; i < this.worlds.length; i++) {
	                this.worlds[i].onLeave();
	            }
            }
            if (this.networkServer != null) {
                try {
                    this.networkServer.halt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
		}
	}

	public void loadConfig() {
		try {
		    File f = new File(WorkingEnv.getConfigFolder(), "server.yml");
		    if (!f.exists()) {
		        this.config.write(f);
		    } else {
	            this.config.load();
		    }
		} catch (InvalidConfigException e) {
			e.printStackTrace();
		}
	}

	public ServerConfig getConfig() {
		return this.config;
	}

    public CommandHandler getCommandHandler() {
        return this.commands;
    }

    @Override
    public void setException(GameError gameError) {
        this.reportedException = gameError;
    }

    public PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public WorldServer getWorld(UUID uuid) {
        return this.worldsMap.get(uuid);
    }

    public WorldServer getSpawnWorld() {
        return this.worlds[0];
    }

    public Thread getThread() {
        return this.mainThread;
    }

    public void save(boolean b) {
        this.playerManager.savePlayers();
        for (int i = 0; i < this.worlds.length; i++) {
            this.worlds[i].save(b);
        }
    }

    public NetworkServer getNetwork() {
        return this.networkServer;
    }

    public WorldServer[] getWorlds() {
        return this.worlds;
    }

}
