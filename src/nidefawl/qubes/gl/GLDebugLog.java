package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.KHRDebug.*;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.MemoryUtil;


public class GLDebugLog {

    public static GLDebugMessageCallback callback;
    
    public static void setup() {
        //make sure context was created with glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GL_TRUE)
        glEnable(GL_DEBUG_OUTPUT);
        KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DONT_CARE, (IntBuffer)null, true);
        KHRDebug.glDebugMessageCallback(callback=new GLDebugMessageCallback() {

            @Override
            public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
                // nvidia
                if (id == 0x00020071) return; // memory usage
                if (id == 0x00020084) return; // Texture state usage warning: Texture 0 is base level inconsistent. Check texture size.
                if (id == 0x00020061) return; // Framebuffer detailed info: The driver allocated storage for renderbuffer 1.
                if (id == 0x00020004) return; // Usage warning: Generic vertex attribute array ... uses a pointer with a small value (...). Is this intended to be used as an offset into a buffer object?
                if (id == 0x00020072) return; // Buffer performance warning: Buffer object ... (bound to ..., usage hint is GL_STATIC_DRAW) is being copied/moved from VIDEO memory to HOST memory.
                if (id == 0x00020074) return; // Buffer usage warning: Analysis of buffer object ... (bound to ...) usage indicates that the GPU is the primary producer and consumer of data for this buffer object.  The usage hint s upplied with this buffer object, GL_STATIC_DRAW, is inconsistent with this usage pattern.  Try using GL_STREAM_COPY_ARB, GL_STATIC_COPY_ARB, or GL_DYNAMIC_COPY_ARB instead.
                //intel
                if (id == 0x00000008) return; // API_ID_REDUNDANT_FBO performance warning has been generated. Redundant state change in glBindFramebuffer API call, FBO 0, "", already bound.

                String srcStr = "UNDEFINED";
                String typeStr = "UNDEFINED";
                PrintStream stream = System.out;
                switch(source)
                {
                    case GL_DEBUG_SOURCE_API:             srcStr = "API"; break;
                    case GL_DEBUG_SOURCE_WINDOW_SYSTEM:   srcStr = "WINDOW_SYSTEM"; break;
                    case GL_DEBUG_SOURCE_SHADER_COMPILER: srcStr = "SHADER_COMPILER"; break;
                    case GL_DEBUG_SOURCE_THIRD_PARTY:     srcStr = "THIRD_PARTY"; break;
                    case GL_DEBUG_SOURCE_APPLICATION:     srcStr = "APPLICATION"; break;
                    case GL_DEBUG_SOURCE_OTHER:           srcStr = "OTHER"; break;
                }
                switch(type)
                {
    
                    case GL_DEBUG_TYPE_ERROR:
                        //  __debugbreak();
                        typeStr = "ERROR";
                        stream = System.err;
                        break;
                    case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR: typeStr = "DEPRECATED_BEHAVIOR"; break;
                    case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR:  typeStr = "UNDEFINED_BEHAVIOR"; break;
                    case GL_DEBUG_TYPE_PORTABILITY:         typeStr = "PORTABILITY"; break;
                    case GL_DEBUG_TYPE_PERFORMANCE:         typeStr = "PERFORMANCE"; break;
                    case GL_DEBUG_TYPE_OTHER:               typeStr = "OTHER"; break;
                }
                ByteBuffer buf;
                buf = MemoryUtil.memByteBuffer(message, length);
                String msg = MemoryUtil.memUTF8(buf);
                stream.println("OpenGL " + typeStr +  " [" + srcStr +"]: " + msg);
            }
        }, 0);
    }

}
