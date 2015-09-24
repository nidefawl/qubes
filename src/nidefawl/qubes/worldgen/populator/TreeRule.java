/**
 * 
 */
package nidefawl.qubes.worldgen.populator;

/**
 * @author Michael Hept 2015 Copyright: Michael Hept
 */
public class TreeRule {

    private final String string;
    private final float  weight;

    /**
     * @param string
     * @param f
     */
    public TreeRule(String string, float f) {
        this.string = string;
        this.weight = f;
    }

    /**
     * @return
     */
    public String getRule() {
        return string;
    }

    /**
     * @return
     */
    public float getWeight() {
        return this.weight;
    }

}
