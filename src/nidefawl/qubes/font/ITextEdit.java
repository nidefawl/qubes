package nidefawl.qubes.font;

public interface ITextEdit {

    void submit(TextInput textInputRenderer, String text);

    void onEscape(TextInput textInput);

}
