package nidefawl.game;

import static org.lwjgl.opengl.EXTDirectStateAccess.glBindMultiTextureEXT;
import static org.lwjgl.opengl.EXTDirectStateAccess.glGenerateTextureMipmapEXT;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.ContextCapabilities;

public class GL {

    public static void bindTexture(int texunit, int target, int texture) {
        glBindMultiTextureEXT(texunit, target, texture);
    }
    public static void generateTextureMipmap(int texture, int target) {
        glGenerateTextureMipmapEXT(texture, target);
    }

    public static List<String> validateCaps() {
        final ContextCapabilities caps = getCaps();
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


    
    public static void glGetObjectParameterivARB(int obj, int pname, IntBuffer buffer) {
        org.lwjgl.opengl.ARBShaderObjects.glGetObjectParameterivARB(obj, pname, buffer);
    }
    public static void glUniformMatrix4fvARB(int location, boolean transpose, FloatBuffer matrix) {
        org.lwjgl.opengl.ARBShaderObjects.glUniformMatrix4fvARB(location, transpose, matrix);
    }
    public static void glFogv(int pname, FloatBuffer buffer) {
        org.lwjgl.opengl.GL11.glFogfv(pname, buffer);
    }
    public static void glGetFloatv(int pname, FloatBuffer buffer) {
        org.lwjgl.opengl.GL11.glGetFloatv(pname, buffer);
    }
    public static void glLoadMatrixf(FloatBuffer matrix) {
        org.lwjgl.opengl.GL11.glLoadMatrixf(matrix);
    }

    public static ContextCapabilities getCaps() {
        return org.lwjgl.opengl.GL.getCurrent().getCapabilities();
    }
}
