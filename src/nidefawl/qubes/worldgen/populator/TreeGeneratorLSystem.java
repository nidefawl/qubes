/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

import java.util.Map;
import java.util.Random;

import nidefawl.qubes.Game;
import nidefawl.qubes.util.CharSequenceIterator;
import nidefawl.qubes.util.Flags;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.vec.*;
import nidefawl.qubes.world.IBlockWorld;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class TreeGeneratorLSystem implements IWorldGen {

    public static final float MAX_ANGLE_OFFSET = (float) Math.toRadians(5);
    private static final Vector3f ZERO = new Vector3f();
    /* SETTINGS */
    private int maxDepth;
    private float angle;
    private int leaves;
    private int log;
    /* RULES */
    private final String initialAxiom;
    private final Map<Character, TreeRule> ruleSet;
    private int variation;

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
        recurse(c, rand, x, y, z, (float)Math.toRadians(angleOffset), new CharSequenceIterator(initialAxiom),
                position, rotation, 0, 0);

        return true;
    }
    Matrix4f tempRotation = new Matrix4f();
    Vector3f dir = new Vector3f(1f, 0f, 0f);
    Vector3f dir2 = new Vector3f(1f, 0f, 0f);

    private void recurse(IBlockWorld view, Random rand, int posX, int posY, int posZ, float angleOffset,
            CharSequenceIterator axiomIterator, Vector3f position, Matrix4f rotation, int depth, int trunkDepth) {
        float randAngle = -angle+rand.nextFloat()*angle*2;
        while (axiomIterator.hasNext()) {
            char c = axiomIterator.nextChar();
            switch (c) {
                case 'G':
                case 'F':
                    // Tree trunk
                    safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z, this.log);
//                    if (depth < 2) {
//                        safelySetBlock(view, posX + (int) position.x, posY + (int) position.y, posZ + (int) position.z + 1, this.log);
//                        safelySetBlock(view, posX + (int) position.x + 1, posY + (int) position.y, posZ + (int) position.z + 1, this.log);
//                        safelySetBlock(view, posX + (int) position.x + 1, posY + (int) position.y, posZ + (int) position.z, this.log);
                    //
//                  }
                  // Generate leaves
                  if (depth > 1 && trunkDepth > 2) {
                      int size = depth > 2 && trunkDepth > 3 ? 1 : 0;

                      for (int x = -size; x <= size; x++) {
                          for (int y = -size; y <= size; y++) {
                              for (int z = -size; z <= size; z++) {
                                  if (size > 0 && Math.abs(x) == size && Math.abs(y) == size && Math.abs(z) == size) {
                                      continue;
                                    }

//                                    if (depth >= maxDepth -1) {
//                                        safelySetBlock(view, posX + (int) position.x + x + 1, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
//                                        safelySetBlock(view, posX + (int) position.x + x - 1, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
//                                        safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z + 1, leaves);
//                                        safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z - 1, leaves);
//
//                                    }
                                    safelySetBlock(view, posX + (int) position.x + x, posY + (int) position.y + y, posZ + z + (int) position.z, leaves);
                                }
                            }
                        }
                    }
                    dir2.set(dir);
                    Vector3f down = new Vector3f(0, -1, 0);
                    rotation.transformVec(dir2);
                    float n = Vector3f.dot(dir2, down);
                    n = Math.min(1, Math.max(1 - n*3, 0));
                    dir2.scale(n*((maxDepth-depth)/(float)maxDepth));
                    position.addVec(dir2);
                    trunkDepth++;
                    break;
                case '[':
                    recurse(view, rand, posX, posY, posZ, angleOffset, axiomIterator, new Vector3f(position), new Matrix4f(rotation), depth, 0);
                    break;
                case ']':
                    return;
                case '+':
                    rotation.mulMat(tempRotation.setIdentity().rotate((angleOffset+randAngle), 0, 0, 1F));
                    break;
                case '-':
                    rotation.mulMat(tempRotation.setIdentity().rotate((angleOffset+randAngle), 0, 0, -1F));
                    break;
                case '&':
                    rotation.mulMat(tempRotation.setIdentity().rotate((angleOffset+randAngle), 0, 1F, 0));
                    break;
                case '^':
                    rotation.mulMat(tempRotation.setIdentity().rotate((angleOffset+randAngle), 0, -1F, 0));
                    break;
                case '*':
                    rotation.mulMat(tempRotation.setIdentity().rotate((angleOffset+randAngle), 1F, 0, 0));
                    break;
                case '/':
                    rotation.mulMat(tempRotation.setIdentity().rotate((angleOffset+randAngle), -1F, 0, 0));
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
                    float randAngle2 = -angleOffset+rand.nextFloat()*angleOffset*2;
                    randAngle2 *= 0.25;
                    rotation.mulMat(tempRotation.setIdentity().rotate((randAngle2), 1, 0, 0));
                    randAngle2 = -angleOffset+rand.nextFloat()*angleOffset*2;
                   randAngle2 *= 0.25;
                   rotation.mulMat(tempRotation.setIdentity().rotate((randAngle2), 0, 0, 1));
                   randAngle2 = -angleOffset+rand.nextFloat()*angleOffset*2;
                  randAngle2 *= 0.25;
                  rotation.mulMat(tempRotation.setIdentity().rotate((randAngle2), 0, 1, 0));

                    recurse(view, rand, posX, posY, posZ, angleOffset, new CharSequenceIterator(rule.getRule()),
                            position, rotation, depth + 1, 0);
            }
        }

    }

    /**
     * @param view
     * @param i
     * @param j
     * @param k
     * @param log2
     */
    private void safelySetBlock(IBlockWorld world, int x, int y, int z, int id) {
//        System.out.println("set block "+x+","+y+","+z+" = "+id);
        world.setType(x, y, z, id, Flags.MARK);
    }

}
