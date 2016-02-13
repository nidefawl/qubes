package nidefawl.qubes.util;

public class Stats {

    public static int   fpsCounter   = 0;
    public static int   uniformCalls = 0;
    public static float avgFrameTime = 0F;
    public static double timeMeshing = 0L;
    public static double timeRendering = 0L;
    public static double fpsInteval;
    public static int regionUpdates = 0;
    public static float lastFrameTimeD;
    public static int tessDrawCalls;
    public static int modelDrawCalls;
    public static int regionDrawCalls;
    public static int lastFrameDrawCalls;
    public static int uploadBytes;
    public static void resetDrawCalls() {
        lastFrameDrawCalls = tessDrawCalls+modelDrawCalls+regionDrawCalls;
        tessDrawCalls = 0;
        modelDrawCalls = 0;
        regionDrawCalls = 0;
        uploadBytes = 0;
    }

}
