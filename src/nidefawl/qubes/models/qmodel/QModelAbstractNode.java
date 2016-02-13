package nidefawl.qubes.models.qmodel;

import java.io.EOFException;

import nidefawl.qubes.models.qmodel.loader.ModelLoaderQModel;
import nidefawl.qubes.vec.Matrix4f;

public abstract class QModelAbstractNode {


    protected int readParentType(ModelLoaderQModel loader) throws EOFException {
        String parent_type_string = loader.readString(32);
        if ("EMPTY".equals(parent_type_string)) {
            return 3;
        } else if ("BONE".equals(parent_type_string)) {
            return 2;
        } else if ("ARMATURE".equals(parent_type_string)) {
            return 1;
        }
        return 0;
    }
    public abstract QModelAbstractNode getAttachementNode();
    public abstract QModelBone getAttachmentBone();
    public abstract Matrix4f getMatDeform();
    public abstract Matrix4f getMatDeformNormal();
}
