/**
 * 
 */
package nidefawl.qubes.util;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
public class SimpleResourceManager implements IResourceManager {

    List<IManagedResource> resources = Lists.newArrayList();
    
    @Override
    public void addResource(IManagedResource r) {
        resources.add(r);
    }

    @Override
    public void release() {
        for (IManagedResource r : resources)
            r.release();
        resources.clear();
    }

    @Override
    public void releaseAll(EResourceType type) {
        for (int i = 0; i < this.resources.size(); i++) {
            if (this.resources.get(i).getType() == type) {
                this.resources.get(i).release();
                this.resources.remove(i--);
            }
        }
    }
    

}
