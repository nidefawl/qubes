package nidefawl.qubes.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import nidefawl.qubes.util.Color;
import nidefawl.qubes.util.ColorPalette;
    
public class BlockGroupLeaves extends BlockGroup {
//    public final static int NUM_COLORS = 8;
    public final static int NUM_COLORS = 2;
    public final static List<String> leaveNames = Lists.newArrayList(new String[] { 
            "acacia", "birch", "cocoa", "coconut", "ebony", "mahagoni", "oak", "pine", "redwood", "spruce", "walnut", "willow"
    });
    public static final ColorPalette COLOR_PALETTE = new ColorPalette();
    final public Block acacia;
    final public Block birch;
    final public Block oak;
    private HashMap<String, List<BlockLeaves>> blocksColored = Maps.newHashMap();
    public BlockGroupLeaves() {
        int nBlock = 0;
        float colHue = 0.12f;
        for (String s : leaveNames) {
            String blockIdStr = s+"_leaves";
            Block leaves = new BlockLeaves(blockIdStr);
            leaves.setTextures("leaves/"+s);
            addBlock(leaves);
            if (nBlock == 0) {
                leaves.setTextures("leaves/acacia", "leaves/acacia_round");
            } else if (nBlock == 1) {
                leaves.setTextures("leaves/birch", "leaves/birch_round");
            }
            ArrayList<BlockLeaves> list = Lists.newArrayList();

            float colSat = 0.63f;
            float colLight = 0.35f;
            for (int i = 0; i < NUM_COLORS; i++) {
                String blockColoredIdStr = s+"_leaves_color_"+i;
                int rgbInit = Color.HSLtoRGB(colHue, colSat, colLight);
                int n = (int)COLOR_PALETTE.getOrSetColor(blockColoredIdStr, rgbInit);
                BlockLeaves leavesColored = new BlockLeaves(blockColoredIdStr, true, n);
                leavesColored.setTextures("leaves/"+s);
                list.add(leavesColored);
                addBlock(leavesColored);
                if (nBlock == 0) {
                    leavesColored.setTextures("leaves/acacia", "leaves/acacia_round");
                } else if (nBlock == 1) {
                    leavesColored.setTextures("leaves/birch", "leaves/birch_round");
                }
//                colLight += 0.08f;
//                colSat += 0.05f;
                colLight += 0.0f;
                colSat += 0.3f;
//                colHue += 1.0f/(float)(leaveNames.size()*NUM_COLORS);
                colHue += 0.012f;
            }
            blocksColored.put(blockIdStr, list);
//            colHue += 1.0f/(float)(leaveNames.size()+1);
            nBlock++;
        }
        acacia = getBlocks().get(0);
        birch = getBlocks().get(1);
        oak = getBlocks().get(6);
//        acacia.setTextures("leaves/acacia", "leaves/acacia_round");
//        birch.setTextures("leaves/birch", "leaves/birch_round");
    }
    public void updateColors() {
        for (String s : leaveNames) {
            String blockIdStr = s+"_leaves";
            List<BlockLeaves> list = blocksColored.get(blockIdStr);
            for (int i = 0; i < NUM_COLORS; i++) {
                BlockLeaves b = list.get(i);
                String blockColoredIdStr = b.getName();
                int n = (int)COLOR_PALETTE.getColor(blockColoredIdStr, -1);
                b.setOverrideColor(n);
            }
        }
    }
    @Override
    public List<String> getNames() {
        return leaveNames;
    }

}
