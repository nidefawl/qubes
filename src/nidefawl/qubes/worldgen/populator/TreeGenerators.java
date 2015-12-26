/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.List;
import java.util.Random;

import com.google.common.collect.ImmutableMap;

import nidefawl.qubes.block.Block;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class TreeGenerators {
    
    public static TreeGeneratorLSystem get(int a, Random rand) {
        List<Block> list = Block.logs.getBlocks();
        Block log = list.get(rand.nextInt(list.size()));
        List<Block> list2 = Block.leaves.getBlocks();
        Block b = list2.get(rand.nextInt(list2.size()));
        switch (a) {
            case 0 :

//                
                return new TreeGeneratorLSystem(
                        "T~[b]*[b]", ImmutableMap.<Character, TreeRule>builder()
                        .put('a', new TreeRule("A*****", 1f))
                        .put('b', new TreeRule("aa?Taa?Taa?Taa?T", 1f))
                        .put('A', new TreeRule("[~&&&TF&F&FB]", 1.0f))
                        .put('B', new TreeRule("[~F^^FF?CT~E+E]", 1f))
                        .put('C', new TreeRule("P[+L][&L][-L]", 1f))
                        .put('E', new TreeRule("LU", 1f)).build(),
                        5, (float) Math.toRadians(9))
                        .setLeafType(b.id)
                        .setBarkType(log.id);
            case 1:

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
                      .setBarkType(log.id);
            case 2:
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
                      .setBarkType(log.id);
            case 3:


                return new TreeGeneratorLSystem(
                        "TT?T?T?TAAAAAATTTTE", ImmutableMap.<Character, TreeRule>builder()
                        .put('A', new TreeRule("[~+++C]", 1.0f))
                        .put('B', new TreeRule("[~&&&&C]", 1.0f))
                        .put('C', new TreeRule("[F^F^FF^FD]", 1.0f))
                        .put('D', new TreeRule("~[T?[&T][+LL?L]PP[-LL]]", 0.8f))
                        .put('E', new TreeRule("TPLDDLDD", 0.5f)).build(),
                        6, (float) Math.toRadians(12))
                        .setLeafType(b.id)
                        .setBarkType(log.id);
            default:

                return new TreeGeneratorLSystem(
                        "TTT?T?T?TAAAAAATTTTE", ImmutableMap.<Character, TreeRule>builder()
                        .put('A', new TreeRule("[~++++++C]", 1.0f))
                        .put('B', new TreeRule("[~&&&&&&C]", 1.0f))
                        .put('C', new TreeRule("[F^F^FF^FD]", 1.0f))
                        .put('D', new TreeRule("~[L?[&L][+LL?L]PP[-LL]]", 0.8f))
                        .put('E', new TreeRule("PSLDDLDD", 0.5f)).build(),
                        6, (float) Math.toRadians(7))
                        .setLeafType(b.id)
                        .setBarkType(log.id);
        }


//        return new TreeGeneratorLSystem(
//                "6Qa2Q7dQaQbQaQbQab", ImmutableMap.<Character, TreeRule>builder()
//                .put('A', new TreeRule("[4Q+++WW3Q8+C]", 1.0f))
//                .put('D', new TreeRule("[2Q3+Q7+2Q4+C]", 1.0f))
//                .put('B', new TreeRule("2CW2C", 1f))
//                .put('a', new TreeRule("[~6+[A]]", 1f))
//                .put('b', new TreeRule("[~6+[D]]", 1f))
//                .put('d', new TreeRule("a4Q", 1f))
//                .put('e', new TreeRule("4Q", 1f))
//                .put('C', new TreeRule("~X&&~X", 1f)).build(),
//                6, (float) Math.toRadians(7))
//                .setLeafType(b.id)
//                .setBarkType(log.id);
//        return new TreeGeneratorLSystem(
//                "6Ta2T5dTaTb", ImmutableMap.<Character, TreeRule>builder()
//                .put('A', new TreeRule("[4T+++W2T8+C]", 1.0f))
//                .put('D', new TreeRule("[2T3+T7+1T4+C]", 1.0f))
//                .put('B', new TreeRule("2CW2C", 1f))
//                .put('a', new TreeRule("[~6+[A]]", 1f))
//                .put('b', new TreeRule("[~6+[D]]", 1f))
//                .put('d', new TreeRule("a2T", 1f))
//                .put('C', new TreeRule("~W&&~W", 1f)).build(),
//                6, (float) Math.toRadians(7))
//                .setLeafType(b.id)
//                .setBarkType(log.id);  

//        
        
        
    }
}
