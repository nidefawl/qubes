package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lwjgl.vulkan.VK10;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetInputStream;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GPUVendor;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.StringUtil;

public class ShaderSource {
    public static enum ProcessMode {
        OPENGL, VULKAN, VULKAN_PREPROCESSED
    };
    static Pattern patternInclude = Pattern.compile("#pragma include \"([^\"]*)\"");
    static Pattern patternDefine = Pattern.compile("#pragma define \"([^\"]*)\"( \"([^\"]*)\")?.*");
    static Pattern patternSet = Pattern.compile("#pragma set \"([^\"]*)\"( \"([^\"]*)\")?.*");
    static Pattern patternAttr = Pattern.compile("#pragma attributes \"([^\"]*)\"");
    static Pattern patternRemoveSetBindingBuffer = Pattern.compile("layout\\s*\\(\\s*set = [0-9]{1,2},\\s*binding = [0-9]{1,2},\\s*std(140|430)\\s*\\) (uniform|buffer) ([^\\s]*).*");
    static Pattern patternOutputCustom = Pattern.compile("layout\\s*\\(location\\s*=\\s*([0-9]{1,2})\\s*\\)\\s+out\\s+([^\\s]*)\\s+([^\\s]*);.*");
    static Pattern patternStageOutput = Pattern.compile("(flat |noperspective )?out ([^\\s]*) ([^\\s]*);.*");
    static Pattern patternStageInput = Pattern.compile("(flat |noperspective )?in ([^\\s]*) ([^\\s]*);.*");
    static Pattern patternStageSamplerInputRemoveSet = Pattern.compile("^\\s*layout\\s*\\(\\s*set = [0-9]{1,2},\\s*binding = [0-9]{1,2}\\s*\\)\\s+uniform\\s+(u)?sampler([^\\s]*)\\s+([^\\s]*)\\s*;.*");

    static Pattern patternStageSamplerInput = Pattern.compile("uniform (u)?sampler([^\\s]*) ([^\\s]*);.*");
    static Pattern patternDebug = Pattern.compile("#print ([^\\s]*) ([^\\s]*) ([^\\s]*)");
    static Pattern lineErrorAMD = Pattern.compile("ERROR: ([0-9]+):([0-9]+): (.*)");
    static Pattern lineErrorNVIDIA = Pattern.compile("([0-9]+)\\(([0-9]+)\\) : (.*)");

    ArrayList<String> extensions = new ArrayList<>();
    HashMap<Integer, String> sources = new HashMap<Integer, String>();

    HashMap<Integer, String> sourceNames = new HashMap<Integer, String>();
    HashMap<String, Integer> customOutputLocations = new HashMap<>();

    private final ProcessMode processMode;
    private String processed;
    int nInclude = 0;
    private String attrTypes = "default";
    private ShaderSourceBundle shaderSourceBundle;
    private String fileName;
    private String version = null;

    /**
     * @param shaderSourceBundle
     * @param processmode 
     */
    public ShaderSource(ShaderSourceBundle shaderSourceBundle, ProcessMode processMode) {
        this.shaderSourceBundle = shaderSourceBundle;
        this.processMode = processMode;
    }
    public void addEnabledExtensions(String... exts) {
        for (String s : exts) {
            this.extensions.add("#extension "+s+" : enable");
        }
    }
    public void setVersionString(String version) {
        this.version = version;
    }
    public void load(AssetManager assetManager, String path, String name, IShaderDef def, int shaderType) throws IOException {
        this.fileName = name;
        this.processed = readParse(assetManager, path, name, def, 0, shaderType, name);
    }
    private String readParse(AssetManager assetManager, String path, String name, IShaderDef def, int resolveDepth, int shaderType, String srcName) throws IOException {
        boolean resolve = resolveDepth > 0;
        AssetInputStream is = null;
        BufferedReader reader = null;
        try {
            String fpath = path + "/" + name;
            is = assetManager.findResource(fpath, (!resolve&&(this.processMode!=ProcessMode.OPENGL)?false:true));
            ArrayList<String> pathChecked = new ArrayList<>();
            //if we should resolve then check the include path first 
            if (is == null && resolve) {
                fpath = "shaders/include/" + name;
                pathChecked.add(fpath);
                is = assetManager.findResource(fpath, true);
            }
            //if file wasn't resolved (not in include) then traverse path up and check each parent directory
            while (is == null && resolve) {
                int a = path.lastIndexOf("/");
                if (a > 0) {
                    path = path.substring(0, a);
                } else {
                    break;
                }
                fpath = path + "/" + name;
                pathChecked.add(fpath);
                is = assetManager.findResource(path + "/" + name, true);
            }
            if (is != null) {
                reader = new BufferedReader(new InputStreamReader(is.inputStream));
                String line;
                ArrayList<String> lines = new ArrayList<>();
                String fullSource = "";
                while (reader != null && (line = reader.readLine()) != null) {
                    lines.add(line);
                    fullSource += line + "\n";
                }
                sources.put(nInclude, fullSource);
                sourceNames.put(nInclude, fpath);
                nInclude++;
                if (this.processMode == ProcessMode.VULKAN_PREPROCESSED) {
                    return fullSource;
                }
                String code = "";
                int nLineOffset = 0;
                boolean insertLine = true;
                if (!resolve) {
                    if (version != null) {
                        lines.set(0, version);
                    }
                    lines.addAll(1, extensions);
                }
                if (processMode == ProcessMode.VULKAN) {
                    lines.add(resolve?0:1, "#define VULKAN_GLSL");
                }
                int texure_descset_index = 1;
                int nFragmentOutputs = 0;
                int nVertexOutputs = 0;
                int nInputs = 0;
                int nSamplers = 0;
                boolean debugPrint = false;//name.contains("wireframe.vsh");
                for (int i = 0; i < lines.size(); i++) {
                    line = lines.get(i);
                    Matcher m;
                    if (processMode == ProcessMode.VULKAN&&shaderType==VK10.VK_SHADER_STAGE_VERTEX_BIT
                            && (line.startsWith("out") || line.startsWith("flat out") || line.startsWith("noperspective out")) 
                            && (m = patternStageOutput.matcher(line)).matches()) {
                        if (debugPrint) 
                            System.out.println("TRANSFORM VERTEX LINE "+line);
                        String output = "layout (location = "+(nVertexOutputs++)+") "+(m.group(1)==null?"":m.group(1))+" out "+m.group(2)+" "+m.group(3)+";\r\n";
                        code += output;
                        if (debugPrint) System.out.println("TRANSFORMED VERTEX LINE "+output);
                    } else if (processMode == ProcessMode.VULKAN&&shaderType==VK10.VK_SHADER_STAGE_FRAGMENT_BIT
                            && (line.startsWith("in") || line.startsWith("flat in") || line.startsWith("noperspective in")) 
                            && (m = patternStageInput.matcher(line)).matches()) {
                        if (debugPrint) System.out.println("TRANSFORM FRAGMENT LINE "+line);
                        String input = "layout (location = "+(nInputs++)+") "+(m.group(1)==null?"":m.group(1))+" in "+m.group(2)+" "+m.group(3)+";\r\n";
                        code += input;
                        if (debugPrint) System.out.println("TRANSFORMED FRAGMENT LINE "+input);

                    } else if (processMode == ProcessMode.VULKAN&&shaderType==VK10.VK_SHADER_STAGE_FRAGMENT_BIT&&line.startsWith("out") && (m = patternStageOutput.matcher(line)).matches()) {
                        if (debugPrint) System.out.println("TRANSFORM FRAGMENT LINE "+line);
                        String input = "layout (location = "+(nFragmentOutputs++)+") out "+m.group(2)+" "+m.group(3)+";\r\n";
                        code += input;
                        if (debugPrint) System.out.println("TRANSFORMED FRAGMENT LINE "+input);

                    } else if (processMode == ProcessMode.VULKAN&&line.startsWith("uniform") && (m = patternStageSamplerInput.matcher(line)).matches()) {
                        if (debugPrint) System.out.println("TRANSFORM LINE "+line);
                        String input = "layout (set = "+texure_descset_index+", binding = "+(nSamplers++)+") uniform "+(m.group(1)==null?"":m.group(1))+"sampler"+m.group(2)+" "+m.group(3)+";\r\n";
                        code += input;
                        if (debugPrint) System.out.println("TRANSFORMED LINE "+input);
                    } else if (processMode == ProcessMode.OPENGL&& line.startsWith("layout") && (m = patternStageSamplerInputRemoveSet.matcher(line)).matches()) {
                        if (debugPrint) System.out.println("TRANSFORM LINE "+line);
                        String input = "uniform "+(m.group(1)==null?"":m.group(1))+"sampler"+m.group(2)+" "+m.group(3)+";\r\n";
                        code += input;
                        if (debugPrint) System.out.println("TRANSFORMED LINE "+input);
                    } else if (processMode == ProcessMode.OPENGL&&line.startsWith("layout") && (m = patternRemoveSetBindingBuffer.matcher(line)).matches()) {
                        String buffer_layout = m.group(1);
                        String buffer_type = m.group(2);
                        String buffer_binding_name = m.group(3);
                        code += ("layout(std"+buffer_layout+") "+buffer_type+" "+buffer_binding_name+"\r\n");
                    } else if (shaderType == GL_FRAGMENT_SHADER && line.startsWith("layout") && (m = patternOutputCustom.matcher(line)).matches()) {
//                        System.out.println("matched");
                        int n = StringUtil.parseInt(m.group(1), -1);
                        if (n >= 0) {
                            String out_type = m.group(2);
                            String out_name = m.group(3);
                            code += "out "+out_type+" "+out_name+ ";\r\n";
                            customOutputLocations.put(out_name, n);
                            nLineOffset++;
                            continue;
                        }
                        throw new ShaderCompileError(line, name, "Preprocessor error: Failed to parse layout directive");
                    } else if (line.startsWith("#print")) {
                        if (this.shaderSourceBundle == null) {
                            throw new GameLogicError("Cannot initialize shader debugging without reference to source bundle");
                        }
                        if ((m = patternDebug.matcher(line)).matches()) {
                            String defType = m.group(1);
                            String defName = m.group(2);
                            String defExpr = m.group(3);
                            String replace = this.shaderSourceBundle.addDebugVar(defType, defName, defExpr);
                            code += replace + "\r\n";
                        } else {
                            throw new ShaderCompileError(line, name, "Preprocessor error: Failed to parse print directive");
                        }
                    } else if (line.startsWith("#pragma")) {
                        if ((m = patternInclude.matcher(line)).matches()) {
                            if (resolveDepth > 4) {
                                throw new ShaderCompileError(line, name, "Recursive resolving failed. Depth > 4");
                            }
                            String filename = m.group(1);
                            String include = readParse(assetManager, path, filename, def, resolveDepth+1, shaderType, name);
                            
                            if (include == null) {
                                throw new ShaderCompileError(line, name, "Preprocessor error: Failed loading include \"" + filename + "\"");
                            }
                            code += "#line " + 1 + " " + (nInclude-1) + "\r\n";
                            code += include;
                            insertLine = true;
                        } else if ((m = patternDefine.matcher(line)).matches()) {
                            String define = m.group(1);
                            String defineDefault = m.groupCount() > 2 ? m.group(3) : null;
                            String s = null;
                            if (def != null) {
                                s = def.getDefinition(define);
                            }
                            if (s == null)
                                s = getGlobalDef(srcName, define);
                            if (s == null && defineDefault != null)
                                s = "#define "+define+" "+defineDefault;
                            String replace = s == null ? "" : s;
                            
                            code += replace + "\r\n";
                        } else if ((m = patternAttr.matcher(line)).matches()) {
                            if (processMode == ProcessMode.VULKAN&&shaderType==VK10.VK_SHADER_STAGE_VERTEX_BIT) {
                                if (def != null) {
                                    String s = def.getDefinition("VK_VERTEX_ATTRIBUTES");
                                    if (s != null)
                                        code += s + "\r\n";
                                }
                            }
                            this.attrTypes = m.group(1);
                        } else if ((m = patternSet.matcher(line)).matches()) {
                            String define = m.group(1);
                            String defineDefault = m.groupCount() > 2 ? m.group(3) : null;
                            if ("TEX_DESCRIPTOR_SET".equals(define)) {
                                texure_descset_index = StringUtil.parseInt(defineDefault, 0);
                            }
                        } else {
//                            throw new ShaderCompileError(line, name, "Preprocessor error: Failed to parse pragma directive");
                        }
                    } else {
                        if (i>0&&insertLine && !resolve) {
                            insertLine = false;
                            code += "#line " + (nLineOffset+1) + " " + 0 + "\r\n";
//                            nLineOffset--;
                        }
                        code += line + "\r\n";
                    }
                    nLineOffset++;
                }
                if (Game.instance!=null&&Game.instance.getVendor() == GPUVendor.INTEL) {
                    Pattern p = Pattern.compile("\\b((0x)?[0-9a-fA-F]+)[uU]\\b");
                    Matcher m = p.matcher(code);
                    StringBuffer sb = new StringBuffer(code.length());
                    while (m.find()) {
                        m.appendReplacement(sb, "uint($1)");
                    }
                    m.appendTail(sb);
                    code = sb.toString();
                }
                
                return code;
            } else if (resolve) {
                String p = "";
                for (String s : pathChecked) {
                    p+=s+"\n";
                }
                throw new GameError("Missing shader resource "+name+"\nFile not found:\n"+p);
                //                System.err.println("missing code for "+path+" - "+name);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    /**
     * @param name
     * @param define 
     * @return
     */
    private String getGlobalDef(String name, String define) {
        if ("IS_SKY".equals(define)) {
            return "#define IS_SKY(blockid) float(blockid=="+Block.air.id+"u)";
        }
        if ("IS_WATER".equals(define)) {
            return "#define IS_WATER(blockid) float(blockid=="+Block.water.id+"u)";
        }
        if ("IS_LIGHT".equals(define)) {
            return "#define IS_LIGHT(blockid) float(blockid==2222u)";
        }
        if ("SRGB_TEXTURES".equals(define)) {
            if (Engine.useSRGBTextures()) {
                return "#define SRGB_TEXTURES"; 
            }
            
        }
        if ("Z_INVERSE".equals(define)) {
//            System.out.println("has inverse z define "+name);
            if (Engine.INVERSE_Z_BUFFER) {
                return "#define Z_INVERSE 1";
            }
            return "#define Z_INVERSE 0"; 
        }
        if ("SHADOW_MAP_RESOLUTION".equals(define)) {
            return "#define SHADOW_MAP_RESOLUTION "+Engine.getShadowMapTextureSize()+".0";
        }
//        if ("IS_LEAVES".equals(define)) {
//            return "#define IS_LEAVES(blockid) (blockid=="+Block.leaves.id+"u)";
//        }
//        if ("IS_WAVING_VERTEX".equals(define)) {
//            String def = "";
//            for (int i = 0; i <= Block.HIGHEST_BLOCK_ID; i++) {
//                Block block = Block.get(i);
//                if (block != null && block.isWaving()) {
//                    if (!def.isEmpty()) {
//                        def += " || ";
//                    }
//                    def += "blockid=="+block.id+"u";
//                }
//            }
//            if (def.isEmpty()) {
//                def = "false";
//            } else {
//                def = "("+def+") && (step(2, mod(gl_VertexID+1.0, 4.0f)) != 0)";
//            }
//            return "#define IS_WAVING_VERTEX(blockid, vertex) ("+def+")";
//        }
        return null;
    }
    public boolean isEmpty() {
        return this.processed == null;
    }
    public String getSource() {
        return this.processed;
    }
    public void setSource(String processed) {
        this.processed = processed;
    }
    public String decorateErrors(String log) {
        String errLog = "";
        try {

            String[] lines = log.split("\r?\n");
            for (int n = 0; n < lines.length; n++) {
                String logLine = lines[n];
                Matcher m = lineErrorAMD.matcher(logLine);
                int offset = 0;
                if (!m.matches()) {
                    offset=-1;
                    m = lineErrorNVIDIA.matcher(logLine);
                }
                if (m.matches()) {
                    Integer i = Integer.parseInt(m.group(1));
                    if (i < 0) i = 0;
                    Integer i2 = Integer.parseInt(m.group(2))-1+offset;
                    String source = sources.get(i);
                    String sourceName = sourceNames.get(i);
                    errLog += sourceName+":"+i2+" "+m.group(3)+"\r\n";
                    if (source == null) {
                        System.err.println("failed getting source idx "+i + " ("+sources.keySet()+")");
                        continue;
                    }
                    String[] lines2 = source.split("\r?\n");
                    if (i2<0 || lines2.length <= i2) {
                        System.err.println("failed getting line idx "+i2+" ("+lines2.length+")");
                        continue;
                    }
                    errLog += lines2[i2] + "\r\n";
                } else {

                    errLog += "-"+logLine + "\r\n"; 
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    
        return errLog;
    }
    public String getAttrTypes() {
        return this.attrTypes;
    }
    public String getFileName() {
        return this.fileName;
    }

}
