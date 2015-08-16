package nidefawl.qubes.util;

import org.lwjgl.Sys;
import org.lwjgl.opengl.GL11;

public class SysInfo {
    public String  osName;
    public boolean isWindows;
    public boolean is64Bit;
    public String  javaVersion;
    public long    memoryMb;
    public String  javaMachineVersion;
    public String  lwjglVersion;
    public String  openGLVersion;
    public String  openGLVendor;

    public SysInfo() {
        this.osName = System.getProperty("os.name");
        isWindows = this.osName.contains("Windows");
        if (isWindows) {
            is64Bit = (System.getenv("ProgramFiles(x86)") != null);
        } else {
            is64Bit = (System.getProperty("os.arch").indexOf("64") != -1);
        }
        this.osName = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + " on " + (is64Bit ? "x64 OS" : "x86 OS") + ") "
                + System.getProperty("os.version");
        this.javaVersion = System.getProperty("java.version") + " - " + System.getProperty("java.vendor");
        this.javaMachineVersion = System.getProperty("java.vm.name") + " - " + System.getProperty("java.vm.info") + " - " + System.getProperty("java.vm.vendor");
        this.memoryMb = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        this.lwjglVersion = Sys.getVersion();
        this.openGLVersion = GL11.glGetString(GL11.GL_RENDERER) + " " + GL11.glGetString(GL11.GL_VERSION);
        this.openGLVendor = GL11.glGetString(GL11.GL_VENDOR);
    }

}
