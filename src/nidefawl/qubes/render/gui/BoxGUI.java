package nidefawl.qubes.render.gui;

import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;

import java.nio.*;

import org.lwjgl.system.MemoryUtil;

import nidefawl.qubes.GameBase;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.vec.Vector4f;
import nidefawl.qubes.vulkan.VkPipelines;
import nidefawl.qubes.vulkan.VkTess;

public class BoxGUI {
	public final static BoxGUI INST = new BoxGUI();
	public Vector4f box = new Vector4f();
	public Vector4f color = new Vector4f();
	public float sigma;
	public float corner;
	public float fade;
	public float zpos;
	public int colorwheel;
	public float valueH;
	public float valueS;
	public float valueL;
	private ByteBuffer buffer;
	private FloatBuffer floatBuffer;
	private IntBuffer intBuffer;
	public BoxGUI() {
	    r();
		this.buffer = MemoryUtil.memAlignedAlloc(8, 256);
		this.floatBuffer = this.buffer.asFloatBuffer();
		this.intBuffer = this.buffer.asIntBuffer();
	}
    public static void reset() {
        INST.r();
    }
	public void r() {
        box.set(0, 0, 100, 100);
        color.set(1.0f, 1.0f, 1.0f, 1.0f);
        sigma = 0.25f;
        corner = 4f;
        fade = 0.3f;
        zpos = 0;
        colorwheel = 0;
        valueH = 0.5f;
        valueS = 1.0f;
        valueL = 0.5f;
    }
    public ByteBuffer update() {
		floatBuffer.position(0);
		box.store(floatBuffer);
		color.store(floatBuffer);
		floatBuffer.put(sigma);
		floatBuffer.put(corner);
		floatBuffer.put(fade);
		floatBuffer.put(zpos);
		intBuffer.position(floatBuffer.position());
		intBuffer.put(colorwheel);
		floatBuffer.position(intBuffer.position());
		floatBuffer.put(valueH);
		floatBuffer.put(valueS);
		floatBuffer.put(valueL);
		this.buffer.position(0).limit(floatBuffer.position()*4);
		return this.buffer;
	}
    public static void setFade(float f) {
    	INST.fade = f;
    }
    public void updateConstants() {
        if (!Engine.isVulkan) {
        }
    }
    public static void setZPos(float z) {
        INST.zpos = z;
    }
    public static void setBox(float x, float y, float z, float w) {
        INST.box.set(x, y, z, w);
    }
    public static void setColor(float r, float g, float b, float a) {
        INST.color.set(r, g, b, a);
    }
    public static void setSigma(float s) {
        INST.sigma = s;
    }
    public static void setRound(float r) {
        INST.corner = r;
    }
    public void drawQuad() {
        Engine.setPipeStateGUI();
        if (!Engine.isVulkan) {
            Shader shader = Shaders.gui;
            shader.setProgramUniform4f("box", box.x, box.y, box.z, box.w);
            shader.setProgramUniform4f("color", color.x, color.y, color.z, color.w);
            shader.setProgramUniform1f("sigma", sigma);
            shader.setProgramUniform1f("corner", corner);
            shader.setProgramUniform1f("fade", fade);
            shader.setProgramUniform1f("zpos", zpos);
            shader.setProgramUniform1i("colorwheel", colorwheel);
            shader.setProgramUniform1f("valueH", valueH);
            shader.setProgramUniform1f("valueS", valueS);
            shader.setProgramUniform1f("valueL", valueL);
        } else {
            vkCmdPushConstants(Engine.getDrawCmdBuffer(), VkPipelines.gui.getLayoutHandle(), VK_SHADER_STAGE_VERTEX_BIT|VK_SHADER_STAGE_FRAGMENT_BIT, 0, update());
        }
        Engine.drawQuad();
    }
    public static void setColorwheel(String string, int i) {
        INST.colorwheel = i;
    }

    public static void setHSL(String string, float h, float s, float l) {
        INST.valueH = h;
        INST.valueS = s;
        INST.valueL = l;
    }
	
}
