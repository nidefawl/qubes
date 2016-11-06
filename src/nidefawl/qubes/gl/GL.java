package nidefawl.qubes.gl;

import static org.lwjgl.opengl.EXTDirectStateAccess.glBindMultiTextureEXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.*;

import nidefawl.qubes.gl.GL;
import nidefawl.qubes.util.GameError;


public class GL {

    private static boolean directStateAccess;
    public static void bindTexture(int texunit, int target, int texture) {
        if (!directStateAccess) {
            int i = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
            if (i != texunit) {
                GL13.glActiveTexture(texunit);    
            }
            GL11.glBindTexture(target, texture);
            if (i != texunit) {
                GL13.glActiveTexture(i);    
            }
        } else {
            glBindMultiTextureEXT(texunit, target, texture);    
        }
    }

    public static List<String> validateCaps() {
        final GLCapabilities caps = getCaps();
        ArrayList<String> missingExt = new ArrayList<>();
        if (!caps.OpenGL44) {
            missingExt.add("OpenGL >= 4.4");
        }
        if (!caps.GL_EXT_texture_array) {
            missingExt.add("GL_EXT_texture_array");
        }
        if (!caps.GL_ARB_fragment_shader) {
            missingExt.add("GL_ARB_fragment_shader");
        }
        if (!caps.GL_ARB_vertex_shader) {
            missingExt.add("GL_EXT_vertex_shader");
        }
        if (!caps.GL_ARB_uniform_buffer_object) {
            missingExt.add("GL_ARB_uniform_buffer_object");
        }
        directStateAccess = caps.GL_EXT_direct_state_access;
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

    public static GLCapabilities getCaps() {
        return org.lwjgl.opengl.GL.getCapabilities();
    }
    public static void glTexStorage3D(int target, int levels, int internalformat, int width, int height, int depth) {
        GLCapabilities caps = getCaps();
        if (caps.OpenGL42) {
            GL42.glTexStorage3D(target, levels, internalformat, width, height, depth);
        } else if (caps.GL_ARB_texture_storage) {
            ARBTextureStorage.glTexStorage3D(target, levels, internalformat, width, height, depth);
        } else {
            if (target == GL12.GL_TEXTURE_3D || target == GL12.GL_PROXY_TEXTURE_3D) {
                for (int i = 0; i < levels; i++)
                {
                    GL12.glTexImage3D(target, i, internalformat, width, height, depth, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_INT, 0);
                    width = Math.max(1, (width / 2));
                    height = Math.max(1, (height / 2));
                    depth = Math.max(1, (depth / 2));
                }
            } else {
                for (int i = 0; i < levels; i++)
                {
                    GL12.glTexImage3D(target, i, internalformat, width, height, depth, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_INT, 0);
                    width = Math.max(1, (width / 2));
                    height = Math.max(1, (height / 2));
                }
            }
        }
    }

    public static void glTexStorage2D(int target, int levels, int internalformat, int width, int height) {
        GLCapabilities caps = getCaps();
        if (caps.OpenGL42) {
            GL42.glTexStorage2D(target, levels, internalformat, width, height);
        } else if (caps.GL_ARB_texture_storage) {
            ARBTextureStorage.glTexStorage2D(target, levels, internalformat, width, height);
        } else {
            throw new GameError("Your GPU is not yet supported");
        }
        
    }

    public static int genStorage(int w, int h, int format, int filter, int wrap) {
        int i = GL11.glGenTextures();
        glActiveTexture(GL_TEXTURE0);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, i);
        GL.glTexStorage2D(GL_TEXTURE_2D, 1, format, w, h);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, filter);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, wrap);
        glTexParameteri(GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, wrap);
        GL.bindTexture(GL_TEXTURE0, GL_TEXTURE_2D, 0);
        return i;
    }

    public static void deleteTexture(int tex) {
        if (tex > 0) {
            GL11.glDeleteTextures(tex);
        }
    }
    public static boolean isBindlessSuppported() {
        return GL.getCaps().GL_NV_shader_buffer_load;
    }
}
