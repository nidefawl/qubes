package nidefawl.qubes.models;

public class ModelOption {
    String name;
    String[] options;
    private EntityModel eModel;
    private int id;
    private int defaultVal;
    public ModelOption(EntityModel eModel, String name) {
        this.eModel = eModel;
        this.id = this.eModel.addOption(this);
        this.name = name;
    }
    public ModelOption setOptions(String... options) {
        this.options = options;
        return this;
    }
    public String getOption(int n) {
        return this.options[n];
    }
    public ModelOption setOptionCount(String string, int n) {
        String[] options = new String[n];
        int i = 0;
        int j = 1;
        if (string.startsWith("\0")) {
            string = string.substring(1);
            n++;
            options = new String[n];
            options[i++] = "-";
        }
        for (; i < n; i++) {
            options[i] = string.replace("#", ""+(j++));
        }
        this.options = options;
        return this;
    }
    public String getTextVal(int val) {
        return this.options==null||this.options.length==0||val>=this.options.length?"-":this.options[val];
    }
    public String[] getOptions() {
        return this.options;
    }
    public int getId() {
        return this.id;
    }
    public String getName() {
        return this.name;
    }
    public void setDefaultVal(int defaultVal) {
        this.defaultVal = defaultVal;
    }
    public int getDefaultVal() {
        return this.defaultVal;
    }
    
}