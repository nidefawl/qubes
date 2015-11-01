/**
 * 
 */
package nidefawl.qubes.util;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IResourceManager {

    public void addResource(IManagedResource r);
    public void release();
    public void releaseAll(EResourceType type);
}
