/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.Random;

import com.google.common.collect.ImmutableMap;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TreeGenerators {
    public static IWorldGen rand(Random rand) {
        switch (rand.nextInt(5)) {
            case 1:
                return oakVariationTree();
            case 2:
                return pineTree();
            case 3:
                return birchTree();
            case 4:
                return redTree();
        }
        return oakTree();
    }

    public static IWorldGen oakTree() {
        return new TreeGeneratorLSystem(
            "FFFFFFA", ImmutableMap.<Character, TreeRule>builder()
            .put('A', new TreeRule("[&FFBFA]////[&BFFFA]////[&FBFFA]", 1.0f))
            .put('B', new TreeRule("[&FFFA]////[&FFFA]////[&FFFA]", 0.8f)).build(),
            4, (float) Math.toRadians(30))
            .setLeafType(Block.leaves_oak.id)
            .setBarkType(Block.log_oak.id);
    }

    public static IWorldGen oakVariationTree() {
        return new TreeGeneratorLSystem(
            "FFFFFFA", ImmutableMap.<Character, TreeRule>builder()
            .put('A', new TreeRule("[&FFFFBFA]////[&BFFFFFA]////[&FBFFFFAFFA]", 1.0f))
            .put('B', new TreeRule("[&FFFAFFFF]////[&FFFAFFF]////[&FFFAFFAA]", 0.8f)).build(),
            4, (float) Math.toRadians(35))
            .setLeafType(Block.leaves_jungle.id)
            .setBarkType(Block.log_jungle.id);
    }

    public static IWorldGen pineTree() {
        return new TreeGeneratorLSystem(
            "FFFFAFFFFFFFAFFFFA", ImmutableMap.<Character, TreeRule>builder()
            .put('A', new TreeRule("[&FFFFFFFFFA]////[&FFFFFA]////[&FFFFFA]", 1.0f)).build(),
            4, (float) Math.toRadians(35))
            .setLeafType(Block.leaves_spruce.id)
            .setBarkType(Block.log_spruce.id);
    }

    public static IWorldGen birchTree() {
        return new TreeGeneratorLSystem(
            "FFFFAFFFFBFFFFAFFFFBFFFFAFFFFBFF", ImmutableMap.<Character, TreeRule>builder()
            .put('A', new TreeRule("[&FFFAFFF]////[&FFAFFF]////[&FFFAFFF]", 1.0f))
            .put('B', new TreeRule("[&FAF]////[&FAF]////[&FAF]", 0.8f)).build(), 4, (float) Math.toRadians(35))
            .setLeafType(Block.leaves_birch.id)
            .setBarkType(Block.log_birch.id);
    }

    public static IWorldGen redTree() {
        return new TreeGeneratorLSystem("FFFFFAFAFAF", ImmutableMap.<Character, TreeRule>builder()
            .put('A', new TreeRule("[&FFAFF]////[&FFAFF]////[&FFAFF]", 1.0f)).build(),
            4, (float) Math.toRadians(40))
            .setLeafType(Block.leaves_acacia.id)
            .setBarkType(Block.log_acacia.id);
    }
}
