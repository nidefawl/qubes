package nidefawl.qubes.shader;

import java.io.File;

public class GLSLOptimizer {

    native public static GLSLOptimizedSource optimize(String s, int type);
    static {
        File f = new File("GLSLOptimizer.x64.dll");
        if (!f.exists()) {
            f = new File("../Game/lib/glsloptimizer/GLSLOptimizer.x64.dll");
        }
        System.load(f.getAbsolutePath());
    }
}
