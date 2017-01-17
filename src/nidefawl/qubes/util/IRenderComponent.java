package nidefawl.qubes.util;

/**
 * This interface describes a part of the engine that needs initialization on startup.
 * I suck at naming things.
 * @author Michael Hept 2017
 * Copyright: Michael Hept
 */
public interface IRenderComponent {

    public void init();
    public void release();
    
    /** Intialise things like SSBOs which may need registration before linking of other components shader programs */
    public void preinit();
}
