/**
 * 
 */
package nidefawl.qubes.util;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public interface IManagedResource {
    
    public void release();
    public EResourceType getType();
}
