package nidefawl.qubes.vulkan.spirvloader;

import java.io.File;

import org.lwjgl.system.Platform;

public class SpirvCompiler {

    public final static int OptionNone = 0;
    public final static int OptionIntermediate = (1 << 0);
    public final static int OptionSuppressInfolog = (1 << 1);
    public final static int OptionMemoryLeakMode = (1 << 2);
    public final static int OptionRelaxedErrors = (1 << 3);
    public final static int OptionGiveWarnings = (1 << 4);
    public final static int OptionLinkProgram = (1 << 5);
    public final static int OptionMultiThreaded = (1 << 6);
    public final static int OptionDumpConfig = (1 << 7);
    public final static int OptionDumpReflection = (1 << 8);
    public final static int OptionSuppressWarnings = (1 << 9);
    public final static int OptionDumpVersions = (1 << 10);
    public final static int OptionSpv = (1 << 11);
    public final static int OptionHumanReadableSpv = (1 << 12);
    public final static int OptionVulkanRules = (1 << 13);
    public final static int OptionDefaultDesktop = (1 << 14);
    public final static int OptionOutputPreprocessed = (1 << 15);
    public final static int OptionOutputHexadecimal = (1 << 16);
    public final static int OptionReadHlsl = (1 << 17);
    public final static int OptionCascadingErrors = (1 << 18);
    public final static int OptionAutoMapBindings = (1 << 19);
    public final static int OptionFlattenUniformArrays = (1 << 20);
    public final static int OptionNoStorageFormat = (1 << 21);
    public final static int OptionKeepUncalled = (1 << 22);

    native public static SpirvCompilerOutput compile(String s, int type, int options);
    native public static SpirvCompilerOutput testshader();
    static {
        create();
    }

    public static void create() {
        String name;
        boolean bitness = System.getProperty("os.arch").indexOf("64") > -1;
        switch ( Platform.get() ) {
            case LINUX:
                name = bitness?"libspirvloader.x64.so.1":"libspirvloader.x86.so.1";
                break;
            case WINDOWS:
                name = bitness?"SPIRVLoader.x64.dll":"SPIRVLoader.x86.dll";
                break;
            default:
                throw new IllegalStateException();
        }
        File f = new File(name);
        if (!f.exists()) {
            f = new File("../Game/lib/spirvloader/", name);
        }
        System.load(f.getAbsolutePath());
    }
}
