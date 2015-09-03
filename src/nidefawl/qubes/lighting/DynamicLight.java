package nidefawl.qubes.lighting;

import nidefawl.qubes.vec.Vector3f;

public class DynamicLight {
    public Vector3f loc;
    public Vector3f color;
    public double intensity;
    public DynamicLight(Vector3f loc, Vector3f color, float intensity) {
        this.loc = loc;
        this.color = color;
        this.intensity = intensity;
        this.color.scale(intensity);
    }

}
