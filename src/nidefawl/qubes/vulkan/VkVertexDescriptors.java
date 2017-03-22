package nidefawl.qubes.vulkan;

import static org.lwjgl.vulkan.VK10.*;

import java.util.ArrayList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;

import nidefawl.qubes.gl.GLVAO.VertexAttrib;
import nidefawl.qubes.util.GameLogicError;

public class VkVertexDescriptors {
    public VkVertexInputAttributeDescription.Buffer attributeDescriptions;
    public int stride;
    private ArrayList<VertexAttrib> list;
    private String defString = "";
    public VkVertexDescriptors(int binding, ArrayList<VertexAttrib> list) {
        this.list = list;
        build();
    }
    public void build() {
        int vertStride = 0;
        for (int i = 0; i < list.size(); i++) {
            VertexAttrib attrib = list.get(i);
            attrib.offset = vertStride;
            vertStride += attrib.intLen;
        }
        this.stride = vertStride*4;
        this.attributeDescriptions = VkVertexInputAttributeDescription.calloc(list.size());
        for (int i = 0; i < list.size(); i++) {
            
            VertexAttrib attrib = list.get(i);
            int vkAttrFormat = 0;
            boolean isUnsigned = false;
            switch (attrib.type) {
                case GL11.GL_BYTE:
                    if (attrib.size == 4) {
                        vkAttrFormat = attrib.normalized ? VK_FORMAT_R8G8B8A8_SNORM : VK_FORMAT_R8G8B8A8_SSCALED;
                        break;
                    }
                    if (attrib.size == 3) {
                        vkAttrFormat = attrib.normalized ? VK_FORMAT_R8G8B8_SNORM : VK_FORMAT_R8G8B8_SSCALED;
                        break;
                    }
                case GL11.GL_UNSIGNED_BYTE:
                    if (attrib.size == 4) {
                        vkAttrFormat = attrib.normalized ? VK_FORMAT_R8G8B8A8_UNORM : VK_FORMAT_R8G8B8A8_UINT;
                        isUnsigned = !attrib.normalized;
                        break;
                    }
                    break;
                case GL11.GL_FLOAT:
                    if (attrib.size == 4) {
                        vkAttrFormat = VK_FORMAT_R32G32B32A32_SFLOAT;
                        break;
                    }
                    if (attrib.size == 3) {
                        vkAttrFormat = VK_FORMAT_R32G32B32_SFLOAT;
                        break;
                    }
                    if (attrib.size == 2) {
                        vkAttrFormat = VK_FORMAT_R32G32_SFLOAT;
                        break;
                    }
                    break;
                case GL30.GL_HALF_FLOAT:
                    if (attrib.size == 4) {
                        vkAttrFormat = VK_FORMAT_R16G16B16A16_SFLOAT;
                        break;
                    }
                    if (attrib.size == 3) {
                        vkAttrFormat = VK_FORMAT_R16G16B16_SFLOAT;
                        break;
                    }
                    if (attrib.size == 2) {
                        vkAttrFormat = VK_FORMAT_R16G16_SFLOAT;
                        break;
                    }
                    break;
                case GL11.GL_UNSIGNED_SHORT:
                    if (attrib.size == 2) {
                        isUnsigned = true;
                        vkAttrFormat = VK_FORMAT_R16G16_UINT;
                        break;
                    }
                    if (attrib.size == 4) {
                        isUnsigned = true;
                        vkAttrFormat = VK_FORMAT_R16G16B16A16_UINT;
                        break;
                    }
                default:
                    break;
            }
            if (vkAttrFormat == 0) {
                throw new GameLogicError("Cannot map format "+attrib.type+","+attrib.size);
            }
            String type ="vec"+attrib.size;
            if (isUnsigned) {
                type = "u"+type;
            }
            String name = attrib.name;
            this.defString += "layout (location = "+attrib.attribindex+") in "+type+" "+name+";\n";
            this.attributeDescriptions.get(i)
                .binding(0)
                .location(attrib.attribindex)
                .format(vkAttrFormat)
                .offset((int) (attrib.offset*4));
        }
        
    }
    public String getVertexDefGLSL() {
        return this.defString;
    }
    public void destroy() {
        if (attributeDescriptions != null) {
            this.attributeDescriptions.free();
        }
    }
}
