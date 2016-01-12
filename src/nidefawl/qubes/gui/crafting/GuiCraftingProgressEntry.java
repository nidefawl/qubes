package nidefawl.qubes.gui.crafting;

import org.lwjgl.opengl.GL11;

import nidefawl.qubes.Game;
import nidefawl.qubes.crafting.CraftingManagerClient;
import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.AbstractUI;
import nidefawl.qubes.gui.Gui;
import nidefawl.qubes.gui.controls.ProgressBar;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;

public class GuiCraftingProgressEntry extends Gui {
    CraftingRecipe recipe;
    private GuiRecipeOutPreviewItemSlot slotPreview;
    public ProgressBar progress;
    int n = 0;
    private final CraftingManagerClient mgr;
    
    public boolean isSelected() {
        return ((GuiCraftingSelect)this.parent.parent).selected2 == this;
    }
    
    public GuiCraftingProgressEntry(CraftingManagerClient mgr) {
        this.mgr = mgr;
    }

    @Override
    public void render(float fTime, double mX, double mY) {
//        FontRenderer fr = ((ScrollList)parent).font;
        this.hovered = this.mouseOver(mX, mY);
        if (this.hovered) {
          Shaders.gui.enable();
          Shaders.gui.setProgramUniform1f("fade", 0.1f);
          renderBox(false, true, color2, color3);
          Shaders.gui.setProgramUniform1f("fade", 0.3f);
        }
        if (this.mgr.isFinished()) {
            int n = -2;
            float c = 0.8f;
            float blink = 0.5f+(float)Math.sin((Game.ticksran/8f)%GameMath.PI)*0.5f;
            Shaders.gui.enable();
            Shaders.gui.setProgramUniform1f("zpos", 2);
            Shaders.gui.setProgramUniform4f("box", posX-n, posY-n, posX+width+n, posY+height+n);
            Shaders.gui.setProgramUniform4f("color", c,1,1-blink, blink);
            Shaders.gui.setProgramUniform1f("sigma", 3);
            Shaders.gui.setProgramUniform1f("corner", 1);
            Engine.drawQuad();
        } 
//        saveBounds();
//        round=0;
//        boxSigma=0.55f;
//        height-=4;
//        posY+=2;
//        Shaders.gui.enable();
//        Shaders.gui.setProgramUniform1f("fade", 0.1f);
//        renderBox(false, true, color2, color3);
//        Shaders.gui.setProgramUniform1f("fade", 0.3f);
//        resetShape();
//        restoreBounds();
        Engine.pxStack.push(0, 0, 10);
//        fr.drawString(string, posX+slotPreview.right()+10, posY+slotPreview.posX+24/2-8, -1, true, 1, 0);
        super.renderButtons(fTime, mX, mY);
//        GL11.glLineWidth(1.0F);
//        Shaders.colored.enable();
//        Tess tessellator = Tess.instance;
//        int yo=1;
//        tessellator.setColorF(color6, 0.25f);
//        tessellator.add(this.posX, this.posY + yo, 4);
//        tessellator.add(this.posX+width, this.posY + yo, 4);
//        tessellator.add(this.posX, this.posY + yo, 4);
//        tessellator.add(this.posX, this.posY + yo+height-yo*2, 4);
//        tessellator.draw(GL11.GL_LINE_STRIP);
        Engine.pxStack.pop();
    }

    @Override
    public void initGui(boolean first) {
        this.clearElements();
        if (this.slotPreview==null)
        this.slotPreview = new GuiRecipeOutPreviewItemSlot(this);
        if (this.progress==null)
        this.progress = new ProgressBar();
        final int slotW = 48;
        int rX = (int) (4);
        int rY = (int) ((this.height-slotW)/2);
        slotPreview.setPos(rX, rY);
        slotPreview.setSize(slotW, slotW);
        slotPreview.parent=this.parent;
        slotPreview.enabled = true;
        slotPreview.draw = true;
        this.add(slotPreview);
        int barh=24;
        progress.setPos(slotPreview.right()+10, slotPreview.bottom()-barh);
        progress.setSize(this.width-this.progress.posX-10, barh);
        this.add(progress);
        if (this.parent != null) {
            updateProgress();
        }
    }
    @Override
    public boolean onGuiClicked(AbstractUI element) {
        return ((Gui) parent).onGuiClicked(element);
    }

    public void updateState() {
        this.recipe=mgr.getRecipe();
        updateProgress();
    }
    @Override
    public void update() {
        super.update();
        updateProgress();
    }

    private void updateProgress() {
        if (this.parent != null) {
            if (mgr.isFinished()) {
                this.progress.setProgress(1);
                this.progress.setText("Complete");
            } else {
                long len = mgr.getEndTime()-mgr.getStartTime();
                long passed = System.currentTimeMillis()-mgr.getStartTime();
                float progress = (float) (((double) passed)/((double) len));
                this.progress.setProgress(progress);   
            }
        }
    }

    public BaseStack getStack() {
        return this.recipe == null ? null : this.recipe.getPreview();
    }

    public long getEndTime() {
        return mgr.getEndTime();
    }

    public CraftingManagerClient getMgr() {
        return mgr;
    }
}