/**
 * 
 */
package nidefawl.qubes.biome;

import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.biomes.HexBiomesServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class Biome {
    public final static Biome[] biomes = new Biome[256];
    public static int maxBiome;
    public final static Biome MEADOW_GREEN = new BiomeMeadow(0)
            .setColor(BiomeColor.GRASS, 0x4f923b)
            .setColor(BiomeColor.LEAVES, 0x4A7818)
            .setColor(BiomeColor.FOLIAGE, 0x408A10)
            .setColor(BiomeColor.FOLIAGE2, 0x64B051)
            .setDebugColor(0x32dd32);
    public final static Biome MEADOW_BLUE = new BiomeMeadow(1)
            .setColor(BiomeColor.GRASS, 0x48AB73)
            .setColor(BiomeColor.LEAVES, 0x48AB73)
            .setColor(BiomeColor.FOLIAGE, 0x48AB73)
            .setColor(BiomeColor.FOLIAGE2, 0x489363)
            .setDebugColor(0x3232dd);
    public final static Biome MEADOW_RED = new BiomeMeadow(2)
            .setColor(BiomeColor.GRASS, 0xE68245)
            .setColor(BiomeColor.LEAVES, 0xB38947)
            .setColor(BiomeColor.FOLIAGE, 0xC88245)
            .setColor(BiomeColor.FOLIAGE2, 0xaa0000)
            .setDebugColor(0x3232dd);
    public final static Biome DESERT = new BiomeDesert(3)
            .setColor(BiomeColor.GRASS, 0x91AB48)
            .setColor(BiomeColor.LEAVES, 0x91AB48)
            .setColor(BiomeColor.FOLIAGE, 0x91AB48)
            .setColor(BiomeColor.FOLIAGE2, 0xEDB12F)
            .setDebugColor(0xdd3232);
    public final static Biome DESERT_RED = new BiomeDesertRed(4)
            .setColor(BiomeColor.GRASS, 0xAB9448)
            .setColor(BiomeColor.LEAVES, 0x702B00)
            .setColor(BiomeColor.FOLIAGE, 0xAB9448)
            .setColor(BiomeColor.FOLIAGE2, 0xEDB12F)
            .setDebugColor(0xdd32dd);
    public final static Biome ICE = new BiomeIce(5)
            .setColor(BiomeColor.GRASS, 0x408019)
            .setColor(BiomeColor.LEAVES, 0x4A7818)
            .setColor(BiomeColor.FOLIAGE, 0x408A10)
            .setColor(BiomeColor.FOLIAGE2, 0x64B051)
            .setDebugColor(0xaaaaaa);
    public final static Biome MEADOW_GREEN2 = new BiomeMeadow(6)
            .setColor(BiomeColor.GRASS, 0x408019)
            .setColor(BiomeColor.LEAVES, 0x4A7818)
            .setColor(BiomeColor.FOLIAGE, 0x408A10)
            .setColor(BiomeColor.FOLIAGE2, 0x64B051)
            .setDebugColor(0x32dd32);
    public int color;
    public int colorFoliage;
    public int colorFoliage2;
    public int colorLeaves;
    public int colorGrass;
    public int id;
    /**
     * 
     */
    public Biome(int id) {
        this.id = id;
        biomes[id] = this;
        if (maxBiome < id + 1)
            maxBiome = id + 1;
        colorGrass = 0x408019;
        colorLeaves = 0x4A7818;
        colorFoliage = 0x408A10;
        colorFoliage2 = 0x6A9C49;
    }
    public Biome setColor(BiomeColor type, int rgb) {
        switch (type) {
            case FOLIAGE:
                this.colorFoliage = rgb;
                break;
            case GRASS:
                this.colorGrass = rgb;
                break;
            case LEAVES:
                this.colorLeaves = rgb;
                break;
            case FOLIAGE2:
                this.colorFoliage2 = rgb;
                break;
            default:
                break;
        }
        return this;
    }

    /**
     * @param i
     * @return
     */
    public Biome setDebugColor(int color) {
        this.color = color;
        return this;
    }

    /**
     * @param id
     * @return
     */
    public static Biome get(int id) {
        if (id < 0 || id >= biomes.length)
            return MEADOW_GREEN;
        if (biomes[id] == null) {
            System.err.println("null biome");
        }
        return biomes[id];
    }

    /**
     * @param colorType
     * @return
     */
    public int getFaceColor(BiomeColor colorType) {
        switch (colorType) {
            case FOLIAGE:
                return this.colorFoliage;
            case GRASS:
                return this.colorGrass;
            case LEAVES:
                return this.colorLeaves;
            case FOLIAGE2:
                if (this == MEADOW_RED)
                    return 0xB3CC58;
                if (this == MEADOW_BLUE)
                    return 0x489363;
                return this.colorFoliage2;
            default:
                break;
        }
        return this.colorGrass;
    }


    public int getStone(WorldServer world, int x, int y, int z, HexBiome hex, Random rand) {
        Block b = hex.biome.getStone();
        if (b != null)
            return b.id;
        HexBiomesServer biomes = (HexBiomesServer) world.biomeManager;
        float centerX = (float) biomes.getCenterX(hex.x, hex.z);
        float centerY = (float) biomes.getCenterY(hex.x, hex.z);
        float fx = (float) biomes.getPointX(hex.x, hex.z, 0);
        float fy = (float) biomes.getPointY(hex.x, hex.z, 0);
        rand.setSeed(x * 89153 ^ z * 31 + y);
        int randRange = 8;
        float randX = -rand.nextInt(randRange)+rand.nextInt(randRange);
        float randZ = -rand.nextInt(randRange)+rand.nextInt(randRange);
        double angle = GameMath.getAngle(x-centerX+randX, z-centerY+randZ, fx-centerX, fy-centerY);
        int scaleTangent = (int) (6+6*(angle/Math.PI)); // 0 == points to corner, 1/-1 == points to half of side
        if (scaleTangent < 3) {
            return Block.stones.basalt.id;
        }
        if (scaleTangent < 6) {
            return Block.stones.diorite.id;
        }
        if (scaleTangent < 9) {
            return Block.stones.marble.id;
        }
        return Block.stones.granite.id;
    }

    public int getOre(WorldServer world, int x, int y, int z, HexBiome hex, Random rand) {
        return Block.ores.getFirst().id;
//        Block b = hex.biome.getStone();
//        if (b != null)
//            return b.id;
//        HexBiomesServer biomes = (HexBiomesServer) world.biomeManager;
//        float centerX = (float) biomes.getCenterX(hex.x, hex.z);
//        float centerY = (float) biomes.getCenterY(hex.x, hex.z);
//        float fx = (float) biomes.getPointX(hex.x, hex.z, 0);
//        float fy = (float) biomes.getPointY(hex.x, hex.z, 0);
//        rand.setSeed(x * 89153 ^ z * 31 + y);
//        int randRange = 8;
//        float randX = -rand.nextInt(randRange)+rand.nextInt(randRange);
//        float randZ = -rand.nextInt(randRange)+rand.nextInt(randRange);
//        double angle = GameMath.getAngle(x-centerX+randX, z-centerY+randZ, fx-centerX, fy-centerY);
//        int scaleTangent = (int) (6+6*(angle/Math.PI)); // 0 == points to corner, 1/-1 == points to half of side
//        if (scaleTangent < 3) {
//            return Block.stones.basalt.id;
//        }
//        if (scaleTangent < 6) {
//            return Block.stones.diorite.id;
//        }
//        if (scaleTangent < 9) {
//            return Block.stones.marble.id;
//        }
//        return Block.stones.granite.id;
    }
    public Block getStone() {
        return null;
    }
    public Block getTopBlock() {
        return Block.grass;
    }
    public Block getSoilBlock() {
        return Block.dirt;
    }
    public Block getWaterBlock() {
        return Block.water;
    }
}
