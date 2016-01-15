package nidefawl.qubes.gui.crafting;

import java.util.*;
import java.util.Locale.Category;
import java.util.Map.Entry;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.block.Block;
import nidefawl.qubes.crafting.CraftingCategory;
import nidefawl.qubes.crafting.CraftingManagerClient;
import nidefawl.qubes.crafting.recipes.CraftingRecipe;
import nidefawl.qubes.crafting.recipes.CraftingRecipes;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.font.FontRenderer;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.Tess;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.gui.controls.Button;
import nidefawl.qubes.gui.controls.ScrollList;
import nidefawl.qubes.gui.controls.TextField;
import nidefawl.qubes.gui.windows.*;
import nidefawl.qubes.input.Mouse;
import nidefawl.qubes.inventory.BaseInventory;
import nidefawl.qubes.inventory.PlayerInventory;
import nidefawl.qubes.inventory.slots.*;
import nidefawl.qubes.item.BaseStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemRenderer;
import nidefawl.qubes.network.packet.PacketCCrafting;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.StringUtil;

public class GuiCraftingSelect extends GuiWindowInventoryBase implements ITextEdit {
    RecipeSlot selected = null;
    GuiCraftingProgressEntry selected2 = null;
    GuiBG bg = new GuiBG();
    ScrollList scr2 = new ScrollList();

    List<GuiButtonCat>             catButtons = Lists.newArrayList();
    TextField txtAmount = null;

    
    private GuiButtonCat cat;
    private Button btnCraft;
    private Button btnIncr;
    private Button btnDecr;
    private Button btnMax;
    
    static final int slotw=48;
    static final float inset = 4;
    static final float inset2 = 2;
    static float entryw=slotw+inset*2;
    static int cols = 6;
    static class GuiControlCat {
        public ScrollList scrolllist;
        List<GuiControlSubCat>             list = Lists.newArrayList();
        private CraftingCategory craftingCategory;
        private CraftingManagerClient craftingClient;
        public GuiControlCat(CraftingCategory craftingCategory) {
            this.scrolllist = new ScrollList();
            this.craftingCategory = craftingCategory;
            for (Entry<String, ArrayList<CraftingRecipe>> s : this.craftingCategory.map.entrySet()) {
                this.list.add(new GuiControlSubCat(this, s.getKey(), s.getValue()));
            }
            PlayerSelf player = Game.instance.getPlayer();
            if (player != null) {
                this.craftingClient = player.getCrafting(this.craftingCategory.getId());
            }
        }
    }
    static class GuiButtonCat extends Button {

        public GuiControlCat category;
        private int catid;
        
        public GuiButtonCat(int id, GuiControlCat category, CraftingCategory craftingCategory) {
            super(id, "");
            this.catid = craftingCategory.getId();
            this.category = category;
        }
        public ScrollList getScrollList() {
            return this.category.scrolllist;
        }
        public Button getScrollbarButton() {
            return this.category.scrolllist.scrollbarbutton;
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
                if (getCraftingManager()!=null&&getCraftingManager().isFinished()) {
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

        public CraftingManagerClient getCraftingManager() {
            return this.category.craftingClient;
        }
        
    }
    static class RecipeSlot extends AbstractUI {

        final CraftingRecipe recipe;
        private int maxAmount = -1;
        GuiControlSubCat guiControlSubCat;
        public RecipeSlot(GuiControlSubCat guiControlSubCat, CraftingRecipe recipe) {
            this.guiControlSubCat = guiControlSubCat;
            this.recipe = recipe;
        }

        public void initGui(boolean first) {

        }
        @Override
        public void update() {
            int n = -1;
            CraftingManagerClient mgr = guiControlSubCat.parentCat.craftingClient;
            if (mgr != null) {
                Player p = Game.instance.getPlayer();
                if (p != null) {
                    n = mgr.calcMaxAmount(recipe, (SlotsInventoryBase)p.getSlots(0));
                }
            }
            this.maxAmount = n;
            super.update();
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
            if (this.maxAmount > 0) {
                int nMult = this.maxAmount*stack.getSize();
                Shaders.textured.enable();
                Engine.pxStack.translate(0, 0, 10);
                FontRenderer font = Engine.itemRender.font;
                int w2 = GameMath.round(font.getStringWidth(""+nMult));
                font.drawString(""+nMult, posX+inset+slotW-inset*2-w2-1, posY+inset+slotW-inset*2+2, 0xf0f0f0, true, 1.0f);
            }
            if (this.hovered) {
                Tooltip tip = Tooltip.item.set(stack, null, null);
                float offsetY = 0;
                if (GuiContext.scrolllist != null) {
                    offsetY+=GuiContext.scrolllist.scrollY;
                }
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
    static class GuiControlSubCat extends AbstractUI {
        private String string;
        private List<CraftingRecipe> list;
        boolean expanded = false;
        private int rows;
        List<AbstractUI> slots = Lists.newArrayList();
        private GuiControlCat parentCat;

        public GuiControlSubCat(GuiControlCat guiControlCat, String string, List<CraftingRecipe> list) {
            this.parentCat = guiControlCat;
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
            GL11.glLineWidth(1.0F);
            Shaders.colored.enable();
            Tess tessellator = Tess.instance;
            int yo=0;
            tessellator.setColorF(-1, 0.05f);
            tessellator.add(this.posX, this.posY + yo, 4);
            tessellator.setColorF(-1, 0.15f);
            tessellator.add(this.posX+width/2, this.posY + yo, 4);
            tessellator.setColorF(-1, 0.05f);
            tessellator.add(this.posX+width, this.posY + yo, 4);
            tessellator.draw(GL11.GL_LINE_STRIP);
            yo=height;
            tessellator.setColorF(-1, 0.1f);
            tessellator.add(this.posX, this.posY + yo, 4);
            tessellator.setColorF(-1, 0.4f);
            tessellator.add(this.posX+width/2, this.posY + yo, 4);
            tessellator.setColorF(-1, 0.1f);
            tessellator.add(this.posX+width, this.posY + yo, 4);
            tessellator.draw(GL11.GL_LINE_STRIP);
//            selectedButton = g;
            restoreBounds();
            Shaders.textured.enable();
            fr.drawString(string, posX+width/2, posY+fr.centerY(height), -1, true, 1, 2);
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
          txtAmount = new TextField(this, 0, "0");
          cat = null;
          this.clearElements();
          this.catButtons.clear();
        int len = (int) (((int)entryw)*(cols+1));
        initLists(first, len);
        bg.setPos(25+len+4+30, titleBarHeight+20-4);
        bg.setSize(this.width-bg.posX-20, this.height/3*2);
        scr2.clearElements();
        scr2.parent = this;
        scr2.setPos(25+len+4+35, bg.bottom()+20);
        scr2.setSize(this.width-bg.posX-40, this.height/3-80);
        
        btnCraft = new Button(200, "Craft");
        btnIncr = new Button(201, "+");
        btnDecr = new Button(202, "-");
        btnMax = new Button(203, "max");
        setCat(catButtons.get(0));
        btnCraft.setSize(bg.width/3*2, 30);
        btnCraft.setPos(bg.posX+(bg.width-btnCraft.width)/2, bg.posY+bg.height-txtAmount.height-60);
        txtAmount.zIndex=2;
        
        txtAmount.setSize(40, 30);
        txtAmount.setPos(bg.posX+bg.width/2-60, bg.posY+bg.height-txtAmount.height-80);

        {

            btnIncr.font = FontRenderer.get(0, 10, 0);
            btnIncr.setPos(txtAmount.posX+txtAmount.width+10, txtAmount.posY);
            btnIncr.setSize(15, 12);
            btnIncr.zIndex=4;
        }
        {

            btnDecr.font = FontRenderer.get(0, 10, 0);
            btnDecr.setPos(txtAmount.posX+txtAmount.width+10, txtAmount.posY+18);
            btnDecr.setSize(15, 12);
            btnDecr.zIndex=4;
        }
        {

            btnMax.font = FontRenderer.get(0, 16, 1);
            btnMax.setPos(txtAmount.posX+txtAmount.width+34, txtAmount.posY);
            btnMax.setSize(44, 30);
            btnMax.zIndex=4;
        }
        
        bg.zIndex = 0;
        txtAmount.zIndex=4;
        btnIncr.zIndex=4;
        btnDecr.zIndex=4;
        btnMax.zIndex=4;
        btnCraft.zIndex=4;
        this.add(bg);
        this.add(scr2.scrollbarbutton);
        this.add(txtAmount);
        
        this.add(btnIncr);
        this.add(btnDecr);
        this.add(btnMax);
        this.add(btnCraft);
    }

    public void setCat(GuiButtonCat catButton) {
        showButtons(false);
        this.slots = null;
        this.selected = null;
        this.selected2 = null;
        int w = slotW;
        int h2 = w + 10;
        int pY = w / 2;
        if (this.cat != null) {
            this.cat.zIndex--;
            this.remove(this.cat.getScrollbarButton());
            int idx = this.cat.id-100;
            int cY =pY+ (idx)*(h2);
            this.cat.setPos(-w, cY);
            this.cat.setSize(w, w);
        }
        this.cat = catButton;
        this.cat.zIndex++;
        this.cat.getScrollList().parent = this;
        this.add(this.cat.getScrollbarButton());
        int w2 = slotW+6;
        int idx = this.cat.id-100;
        int cY =pY+ (idx)*(h2);
        this.cat.setPos(-w2-2, cY-3);
        this.cat.setSize(w2, w2);
        sortElements();
        updateProgress();
        System.out.println("cat  "+cat);
        System.out.println("cat.catid "+cat.catid);
        Game.instance.sendPacket(new PacketCCrafting(cat.catid, id, 0));
    }

    public void initLists(boolean first, int len) {
        int w = slotW;
        int h2 = w + 10;
        int pY = w / 2;
        for (int i = 0; i < CraftingCategory.categories.length; i++) {
            if (CraftingCategory.categories[i] == null) {
                continue;
            }
            GuiButtonCat b = new GuiButtonCat(100+i, new GuiControlCat(CraftingCategory.categories[i]), CraftingCategory.categories[i]);
            b.setPos(-w, pY);
            pY+=h2;
            b.setSize(w, w);
            addBackground(b);
            catButtons.add(b);
            System.err.println("catbutn");
        }
        for (GuiButtonCat cat : catButtons) {
            GuiControlCat ctrlCat = cat.category;
            ctrlCat.scrolllist.setPos(25, titleBarHeight+20);
            ctrlCat.scrolllist.setSize(len+4, this.height-titleBarHeight-40);
            int y = 0;
            int h = 30;
            for (GuiControlSubCat s : ctrlCat.list) {
                s.setPos(0, y);
                s.setSize(len, h);
                y += h+6;
                s.expanded = true;
                cat.getScrollList().add(s);
                s.initGui(first);
            }
            int posY = 0;
            for (int i = 0; i < ctrlCat.list.size(); i++) {
                GuiControlSubCat n = ctrlCat.list.get(i);
                n.posY=posY;
                posY=Math.max(n.posY+n.layout()+6, posY);
            }
        }
    }
    
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action) 
                || this.cat.getScrollList().onMouseClick(button, action)
                || this.scr2.onMouseClick(button, action);
    }

    public void update() {
        super.update();
        this.cat.getScrollList().update();
        this.scr2.update();
        if (this.slots != null) {
            ((PreviewSlots)slots).update();
        }

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

//        System.out.println(cat.hashCode()+"/"+cat.getScrollList().buttons);
        this.cat.getScrollList().render(fTime, mX-this.posX, mY-this.posY);
        this.scr2.render(fTime, mX-this.posX, mY-this.posY);
        Shaders.gui.enable();
        Engine.pxStack.pop();
        super.renderButtons(fTime, mX, mY);
        Engine.pxStack.push(0,0,32);
        if (selected != null) {
            Shaders.gui.enable();
            saveBounds();
            Engine.pxStack.push(this.posX, this.posY, 6);
            width = this.bg.width / 3 * 2;
            posX = this.bg.posX + this.bg.width / 2 - width / 2;
            height = 30;
            posY = this.bg.posY + 5;
            renderBox();
            Shaders.textured.enable();
            FontRenderer font = this.cat.getScrollList().font;
            font.drawString(selected.recipe.getPreview().getName(), posX + width / 2, posY + font.centerY(height), -1, true, 1, 2);

            font.drawString("Recipe", bg.posX + bg.width / 4, posY + font.getLineHeight() + 6 + 35, -1, true, 1, 2);
            font.drawString("Inventory", bg.posX + bg.width / 4 * 3, posY + font.getLineHeight() + 6 + 35, -1, true, 1, 2);
            Shaders.colored.enable();

            posX = this.bg.posX + this.bg.width / 2;
            posY = this.bg.posY + 45;
            //          OpenGlHelper.glColor3f(fa, fa, fa);
            GL11.glLineWidth(1.0F);
            Tess tessellator = Tess.instance;
            //          GL11.glBegin(GL11.GL_LINE_STRIP);
            tessellator.setColorF(-1, 0.1f);

            tessellator.add(this.posX, this.posY + 0);
            tessellator.setColorF(-1, 0.4f);
            tessellator.add(this.posX, this.posY + 90);
            tessellator.setColorF(-1, 0.1f);
            tessellator.add(this.posX, this.posY + 180);
            tessellator.draw(GL11.GL_LINE_STRIP);
            restoreBounds();
            //            Shaders.textured.enable();
            //            Engine.itemRender.drawItem(this.selected.recipe.getPreview(), bg.posX+30+inset, bg.posY+30+inset, slotW-inset*2, slotW-inset*2);

            Engine.pxStack.pop();
            //            Shaders.colored.enable();

        }
        if (slots != null) {
            renderSlots(fTime, mX, mY);
        }
        Engine.pxStack.pop();
    }
    @Override
    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (action == GLFW.GLFW_PRESS) {
            
        }
        return super.onKeyPress(key, scancode, action, mods);
    }
    
    

    public boolean onGuiClicked(AbstractUI element) {
        if (element instanceof GuiCraftingProgressEntry||element.parent instanceof GuiCraftingProgressEntry) {
            GuiCraftingProgressEntry ctrl = (GuiCraftingProgressEntry)(element instanceof GuiCraftingProgressEntry?element:element.parent);
            this.selected2 = ctrl;
            if (ctrl.getMgr().isFinished()) {
//                GuiWindowManager.openWindow();
                try {
                    GuiInventoryResult res = (GuiInventoryResult) GuiInventoryResult.getWindowClass(ctrl.getMgr().getId()).newInstance();
                    setPopup(res);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                
                
            }
        }
        if (element instanceof RecipeSlot) {
            setRecipe((RecipeSlot) element);
        }
        if (element instanceof GuiButtonCat) {
            setCat((GuiButtonCat)element);
        }
        if (element instanceof GuiControlSubCat) {
            GuiControlSubCat ctrl = (GuiControlSubCat) element;
            ctrl.expanded=!ctrl.expanded;
            ctrl.height = 30;
            for (AbstractUI a : ctrl.slots) {
                a.draw = ctrl.expanded;
                a.enabled = ctrl.expanded;
            }
            int a = -1;
            int posY = -1;
            for (int i = 0; i < this.cat.category.list.size(); i++) {
                GuiControlSubCat n = this.cat.category.list.get(i);
                if (n==ctrl||a >= 0) {
                    if (a >= 0)
                    n.posY=posY;
                    posY=Math.max(n.posY+n.layout()+6, posY);
                    a = i;
                }
            }
            
        }
        if (element instanceof Button) {
            if (this.selected != null) {
                int n=0;
                switch (element.id) {
                    case 200:
                        if (this.selected != null) {
//                            GuiCraftingProgressEntry entry = new GuiCraftingProgressEntry(recipe.getPreview().getName(), recipe);
//                            this.cat.category.addProgress(entry);
//                            populateScrolllist(this.cat.category.list2);
                            int n1 = StringUtil.parseInt(txtAmount.getTextInput().editText, 0);
                            startcraft(selected, n1);
                        }
                        break;
                    case 201:
                        n = StringUtil.parseInt(txtAmount.getText(), 0) + 1;
                        txtAmount.getTextInput().editText = "" + Math.min(100, n);
                        break;
                    case 202:
                        n = StringUtil.parseInt(txtAmount.getText(), 0) - 1;
                        txtAmount.getTextInput().editText = "" + Math.max(0, n);
                        break;
                    case 203:
                        CraftingRecipe recipe = this.selected.recipe;
                        Player p = Game.instance.getPlayer();
                        if (p != null) {
                            
                            int nMax = cat.getCraftingManager().calcMaxAmount(recipe, (SlotsInventoryBase)p.getSlots(0));
                            txtAmount.getTextInput().editText = "" + Math.min(100, nMax);
                        }
                        break;
                }
            }
        }
        return true;
    }
    private void startcraft(RecipeSlot selected, int n1) {
        if (n1 <= 0) {
            return;//idiot!
        }
        CraftingRecipe recipe = selected.recipe;
        CraftingCategory category = recipe.getCategory();
        int id = recipe.getId();
        Game.instance.sendPacket(new PacketCCrafting(category.getId(), id, 1, n1));
    }
    public void updateProgress() {
        for (int i = 0; i < this.catButtons.size(); i++) {
            GuiButtonCat n = this.catButtons.get(i);
            CraftingManagerClient mgr = n.getCraftingManager();
            if (mgr == null) continue;
            GuiCraftingProgressEntry ui = mgr.getGuiElement();
            if (mgr.isRunning()||mgr.isFinished()) {
                if (!this.scr2.hasElement(ui)) {
                    this.scr2.add(ui);
                }
            } else {
                this.scr2.remove(ui);
            }
            layoutList();
        }
    }
    private void layoutList() {
        int y = 0;
        int h = 70;
        int x = 0;
        Collections.sort(this.scr2.buttons, new Comparator<AbstractUI>() {

            @Override
            public int compare(AbstractUI o1, AbstractUI o2) {
                GuiCraftingProgressEntry s1 = (GuiCraftingProgressEntry) o1;
                GuiCraftingProgressEntry s2 = (GuiCraftingProgressEntry) o2;
                return Long.compare(s1.getEndTime(), s2.getEndTime());
            }
        });
        for (int i = 0; i < this.scr2.buttons.size(); i++) {
            GuiCraftingProgressEntry s = (GuiCraftingProgressEntry) this.scr2.buttons.get(i);
            s.id=i;
            s.setPos(x, y);
//            x+=5;
            s.setSize(this.scr2.width-13, h);
            y += h+2;
            s.initGui(true);
        }
    }
    public void onRemoteUpdate(CraftingManagerClient craftingManagerClient, int action) {
        updateProgress();
    }
    private void populateScrolllist(List<GuiCraftingProgressEntry> list) {

        int y = 0;
        int h = 42;
        int x = 0;
        for (int i = 0; i < list.size(); i++) {
            GuiCraftingProgressEntry s = list.get(i);
            s.id=i;
            s.setPos(x, y);
//            x+=5;
            s.setSize(this.scr2.width-13, h);
            y += h+2;
            this.scr2.add(s);
            s.initGui(false);
        }
        if (this.selected2 == null && list.size() > 0) {
            this.selected2 = list.get(0);
        }
    }
    public void renderSlotOverlay(Slot s, float posx, float posy) {
        if (s instanceof SlotStock) {
            int stackSize = s.getItem().getSize();
            int w2 = GameMath.round(Engine.itemRender.font.getStringWidth(""+stackSize));
            float w = s.w-inset*2;
            int color = 0x60f060;
            if (stackSize < ((SlotStock)s).stackReq.getSize()) {
                color = 0xf06060;
            }
            Engine.itemRender.font.drawString(""+stackSize, posx+s.x+inset+w-w2-1, posy+s.y+inset+w+2, color, true, 1.0f);
            return;
        }
        BaseStack stack = s.getItem();
        if (stack != null) {
            float inset = 4;
            Shaders.textured.enable();
            Engine.itemRender.drawItemOverlay(stack, posx+s.x+inset, posy+s.y+inset+0, s.w-inset*2, s.w-inset*2);
        }
    }
    static class PreviewSlots extends Slots {

        private CraftingRecipe recipe;
        private BaseInventory baseInv;
        private BaseInventory playerInv;

        public PreviewSlots(int id, GuiBG bg, CraftingRecipe recipe, PlayerInventory playerInv, BaseInventory baseInventory) {
            super(id);
            this.recipe = recipe;
            this.baseInv = baseInventory;
            this.playerInv = playerInv;
            int xPos = bg.posX+(bg.width-slotW-20)/2;
            for (int i = 0; i < recipe.getIn().length; i++) {
                addSlot(new SlotPreview(this, recipe.getIn()[i], i, xPos-bg.width/4, bg.posY+45+i*(slotw+5), slotw));
                SlotStock st = new SlotStock(this, recipe.getIn()[i], playerInv, i, xPos+bg.width/4, bg.posY+45+i*(slotw+5), slotw);
                addSlot(st);
                st.update();
            }
            int w2 = slotW+(recipe.getIn().length>1?5:0)+20;
             xPos = bg.posX+(bg.width-(recipe.getIn().length*w2))/2;
            for (int i = 0; i < recipe.getOut().length; i++) {
                addSlot(new SlotPreview(this, recipe.getOut()[i], i, xPos+i*slotw, bg.posY+220, slotw));
            }
        }
        public void update() {
            for (Slot b : slots) {
                if (b instanceof SlotStock) {
                    ((SlotStock)b).update();
                }
            }
        }

        @Override
        public BaseStack slotClicked(Slot s, int button, int action) {
            return null;
        }
        
    }
    public void setRecipe(RecipeSlot element) {
        selected = element;
        showButtons(true);
        CraftingRecipe recipe = selected.recipe;
        int inventory = element.id-100;
        PlayerSelf p = Game.instance.getPlayer();
        if (p != null) {
            p.getInv(1+inventory);
            this.slots = new PreviewSlots(2, bg, recipe, (PlayerInventory) p.getInv(0), p.getInv(1+inventory));
        } else {
            this.slots = new PreviewSlots(2, bg, recipe, null, null);
        }
    }
    private void showButtons(boolean b) {
        txtAmount.setDisableDraw(b);
        btnCraft.setDisableDraw(b);
        btnDecr.setDisableDraw(b);
        btnIncr.setDisableDraw(b);
        btnMax.setDisableDraw(b);
    }
    public boolean onWheelScroll(double xoffset, double yoffset) {
        if (this.scr2.mouseOver(Mouse.getX()-mouseOffsetX(), Mouse.getY()-mouseOffsetY())) {
            return this.scr2.onWheelScroll(xoffset, yoffset);
        }
        return this.cat.getScrollList().onWheelScroll(xoffset, yoffset) || this.scr2.onWheelScroll(xoffset, yoffset);
    }
    @Override
    public void submit(TextInput textInput) {
    }
    @Override
    public void onEscape(TextInput textInput) {
    }
}
