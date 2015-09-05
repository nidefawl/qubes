package nidefawl.qubes.server;

import java.io.IOException;
import java.io.PrintWriter;

import jline.console.UserInterruptException;
import nidefawl.qubes.server.commands.Command;
import nidefawl.qubes.server.commands.ICommandSource;

public class ConsoleReader implements Runnable, ICommandSource {

    private static Thread thread;
    jline.console.ConsoleReader reader;
    PrintWriter out;
    private final GameServer server;

    public ConsoleReader(GameServer instance) {
        this.server = instance;
    }

    @Override
    public void run() {
        try {
            reader = new jline.console.ConsoleReader();
            reader.setPrompt("> ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            out = new PrintWriter(reader.getOutput());
            String line;
            reader.setBellEnabled(false);
            reader.setHandleUserInterrupt(true);
            try {

                while (this.server.isRunning() && (line = reader.readLine()) != null) {
//                    if (color) {
//                        out.println("\u001B[33m======>\u001B[0m\"" + line + "\"");
    //
//                    } else {
//                        out.println("======>\"" + line + "\"");
//                    }
                    out.flush();

                    // If we input the special word then we will mask
                    // the next line.
//                    if ((trigger != null) && (line.compareTo(trigger) == 0)) {
//                        line = reader.readLine("password> ", mask);
//                    }
                    if (line.length() > 0) {
                        server.getCommandHandler().handle(this, line);
                        if (line.equalsIgnoreCase("cls")) {
                            reader.clearScreen();
                        }
                    }
                }
            } catch (UserInterruptException interrupt) {
                server.getCommandHandler().handle(this, "stop");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    
    }

    public static void startThread(GameServer instance) {
        thread = new Thread(new ConsoleReader(instance));
        thread.setName("ConsoleReader");
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void preExecuteCommand(Command c) {
    }

    @Override
    public void onError(Command c, Exception e) {
        out.println("An exception occured while executing '"+c.getName()+"': "+e.getMessage());
        e.printStackTrace();
    }

    @Override
    public void onUnknownCommand(String cmd, String line) {
        out.println("Unknown command '"+cmd+"'");
    }

    @Override
    public GameServer getServer() {
        return this.server;
    }

    @Override
    public void sendMessage(String string) {
        out.println(string);
        out.flush();
    }

}
