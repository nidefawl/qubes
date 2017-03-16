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
import nidefawl.qubes.util.ITess;

public class GuiOverlayChat extends Gui {

    public boolean mouseRes;

    public GuiOverlayChat() {
    }


    public void render(float fTime, double mx, double mY) {
        
        if (Game.instance.canRenderGui3d()) {
            int x = Engine.getGuiWidth()/2-Engine.getGuiWidth()/6;
            setPos(x, 0);
            setSize(Engine.getGuiWidth()/3, Engine.getGuiHeight()/3);
            renderAt(posX, posY+height, false);
        } else {

            this.posY = Engine.getGuiHeight()-12;
            renderAt(0, Engine.getGuiHeight()-12, false);
        }
        if (Game.instance.canRenderGui3d()) {
            
        }
 
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
        renderOutlinedBox();
        if (full) {
            Engine.setPipeStateColored2D();
            
            ITess tess = Engine.getTess();
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
            tess.drawTris();
        }
        
        Engine.enableScissors();
        Engine.pxStack.setScissors(this.posX+2, this.posY, this.width- insetX , this.height);
        for (int i = 0; i < lines.size() && i < maxLines; i++) {
            ChatLine s = lines.get(i);
            font.drawString(s.getLine(), this.posX+4, posY-6-(i)*fLine, 0xFFFFFF, true, 1.0F);    
        }
        Engine.disableScissors();
        Engine.enableDepthMask(true);
        this.height = cHeight;
    }
}
