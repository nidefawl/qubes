package nidefawl.qubes.render;

public abstract class FinalRenderer extends AbstractRenderer {

    public abstract void onAASettingChanged();

    public abstract void setSSR(int id);

    public abstract void onAOSettingUpdated();
    
    public abstract void aoReinit();
    
    public abstract void onVRModeChanged();
    public abstract void onSSRSettingChanged();

}
