/**
 * 
 */
package nidefawl.qubes.biome;

import java.util.Random;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.util.Color;
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
            .setColor(BiomeColor.GRASS, 0x13490b)
            .setColor(BiomeColor.LEAVES, 0x112f02)
            .setColor(BiomeColor.FOLIAGE, 0xd4001)
            .setColor(BiomeColor.FOLIAGE2, 0x206e14)
            .setDebugColor(0x32dd32);
    public final static Biome MEADOW_BLUE = new BiomeMeadow(1)
            .setColor(BiomeColor.GRASS, 0x10672b)
            .setColor(BiomeColor.LEAVES, 0x10672b)
            .setColor(BiomeColor.FOLIAGE, 0x10672b)
            .setColor(BiomeColor.FOLIAGE2, 0x104a1f)
            .setDebugColor(0x3232dd);
    public final static Biome MEADOW_RED = new BiomeMeadow(2)
            .setColor(BiomeColor.GRASS, 0xc9380f)
            .setColor(BiomeColor.LEAVES, 0x723f10)
            .setColor(BiomeColor.FOLIAGE, 0x93380f)
            .setColor(BiomeColor.FOLIAGE2, 0x660000)
            .setDebugColor(0x3232dd);
    public final static Biome MEADOW_GREEN2 = new BiomeMeadow(3)
            .setColor(BiomeColor.GRASS, 0xd3702)
            .setColor(BiomeColor.LEAVES, 0x112f02)
            .setColor(BiomeColor.FOLIAGE, 0xd4001)
            .setColor(BiomeColor.FOLIAGE2, 0x206e14)
            .setDebugColor(0x32dd32);
    public final static Biome MEADOW_YE = new BiomeMeadow(4)
            .setColor(BiomeColor.GRASS, 0x373102)
            .setColor(BiomeColor.LEAVES, 0x112f02)
            .setColor(BiomeColor.FOLIAGE, 0xd4001)
            .setColor(BiomeColor.FOLIAGE2, 0x206e14)
            .setDebugColor(0x32dd32);
    public final static Biome DESERT = new BiomeDesert(5)
            .setColor(BiomeColor.GRASS, 0x486710)
            .setColor(BiomeColor.LEAVES, 0x486710)
            .setColor(BiomeColor.FOLIAGE, 0x486710)
            .setColor(BiomeColor.FOLIAGE2, 0xd77007)
            .setDebugColor(0xdd3232);
    public final static Biome DESERT_RED = new BiomeDesertRed(6)
            .setColor(BiomeColor.GRASS, 0x674b10)
            .setColor(BiomeColor.LEAVES, 0x290600)
            .setColor(BiomeColor.FOLIAGE, 0x674b10)
            .setColor(BiomeColor.FOLIAGE2, 0xd77007)
            .setDebugColor(0xdd32dd);
    public final static Biome ICE = new BiomeIce(7)
            .setColor(BiomeColor.GRASS, 0xd3702)
            .setColor(BiomeColor.LEAVES, 0x112f02)
            .setColor(BiomeColor.FOLIAGE, 0xd4001)
            .setColor(BiomeColor.FOLIAGE2, 0x206e14)
            .setDebugColor(0xaaaaaa);
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
