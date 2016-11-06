package nidefawl.qubes.block;

import java.util.ArrayList;

import com.google.common.collect.Lists;

public class BlockGravel extends BlockStone {

    public BlockGravel(String string) {
        super(string);
        ArrayList<String> tex = Lists.newArrayList();
        for (int i = 0; i < 9; i++) {
            tex.add("ground/gravel_"+i);
        }
        String[] texarr = tex.toArray(new String[tex.size()]);
        
        setTextures(texarr);
        setTextureMode(BlockTextureMode.SUBTYPED_TEX_PER_TYPE);
        
    }

}
