/**
 * 
 */
package nidefawl.qubes.models.qmodel;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public enum QModelType {
    STATIC, RIGGED, BLOCK;

    /**
     * @param readUByte
     * @return
     */
    public static QModelType get(int type) {
        switch (type) {
            case 0:
                return STATIC;
            case 1:
                return RIGGED;
            case 2:
                return BLOCK;
        }
        return null;
    }
}
