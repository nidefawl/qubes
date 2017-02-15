package nidefawl.qubes.shader;

import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetInputStream;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GPUVendor;
import nidefawl.qubes.texture.array.BlockTextureArray;
import nidefawl.qubes.util.GameError;
import nidefawl.qubes.util.GameLogicError;
import nidefawl.qubes.util.StringUtil;

public class ShaderSource {
    static Pattern patternInclude = Pattern.compile("#pragma include \"([^\"]*)\"");
    static Pattern patternDefine = Pattern.compile("#pragma define \"([^\"]*)\"( \"([^\"]*)\")?.*");
    static Pattern patternAttr = Pattern.compile("#pragma attributes \"([^\"]*)\"");
    static Pattern patternOutputCustom = Pattern.compile("layout\\(location = ([0-9]{1,2})\\) out ([^\\s]*) ([^\\s]*);.*");
    static Pattern patternDebug = Pattern.compile("#print ([^\\s]*) ([^\\s]*) ([^\\s]*)");
    static Pattern lineErrorAMD = Pattern.compile("ERROR: ([0-9]+):([0-9]+): (.*)");
    static Pattern lineErrorNVIDIA = Pattern.compile("([0-9]+)\\(([0-9]+)\\) : (.*)");

    ArrayList<String> extensions = new ArrayList<>();
    HashMap<Integer, String> sources = new HashMap<Integer, String>();

    HashMap<Integer, String> sourceNames = new HashMap<Integer, String>();
    HashMap<String, Integer> customOutputLocations = new HashMap<>();

    private String processed;
    int nInclude = 0;
    private String attrTypes = "default";
    private ShaderSourceBundle shaderSourceBundle;
    private String fileName;
    private String version = null;

    /**
     * @param shaderSourceBundle
     */
    public ShaderSource(ShaderSourceBundle shaderSourceBundle) {
        this.shaderSourceBundle = shaderSourceBundle;
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
            is = assetManager.findResource(fpath, true);
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
                    fullSource += line + "\r\n";
                }
                sources.put(nInclude, fullSource);
                sourceNames.put(nInclude, fpath);
                nInclude++;
                String code = "";
                int nLineOffset = 0;
                boolean insertLine = true;
                if (!resolve) {
                    if (version != null) {
                        lines.set(0, version);
                    }
                    lines.addAll(1, extensions);
                }
                for (int i = 0; i < lines.size(); i++) {
                    line = lines.get(i);
                    Matcher m;
                    if (shaderType == GL_FRAGMENT_SHADER && line.startsWith("layout") && (m = patternOutputCustom.matcher(line)).matches()) {
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
                            this.attrTypes = m.group(1);
                        } else {
                            throw new ShaderCompileError(line, name, "Preprocessor error: Failed to parse pragma directive");
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
            if (BlockTextureArray.getInstance().isSRGB()) {
                return "#define SRGB_TEXTURES"; 
            }
            return "#define SRGB_TEXTURES";
            
        }
        if ("Z_INVERSE".equals(define)) {
//            System.out.println("has inverse z define "+name);
            if (Engine.INVERSE_Z_BUFFER) {
                return "#define Z_INVERSE 1";
            }
            return "#define Z_INVERSE 0"; 
            
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
