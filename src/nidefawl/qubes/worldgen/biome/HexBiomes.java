/**
 * 
 */
package nidefawl.qubes.worldgen.biome;

import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.biome.BiomeColor;
import nidefawl.qubes.hex.*;
import nidefawl.qubes.texture.TextureUtil;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.IWorldSettings;
import nidefawl.qubes.world.World;
import nidefawl.qubes.world.WorldServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public abstract class HexBiomes extends HexagonGridStorage<HexBiome> implements IBiomeManager {
    public static class HexBiomeEnd extends HexBiome {
        public HexBiomeEnd(HexagonGridStorage<HexBiome> grid, int x, int z) {
            super(grid, x, z);
            this.biome = Biome.MEADOW_GREEN;
        }
    }

    final World world;

    public HexBiomes(World world, long seed, IWorldSettings settings) {
        super(256, 32);
        this.world = world;
    }
    
    public Biome getBiome(int x, int z) {
        HexBiome b = (HexBiome) blockToHex(x, z);
        return b.biome;
    }
    
    @Override
    public int getWorldType() {
        return 1;
    }

    @Override
    public int getBiomeFaceColor(World world, int x, int y, int z, int faceDir, int pass, BiomeColor colorType) {
        

        HexBiome hex = (HexBiome) this.blockToHex(x, z);
        if (hex == null) {
            return 0;
        }
        double dist = hex.getDistanceCenter(x, z);
        int rgb = hex.biome.getFaceColor(colorType);
        float innerScale = colorType == BiomeColor.FOLIAGE2 ? 0.25f : 0.55f;
        if (dist < this.hwidth*innerScale) {
            return rgb;
        }
        double centerX = hex.getCenterX();
        double centerY = hex.getCenterY();
        
        double dist2 = dist-this.hwidth*innerScale;
        double scale = dist2/(this.hwidth*(1-innerScale));
        float fscale = (float) (scale > 1 ? 1 : scale);
        int n = hex.getClosesCorner(x, z);
        float fx = (float) this.getPointX(hex.x, hex.z, n);
        float fy = (float) this.getPointY(hex.x, hex.z, n);
        double angle = GameMath.getAngle(x-centerX, z-centerY, fx-centerX, fy-centerY);
        double scaleTangent = angle/(Math.PI/6.0);
        
        int offset = 5-n;
        int rgbHex1 = rgb;
        int rgbHex2 = rgb;
        {
            long k1 = HexagonGrid.offset(hex.x, hex.z, (offset+0)%6);
            HexBiome hex1 = (HexBiome) this.getPos(k1);
            if (hex1 != null) {
                rgbHex1 = hex1.biome.getFaceColor(colorType);
            }
            long k2 = HexagonGrid.offset(hex.x, hex.z, (offset+1)%6);
            HexBiome hex2 = (HexBiome) this.getPos(k2);
            if (hex2 != null) {
                rgbHex2 = hex2.biome.getFaceColor(colorType);
            }
        }
        int rgbCorner = TextureUtil.mix3RGB(rgb, rgbHex2, rgbHex1);
        if (scaleTangent > 0) {
            int rgbT = TextureUtil.mixRGB(rgb, rgbHex2, 0.5f);
            rgbCorner = TextureUtil.mixRGB(rgbCorner, rgbT, (float)scaleTangent);
//          return -1;
        } else if  (scaleTangent < 0) {
            int rgbT = TextureUtil.mixRGB(rgb, rgbHex1, 0.5f);
            rgbCorner = TextureUtil.mixRGB(rgbCorner, rgbT, ((float)-scaleTangent));
        }
        rgbCorner = TextureUtil.mixRGB(rgb, rgbCorner, fscale);
        return rgbCorner;
    }
}
