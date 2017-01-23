package nidefawl.qubes.gui;

import java.util.List;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.chat.client.ChatLine;
import nidefawl.qubes.chat.client.ChatManager;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.shader.Shader;
import nidefawl.qubes.shader.Shaders;

public class GuiOverlayChat extends Gui {

    public boolean mouseRes;

    public GuiOverlayChat() {
    }


    public void render(float fTime, double mx, double mY) {
        Shaders.textured.enable();
        if (Game.instance.canRenderGui3d()) {
            int x = Game.guiWidth/2-Game.guiWidth/6;
            setPos(x, 0);
            setSize(Game.guiWidth/3, Game.guiHeight/3);
            renderAt(posX, posY+height, false);
        } else {

            this.posY = Game.guiHeight-12;
            renderAt(0, Game.guiHeight-12, false);
        }
        if (Game.instance.canRenderGui3d()) {
            
        }
        
        Shader.disable();
 
    }

    @Override
    public void initGui(boolean first) {
    }

    
    

    /**
     * @param posY
     */
    public void renderAt(int posX, int posY, boolean full) {
        float fLine = font.getLineHeight()*1.2f;
        float n = this.height / fLine;
        int maxLines = (int) Math.ceil(n);
        List<ChatLine> lines = ChatManager.getInstance().getLines();
        int cHeight = this.height;
        if (!full) {
            maxLines = Math.min(ChatManager.getInstance().getNumNewLines(), maxLines);
            if (maxLines == 0) {
                return;
            }
            this.height = 8+(int) (maxLines*fLine);
        }
        int h = 16;
        int insetX = 2;
        int insetY = 2;
        Engine.enableDepthMask(false);
        this.posX = posX;
        this.posY=posY-this.height;
        Shaders.colored.enable();
        renderOutlinedBox();
        if (full) {
            
            Tess tess = Tess.instance;
            tess.setColor(0xffffff, 122);
    
            tess.add(this.posX+this.width-insetX-h, this.posY+insetY, 0);
            tess.add(this.posX+this.width-insetX, this.posY+insetY+h, 0);
            tess.add(this.posX+this.width-insetX, this.posY+insetY, 0);
            if (mouseRes) {
                tess.setColor(0x353535, 182);
            } else {
                tess.setColor(0xafafaf, 122);
            }
            h -= 4;
            insetX++;
            insetY++;
            tess.add(this.posX+this.width-insetX-h, this.posY+insetY, 0);
            tess.add(this.posX+this.width-insetX, this.posY+insetY+h, 0);
            tess.add(this.posX+this.width-insetX, this.posY+insetY, 0);
            tess.draw(GL11.GL_TRIANGLES);
        }
        Shader.disable();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(this.posX + 2, Engine.getViewport()[3]-(this.posY+this.height), this.width - insetX , height);
        Shaders.textured.enable();
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            ChatLine s = lines.get(i);
            font.drawString(s.getLine(), this.posX+4, posY-6-(i)*fLine, 0xFFFFFF, true, 1.0F);    
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        Engine.enableDepthMask(true);
        this.height = cHeight;
    }
}
