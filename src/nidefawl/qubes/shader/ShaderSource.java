package nidefawl.qubes.shader;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.gl.Engine;

public class ShaderSource {
    static Pattern patternInclude = Pattern.compile("#pragma include \"([^\"]*)\"");
    static Pattern patternDefine = Pattern.compile("#pragma define \"([^\"]*)\"");
    static Pattern patternAttr = Pattern.compile("#pragma attributes \"([^\"]*)\"");
    static Pattern lineErrorAMD = Pattern.compile("ERROR: ([0-9]+):([0-9]+): (.*)");
    static Pattern lineErrorNVIDIA = Pattern.compile("([0-9]+)\\(([0-9]+)\\) : (.*)");

    HashMap<Integer, String> sources = new HashMap<Integer, String>();

    HashMap<Integer, String> sourceNames = new HashMap<Integer, String>();

    private String processed;
    int nInclude = 0;
    private String attrTypes = "default";

    void load(AssetManager assetManager, String path, String name, IShaderDef def) throws IOException {
        this.processed = readParse(assetManager, path, name, def, false);
    }
    private String readParse(AssetManager assetManager, String path, String name, IShaderDef def, boolean resolve) throws IOException {
        InputStream is = null;
        BufferedReader reader = null;
        try {
            String fpath = path + "/" + name;
            is = assetManager.findResource(fpath, true);
            while (is == null && resolve) {
                int a = path.lastIndexOf("/");
                if (a > 0) {
                    path = path.substring(0, a);
                } else {
                    break;
                }
                fpath = path + "/" + name;
                is = assetManager.findResource(path + "/" + name, false);
            }
            if (is != null) {
                reader = new BufferedReader(new InputStreamReader(is));
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
                for (int i = 0; i < lines.size(); i++) {
                    line = lines.get(i);
                    if (line.startsWith("#pragma")) {
                        if (resolve) {
                            throw new ShaderCompileError(line, name, "Recursive inlcudes are not supported");
                        }
                        Matcher m;
                        if ((m = patternInclude.matcher(line)).matches()) {
                            String filename = m.group(1);
                            String include = readParse(assetManager, path, filename, def, true);
                            if (include == null) {
                                throw new ShaderCompileError(line, name, "Preprocessor error: Failed loading include \"" + filename + "\"");
                            }
                            code += "#line " + 1 + " " + (nInclude-1) + "\r\n";
                            code += include;
                            insertLine = true;
                        } else if ((m = patternDefine.matcher(line)).matches()) {
                            String define = m.group(1);
                            String s = null;
                            if (def != null) {
                                s = def.getDefinition(define);
                            }
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
                return code;
            } else {
                //                System.err.println("missing code for "+path+" - "+name);
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return this.processed == null;
    }
    public String getSource() {
        return this.processed;
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
                    if (lines2.length <= i2) {
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
}
