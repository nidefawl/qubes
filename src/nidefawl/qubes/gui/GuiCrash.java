package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;

public class GuiCrash extends Gui {
    FontRenderer         fontSmall = FontRenderer.get(0, 16, 1);
    FontRenderer         fontBig   = FontRenderer.get(0, 20, 0);
    private List<String> desc;
    private List<String> desc2;
    private String       title;
    private Throwable throwable;

    public GuiCrash(String title, List<String> desc) {
        this(title, desc, null);
    }

    public GuiCrash(String title, List<String> desc, Throwable throwable) {
        this.desc = new ArrayList<>(desc);
        this.desc2 = new ArrayList<>();
        this.title = title;
        this.throwable = throwable;
        if (throwable != null) {
            throwable.printStackTrace();
            StringWriter errors = new StringWriter();
            throwable.printStackTrace(new PrintWriter(errors));
            String[] split = errors.toString().split("\r?\n");
            this.desc.add("");
            this.desc.add("__ STACKTRACE OF ERROR __");
            this.desc2.addAll(Arrays.asList(split));
        }
    }

    @Override
    public void render(float fTime, double mx, double mY) {

        glEnable(GL_BLEND);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
        
        fontBig.drawString(this.title, width / 2, height / 6, -1, true, 1.0F, 2);

        glEnable(GL_BLEND);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_TEXTURE_2D);
        int l = width / 5;
        int t = height / 5;
        Tess.instance.setColor(0x333333, 255);
        Tess.instance.add(width - l, height - t);
        Tess.instance.add(width - l, t);
        Tess.instance.add(l, t);
        Tess.instance.add(l, height - t);
        Tess.instance.draw(GL11.GL_QUADS);

        glEnable(GL_BLEND);
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
        int yp = t+20;
        for (int i = 0; i < this.desc.size(); i++) {
            fontSmall.drawString(this.desc.get(i), width / 2, yp, -1, true, 1.0F, 2);
            yp+=fontSmall.getLineHeight();
        }
        yp+=10;

        for (int i = 0; i < this.desc2.size(); i++) {
            fontSmall.drawString(this.desc2.get(i), l + 10, yp, -1, true, 1.0F, 0);
            yp+=fontSmall.getLineHeight();
        }

    }
    @Override
    public void initGui(boolean first) {
    }

}
