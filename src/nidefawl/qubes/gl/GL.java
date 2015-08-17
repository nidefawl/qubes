package nidefawl.qubes.gl;

import static org.lwjgl.opengl.EXTDirectStateAccess.glBindMultiTextureEXT;
import static org.lwjgl.opengl.EXTDirectStateAccess.glGenerateTextureMipmapEXT;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

public class GL {
    final static ContextCapabilities caps = GLContext.getCapabilities();

    public static void bindTexture(int texunit, int target, int texture) {
        glBindMultiTextureEXT(texunit, target, texture);
    }
    public static void generateTextureMipmap(int texture, int target) {
        glGenerateTextureMipmapEXT(texture, target);
    }

    public static List<String> validateCaps() {
        ArrayList<String> missingExt = new ArrayList<>();
        if (!caps.OpenGL30) {
            missingExt.add("OpenGL >= 3.0");
        }
        if (!caps.GL_EXT_direct_state_access) {
            missingExt.add("GL_EXT_direct_state_access");
        }
        if (!caps.GL_EXT_texture_array) {
            missingExt.add("GL_EXT_texture_array");
        }
        if (!caps.GL_ARB_fragment_shader) {
            missingExt.add("GL_EXT_vertex_shader");
        }
        if (!caps.GL_ARB_vertex_shader) {
            missingExt.add("GL_EXT_vertex_shader");
        }
        return missingExt;
    }

    public static boolean hasShaders() {
        return caps.OpenGL30;
    }


}
