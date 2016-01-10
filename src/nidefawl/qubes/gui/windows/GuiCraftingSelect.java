package nidefawl.qubes.gui.windows;

import java.util.List;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.inventory.slots.Slots;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.shader.Shaders;

public class GuiCraftingSelect extends GuiInventoryBase {
    RecipeSlot selected = null;
    GuiBG bg = new GuiBG();

    List<CatButton>             catButtons = Lists.newArrayList();

    
    private CatButton cat;
    
    private Category[] categories;
    
    static final int slotw=48;
    static final float inset = 4;
    static final float inset2 = 2;
    static float entryw=slotw+inset*2;
    static int cols = 6;
    static class Category {
        public ScrollList scrolllist;
        List<Control>             list = Lists.newArrayList();
        public Category() {
        }
    }
    static class CatButton extends Button {

        public Category category;

        public CatButton(int id, String text, Category category) {
            super(id, text);
            this.category = category;
        }
        
        @Override
        public void render(float fTime, double mX, double mY) {
            this.hovered = this.mouseOver(mX, mY);
            if (!draw)
                return;
            int c1 = this.hovered ? this.color3 : this.color;
            int c2 = this.hovered ? this.color4 : this.color2;
            if (selectedButton == this) {
                c1 = color5;
                c2 = color6;
            }
            int a = this.width;
            int slotW = a;
            int color = 0xdadada;
            float alpha = 0.8f;
            Engine.pxStack.push(0, 0, -22);
            int nx=1;
            int ny=2;
            Shaders.gui.enable();
            Shaders.gui.setProgramUniform1f("fade", 0.1f);
            round=2;
            if (((GuiCraftingSelect)parent).cat == this) {
                Engine.pxStack.translate(0, 0, 15);
                shadowSigma=10;
                renderBox2(true, false, color3, c2);
            } else {
                renderBox2(true, false, c1, c2);
            }
            resetShape();
            Shaders.gui.setProgramUniform1f("fade", 0.3f);
            Engine.pxStack.translate(0, 0, 3);
//            ((GuiCraftingSelect) parent).renderSlotBackground(posX+nx, posY+ny, 0, slotW, slotW, color, alpha, true, 4);
//            ((GuiWindow)this.parent).renderSlotBackground(posX+inset, posY+inset, 0, slotW-inset*2,slotW-inset*2,0, 0.8f, true, 0);
            Shaders.textured.enable();
            BaseStack stack = null;
            if (this.id%100==0) {
                stack = BaseStack.getTemp(Item.axe);   
            }
            if (this.id%100==1) {
                stack = BaseStack.getTemp(Block.logs.oak);   
            }
            if (this.id%100==2) {
                stack = BaseStack.getTemp(Block.stones.getBlocks().get(0));   
            }
            if (this.id%100==3) {
                stack = BaseStack.getTemp(Block.slabs.getBlocks().get(0));   
            }
            if (stack !=null) {
                Engine.pxStack.translate(0, 0, 1);
                Engine.enableDepthMask(false);
                Engine.itemRender.drawItem(stack, posX+nx+inset, posY+ny+inset, slotW-inset*2, slotW-inset*2);
                Engine.enableDepthMask(true);
            }
            Engine.pxStack.pop();
        }
        
    }
    static class RecipeSlot extends AbstractUI {

        final CraftingRecipe recipe;
        public RecipeSlot(AbstractUI parent, CraftingRecipe recipe) {
            this.parent = parent;
            this.recipe = recipe;
        }

        public void initGui(boolean first) {

        }

        @Override
        public void render(float fTime, double mX, double mY) {
            if (!this.draw)
                return;
            BaseStack stack = recipe.getPreview();
            this.hovered = this.mouseOver(mX, mY);
            int c2 = this.hovered ? this.color4 : this.color2;
            float inset = 4;
            float inset2 = 3;
            Engine.pxStack.push(0, 0, 1);
            Shaders.gui.enable();
            int color = 0xdadada;
            float alpha = 0.8f;
            renderSlotBackground(posX, posY, 0, slotW, slotW, color, alpha, true, 4);

            if (this.hovered) {
                Engine.pxStack.translate(0, 0, 2);
                Shaders.gui.enable();
                renderSlotBackground(posX + inset2, posY + inset2, 0, slotW - inset2 * 2, slotW - inset2 * 2, -1, 0.6f, true, 1);
            }
            Engine.pxStack.translate(0, 0, 2);
            if (((GuiCraftingSelect)parent.parent).selected==this) {
                renderSlotBackground(posX + inset, posY + inset, 0, slotW - inset * 2, slotW - inset * 2, -1, 1f, false, 1);
            }

            Engine.pxStack.translate(0, 0, 2);

            Shaders.textured.enable();
            Engine.itemRender.drawItem(stack, posX+inset, posY+inset, slotW-inset*2, slotW-inset*2);
            if (this.hovered) {
                Tooltip tip = Tooltip.item.set(stack, null, null);
                float offsetY = ((ScrollList) parent).scrollY;
                tip.setPos((int) (getWindowPosX()+posX + slotW + 19), (int) (getWindowPosY()-offsetY + posY+slotW+8));
                GuiWindowManager.setTooltip(tip);
            }
            if (this.hovered) {
                Engine.pxStack.translate(0, 0, 10);
                Shaders.gui.enable();
                renderSlotBackground(posX + inset2, posY + inset2, 32, slotW - inset2 * 2, slotW - inset2 * 2, -1, 0.36f, false, 2);

            }

            Engine.pxStack.pop();

        }
    
    }
    static class Control extends AbstractUI {
        private String string;
        private List<CraftingRecipe> list;
        boolean expanded = false;
        private int rows;
        List<AbstractUI> slots = Lists.newArrayList();

        public Control(String string, List<CraftingRecipe> list) {
            this.string = string;
            this.list = list;
            this.rows = list.size()/cols;
            if (list.size() % cols != 0)this.rows++;
            this.height = this.expanded?(30+(int) (this.rows*entryw)):30;
        }

        @Override
        public void render(float fTime, double mX, double mY) {
            FontRenderer fr = ((ScrollList)parent).font;
            Shaders.colored.enable();
            this.hovered = this.mouseOver(mX, mY);
            saveBounds();
            int inset = 15;
            this.posX = inset;
            this.height = 30;
            this.width-=8+inset*2;
            Shaders.gui.enable();
//            AbstractUI g = selectedButton;
//            if (this.hovered)
//            selectedButton = this;
            renderBox();
//            selectedButton = g;
            restoreBounds();
            Shaders.textured.enable();
            fr.drawString(string, posX+width/2, posY+fr.getLineHeight()+6, -1, true, 1, 2);
        }

        @Override
        public void initGui(boolean first) {
            final int slotW = 48;;
            for (int i = 0; i < list.size(); i++) {
                final CraftingRecipe recipe = list.get(i);
                RecipeSlot gui = new RecipeSlot(this, recipe);
                int rrr = i % cols;
                int ccc = i / cols;
                int rX = (int) (posX +entryw/2+ entryw * rrr);
                int rY = (int) (posY + 38 + entryw * ccc);
                gui.setPos(rX, rY);
                gui.setSize(slotW, slotW);
                gui.parent=this.parent;
                gui.enabled = true;
                gui.draw = true;
                slots.add(gui);
                this.parent.add(gui);
            }
        }

        public int layout() {
            int height = this.posY+30;
            if (expanded)
            for (int i = 0; i < slots.size(); i++) {
                AbstractUI slot = slots.get(i);
                int rrr = i % cols;
                int ccc = i / cols;
                int rX = (int) (posX + entryw/2+entryw * rrr);
                int rY = (int) (posY + 38 + entryw * ccc);
                slot.setPos(rX, rY);
                height = Math.max(height, slot.posY+slot.height);

            }
            return height+4-this.posY;
        }
    }
    

    public String getTitle() {
        return "Crafting";
    }
    @Override
    public void initGui(boolean first) {
//      if (this.bounds != null) {
//          setPos(this.bounds[0], this.bounds[1]);
//          setSize(this.bounds[2], this.bounds[3]);
//      } else {
          int width = 800;
          int height = titleBarHeight + 600;
          int xPos = (Game.displayWidth-width)/2;
          int yPos = (Game.displayHeight-height)/2;
          setPos(xPos, yPos);
          setSize(width, height);
//      }
          cat = null;
          this.clearElements();
          this.catButtons.clear();
        int len = (int) (((int)entryw)*(cols+1));
        initLists(first, len);
        bg.setPos(25+len+4+30, titleBarHeight+20-4);
        bg.setSize(this.width-bg.posX-20, this.height-titleBarHeight-40+8);
        this.add(bg);
        int w = slotW;
        int h2 = w + 10;
        int pY = w / 2;
        for (int i = 0; i < categories.length; i++) {
            CatButton b = new CatButton(100+i, "", categories[i]);
            b.setPos(-w, pY);
            pY+=h2;
            b.setSize(w, w);
            addBackground(b);
            catButtons.add(b);
        }
        setCat(catButtons.get(0));
    }

    public void setCat(CatButton catButton) {
        int w = slotW;
        int h2 = w + 10;
        int pY = w / 2;
        if (this.cat != null) {
            this.cat.zIndex--;
            this.remove(this.cat.category.scrolllist.scrollbarbutton);
            int idx = this.cat.id-100;
            int cY =pY+ (idx)*(h2);
            this.cat.setPos(-w, cY);
            this.cat.setSize(w, w);
        }
        this.cat = catButton;
        this.cat.zIndex++;
        this.add(this.cat.category.scrolllist.scrollbarbutton);
        int w2 = slotW+6;
        int idx = this.cat.id-100;
        int cY =pY+ (idx)*(h2);
        this.cat.setPos(-w2-2, cY-3);
        this.cat.setSize(w2, w2);
        sortElements();
    }

    public void initLists(boolean first, int len) {
        List<Category> list = Lists.newArrayList();
        {
            Category cat = new Category();
            cat.scrolllist = new ScrollList(this);
            cat.list.add(new Control("Axes", CraftingRecipes.getList("axes")));
            cat.list.add(new Control("Pickaxes", CraftingRecipes.getList("pickaxes")));
            list.add(cat);
        }
        {
            Category cat = new Category();
            cat.scrolllist = new ScrollList(this);
            cat.list.add(new Control("Logs", CraftingRecipes.getList("logs")));
            cat.list.add(new Control("Planks", CraftingRecipes.getList("planks")));
            cat.list.add(new Control("Wood", CraftingRecipes.getList("wood")));
            list.add(cat);
        }
        {
            Category cat = new Category();
            cat.scrolllist = new ScrollList(this);
            cat.list.add(new Control("Stones", CraftingRecipes.getList("stones")));
            cat.list.add(new Control("Bricks", CraftingRecipes.getList("bricks")));
            cat.list.add(new Control("Stonebricks", CraftingRecipes.getList("stonebricks")));
            cat.list.add(new Control("Cobblestones", CraftingRecipes.getList("cobblestones")));
            list.add(cat);
        }
        {
            Category cat = new Category();
            cat.scrolllist = new ScrollList(this);
            cat.list.add(new Control("Slabs", CraftingRecipes.getList("slabs")));
            cat.list.add(new Control("Stairs", CraftingRecipes.getList("stairs")));
            cat.list.add(new Control("Walls", CraftingRecipes.getList("walls")));
            cat.list.add(new Control("Fences", CraftingRecipes.getList("fences")));
            list.add(cat);
        }
        categories = list.toArray(new Category[list.size()]);
        for (Category cat : categories) {
            cat.scrolllist.setPos(25, titleBarHeight+20);
            cat.scrolllist.setSize(len+4, this.height-titleBarHeight-40);
            int y = 0;
            int h = 30;
            for (Control s : cat.list) {
                s.setPos(0, y);
                s.setSize(len, h);
                y += h+6;
                s.expanded = true;
                cat.scrolllist.add(s);
                s.initGui(first);
            }
            int posY = 0;
            for (int i = 0; i < cat.list.size(); i++) {
                Control n = cat.list.get(i);
                n.posY=posY;
                posY=Math.max(n.posY+n.layout()+6, posY);
            }
        }
    }
    
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action) || this.cat.category.scrolllist.onMouseClick(button, action);
    }

    public void update() {
        this.cat.category.scrolllist.update();

    }
    public void renderFrame(float fTime, double mX, double mY) {
//        Engine.pxStack.push(this.posX, this.posY, -20);
//        saveBounds();
//        int w = slotW+4;
//        posX=-w;
//        width=w;
//        height=w;
//        posY=w/3;
//        for (int i = 0; i < 4; i++) {
//            int c1 = this.hovered ? this.color3 : this.color;
//            int c2 = this.hovered ? this.color4 : this.color2;
//            if (i==1||selectedButton == this) {
//                c1 = color5;
//                c2 = color6;
//            }
//            renderBox(true, false, c1, c2);
//            posY+=w+3;
//        }
//        restoreBounds();
//        Engine.pxStack.pop();
        super.renderFrame(fTime, mX, mY);
    }
    public void render(float fTime, double mX, double mY) {
        Engine.pxStack.push(this.posX, this.posY, 2);

//        System.out.println(cat.hashCode()+"/"+cat.category.scrolllist.buttons);
        this.cat.category.scrolllist.render(fTime, mX-this.posX, mY-this.posY);
        Shaders.gui.enable();
        Engine.pxStack.pop();
        Engine.pxStack.push(0, 0, 4);
        super.renderButtons(fTime, mX, mY);
        Engine.pxStack.pop();
        if (selected != null) {
            Engine.pxStack.push(this.posX, this.posY, 4);
            Shaders.textured.enable();
            Engine.itemRender.drawItem(this.selected.recipe.getPreview(), bg.posX+30+inset, bg.posY+30+inset, slotW-inset*2, slotW-inset*2);
            Engine.pxStack.pop();
        }
    }
    @Override
    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            
        }
        return super.onKeyPress(key, scancode, action, mods);
    }
    
    

    public boolean onGuiClicked(AbstractUI element) {
        if (element instanceof RecipeSlot) {
            setRecipe((RecipeSlot) element);
        }
        if (element instanceof CatButton) {
            setCat((CatButton)element);
        }
        if (element instanceof Control) {
            Control ctrl = (Control) element;
            ctrl.expanded=!ctrl.expanded;
            ctrl.height = 30;
            for (AbstractUI a : ctrl.slots) {
                a.draw = ctrl.expanded;
                a.enabled = ctrl.expanded;
            }
            int a = -1;
            int posY = -1;
            for (int i = 0; i < this.cat.category.list.size(); i++) {
                Control n = this.cat.category.list.get(i);
                if (n==ctrl||a >= 0) {
                    if (a >= 0)
                    n.posY=posY;
                    posY=Math.max(n.posY+n.layout()+6, posY);
                    a = i;
                }
            }
            
        }
        return true;
    }
    public void setRecipe(RecipeSlot element) {
        selected = element;
    }
    public boolean onWheelScroll(double xoffset, double yoffset) {
        return this.cat.category.scrolllist.onWheelScroll(xoffset, yoffset);
    }
}
