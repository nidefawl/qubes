package nidefawl.qubes.world.structure;

import nidefawl.qubes.nbt.Tag.Compound;

public abstract class StructureFactory<T> {
    
    public abstract T newInstance();
}
