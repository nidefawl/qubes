/**
 * 
 */
package nidefawl.qubes.gui;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import nidefawl.qubes.Game;
import nidefawl.qubes.chat.channel.GlobalChannel;
import nidefawl.qubes.chat.client.ChatLine;
import nidefawl.qubes.chat.client.ChatManager;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.TextField;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.network.packet.PacketChatMessage;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class GuiChatInput extends Gui implements ITextEdit {

    final public FontRenderer font;
    private TextField         field;
    private boolean wasGrab;
    private GuiOverlayChat overlay;
    private int mouseResize;

    public GuiChatInput() {
        this.font = FontRenderer.get("Arial", 18, 0, 20);
        this.overlay = Game.instance.chatOverlay;
    }
    /* (non-Javadoc)
     * @see nidefawl.qubes.gui.AbstractUI#setSize(int, int)
     */
    @Override
    public void setSize(int w, int h) {
        // TODO Auto-generated method stub
        super.setSize(w, h);
        if (this.field != null)
        this.field.width = h;
    }

    @Override
    public void initGui(boolean first) {
        if (first) {
            wasGrab = Game.instance.movement.grabbed();
            Game.instance.movement.setGrabbed(false);
        }
        this.buttons.clear();
        {

            int h = 30;
            int xP = 8;
            int yP = 8;
            this.field = new TextField(this, 2, "");
            field.setPos(xP, Game.displayHeight-h-yP);
            field.setSize(Game.displayWidth/2, h);
            field.focused = true;
            //            field.
            this.buttons.add(field);
            this.field.width = this.overlay.width;
        }
    }

    @Override
    public boolean onGuiClicked(AbstractUI element) {
        return super.onGuiClicked(element);
    }

    @Override
    public boolean onMouseClick(int button, int action) {
        if (action == GLFW.GLFW_RELEASE) {
            
            this.mouseResize = 0;
        }
        // TODO Auto-generated method stub
        else if (action == GLFW.GLFW_PRESS && over(Mouse.getX(), Mouse.getY())) {
            
            this.mouseResize = 1;
        }
        return super.onMouseClick(button, action);
    }

    public boolean over(double mX, double mY) {
        int h = 16;
        int insetX = 2;
        int insetY = 2;
        boolean mouseRes = mX>=this.overlay.posX+this.overlay.width-insetX-h&&mX<=this.overlay.posX+this.overlay.width-insetX;
        mouseRes &= mY>=this.field.posY-10-this.overlay.height+insetY&&mY<=this.field.posY-10-this.overlay.height+insetY+h;
        return mouseRes;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
        Shaders.textured.enable();
        if (this.mouseResize == 1) {
            this.overlay.width = (int) (mX-this.overlay.posX+4);
            this.overlay.height = (int) (this.field.posY-10-mY+4);
            if (this.overlay.width < 30)
                this.overlay.width = 30;
            if (this.overlay.height < 30)
                this.overlay.height = 30;
            this.width = this.overlay.width;
            this.field.width = this.width;
        }
        this.overlay.mouseRes = over(mX, mY);
        int h = 16;
        int insetX = 2;
        int insetY = 2;
        int asdf = this.overlay.posY-this.overlay.height+insetY;
//        System.out.println(mX+"/"+(this.overlay.posX+this.overlay.width-insetX-h)+"/"+asdf+"/"+mY+"/"+overlay.mouseRes);
        this.overlay.renderAt(this.field.posY-10, true);
        super.renderButtons(fTime, mX, mY);
    }

    @Override
    public void submit(TextInput textInputRenderer, String text) {
        Game.instance.showGUI(null);
        if (text.length() > 0)
            Game.instance.sendPacket(new PacketChatMessage(GlobalChannel.TAG, text));
    }

    @Override
    public void onEscape(TextInput textInput) {
        Game.instance.showGUI(null);
    }

    /* (non-Javadoc)
     * @see nidefawl.qubes.gui.Gui#onClose()
     */
    @Override
    public void onClose() {
        if (wasGrab) {
            Game.instance.setGrabbed(true);
        }
    }
}