package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiMainMenu extends Gui implements ITextEdit {

    final public FontRenderer font;
    final FontRenderer fontSmall;
    private TextField field;

    public GuiMainMenu() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.fontSmall = FontRenderer.get("Arial", 14, 0, 16);
    }
    @Override
    public void initGui(boolean first) {
        this.buttons.clear();
        {
            this.buttons.add(new Button(1, "Connect"));
            int w = 200;
            int h = 30;
            this.buttons.get(0).setPos(this.posX+this.width/2-w/2, this.posY+this.height/2+40);
            this.buttons.get(0).setSize(w, h);
        }
        {

            int w = 200;
            int h = 30;
            this.field = new TextField(this, 2, "debian:21087");
            field.setPos(this.posX+this.width/2-w/2, this.posY+this.height/2);
            field.setSize(w, h);
//            field.
            this.buttons.add(field);
        }
    }
    public boolean onMouseClick(int button, int action) {
        if (action == GLFW.GLFW_PRESS) {
            this.buttons.get(1).focused = false;
        }
        return super.onMouseClick(button, action);
    }

    public void update() {
        
    }

    public void render(float fTime, double mX, double mY) {
        Shaders.colored.enable();
        Tess.instance.setColor(2, 255);
        Tess.instance.add(this.posX, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY+this.height);
        Tess.instance.add(this.posX+this.width, this.posY);
        Tess.instance.add(this.posX, this.posY);
        Tess.instance.draw(GL_QUADS);
//        Shaders.textured.enable();
//        Shader.disable();
        super.renderButtons(fTime, mX, mY);
 
    }

    public boolean onGuiClicked(AbstractUI element) {
//        element.posX+=30;
//        if (element.posX+element.width>this.width) {
//            element.posX = 0;
//        }
        if (element instanceof Button) {
            String s = field.getText();
            if (!s.isEmpty()) {
                Game.instance.connectTo(s);    
            }
        }
        return true;
    }
    @Override
    public void submit(TextInput textInputRenderer, String text) {
        String s = field.getText();
        if (!s.isEmpty()) {
            Game.instance.connectTo(s);    
        }
    }
    @Override
    public void onEscape(TextInput textInput) {
        this.field.focused = false;
    }


}