package nidefawl.qubes.world;

import nidefawl.qubes.vec.Vector3f;

public class Light {
    public Vector3f loc;
    public Vector3f color;
    public double intensity;
    public Light(Vector3f loc, Vector3f color, float intensity) {
        this.loc = loc;
        this.color = color;
        this.intensity = intensity;
        this.color.scale(intensity);
    }

}
