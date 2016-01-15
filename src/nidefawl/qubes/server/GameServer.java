package nidefawl.qubes.server;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import nidefawl.qubes.chat.ChannelManager;
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
	Thread mainThread;
	Thread handshakeThread;
	NetworkServer networkServer;
	private boolean running;
	private boolean finished = false;
    private WorldServer[] worlds;
    private HashMap<UUID, WorldServer> worldsMap = new HashMap<>();
    static final long TICK_LEN_MS = 50;
    long lastTick = System.currentTimeMillis();
    long lastSaveTick = System.currentTimeMillis();
    int lastSaveStep = 0;
    private GameError reportedException;
    private int nextWorldID = 0;
    public ConcurrentLinkedQueue<PreparedCommand> commandQueue = new ConcurrentLinkedQueue<>();

    final PlayerManager playerManager = new PlayerManager(this);
    final ChannelManager channelManager = new ChannelManager(this);
    
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
		    onShutdown();
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
                File settingsFile = new File(worldDirectory, "world.yml");
                settings.load(settingsFile);
                settings.setId(getNextWorldID());
                settings.write(settingsFile);
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
            if (passed > 80) {
                System.err.println("SLOW TICK "+passed+"ms");
            }
            updateTick();
            lastTick = System.currentTimeMillis();
        }
        long start2 = System.currentTimeMillis();
        if (start2-this.lastSaveTick > 1000) {
            resyncTime();
            this.lastSaveTick = start2;
            long start1 = System.currentTimeMillis();
            this.saveAndUnloadData();
            long passed2 = System.currentTimeMillis()-start1;
            if (passed2 > 80) {
                System.err.println("SLOW saveAndUnloadData "+passed2+"ms");
            }
//            ServerStats.dump();
        }
        start2 = System.currentTimeMillis();
        PreparedCommand run;
        while ((run = commandQueue.poll()) != null) {

            run.run();
            long passed2 = System.currentTimeMillis()-start2;
            if (passed2 > TICK_LEN_MS/5) {
                break;
            }
        }
            
        passed = System.currentTimeMillis()-start;
        if (passed < 4) {
            long lSleep = 4-passed;
            if (lSleep > 0)
                Thread.sleep(lSleep);
        }
	}
    private void resyncTime() {
        for (int i = 0; i < this.worlds.length; i++) {
            try {
                this.worlds[i].resyncTime();
            } catch (Exception e) {
                e.printStackTrace();
            }
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
        this.playerManager.updateTick();
    }
    private void saveAndUnloadData() {
        int idx = this.lastSaveStep++%this.worlds.length;
        try {
            this.worlds[idx].unloadUnused();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
		return this.running;
	}
    public void stopServer() {
        this.running = false;
    }

	private void onShutdown() {
		if (!this.finished) {
			this.finished = true;
			System.out.println("Shutting down server...");
            try {
                save(true);
            } catch (Throwable t) {
                t.printStackTrace();
            }
            if (this.worlds != null) {
                for (int i = 0; i < this.worlds.length; i++) {
                    try {
                        this.worlds[i].onLeave();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
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
		    if (f.exists()) {
	            this.config.load(f);
		    } else
		        this.config.write(f);
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
        if (this.playerManager != null)
            this.playerManager.savePlayers();
        if (this.worlds != null)
            for (int i = 0; i < this.worlds.length; i++) {
                if (this.worlds[i] != null)
                    this.worlds[i].save(b);
            }
    }

    public NetworkServer getNetwork() {
        return this.networkServer;
    }

    public WorldServer[] getWorlds() {
        return this.worlds;
    }

    /**
     * @return 
     * 
     */
    public ChannelManager getChatChannelMgr() {
        return this.channelManager;
    }

    public long getServerTime() {
        return System.currentTimeMillis();
    }

}
