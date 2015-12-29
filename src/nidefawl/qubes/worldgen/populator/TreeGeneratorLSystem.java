/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Lists;

import nidefawl.qubes.block.Block;
import nidefawl.qubes.util.*;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.IBlockWorld;
import nidefawl.qubes.worldgen.trees.Tree;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class TreeGeneratorLSystem implements IWorldGen {

    public static final float MAX_ANGLE_OFFSET = (float) Math.toRadians(5);
    /* SETTINGS */
    private int maxDepth;
    private float angle;
    private int leaves;
    private int log;
    /* RULES */
    private final String initialAxiom;
    private final Map<Character, TreeRule> ruleSet;
    public final Map<Long, Integer> blocks;
    public final Map<Long, Integer> trunk;
    private int variation;
    Block vines;
    public void setVines(Block vines) {
        this.vines = vines;
    }

    /**
     * @param string
     * @param build
     * @param i
     * @param radians
     */
    public TreeGeneratorLSystem(String initialAxiom, Map<Character, TreeRule> ruleSet, int maxDepth, float angle) {
        this.angle = angle;
        this.maxDepth = maxDepth+2;

        this.initialAxiom = initialAxiom;
        this.ruleSet = ruleSet;
        this.variation = new Random().nextInt(2);
        this.blocks = new HashMap<>();
        this.trunk = new HashMap<>();
    }

    /**
     * @param id
     * @return
     */
    public TreeGeneratorLSystem setLeafType(int id) {
        this.leaves = id;
        return this;
    }

    /**
     * @param id
     * @return
     */
    public TreeGeneratorLSystem setBarkType(int id) {
        this.log = id;
        return this;
    }
    /* (non-Javadoc)
     * @see nidefawl.qubes.worldgen.populator.IWorldGen#generate(nidefawl.qubes.world.IBlockWorld, int, int, int, java.util.Random)
     */
    @Override
    public boolean generate(IBlockWorld c, int x, int y, int z, Random rand) {
        Vector3f position = new Vector3f(0f, 0f, 0f);

        Matrix4f rotation = new Matrix4f();
        rotation.setIdentity();
        rotation = rotation.rotate(GameMath.PI_OVER_180*-90, 0, 0, 1);
        float angleOffset = -4+rand.nextFloat()*4*2;
        angleOffset*=1.2f;
        dir.x = 0.6f;
        recurse(c, rand, x, y, z, (float)Math.toRadians(angleOffset), new CharSequenceIterator(initialAxiom),
                position, rotation, 0, 0);
        postGenerate(c, x, y, z, rand);
        return true;
    }
    private void postGenerate(IBlockWorld world, int x, int y, int z, Random rand) {
        if (this.vines != null) {
            List<Long> blockpos = Lists.newArrayList();
            for (Entry<Long, Integer> e : this.blocks.entrySet()) {
                Block block = Block.get(e.getValue());
                if (Block.leaves.getBlocks().contains(block))
                    blockpos.add(e.getKey());
            }
            for (Long l : blockpos) {
                int x1 = TripletLongHash.getX(l);
                int y1 = TripletLongHash.getY(l)-1-rand.nextInt(5);
                int z1 = TripletLongHash.getZ(l);
                Block block = Block.get(world.getType(x1, y1, z1));
                if (!Block.leaves.getBlocks().contains(block))
                    continue;
                for (int k = 0; k <4; k++) { 
                    int offx = k==0?-1:k==2?1:0;
                    int offz = k==1?-1:k==3?1:0;
                    int bX = x1+offx;
                    int bY = y1;
                    int bZ = z1+offz;
                    int typeb = world.getType(bX, bY, bZ);
                    if (typeb == 0) {
                        boolean fail = false;
                        for (int y3 = 1; y3 < 7; y3++) {
                            if (world.getType(bX, bY-y3, bZ) != 0) {
                                fail = true;
                                break;
                            }
                        }
                        if (!fail) {
                            int rotData = 1;
                            switch (k) {
                                case 0:
                                    rotData = 8; break;
                                case 1:
                                    rotData = 1; break;
                                case 2:
                                    rotData = 2; break;
                                case 3:
                                    rotData = 4; break;
                            }
                            int y4 = 3+rand.nextInt(4);
                            for (int y3 = 0; y3 < y4; y3++) {
                                safelySetBlock(world, bX, bY-y3, bZ, this.vines.id, rotData, 0);
                            }
                            break;
                        }
                    }
                }
            
            }
        }
    }
    Matrix4f tempRotation = new Matrix4f();
    Vector3f dir = new Vector3f(1f, 0f, 0f);
    Vector3f dir2 = new Vector3f(1f, 0f, 0f);
    private int nPlaced;

    private void recurse(IBlockWorld view, Random rand, int posX, int posY, int posZ, float angleOffset,
            CharSequenceIterator axiomIterator, Vector3f position, Matrix4f rotation, int depth, int trunkDepth) {
        float randAngle = -angle+rand.nextFloat()*angle*2;
        angleOffset*=0.1;
        randAngle*=1;
        float radAngle = angle;//(float) (Math.PI*2/360)*25;
        Vector3f down = new Vector3f(0, -1, 0);
        while (axiomIterator.hasNext()) {
            char c = axiomIterator.nextChar();
            if (c == ' ') {
                continue;
            }
            int repeat = 1;
            if (c >= '0' && c <= '9' && axiomIterator.hasNext()) {
                repeat = (int) c - '0';
                c = axiomIterator.nextChar();
            }

            for (int i = 0; i < repeat; i++)
            {

                int size = 1;
                switch (c) {
                    case '?':
                        if (rand.nextInt(4)==0 && axiomIterator.hasNext()) {
                            char cNext = axiomIterator.nextChar();
                            if (cNext == '[') {
                                while (axiomIterator.hasNext()) {
                                    cNext = axiomIterator.nextChar();
                                    if (cNext == ']')
                                        break;
                                }
                            }
                        }
                        break;
                    case 'Q':
                        safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z, this.log, 0, 1);
                        safelySetBlock(view, posX + (int) position.x+1, posY + (int) position.y, posZ + (int) position.z, this.log, 0, 1);
                        safelySetBlock(view, posX + (int) position.x+1, posY + (int) position.y, posZ + (int) position.z+1, this.log, 0, 1);
                        safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z+1, this.log, 0, 1);
                        dir2.set(1, 0, 0);
                        rotation.transformVec(dir2);
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case 'T':
                        safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z, this.log, 0, 1);
                        dir2.set(1, 0, 0);
                        rotation.transformVec(dir2);
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case 'Z':

                        dir2.set(dir);
                        rotation.transformVec(dir2);
//                        float n = Vector3f.dot(dir2, down);
//                        n = Math.min(1, Math.max(1 - n*3, 0));
//                        dir2.scale(n*((maxDepth-depth)/(float)maxDepth));
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case 'X':
                        size+=1;
                    case 'W':
                        size++;
                        // Generate leaves
                        for (int x = -size; x <= size; x++) {
                            for (int y = -1; y <= 1; y++) {
                                for (int z = -size; z <= size; z++) {
                                    int ds = x*x+y*y+z*z;
                                    if (ds > size*size-2-Math.abs(y)*3) {
                                        continue;
                                    }
                                    if (size > 0 && Math.abs(x) == size && Math.abs(y) == size && Math.abs(z) == size) {
                                        continue;
                                    }
                                    safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x + 1, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x - 1, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z + 1, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z - 1, leaves);
                                }
                            }
                        }
                        dir2.set(dir);
                        rotation.transformVec(dir2);
//                        float n = Vector3f.dot(dir2, down);
//                        n = Math.min(1, Math.max(1 - n*3, 0));
//                        dir2.scale(n*((maxDepth-depth)/(float)maxDepth));
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case 'S':
                        size = 2;
                    case 'P':
                        // Generate leaves
                        for (int x = -size; x <= size; x++) {
                            for (int y = -size; y <= size; y++) {
                                for (int z = -size; z <= size; z++) {
                                    if (size > 0 && Math.abs(x) == size && Math.abs(y) == size && Math.abs(z) == size) {
                                        continue;
                                    }
                                    safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x + 1, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x - 1, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z + 1, leaves);
                                  safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z - 1, leaves);
                                }
                            }
                        }
                        dir2.set(dir);
                        rotation.transformVec(dir2);
//                        float n = Vector3f.dot(dir2, down);
//                        n = Math.min(1, Math.max(1 - n*3, 0));
//                        dir2.scale(n*((maxDepth-depth)/(float)maxDepth));
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case 'U':
                        size = 0;
                    case 'L':
                        // Generate leaves
                        for (int x = -size; x <= size; x++) {
                            for (int y = -size; y <= size; y++) {
                                for (int z = -size; z <= size; z++) {
                                    if (size > 0 && Math.abs(x) == size && Math.abs(y) == size && Math.abs(z) == size) {
                                        continue;
                                    }
                                    safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                }
                            }
                        }
                        dir2.set(dir);
                        rotation.transformVec(dir2);
//                        float n = Vector3f.dot(dir2, down);
//                        n = Math.min(1, Math.max(1 - n*3, 0));
//                        dir2.scale(n*((maxDepth-depth)/(float)maxDepth));
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case 'G':
                    case 'F':
                        // Tree trunk
                        safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z, this.log, 3, 1);
//                        if (depth < 2) {
//                            safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z + 1, this.log);
//                            safelySetBlock(view, posX + (int) position.x + 1, posY + (int) position.y, posZ + (int) position.z + 1, this.log);
//                            safelySetBlock(view, posX + (int) position.x + 1, posY + (int) position.y, posZ + (int) position.z, this.log);
//                        //
//                      }
                        dir2.set(dir);
                        rotation.transformVec(dir2);
//                        float n = Vector3f.dot(dir2, down);
//                        n = Math.min(1, Math.max(1 - n*3, 0));
//                        dir2.scale(n*((maxDepth-depth)/(float)maxDepth));
                        position.addVec(dir2);
                        trunkDepth++;
                        break;
                    case '[':
                        dir.x+=0.2f;
                        recurse(view, rand, posX, posY, posZ, angleOffset, axiomIterator, new Vector3f(position), new Matrix4f(rotation), depth, 0);
                        break;
                    case ']':
                        dir.x-=0.2f;
                        return;
                    case '+':
                        rotation.mulMat(tempRotation.setIdentity().rotate((radAngle), 0, 0, 1F));
                        break;
                    case '-':
                        rotation.mulMat(tempRotation.setIdentity().rotate((radAngle), 0, 0, -1F));
                        break;
                    case '&':
                        rotation.mulMat(tempRotation.setIdentity().rotate((radAngle), 0, 1F, 0));
                        break;
                    case '^':
                        rotation.mulMat(tempRotation.setIdentity().rotate((radAngle), 0, -1F, 0));
                        break;
                    case '*':
                        rotation.mulMat(tempRotation.setIdentity().rotate((radAngle), 1F, 0, 0));
                        break;
                    case '/':
                        rotation.mulMat(tempRotation.setIdentity().rotate((radAngle), -1F, 0, 0));
                        break;
                    case '~':
                        rotation.mulMat(tempRotation.setIdentity().rotate(rand.nextFloat()*(float)Math.PI*2.0f, -1F, 0, 0));
                        break;
                    default:
                        // If we have already reached the maximum depth, don't ever bother to lookup in the map
                        if (depth >= maxDepth) {
                            break;
                        }
                        TreeRule rule = ruleSet.get(c);
                        if (rule == null) {
                            break;
                        }

                        float weightedFailureProbability = GameMath.pow(1f - rule.getWeight(), maxDepth - depth);
                        if (rand.nextFloat() < weightedFailureProbability) {
                            break;
                        }
//                        float randAngle2 = -angleOffset+rand.nextFloat()*angleOffset*2;
//                        randAngle2 *= 0.25;
//                        rotation.mulMat(tempRotation.setIdentity().rotate((randAngle2), 1, 0, 0));
//                        randAngle2 = -angleOffset+rand.nextFloat()*angleOffset*2;
//                       randAngle2 *= 0.25;
//                       rotation.mulMat(tempRotation.setIdentity().rotate((randAngle2), 0, 0, 1));
//                       randAngle2 = -angleOffset+rand.nextFloat()*angleOffset*2;
//                      randAngle2 *= 0.25;
//                      rotation.mulMat(tempRotation.setIdentity().rotate((randAngle2), 0, 1, 0));

                        recurse(view, rand, posX, posY, posZ, angleOffset, new CharSequenceIterator(rule.getRule()),
                                position, rotation, depth + 1, trunkDepth);
                }   
            }
        }

    }

    private void safelySetBlock(IBlockWorld world, int x, int y, int z, int id) {
        safelySetBlock(world, x, y, z, id, 0, 0);
    }
    private void safelySetBlock(IBlockWorld world, int x, int y, int z, int id, int data, int flags) {
        long l = TripletLongHash.toHash(x, y, z);
        Integer i = blocks.get(l);
        if (i==null||i != id) {
            blocks.put(l, id);
            if ((flags & 1) != 0) {
                this.trunk.put(l, id);
            }
            world.setTypeData(x, y, z, id, data, Flags.MARK);
        }
    }

    public Tree getTree() {
        if (this.blocks.isEmpty()) {
            return null;
        }
        Tree tree = new Tree();
        Iterator<Entry<Long, Integer>> it = this.blocks.entrySet().iterator();
        boolean first = false;
        long[] blocks = new long[this.blocks.size()];
        
        int pos = 0;
        AABBInt treeBB = null;
        while (it.hasNext()) {
            Entry<Long, Integer> e = it.next();
            long l = e.getKey();
            int x1 = TripletLongHash.getX(l);
            int y1 = TripletLongHash.getY(l);
            int z1 = TripletLongHash.getZ(l);
            if (treeBB == null) {
                treeBB = new AABBInt(x1, y1, z1, x1, y1, z1);
            } else {
                treeBB.minX = Math.min(x1, treeBB.minX);
                treeBB.minY = Math.min(y1, treeBB.minY);
                treeBB.minZ = Math.min(z1, treeBB.minZ);
                treeBB.maxX = Math.max(x1, treeBB.maxX);
                treeBB.maxY = Math.max(y1, treeBB.maxY);
                treeBB.maxZ = Math.max(z1, treeBB.maxZ);
            }
            blocks[pos++] = l;
        }
        AABBInt trunkBB = null;
        int nblocks=0;
        for (int y = treeBB.minY+2; y <= treeBB.minY+4; y++) {
            for (int x = treeBB.minX; x <= treeBB.maxX; x++) {
                for (int z = treeBB.minZ; z <= treeBB.maxZ; z++) {
                    long l = TripletLongHash.toHash(x, y, z);
                    Integer n = trunk.get(l);
                    nblocks++;
                    if (n != null) {
                        if (trunkBB == null)
                            trunkBB = new AABBInt(x, y, z, x, y, z);
                        trunkBB.minX = Math.min(x, trunkBB.minX);
                        trunkBB.maxX = Math.max(x, trunkBB.maxX);
                        trunkBB.minZ = Math.min(z, trunkBB.minZ);
                        trunkBB.maxZ = Math.max(z, trunkBB.maxZ);
                        trunkBB.minY = Math.min(y, trunkBB.minY);
                        trunkBB.maxY = Math.max(y, trunkBB.maxY);
                    }
                }
            }
        }

        if (trunkBB == null)
            return null;
        tree.bb.set(treeBB);
        tree.trunkBB.set(trunkBB);
        tree.setBlocks(blocks);
        return tree;
    }


}
