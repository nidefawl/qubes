package nidefawl.qubes.gui;

import static org.lwjgl.opengl.GL11.GL_QUADS;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.BootClient;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiMainMenu extends Gui {

    final public FontRenderer font;
    final FontRenderer fontSmall;

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
            this.buttons.add(new TextField(2, "localhost:21087"));
            this.buttons.get(1).setPos(this.posX+this.width/2-w/2, this.posY+this.height/2);
            this.buttons.get(1).setSize(w, h);
        }
    }
    public boolean onMouseClick(int button, int action) {
        if (action == GLFW.GLFW_PRESS) {
            this.buttons.get(1).focused = false;
        }
        return super.onMouseClick(button, action);
    }

    public void update(float dTime) {
        
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
        BootClient.instance.showGUI(new GuiMainMenu());
        return true;
    }


}
