/**
 * 
 */
package nidefawl.qubes.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class CrashInfo {

    private String title = "";
    private String desc = "";
    private String outBuf = "";
    private String errBuf = "";
    private String exc = "";

    /**
     * @param title
     * @param desc
     */
    public CrashInfo(String title, List<String> desc) {
        this.title = title;
        this.desc ="";
        for (String s : desc) {
            if (!this.desc.isEmpty()) {
                this.desc+="\n";
            }
            this.desc += s;
        }
    }

    /**
     * @param buf1
     */
    public void setLogBuf(String buf1) {
        if (buf1 != null)
        this.outBuf = buf1;
    }

    /**
     * @param buf2
     */
    public void setErrBuf(String buf2) {
        if (buf2 != null)
        this.errBuf = buf2;
    }

    /**
     * @param throwable
     */
    public void setException(Throwable throwable) {
        if (throwable != null) {
            StringWriter errors = new StringWriter();
            throwable.printStackTrace(new PrintWriter(errors));
            this.exc = errors.toString();
        }
    }

}
