package nidefawl.qubes.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.KHRDebug.*;

import java.io.PrintStream;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.GameBase;

public class GLDebugLog {

    public static GLDebugMessageCallback callback;

    public static void setup() {
        glEnable(GL_DEBUG_OUTPUT);
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS);
        KHRDebug.glDebugMessageControl(GL_DONT_CARE, GL_DONT_CARE, GL_DEBUG_SEVERITY_MEDIUM, 0, ByteBuffer.allocateDirect(0), true);
        KHRDebug.glDebugMessageCallback(callback=new GLDebugMessageCallback() {

            @Override
            public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {

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

                String msg = MemoryUtil.memDecodeUTF8(MemoryUtil.memByteBuffer(message, length));
                stream.println("OpenGL " + typeStr +  " [" + srcStr +"]: " + msg);
            }
            
        }, 0);
    }

}
