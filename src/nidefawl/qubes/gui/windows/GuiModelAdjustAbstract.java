package nidefawl.qubes.gui.windows;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL40;

import com.google.common.collect.Lists;

import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.*;
import nidefawl.qubes.gui.*;
import nidefawl.qubes.gui.controls.*;
import nidefawl.qubes.gui.controls.ComboBox.ComboBoxList;
import nidefawl.qubes.gui.windows.GuiWindow;
import nidefawl.qubes.models.*;
import nidefawl.qubes.models.qmodel.QModelProperties;
import nidefawl.qubes.models.qmodel.animation.QModelAction;
import nidefawl.qubes.models.render.*;
import nidefawl.qubes.network.packet.PacketCSetProperty;
import nidefawl.qubes.shader.Shaders;
import nidefawl.qubes.shader.UniformBuffer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Project;
import nidefawl.qubes.vec.Matrix4f;

public abstract class GuiModelAdjustAbstract extends GuiWindow {
	static int nextID = 50;
	public static class GuiModelView extends GuiModelAdjustAbstract {
        @Override
        boolean isPlayerAdjust() {
            return false;
        }
    }
	public static class GuiPlayerAdjust extends GuiModelAdjustAbstract {
        @Override
        boolean isPlayerAdjust() {
            return true;
        }
    }
	abstract boolean isPlayerAdjust();
	static class Setting {
		Object[] vals = new String[0];
		ComboBox box;
		Button button1;
		Button button2;
		int curVal = 0;
		GuiModelAdjustAbstract gui;

		public Setting() {
		}

		public Setting(GuiModelAdjustAbstract g, String string, Object current, Object[] vals) {
			this.gui = g;
			this.box = new ComboBox(g, nextID++, string);
			this.box.setValue(current);
			this.box.titleLeft = true;
			this.button1 = new Button(this.box.id, "<");
			this.button2 = new Button(this.box.id, ">");
			this.vals = vals;
		}

		void callback(int id) {
		};
	}

	static class ModelSettingControl extends Setting {

		private ModelOption option;

		/**
		 * @param string
		 * @param g
		 * @param string2
		 */
        public ModelSettingControl(GuiModelAdjustAbstract g, ModelOption option) {
            this(g, option, option.getName());
        }
		public ModelSettingControl(GuiModelAdjustAbstract g, ModelOption option, String name) {
			this.gui = g;
			this.option = option;
			this.curVal = option.getDefaultVal();
			this.vals = option.getOptions();
			this.box = new ComboBox(g, nextID++, name);
			this.box.setValue(option.getTextVal(this.curVal));
			this.box.titleLeft = true;
			this.button1 = new Button(this.box.id, "<");
			this.button2 = new Button(this.box.id, ">");
//			callback(this.curVal);
		}

		void callback(int id) {
			if (id >= 0) {
				curVal = id;
				this.box.setValue(option.getTextVal(this.curVal));
	            if (this.gui.isPlayerAdjust()) {
	                if (Game.instance!=null) {
	                    Game.instance.sendPacket(new PacketCSetProperty(option.getId(), curVal));
	                }
	            } else {
	                this.gui.properties.options.put(option.getId(), curVal);
	            }
			}
		}
	}

	static class ModelActionList extends Setting {

		private EntityModel entityModel;
		private int idx;

		/**
		 * @param string
		 * @param g
		 * @param string2
		 */
		public ModelActionList(GuiModelAdjustAbstract g, EntityModel entityModel, int idx) {
			this.gui = g;
			this.entityModel = entityModel;
			this.curVal = 0;
			this.idx = idx;
			List<QModelAction> actions = entityModel.getActions();

			String[] list = new String[actions.size()];
			for (int i = 0; i < list.length; i++) {
				list[i] = actions.get(i).name;
			}
			this.vals = list;
			this.box = new ComboBox(g, nextID++, "Action #" + idx);
			this.box.setValue(list.length == 0 ? "" : list[this.curVal]);
			this.box.titleLeft = true;
			this.button1 = new Button(this.box.id, "<");
			this.button2 = new Button(this.box.id, ">");
			callback(this.curVal);
		}

		void callback(int id) {
			if (id >= 0) {
				curVal = id;
				List<QModelAction> list = this.entityModel.getActions();
				QModelAction act = !list.isEmpty() && list.size() > curVal ? list.get(curVal) : null;
				if (act == null) {
					this.box.setValue("");
				} else
					this.box.setValue(act.name);

				this.gui.properties.setAction(this.idx, act);
			}
		}
	}

	private Button reload;
	List<Setting> list = Lists.newArrayList();
	List<Setting> listDyn = Lists.newArrayList();
	private Setting testSetting;

	EntityModel entityModel;

	int modelidx = 0;
    public QModelProperties properties = new QModelProperties();

	public GuiModelAdjustAbstract() {
	}

	private void reloadModel() {

		try {
			this.entityModel = EntityModel.models[this.modelidx];
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void setModel(EntityModel entityModel, QModelProperties config) {
		for (Setting s : this.list) {
			this.remove(s.box);
			this.remove(s.button1);
			this.remove(s.button2);
		}
		this.list.removeAll(this.listDyn);
		this.listDyn.clear();
		if (isPlayerAdjust()) {
            EntityModelPlayer p = (EntityModelPlayer)entityModel;
            PlayerSelf self = Game.instance!=null?Game.instance.getPlayer():null;
            if (self != null) {
//                config = this.curConfig = self.getProperties();
                
                System.out.println("this.curConfig = "+this.properties);
                System.out.println("texFace "+this.properties.options.get(p.texFace.getId()));
                System.out.println(2+" = "+this.properties.options.get(2));
            }

		    config.setAction(0, entityModel.getIdle());
            config.setAction(1, null);
            listDyn.add(new ModelSettingControl(this, p.texFace, "Face"));
            listDyn.add(new ModelSettingControl(this, p.texSkin, "Cloth"));
            listDyn.add(new ModelSettingControl(this, p.modelSize, "Body shape"));
            listDyn.add(new ModelSettingControl(this, p.modelHair, "Hair"));
            listDyn.add(new ModelSettingControl(this, p.texHair, "Hair color"));
            if (p.isMale) {
                listDyn.add(new ModelSettingControl(this, p.modelBeard, "Beard"));
                listDyn.add(new ModelSettingControl(this, p.texBeard, "Beard color"));
            }
            if (self != null) {
                for (Setting c : this.listDyn) {
                    ModelSettingControl msetting = (ModelSettingControl) c;
                    c.curVal = self.getEntityProperties().getOption(msetting.option.getId(), 0);
                    c.box.setValue(msetting.option.getTextVal(c.curVal));
                }
            }
		} else {
	        for (final ModelOption option : entityModel.getModelOptions()) {
	            listDyn.add(new ModelSettingControl(this, option));
	        }
	        listDyn.add(new ModelActionList(this, entityModel, 0));
	        listDyn.add(new ModelActionList(this, entityModel, 1));
		}
		this.list.addAll(this.listDyn);
		int he = layout();
	}

	@Override
	public void initGui(boolean first) {
		setSize(340, 600);
		setPos(20, 20);
		this.clearElements();
		this.list.clear();
		int w1 = this.width / 2;
		int h = 30;
		final List<String> l = Lists.newArrayList();
		for (int i = 0; i < EntityModel.HIGHEST_MODEL_ID; i++) {
			l.add(EntityModel.models[i].getName());
		}
        PlayerSelf self = isPlayerAdjust()&&Game.instance!=null?Game.instance.getPlayer():null;
		if (isPlayerAdjust()) {
	        String[] arr = new String[] { "male", "female" };
	        String cur = EntityModel.models[0].getName();
            list.add((this.testSetting = new Setting(this, "Gender", cur, arr) {
                void callback(int id) {
                    if (id >= 0) {
                        this.box.setValue(l.get(id));
                        this.curVal = id;
                        if (GuiModelAdjustAbstract.this.isPlayerAdjust()) {
                            if (Game.instance!=null) {
                                Game.instance.sendPacket(new PacketCSetProperty(100, id));
                            }
                        } else {
                            GuiModelAdjustAbstract.this.setModel(id);
                        }
                    }
                }
            }));
            if (self != null) {
                int gender = self.getEntityProperties().getOption(100, 0);
                this.testSetting.box.setValue(l.get(gender));
                this.testSetting.curVal = gender;
            }
		} else {
	        String[] arr = l.toArray(new String[l.size()]);
	        String cur = EntityModel.models[0].getName();
            list.add((this.testSetting = new Setting(this, "Select model", cur, arr) {
                void callback(int id) {
                    if (id >= 0) {
                        this.box.setValue(l.get(id));
                        this.curVal = id;
                        GuiModelAdjustAbstract.this.setModel(id);
                    }
                }
            }));
		}
        if (!isPlayerAdjust()) {
            reload = new Button(4, "Reload");
            int he = layout();
            this.add(reload);
        }
        setModel(0);
	}

	private int layout() {
		int w = 390;
		int w1 = w / 3;
		int w2 = w1 * 2;
		int h = 30;

		int left = w / 2 - 10;
		int y = 10 + 40;
		int leftB = 10;
		if (isPlayerAdjust()) {
		    y+=20;
		    left+=10;
		    leftB+=10;
		}
		int comboH = Gui.FONT_SIZE_BUTTON + 4;
		for (Setting s : list) {
			s.box.setPos(left, y);
			s.box.setSize(w2 - 110, comboH);
			s.box.titleWidth = left - leftB;
			s.button1.setPos(s.box.right() + 10, y);
			s.button1.setSize(Gui.FONT_SIZE_BUTTON + 2, comboH);
			s.button2.setPos(s.button1.right() + 10, y);
			s.button2.setSize(Gui.FONT_SIZE_BUTTON + 2, comboH);
			y += Gui.FONT_SIZE_BUTTON + 2 + 10;
			this.add(s.box);
			this.add(s.button1);
			this.add(s.button2);
		}
		y += 20;

		y = Math.max(320, y);
		if (reload != null) {
	        y += 25;
		    reload.setPos(30, y);
		    reload.setSize(w - 40, h);
	        y += h;
	        y += 20;
		}
		setSize(w + 370, y);
		return y;
	}

	public void render(float fTime, double mX, double mY) {
		renderModel(fTime, mX, mY);
        PlayerSelf self = Game.instance!=null?Game.instance.getPlayer():null;
        if (self != null) {
            int modelIdx = self.getEntityProperties().getOption(100, 0);
            if (this.modelidx != modelIdx) {
                setModel(modelIdx);
            }
        }
        super.renderButtons(fTime, mX, mY);
	}
	
    private void renderModel(float fTime, double mX, double mY) {
        Engine.getSceneFB().bind();
        Engine.getSceneFB().clearFrameBuffer();
        Engine.getSceneFB().clearColorBlack();
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        for (int i = 0; i < 3; i++) {
            GL40.glBlendFuncSeparatei(1 + i, GL_ONE, GL_ZERO, GL_ONE, GL_ZERO);
        }
        QModelBatchedRender renderBatched = Engine.renderBatched;
        float absTimeInSeconds = GameBase.absTime;
        renderBatched.reset();
        renderBatched.setRenderer(QModelBatchedRender.RENDERER_SCREEN_MODELVIEWER);

        BufferedMatrix temp = Engine.getTempMatrix();
        BufferedMatrix temp2 = Engine.getTempMatrix2();
        temp.setIdentity();
        temp2.setIdentity();
        int w = 350;
        int h = this.height - 30;
        float aspect = w / (float) h;
        //      Engine.setViewport(posX+this.width-w, Game.displayHeight-posY-this.height, w, h);
        Engine.setViewport(0, 0, w, h);

        Project.fovProjMat(70, aspect, 0.2f, 200, temp);
        //        temp2.rotate(90*GameMath.P_180_OVER_PI, 0, 0, 1);
        float zTranslate = -2.5f;
        if (isPlayerAdjust()) {
            zTranslate = -2f;
            
        }
        temp2.translate(0, -1, zTranslate);
        temp2.rotate(90 * GameMath.PI_OVER_180, 0, 1, 0);
        Matrix4f.mul(temp, temp2, temp);
        temp.update();
        renderBatched.setForwardRenderMVP(temp);
        renderBatched.setModel(entityModel);
        this.properties.rot.x = 0;
        this.properties.rot.y = -90;
        this.properties.pos.set(0,0,0);
        if (isPlayerAdjust()) {
            PlayerSelf p = Game.instance != null ? Game.instance.getPlayer() : null;
            if (p != null) {
                p.adjustRenderProps(properties, fTime);
            }
        }
        this.entityModel.setPose(renderBatched, this.properties, absTimeInSeconds, fTime);

        renderBatched.render(fTime);
        renderBatched.reset();

        Engine.setDefaultViewport();
        GL40.glBlendFuncSeparatei(0, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Engine.checkGLError("Pass0");
        FrameBuffer.unbindFramebuffer();
        Shaders.texturedAlphaTest.enable();
        int xOffset = 400;
        Engine.pxStack.push(posX + xOffset, posY + 20, 4);
        float ftexW = w / (float) Game.displayWidth;
        float ftexH = h / (float) Game.displayHeight;
        GL.bindTexture(GL13.GL_TEXTURE0, GL11.GL_TEXTURE_2D, Engine.getSceneFB().getTexture(0));
        Tess.instance.setColorF(-1, 1.0f);
        Tess.instance.add(w, h, 0, ftexW, 0);
        Tess.instance.add(w, 0, 0, ftexW, ftexH);
        Tess.instance.add(0, 0, 0, 0, ftexH);
        Tess.instance.add(0, h, 0, 0, 0);
        Tess.instance.drawQuads();
        Engine.pxStack.pop();
        Engine.getSceneFB().bind();
        Engine.getSceneFB().clearFrameBuffer();
        FrameBuffer.unbindFramebuffer();
    }

	@Override
	public String getTitle() {
		return "Adjust Character";
	}

	public boolean onGuiClicked(AbstractUI element) {
		if (element instanceof CheckBox) {

			((CheckBox) element).checked = !((CheckBox) element).checked;
		}
		for (int i = 0; i < this.list.size(); i++) {
			final Setting s = this.list.get(i);
			if (s.button1 == element) {
				s.curVal--;
				if (s.curVal < 0) {
					s.curVal = s.vals.length - 1;
				}
				s.callback(s.curVal);
				return true;
			}
			if (s.button2 == element) {
				s.curVal++;
				if (s.curVal >= s.vals.length) {
					s.curVal = 0;
				}
				s.callback(s.curVal);
				return true;
			}
			if (s.box == element) {
				if (s.box.onClick(this)) {
					setPopup(new ComboBox.ComboBoxList(new ComboBox.CallBack() {
						@Override
						public void call(ComboBoxList c, int id) {

						    GuiModelAdjustAbstract.this.setPopup(null);
							if (id < 0 || id >= s.vals.length)
								return;
							s.box.setValue(s.vals[id]);
							s.callback(id);
						}
					}, this, s.box, s.vals));
				}
			}
		}
		if (element == reload) {
			EntityModelManager.getInstance().reload();
		}
		return true;
	}

	@Override
	public boolean onKeyPress(int key, int scancode, int action, int mods) {
	    if (!isPlayerAdjust()) {

	        if (action == GLFW.GLFW_PRESS)
	            switch (key) {
	            case GLFW.GLFW_KEY_KP_ADD:
	                setModel(this.modelidx + 1);
	                return true;
	            case GLFW.GLFW_KEY_KP_SUBTRACT:
	                setModel(this.modelidx - 1);
	                return true;
	            }
	    }
		return super.onKeyPress(key, scancode, action, mods);
	}

	public void setModel(int i) {
		modelidx = i;
		if (modelidx < 0) {
			modelidx = EntityModel.HIGHEST_MODEL_ID;
		}
		if (modelidx > EntityModel.HIGHEST_MODEL_ID) {
			modelidx = 0;
		}
		reloadModel();
		setModel(this.entityModel, this.properties);
	}

}
