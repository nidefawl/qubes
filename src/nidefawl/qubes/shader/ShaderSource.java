package nidefawl.qubes.shader;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nidefawl.qubes.assets.AssetManager;

public class ShaderSource {
    static Pattern patternInclude = Pattern.compile("#pragma include \"([^\"]*)\"");
    static Pattern lineError = Pattern.compile("ERROR: ([0-9]+):([0-9]+): (.*)");

    HashMap<Integer, String> sources = new HashMap<Integer, String>();

    private String processed;
    int nInclude = 0;

    void load(AssetManager assetManager, String path, String name) throws IOException {
        this.processed = readParse(assetManager, path, name, false);
    }
    private String readParse(AssetManager assetManager, String path, String name, boolean resolve) throws IOException {
        InputStream is = null;
        BufferedReader reader = null;
        try {
            is = assetManager.findResource(path + "/" + name);
            while (is == null && resolve) {
                int a = path.lastIndexOf("/");
                if (a > 0) {
                    path = path.substring(0, a);
                } else {
                    break;
                }
                is = assetManager.findResource(path + "/" + name);
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
                sources.put(nInclude++, fullSource);

                String code = "";
                int nLineOffset = 0;
                boolean insertLine = false;
                for (int i = 0; i < lines.size(); i++) {
                    line = lines.get(i);
                    nLineOffset++;
                    if (line.startsWith("#pragma")) {
                        if (resolve) {
                            throw new ShaderCompileError(line, name, "Recursive inlcudes are not supported");
                        }
                        Matcher m = patternInclude.matcher(line);
                        if (m.matches()) {
                            String filename = m.group(1);
                            String include = readParse(assetManager, path, filename, true);
                            if (include == null) {
                                throw new ShaderCompileError(line, name, "Preprocessor error: Failed loading include \"" + filename + "\"");
                            }
                            code += "#line " + 1 + " " + (nInclude-1) + "\r\n";
                            code += include;
                            insertLine = true;
                        } else {
                            throw new ShaderCompileError(line, name, "Preprocessor error: Failed to parse pragma directive");
                        }
                    } else {
                        if (insertLine) {
                            insertLine = false;
                            code += "#line " + (nLineOffset+1) + " " + 0 + "\r\n";
                        }
                        code += line + "\r\n";
                    }
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
                errLog += logLine + "\r\n";
                Matcher m = lineError.matcher(logLine);
                if (m.matches()) {
                    Integer i = Integer.parseInt(m.group(1));
                    if (i < 0) i = 0;
                    Integer i2 = Integer.parseInt(m.group(2))-1;
                    String source = sources.get(i);
                    if (source == null) {
                        System.err.println("failed getting source idx "+i + " ("+sources.keySet()+")");
                        continue;
                    }
                    String[] lines2 = source.split("\r?\n");
                    if (lines2.length < i2) {
                        System.err.println("failed getting line idx "+i2+" ("+lines2.length+")");
                        continue;
                    }
                    errLog += lines2[i2] + "\r\n";
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    
        return errLog;
    }
}
