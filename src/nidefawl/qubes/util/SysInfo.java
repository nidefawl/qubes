package nidefawl.qubes.util;

import static org.lwjgl.vulkan.VK10.VK_VERSION_MAJOR;
import static org.lwjgl.vulkan.VK10.VK_VERSION_MINOR;
import static org.lwjgl.vulkan.VK10.VK_VERSION_PATCH;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.gl.Engine;

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
        
        if (Engine.isVulkan) {
            int apiVersion = Engine.vkContext.properties.apiVersion();
            int major = VK_VERSION_MAJOR(apiVersion);
            int minor = VK_VERSION_MINOR(apiVersion);
            int patch = VK_VERSION_PATCH(apiVersion);
            this.openGLVendor = Engine.vkContext.properties.deviceNameString();
            this.openGLVersion = "Vulkan "+major+"."+minor+"."+patch;
        } else {
//          this.lwjglVersion = Sys.getVersion();
            this.openGLVersion = GL11.glGetString(GL11.GL_RENDERER) + " " + GL11.glGetString(GL11.GL_VERSION);
            this.openGLVendor = GL11.glGetString(GL11.GL_VENDOR);
        }
    }

}
