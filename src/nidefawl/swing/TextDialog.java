package nidefawl.swing;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import nidefawl.qubes.assets.AssetManager;
import nidefawl.qubes.shader.ShaderCompileError;
import nidefawl.qubes.shader.ShaderSource;
import nidefawl.qubes.util.NoDeploy;

@SuppressWarnings("serial")
@NoDeploy
public class TextDialog extends JFrame implements ActionListener, ClipboardOwner {
    class JTextAreaWithScroll extends JTextArea {
        private final JScrollPane scrollPane;

        public JTextAreaWithScroll(final int vsbPolicy, final int hsbPolicy) {
            this.scrollPane = new JScrollPane(this, vsbPolicy, hsbPolicy);
        }

        public JScrollPane getScrollPane() {
            return this.scrollPane;
        }
    }
    private final JPanel              topPanel         = new JPanel();
    private final JTextAreaWithScroll errorText;
    private final JLabel              errorOccured;
    public boolean reqRestart;
    private Throwable throwable;
    private AbstractButton shaderBtn;
    private ShaderSource shader;

    /**
     * Create the dialog.
     * @param b 
     * 
     * @param string
     */
    public TextDialog(String title, List<String> desca, Throwable t, boolean b) {
        this.throwable = t;
        this.setTitle(title);
        this.setLayout(new BorderLayout());
        this.setResizable(true);
        this.errorOccured = new JLabel(title);
        this.errorOccured.setFont(this.errorOccured.getFont().deriveFont(Font.BOLD, 16.0F));
        this.errorOccured.setBounds(5, 5, 100, 14);
        this.errorText = new JTextAreaWithScroll(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        errorText.setFont(Font.decode("Consolas"));
        this.topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.add(this.topPanel, BorderLayout.NORTH);
        this.topPanel.add(this.errorOccured);
        final JPanel bottomPane = new JPanel();
        final JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        final JPanel forceUpdatePane = new JPanel();
        forceUpdatePane.setLayout(new FlowLayout(FlowLayout.LEFT));
        this.getContentPane().add(bottomPane, BorderLayout.SOUTH);
        bottomPane.setLayout(new BorderLayout());
        bottomPane.add(forceUpdatePane, BorderLayout.WEST);
        bottomPane.add(buttonPane, BorderLayout.EAST);
        final JButton restartbutton = new JButton("Restart");
        restartbutton.setActionCommand("Restart");
        restartbutton.addActionListener(this);
         shaderBtn = new JButton("Show source");
        shaderBtn.setActionCommand("source");
        shaderBtn.addActionListener(this);
        final JButton closeButtone = new JButton("Close");
        closeButtone.setActionCommand("Close");
        closeButtone.addActionListener(this);
        AssetManager mgr = AssetManager.getInstance();
        shader = mgr == null ? null : mgr.getLastFailedShaderSource();
        if (shader != null) {
            buttonPane.add(shaderBtn);
        }
        if (b)
        buttonPane.add(restartbutton);
        buttonPane.add(closeButtone);
        this.add(this.errorText.getScrollPane());

        ArrayList<String> desc = new ArrayList<>(desca);
        ArrayList<String> desc2 = new ArrayList<>();
        if (throwable != null) {
            throwable.printStackTrace();
            StringWriter errors = new StringWriter();
            throwable.printStackTrace(new PrintWriter(errors));
            String[] split = errors.toString().split("\r?\n");
            desc.add("");
            desc.add("__ STACKTRACE OF ERROR __");
            desc2.addAll(Arrays.asList(split));
            if (throwable instanceof ShaderCompileError) {
                System.err.println(((ShaderCompileError)throwable).getLog());
            }
        }
        for (String s : desc)
            appendLine(s);
        appendLine("");
        for (String s : desc2)
            appendLine(s);
    }
    public void setVisible(int w, int h) {
        this.setBounds(0, 0, w, h);
        final GraphicsConfiguration gcfg = this.getGraphicsConfiguration();
        final Rectangle bounds = gcfg.getBounds();

        final Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gcfg);

        final Rectangle effectiveScreenArea = new Rectangle();

        effectiveScreenArea.x = bounds.x + screenInsets.left;
        effectiveScreenArea.y = bounds.y + screenInsets.top;
        effectiveScreenArea.height = bounds.height - screenInsets.top - screenInsets.bottom;
        effectiveScreenArea.width = bounds.width - screenInsets.left - screenInsets.right;
        this.setLocation((effectiveScreenArea.width - w) / 2, (effectiveScreenArea.height - h) / 2);
        super.setVisible(true);
    }

    public void setText(final String s) {
        this.errorText.setText(s);
    }

    public void appendLine(final String s) {
        this.errorText.append(s + "\n");
        final JScrollBar vertical = this.errorText.getScrollPane().getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    public void prepend(String buf1) {
        this.errorText.setText(buf1+this.errorText.getText());
        final JScrollBar vertical = this.errorText.getScrollPane().getVerticalScrollBar();
        vertical.setValue(vertical.getMaximum());
    }
    String flip = null;
    @Override
    public void actionPerformed(final ActionEvent evt) {
        final String btnID = evt.getActionCommand();
        if (btnID.equals("Close")) {
            this.setVisible(false);
            this.dispose();
        }
        if (btnID.equals("Restart")) {
            this.reqRestart = true;
            this.setVisible(false);
            this.dispose();
        }
        if (btnID.equals("source")) {
            if (flip == null) {
                flip = this.errorText.getText();
                this.errorText.setText(shader.getSource());   
            } else {
                this.errorText.setText(flip);
                flip = null;
            }
            shaderBtn.setText(flip == null ? "Show source" : "Show error");
            DefaultCaret caret = (DefaultCaret) this.errorText.getCaret();
            caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

            final JScrollBar vertical = this.errorText.getScrollPane().getVerticalScrollBar();
            vertical.setValue(0);
            this.errorText.updateUI();
            System.out.println(vertical.getValue());
        }
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
