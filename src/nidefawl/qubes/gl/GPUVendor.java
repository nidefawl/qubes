/**
 * 
 */
package nidefawl.qubes.gl;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public enum GPUVendor {
    NVIDIA, AMD, INTEL, OTHER;

    /**
     * @param glGetString
     * @return
     */
    public static GPUVendor parse(String glGetString) {
        if (glGetString != null) {
            glGetString = glGetString.toLowerCase();
            if (glGetString.contains("intel")) {
                return INTEL;
            }
            if (glGetString.contains("nvidia")) {
                return NVIDIA;
            }
            if (glGetString.contains("amd")) {
                return AMD;
            }
            if (glGetString.contains("geforce")) {
                return NVIDIA;
            }
            if (glGetString.contains("radeon")) {
                return AMD;
            }
        }
        return OTHER;
    }
}
