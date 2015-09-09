package nidefawl.qubes;

public class PlayerProfile {
    public String name = "me";
    private String ingameName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param name2
     */
    public void setIngameName(String ingameName) {
        this.ingameName = ingameName;
    }
    
    /**
     * @return the ingameName
     */
    public String getIngameName() {
        return this.ingameName;
    }
}
