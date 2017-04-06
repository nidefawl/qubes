package nidefawl.qubes.perf;

public class GPUTaskProfileFrame extends GPUTaskProfile {

    public GPUTaskProfileFrame init(String name, int startQuery) {

        this.parent = null;
        this.name = name;
        this.startQuery = startQuery;
        return this;
    }

}