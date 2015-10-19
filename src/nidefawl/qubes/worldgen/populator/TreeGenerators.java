/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.ArrayList;
import java.util.Random;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.block.BlockLeaves;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TreeGenerators {
    public static IWorldGen rand(Random rand) {
//        switch (rand.nextInt(5)) {
//            case 1:
//                return oakVariationTree();
//            case 2:
//                return pineTree();
//            case 3:
//                return birchTree();
//            case 4:
//                return redTree();
//      }
//      return oakTree();
      return oakVariationTree();
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
        Random r = new Random();
        int a = r.nextInt(7);
        ArrayList<Block> bl = Lists.newArrayList();
        for (Block b : Block.block) {
            if (b instanceof BlockLeaves) {
                bl.add(b);
            }
        }
        Block b = bl.get(r.nextInt(bl.size()));
        switch (a) {
            case 0 :

//                
//                return new TreeGeneratorLSystem(
//                        "T~[b]*[b]", ImmutableMap.<Character, TreeRule>builder()
//                        .put('a', new TreeRule("A*****", 1f))
//                        .put('b', new TreeRule("aa?Taa?Taa?Taa?T", 1f))
//                        .put('A', new TreeRule("[~&&&TF&F&FB]", 1.0f))
//                        .put('B', new TreeRule("[~F^^FF?CT~E+E]", 1f))
//                        .put('C', new TreeRule("P[+L][&L][-L]", 1f))
//                        .put('E', new TreeRule("LU", 1f)).build(),
//                        5, (float) Math.toRadians(9))
//                        .setLeafType(Block.leaves_jungle.id)
//                        .setBarkType(Block.log_jungle.id);
            case 1:
//
//                    return new TreeGeneratorLSystem(
//                            "6Qa2Q7dQaQbQaQbQab", ImmutableMap.<Character, TreeRule>builder()
//                            .put('A', new TreeRule("[4Q+++WW3Q8+C]", 1.0f))
//                            .put('D', new TreeRule("[2Q3+Q7+2Q4+C]", 1.0f))
//                            .put('B', new TreeRule("2CW2C", 1f))
//                            .put('a', new TreeRule("[~6+[A]]", 1f))
//                            .put('b', new TreeRule("[~6+[D]]", 1f))
//                            .put('d', new TreeRule("a4Q", 1f))
//                            .put('e', new TreeRule("4Q", 1f))
//                            .put('C', new TreeRule("~X&&~X", 1f)).build(),
//                            6, (float) Math.toRadians(7))
//                            .setLeafType(Block.leaves_jungle.id)
//                            .setBarkType(Block.log_jungle.id);
            case 2:

//                
////              
//              return new TreeGeneratorLSystem(
//                      "6Ta2T5dTaTb", ImmutableMap.<Character, TreeRule>builder()
//                      .put('A', new TreeRule("[4T+++W2T8+C]", 1.0f))
//                      .put('D', new TreeRule("[2T3+T7+1T4+C]", 1.0f))
//                      .put('B', new TreeRule("2CW2C", 1f))
//                      .put('a', new TreeRule("[~6+[A]]", 1f))
//                      .put('b', new TreeRule("[~6+[D]]", 1f))
//                      .put('d', new TreeRule("a2T", 1f))
//                      .put('C', new TreeRule("~W&&~W", 1f)).build(),
//                      6, (float) Math.toRadians(7))
//                      .setLeafType(Block.leaves_jungle.id)
//                      .setBarkType(Block.log_jungle.id);
            case 3:
                //
//              
              return new TreeGeneratorLSystem(
                      "6Qa3Q7de1de[+++4C]ababab", ImmutableMap.<Character, TreeRule>builder()
                      .put('A', new TreeRule("[4Q+++WW3Q8+C]", 1.0f))
                      .put('D', new TreeRule("[2Q3+Q7+2Q4+C]", 1.0f))
                      .put('B', new TreeRule("2CW2C", 1f))
                      .put('a', new TreeRule("[~6+[A]]", 1f))
                      .put('b', new TreeRule("[~6+[D]]", 1f))
                      .put('d', new TreeRule("a1Q", 1f))
                      .put('e', new TreeRule("4Q", 1f))
                      .put('C', new TreeRule("[~XX]&&[~XX]", 1f)).build(),
                      6, (float) Math.toRadians(7))
                      .setLeafType(b.id)
                      .setBarkType(Block.log_jungle.id);
            case 4:

//              
              return new TreeGeneratorLSystem(
                      "T?T?T~[b]*[b]", ImmutableMap.<Character, TreeRule>builder()
                      .put('a', new TreeRule("A*****", 1f))
                      .put('b', new TreeRule("aTaTaTaTaTaTaTa", 1f))
                      .put('A', new TreeRule("[&&&TF&F&FB]", 1.0f))
                      .put('B', new TreeRule("[F^F^F?CF?CFTT~E+E?E]", 1f))
                      .put('C', new TreeRule("P[+LL][&LL][-L]", 1f))
                      .put('E', new TreeRule("LU", 1f)).build(),
                      6, (float) Math.toRadians(9))
                      .setLeafType(b.id)
                      .setBarkType(Block.log_jungle.id);
            case 5:


                return new TreeGeneratorLSystem(
                        "TT?T?T?TAAAAAATTTTE", ImmutableMap.<Character, TreeRule>builder()
                        .put('A', new TreeRule("[~+++C]", 1.0f))
                        .put('B', new TreeRule("[~&&&&C]", 1.0f))
                        .put('C', new TreeRule("[F^F^FF^FD]", 1.0f))
                        .put('D', new TreeRule("~[T?[&T][+LL?L]PP[-LL]]", 0.8f))
                        .put('E', new TreeRule("TPLDDLDD", 0.5f)).build(),
                        6, (float) Math.toRadians(12))
                        .setLeafType(b.id)
                        .setBarkType(Block.log_jungle.id);
        }

      return new TreeGeneratorLSystem(
              "TTT?T?T?TAAAAAATTTTE", ImmutableMap.<Character, TreeRule>builder()
              .put('A', new TreeRule("[~++++++C]", 1.0f))
              .put('B', new TreeRule("[~&&&&&&C]", 1.0f))
              .put('C', new TreeRule("[F^F^FF^FD]", 1.0f))
              .put('D', new TreeRule("~[L?[&L][+LL?L]PP[-LL]]", 0.8f))
              .put('E', new TreeRule("PSLDDLDD", 0.5f)).build(),
              6, (float) Math.toRadians(7))
              .setLeafType(b.id)
              .setBarkType(Block.log_jungle.id);
        

//        
        
        
//        return new TreeGeneratorLSystem(
//                "FFFFFA", ImmutableMap.<Character, TreeRule>builder()
//                .put('A', new TreeRule("[&FFFFBFA]////[&BFFFFFA]////[&FBFFFFAFFA]", 1.0f))
//                .put('B', new TreeRule("[&FFFAFFFF]////[&FFFAFFF]////[&FFFAFFAA]", 0.8f)).build(),
//                4, (float) Math.toRadians(35))
//                .setLeafType(Block.leaves_jungle.id)
//                .setBarkType(Block.log_jungle.id);
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
