package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.TextField;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiGameMenu extends Gui {

    final public FontRenderer font;
    final FontRenderer fontSmall;
    private Button resume;
    private Button settings;
    private Button bindings;
    private Button back;
    private Button quit;

    public GuiGameMenu() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.fontSmall = FontRenderer.get("Arial", 14, 0, 16);
    }
    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        int w1 = 240;
        int h = 24;
        int left = this.posX+this.width/2-w1/2;
        {
            resume = new Button(1, "Resume");
            this.buttons.add(resume);
            resume.setPos(left, this.posY+this.height/2-20);
            resume.setSize(w1, h);
        }
        {
            settings = new Button(2, "Settings");
            this.buttons.add(settings);
            settings.setPos(left, this.posY+this.height/2+20);
            settings.setSize(w1, h);
        }
        {
            bindings = new Button(3, "Bindings");
            this.buttons.add(bindings);
            bindings.setPos(left, this.posY+this.height/2+60);
            bindings.setSize(w1, h);
        }
        {
            back = new Button(4, "Back to Main Menu");
            this.buttons.add(back);
            back.setPos(left, this.posY+this.height/2+100);
            back.setSize(w1, h);
        }
        {
            quit = new Button(5, "Squid Game");
            this.buttons.add(quit);
            quit.setPos(left, this.posY+this.height/2+140);
            quit.setSize(w1, h);
        }
    }
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action);
    }

    public void update() {
        
    }

    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        super.renderButtons(fTime, mX, mY);
    }
    public boolean onGuiClicked(AbstractUI element) {
        if (element == this.resume) {
            Game.instance.showGUI(null);
        }
        if (element == this.settings) {
            Game.instance.showGUI(new GuiSettings());
        }
        if (element == this.bindings) {

        }
        if (element == this.back) {
            Game.instance.returnToMenu();
        }
        if (element == this.quit) {
            Game.instance.shutdown();
        }
        return true;
    }

    public boolean requiresTextInput() {
        return true;
    }

}
