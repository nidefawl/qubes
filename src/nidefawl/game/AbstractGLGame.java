package nidefawl.game;


public abstract class AbstractGLGame implements Runnable {
    public static String       appName         = "LWJGL Test App";
    public static int          displayWidth;
    public static int          displayHeight;
    public static boolean      glDebug         = false;
    public static boolean      DO_TIMING       = false;
    public static boolean      GL_ERROR_CHECKS = false;
    public static long         windowId        = 0;
    protected boolean          vsync           = true;
    protected volatile boolean running         = false;
    protected volatile boolean wasrunning      = false;
    private Thread             thread;

	public AbstractGLGame() {
	}

	public void startGame() {
		this.thread = new Thread(this, appName + " main thread");
		this.thread.setPriority(Thread.MAX_PRIORITY);
		this.thread.start();
	}

	public abstract void initDisplay(boolean debugContext);

	public boolean isRunning() {
		return running;
	}

	@Override
	public final void run() {
		try {
			initDisplay(false);
		} catch (Throwable t) {
			t.printStackTrace();
			return;
		}

		initGLContext();
		mainLoop();
	}

	public void shutdown() {
		this.running = false;
	}

	public void setVSync(boolean b) {
		this.vsync = b;
		setVSync_impl(b);
	}

	public boolean getVSync() {
		return this.vsync;
	}

	public abstract void onStatsUpdated(float dSec);

	protected abstract void setVSync_impl(boolean b);

	protected abstract void destroyContext();

	protected abstract void onDestroy();
	protected abstract void onKeyPress(long window, int key, int scancode, int action, int mods);
	protected abstract void onMouseClick(long window, int button, int action, int mods);

	protected abstract void checkResize();

	public abstract void updateDisplay();

	public abstract void updateInput();

	public abstract boolean isCloseRequested();

	public Thread getMainThread() {
		return this.thread;
	}
	
	public abstract void render(float f);

	public abstract void input(float f);
	public abstract void updateTime();

	public abstract void preRenderUpdate(float f);

	public abstract void postRenderUpdate(float f);

	public abstract void onResize(int displayWidth, int displayHeight);

	public abstract void tick();

	public abstract void runFrame();

	public abstract void initGame();

	public abstract void mainLoop();

	public abstract void initGLContext();
	
	public abstract void setTitle(String title);
}
