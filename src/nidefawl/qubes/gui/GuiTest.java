package nidefawl.qubes.gui;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.font.ITextEdit;
import nidefawl.qubes.font.TextInput;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gui.controls.*;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.network.client.ClientHandler;

public class GuiTest extends Gui implements ITextEdit {
    
    private Button back;
    private CheckBox checkBox;
    private ComboBox combobox;
    final String[] vals = new String[16];
    private ColorPicker colorPick;
    private ProgressBar progress;
    private ScrollList scrolllist;
    private TextField txtAmount;
    

    @Override
    public void render(float fTime, double mX, double mY) {
        renderBackground(fTime, mX, mY, true, 0.7f);
        font.drawString("This is text...", this.posX+this.width / 2, this.posY+titleBarOffset+5, -1, true, 1, 2);
        Engine.pxStack.push(posX, posY, 4);
        this.colorPick.render(fTime, mX-posX, mY-posY);
        Engine.pxStack.pop();
        this.scrolllist.render(fTime, mX, mY);
        super.renderButtons(fTime, mX, mY);
    }
    @Override
    protected String getTitle() {
        return "test gui!";
    }
    
    @Override
    public void initGui(boolean first) {
        for (int i = 0; i < vals.length; i++) {
            vals[i] = "Option "+i;
        }
        System.out.println(Engine.getGuiWidth()+","+Engine.getGuiHeight());
        this.width = Engine.getGuiWidth()/2;
        this.height = Engine.getGuiHeight()/3*2;
        this.posX = Engine.getGuiWidth()/2-this.width/2;
        this.posY = Engine.getGuiHeight()/2-this.height/2;
        this.clearElements();
        int cbSize = Gui.FONT_SIZE_BUTTON+2;
        {
            back = new Button(1, "Back");
            back.setSize(260, 40);
            back.setPos(this.width/2-back.width/2, this.height-back.height-10);
            this.add(back);
        }
        {
            this.checkBox = new CheckBox(2, "Test checkbox");
            checkBox.setSize(cbSize, cbSize);
            checkBox.setPos(back.posX, back.posY-checkBox.height-10);
            checkBox.titleLeft = false;
            this.add(checkBox);
            
        }
        {
            this.combobox = new ComboBox(this, 3, "Test combobox");
            this.combobox.setValue(vals[0]);
            combobox.setSize(back.width, cbSize*2);
            combobox.setPos(checkBox.posX, checkBox.posY-combobox.height-10);
            combobox.titleWidth = 205;
            this.add(combobox);
        }
        {

            this.colorPick = new ColorPicker(this) {
                @Override
                public void onColorChange(int rgb2) {
                }
            };
            this.colorPick.setSize(back.width, 130);
            this.colorPick.setPos(combobox.posX, combobox.posY-this.colorPick.getPickerHeight());
            this.colorPick.initGui(first);
        }
        {

            this.progress = new ProgressBar();
            this.progress.setProgress(0);
            this.progress.setText("waiting");
            this.progress.setSize(back.width, (int) (cbSize*1.5f));
            this.progress.setPos(colorPick.posX, colorPick.posY-this.progress.height-10);
            this.add(progress);
        }
        {

            scrolllist = new ScrollList(this);
            this.scrolllist.setSize(back.width, 100);
            this.scrolllist.setPos(posX+progress.posX, posY+progress.posY-this.scrolllist.height-10);
            this.add(this.scrolllist.scrollbarbutton);
            for (int i = 0; i < 10; i++) {
                int h = 60;
                Button btn = new Button(10+i, "Scrolllist button "+i);
                btn.setSize(this.scrolllist.width-12, h-12);
                btn.setPos(2, 2+i*(h+10));
                this.scrolllist.add(btn);
            }
        }
        {
            this.txtAmount = new TextField(this, 0, "Textfield\nType here");
            this.txtAmount.zIndex=2;
            this.txtAmount.getTextInput().multiline = true;
            this.txtAmount.setSize(back.width, 200);
            this.txtAmount.setPos(this.progress.posX, this.progress.posY-this.scrolllist.height-10-this.txtAmount.height-20);
            this.add(txtAmount);
        }
    }
    @Override
    public void update() {
        super.update();
        this.scrolllist.update();
        updateProgress();
    }
    long startTime = System.currentTimeMillis();
    long endTime = System.currentTimeMillis()+(8000);
    private void updateProgress() {
        if (System.currentTimeMillis() > endTime) {
            this.progress.setProgress(1);
            this.progress.setText("Complete");
            if (System.currentTimeMillis() > endTime + 5000) {
                startTime = System.currentTimeMillis();
                endTime = System.currentTimeMillis() + (8000);
                this.progress.setProgress(0);
            }
        } else {
            long len = endTime-startTime;
            long passed = System.currentTimeMillis()-startTime;
            float progress = (float) (((double) passed)/((double) len));
            this.progress.setProgress(progress);   
        }
    
    }
    @Override
    public boolean onGuiClicked(AbstractUI element) {
        if (element instanceof CheckBox) {

            ((CheckBox)element).checked = !((CheckBox)element).checked;
        }
        if (element instanceof ComboBox) {
            if (((ComboBox) element).onClick(this)) {
                final Gui guiParent = this;
                final ComboBox guiParentBox = (ComboBox) element;
                setPopup(new ComboBox.ComboBoxList(new ComboBox.CallBack() {
                    @Override
                    public void call(ComboBoxList c, int id) {
                        guiParent.setPopup(null);
                        if (id < 0 || id >= vals.length)
                            return;
                        guiParentBox.setValue(vals[id]);
                    }
                }, this, ((ComboBox) element), vals));
            }
        }
        if (this.colorPick.hasElement(element)) {
            return true;
        }
        return super.onGuiClicked(element);
    }
    public boolean onKeyPress(int key, int scancode, int action, int mods) {
        if (super.onKeyPress(key, scancode, action, mods)) {
            return true;
        }
        return true;
    }
    public boolean onMouseClick(int button, int action) {
        return super.onMouseClick(button, action) || this.scrolllist.onMouseClick(button, action);
    }
    public boolean onWheelScroll(double xoffset, double yoffset) {
        if (this.scrolllist.hovered)
        this.scrolllist.onWheelScroll(xoffset, yoffset);
        return true;
    }
    @Override
    public void submit(TextInput textInput) {
    }
    @Override
    public void onEscape(TextInput textInput) {
    }
};