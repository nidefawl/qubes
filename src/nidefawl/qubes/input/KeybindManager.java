package nidefawl.qubes.input;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;

import nidefawl.qubes.Game;
import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.config.AbstractYMLConfig;
import nidefawl.qubes.config.InvalidConfigException;
import nidefawl.qubes.config.WorkingEnv;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerSelf;
import nidefawl.qubes.gl.Engine;
import nidefawl.qubes.gl.GLDebugTextures;
import nidefawl.qubes.gui.GuiChatInput;
import nidefawl.qubes.gui.GuiGameMenu;
import nidefawl.qubes.gui.GuiSelectBlock;
import nidefawl.qubes.gui.crafting.GuiCraftingSelect;
import nidefawl.qubes.gui.windows.*;
import nidefawl.qubes.gui.windows.GuiModelAdjustAbstract.GuiModelView;
import nidefawl.qubes.gui.windows.GuiModelAdjustAbstract.GuiPlayerAdjust;
import nidefawl.qubes.io.network.DataListType;
import nidefawl.qubes.models.BlockModelManager;
import nidefawl.qubes.models.ItemModelManager;
import nidefawl.qubes.network.packet.PacketCListRequest;
import nidefawl.qubes.render.gui.SingleBlockRenderAtlas;
import nidefawl.qubes.texture.array.BlockNormalMapArray;
import nidefawl.qubes.texture.array.BlockTextureArray;
import nidefawl.qubes.texture.array.ItemTextureArray;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.vec.Vector3f;
import nidefawl.qubes.world.World;

public class KeybindManager {
    protected static final int VERSION = 1;
    private static AbstractYMLConfig settings = new AbstractYMLConfig(true) {
        
        @Override
        public void setDefaults() {
        }
        
        @Override
        public void save() {
            for (Keybinding keybinding : keybindings) {
                setString(keybinding.getName(), "kb:"+keybinding.getKey());
            }
        }
        
        @Override
        public void load() throws InvalidConfigException {
            if (getInt("version", 0) == KeybindManager.VERSION)
            for (Keybinding keybinding : keybindings) {
                String s = getString(keybinding.getName(), "kb:"+keybinding.getKey());
                if (s.startsWith("kb:")&&s.length()>3) {
                    int kb = StringUtil.parseInt(s.substring(3), -9999);
                    if (kb != -9999) {
                        keybinding.setKey(kb);
                    }
                }
            }
        }
    };
    private static Keybinding kb_forward;
    private static Keybinding kb_backward;
    private static Keybinding kb_left;
    private static Keybinding kb_right;
    private static Keybinding kb_sneak;
    private static Keybinding kb_jump;
    public float strafe, forward;
    boolean grabbed = false;
    public int mX;
    public int mY;
    public boolean jump;
    public boolean sneak;

    static final Map<Integer, Keybinding> keyToKeyBinding = new MapMaker().makeMap();
    static final ArrayList<Keybinding> keybindings = Lists.newArrayList();
    static final ConcurrentMap<String, Keybinding> keybindingsStr = new MapMaker().makeMap();
    public static float SPEED_MODIFIER = 1.0f;
    static {

        kb_forward = new Keybinding("forward", GLFW.GLFW_KEY_W).setNoCallBack();
        kb_backward = new Keybinding("backward", GLFW.GLFW_KEY_S).setNoCallBack();
        kb_left = new Keybinding("left", GLFW.GLFW_KEY_A).setNoCallBack();
        kb_right = new Keybinding("right", GLFW.GLFW_KEY_D).setNoCallBack();
        kb_jump = new Keybinding("jump", GLFW.GLFW_KEY_SPACE).setNoCallBack();
        kb_sneak = new Keybinding("sneak", GLFW.GLFW_KEY_LEFT_SHIFT).setNoCallBack();
        addKeyBinding(kb_forward);
        addKeyBinding(kb_backward);
        addKeyBinding(kb_left);
        addKeyBinding(kb_right);
        addKeyBinding(kb_jump);
        addKeyBinding(kb_sneak);
    }
    /**
     * @param keybinding
     */
    public static void addKeyBinding(Keybinding keybinding) {
        keybindings.add(keybinding);
        keybindingsStr.put(keybinding.getName(), keybinding);
    }
    public static Keybinding getKeyBindingByName(String name) {
        return keybindingsStr.get(name);
    }
    public static void updateKeybindMap() {
        keyToKeyBinding.clear();
        for (Keybinding keybinding : keybindings) {
            if (keybinding.hasCallback()&&keybinding.getKey()!=-1)
                keyToKeyBinding.put(keybinding.getKey(), keybinding);
        }
    }

    public static Keybinding getKeyBinding(int key) {
        return keyToKeyBinding.get(key);
    }
    
    public void update(double mdX, double mdY) {
        this.strafe = 0;
        this.forward = 0;
        this.jump = false;
        this.sneak = false;
        boolean hasTextInput=(GuiContext.input != null && GuiContext.input.isFocusedAndContext());
        if (this.grabbed && (!hasTextInput)) {
            float mult = SPEED_MODIFIER;
            this.sneak=isKeyDown(kb_sneak.getKey());
            this.jump=isKeyDown(kb_jump.getKey());
            if (this.sneak) {
                mult*=0.3f;
            }
            if (isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL)) {
                mult = 0.05f;
            }
//            if (isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
//                mult = 0.01F;
            if (isKeyDown(kb_forward.getKey())) {
                this.forward += mult;
            }
            if (isKeyDown(kb_backward.getKey())) {
                this.forward-=mult;
            }
            if (isKeyDown(kb_right.getKey())) {
                this.strafe-=mult;
            }
            if (isKeyDown(kb_left.getKey())) {
                this.strafe+=mult;
            }
//            if (isKeyDown(GLFW.GLFW_KEY_LEFT_CONTROL))
//                mult = 0.03F;
            this.mX += mdX;
            this.mY += mdY;
        }
    }

    private boolean isKeyDown(int key) {
        return Keyboard.isKeyDown(key);
    }

    public void setGrabbed(boolean b) {
        if (this.grabbed != b) {
            this.mX = 0;
            this.mY = 0;
            this.strafe = 0;
            this.forward = 0;
            this.sneak = false;
            this.jump = false;
        }
        this.grabbed = b;
    }

    public boolean grabbed() {
        return this.grabbed;
    }

    public static void initKeybinds() {
        final Game game = Game.instance;

        addKeyBinding(new Keybinding("open_chat", GLFW.GLFW_KEY_ENTER) {
            public void onDown() {
                if (game.getWorld() != null)
                    game.showGUI(new GuiChatInput());
            }
        });
        addKeyBinding(new Keybinding("toggle_gamemode", GLFW.GLFW_KEY_M) {
            public void onDown() {
                game.toggleGameMode();
            }
        });
        addKeyBinding(new Keybinding("toggle_quarter_mode", GLFW.GLFW_KEY_Q) {
            public void onDown() {
                game.getSelection(0).toggleQuarterMode();
                game.getSelection(1).toggleQuarterMode();
            }
        });
        addKeyBinding(new Keybinding("toggle_noclip", GLFW.GLFW_KEY_H) {
            public void onDown() {
                PlayerSelf player = game.getPlayer();
                World world = game.getWorld();
                if (player != null && world != null) {
                    player.toggleFly();
                }
            }
        });
        addKeyBinding(new Keybinding("show_blocks", GLFW.GLFW_KEY_B) {
            public void onDown() {
                game.showGUI(new GuiSelectBlock());
            }
        });
        addKeyBinding(new Keybinding("show_inventory", GLFW.GLFW_KEY_I) {
            public void onDown() {
                GuiWindowManager.openWindow(GuiInventory.class);
            }
        });

        addKeyBinding(new Keybinding("show_crafting", GLFW.GLFW_KEY_O) {
            public void onDown() {
                GuiWindowManager.openWindow(GuiCraftingSelect.class);
            }
        });

        addKeyBinding(new Keybinding("show_look", GLFW.GLFW_KEY_L) {
            public void onDown() {
                GuiWindowManager.openWindow(GuiPlayerAdjust.class);
            }
        });
        addKeyBinding(new Keybinding("show_models", GLFW.GLFW_KEY_K) {
            public void onDown() {
                GuiWindowManager.openWindow(GuiModelView.class);
            }
        });
        addKeyBinding(new Keybinding("show_select_world", GLFW.GLFW_KEY_N) {
            public void onDown() {
                Game.instance.sendPacket(new PacketCListRequest(0, DataListType.WORLDS));
            }
        });
        addKeyBinding(new Keybinding("spawn_light", -1) {
            public void onDown() {
//                if (step+1 < edits.size()) {
//                    step++;
//                    edits.get(step).apply(world);
//                }
                Player player = game.getPlayer();
                World world = game.getWorld();
                if (player != null && world != null)
                    world.addLight(new Vector3f(player.pos).translate(0, 1, 0));
            }
        });
        addKeyBinding(new Keybinding("remove_lights", -1) {
            public void onDown() {
                World world = game.getWorld();
                if (world != null) {
                    for (int i = 0; i < 22; i++) {
    
                        world.removeLight(0);
                    }
                }
            }
        });
        addKeyBinding(new Keybinding("toggle_vr", GLFW.GLFW_KEY_F1) {
            public void onDown() {
                Game.instance.toggleVR();
            }
        });
        addKeyBinding(new Keybinding("show_chunk_grid", GLFW.GLFW_KEY_F2) {
            public void onDown() {
                Game.showGrid = !Game.showGrid;
            }
        });
        addKeyBinding(new Keybinding("show_debug_textures", GLFW.GLFW_KEY_F3) {
            public void onDown() {
                GLDebugTextures.setShow(!GLDebugTextures.show);
            }
        });
        addKeyBinding(new Keybinding("flush_renderers", GLFW.GLFW_KEY_F4) {
            public void onDown() {
                Engine.regionRenderer.reRender();
            }
        });
        addKeyBinding(new Keybinding("firstperson", GLFW.GLFW_KEY_F5) {
            public void onDown() {
                game.lastShaderLoadTime = System.currentTimeMillis();
                game.thirdPerson = !game.thirdPerson;
            }
        });
        addKeyBinding(new Keybinding("show_vr_controllers", GLFW.GLFW_KEY_F6) {
            public void onDown() {
                game.showControllers = !game.showControllers;
            }
        });
        addKeyBinding(new Keybinding("toggle_bindless", GLFW.GLFW_KEY_F7) {
            public void onDown() {
//                Engine.userSettingUseBindless=!Engine.userSettingUseBindless;
                Engine.regionRenderer.threadedCulling=!Engine.regionRenderer.threadedCulling;
                
            }
        });
        addKeyBinding(new Keybinding("vsync", GLFW.GLFW_KEY_F8) {
            public void onDown() {
                game.setVSync(!game.getVSync());
            }
        });
        addKeyBinding(new Keybinding("wireframe", GLFW.GLFW_KEY_F9) {
            public void onDown() {
                Engine.toggleWireFrame();
            }
        });
        addKeyBinding(new Keybinding("reload_textures", GLFW.GLFW_KEY_F10) {
            public void onDown() {
//                Engine.flushRenderTasks();
//                Engine.regionRenderer.resetAll();
//                Engine.toggleDrawMode();
                ItemTextureArray.getInstance().reload();
                BlockTextureArray.getInstance().reload();
                BlockNormalMapArray.getInstance().reload();
                SingleBlockRenderAtlas.getInstance().reset();
            }
        });
        addKeyBinding(new Keybinding("toggle_external_resources", GLFW.GLFW_KEY_F11) {
            public void onDown() {
//                toggleTiming = true;
                AssetManager.getInstance().toggleExternalResources();
            }
        });
        addKeyBinding((new Keybinding("show_menu", GLFW.GLFW_KEY_ESCAPE) {
            public void onDown() {
                if( game.getGui() == null && game.getWorld() != null)
                    game.showGUI(new GuiGameMenu());
            }
        }).setStatic());


        addKeyBinding(new Keybinding("toggle_debug_texture", GLFW.GLFW_KEY_P) {
            public void onDown() {
                GLDebugTextures.toggleDebugTex();
            }
        });

        addKeyBinding(new Keybinding("reload_models", -1) {
            public void onDown() {
                ItemModelManager.getInstance().reload();
                BlockModelManager.getInstance().reload();
            }
        });
        addKeyBinding(new Keybinding("show_colorpicker", GLFW.GLFW_KEY_KP_8) {
            public void onDown() {
                GuiWindowManager.openWindow(GuiColor.class);
            }
        });
        addKeyBinding(new Keybinding("spawn_random_lights", -1) {
            public void onDown() {
                PlayerSelf player = game.getPlayer();
                World world = game.getWorld();
                if (player != null && world != null) {
                    world.spawnLights(player.pos.toBlock());
                }
            }
        });
        addKeyBinding(new Keybinding("increase_clouds", GLFW.GLFW_KEY_KP_ADD) {
            public void onRepeat() {
                Engine.skyRenderer.increaseClouds();
            }
        });
        addKeyBinding(new Keybinding("decrease_clouds", GLFW.GLFW_KEY_KP_SUBTRACT) {
            public void onRepeat() {
                Engine.skyRenderer.decreaseClouds();
            }
        });
    }

    public static void load() {
        try {
            File f = new File(WorkingEnv.getConfigFolder(), "controls.yml");
            settings.load(f);
            if (!f.exists())
                settings.write(f);
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
        updateKeybindMap();
    }
    public static void saveBindings() {
        if (settings == null) return;
        try {
            settings.save();
            File f = new File(WorkingEnv.getConfigFolder(), "controls.yml");
            settings.write(f);
        } catch (InvalidConfigException e) {
            e.printStackTrace();
        }
    }
    public static Collection<Keybinding> getBindings() {
        return keybindings;
    }
}