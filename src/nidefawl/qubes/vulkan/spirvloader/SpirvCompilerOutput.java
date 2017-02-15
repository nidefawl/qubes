package nidefawl.qubes.vulkan.spirvloader;
import static org.lwjgl.vulkan.VK10.*;
public class SpirvCompilerOutput {

    public SpirvCompilerOutput() {
    }
    public String[] uniforms = new String[10];
    public byte[] vertex;
    public byte[] fragment;
    public byte[] compute;
    public byte[] geometry;
    public byte[] tessControl;
    public byte[] tessEvaluation;
    public String log = "";
    public int status;
    public byte[] get(int stage) {
        switch (stage) {
            case VK_SHADER_STAGE_VERTEX_BIT:
                return this.vertex;
            case VK_SHADER_STAGE_COMPUTE_BIT:
                return this.compute;
            case VK_SHADER_STAGE_FRAGMENT_BIT:
                return this.fragment;
            case VK_SHADER_STAGE_GEOMETRY_BIT:
                return this.geometry;
            case VK_SHADER_STAGE_TESSELLATION_CONTROL_BIT:
                return this.tessControl;
            case VK_SHADER_STAGE_TESSELLATION_EVALUATION_BIT:
                return this.tessEvaluation;
        }
        return null;
    }
    
}
