package nidefawl.qubes.vulkan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import net.freeutils.httpserver.HTTPServer;
import net.freeutils.httpserver.HTTPServer.ContextHandler;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;
import nidefawl.qubes.Game;
import nidefawl.qubes.GameBase;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.vulkan.VkMemoryManager.MemoryBlock;
import static nidefawl.qubes.vulkan.VkMemoryManager.MB;
import nidefawl.qubes.vulkan.VkMemoryManager.MemoryChunk;

public class DebugServer implements ContextHandler {
    private final static DebugServer INSTANCE = new DebugServer();

    public static DebugServer getInstance() {
        return INSTANCE;
    }

    HTTPServer serv = null;

    public void start() {
        try {
            HTTPServer server = new HTTPServer(8880);
            server.getVirtualHost(null).addContext("/", this);
            server.start();
            this.serv = server;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            HTTPServer server = this.serv;
            this.serv = null;
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int serve(Request req, Response resp) throws IOException {
        GameBase instance = Game.baseInstance;
        if (!req.getPath().equals("/memory")) {
            resp.send(404, "not found");
            return 0;
        }
        int pBlock = StringUtil.parseInt(req.getParams().get("b"), 0);
        int cellW = 16;
        String response = "";
        
        String css = "/* DivTable.com */\n" + 
                ".divTable{\n" + 
                "    display: table;\n" + 
                "    width: 100%;\n" + 
                "}\n" + 
                ".divTableRow {\n" + 
                "    display: table-row;\n" + 
                "}\n" + 
                ".divTableCell {\n" + 
                "    border: 1px solid #999999;\n" + 
                "    display: table-cell;\n" + 
                "    float: left;\n" + 
                "    width: "+(cellW-2)+"px;\n" + 
                "    height: "+(cellW-2)+"px;\n" + 
                "}\n" + 
                ".green {\n" + 
                "    background-color: #faa;\n" + 
                "}\n" + 
                ".red {\n" + 
                "    background-color: #afa;\n" +
                "}\n" +
                ".yellow {\n" + 
                "    background-color: #ffa;\n" +
                "}\n" +
                ".blue {\n" + 
                "    background-color: #aaf;\n" +
                "}\n" + 
                ".divTableBody {\n" + 
                "    display: table-row-group;\n" + 
                "}";
        if (instance != null) {
            VKContext ctxt = instance.vkContext;
            if (ctxt != null) {
                ArrayList<MemoryBlock> allBlocks = ctxt.memoryManager.allBlocks;
                
                if (pBlock > allBlocks.size()-1) {
                    pBlock = allBlocks.size()-1;
                }
                MemoryBlock block = allBlocks.get(pBlock);
                String header = "<!DOCTYPE html><html><head><style type=\"text/css\">"+css+"</style></head><body>";
                String footer = "</body></html>";
                String nav = "";
                for (int i =0; i < allBlocks.size(); i++) {
                    nav +="<a href=\"/memory?b="+i+"\">"+i+"</a> ";
                }
                nav += "<br>";
                nav += "<br>";
                long l = block.getAllocSum();

                
                nav += "Block "+pBlock+ "<br>device idx "+block.memType.idx+"<br>"+VulkanErr.memFlagsToStr(block.flags)+"<br>Usage "+(l/MB)+"/"+(block.blockSize/MB)+"MB" + " <br>";
                String content = "<div class=\"divTable\">\n" + "<div class=\"divTableBody\">";
                String row = "<div class=\"divTableRow\">\n";
                String cellRed = "<div class=\"divTableCell green\" title=\"";
                String cellGreen = "<div class=\"divTableCell red\" title=\"";
                String cellYellow = "<div class=\"divTableCell yellow\" title=\"";
                String cellBlue = "<div class=\"divTableCell blue\" title=\"";
                String cellClose = "\">\n&nbsp;</div>\n";
                String close = "</div>\n";
                
//                for (int i = 0; i < allBlocks.size(); i++) {
                    
                    ArrayList<MemoryChunk> allChunks = new ArrayList<>();
                    allChunks.addAll(block.list);
//                    allChunks.addAll(block.unused);
                    Collections.sort(allChunks, new Comparator<MemoryChunk>() {
                        @Override
                        public int compare(MemoryChunk o1, MemoryChunk o2) {
                            return Long.compare(o1.offset, o2.offset);
                        }
                    });
                    long cellSize = VkMemoryManager.MB/16;
                    long lNumCells = block.blockSize/cellSize + 1;
                    int wh = (int) Math.sqrt(lNumCells);
                    int nIdx = 0;
                    String cellColored = null;
                    String[] cellsColored = new String[] {
                        cellRed, cellBlue, cellYellow
                    };
                    MemoryChunk cLast = null;
                    for (int y = 0; y < wh; y++) {
                        content += row;
                        for (int x = 0; x < wh; x++) {
                            long memLoc = y*wh+x;
                            memLoc*=cellSize;
                            boolean fnd = false;
                            for (int j = 0; j < allChunks.size(); j++) {
                                MemoryChunk c = allChunks.get(j);
                                if (memLoc >= c.offset && memLoc <= c.offset+c.size) {
                                    if (cLast != c) {
                                        cellColored = cellsColored[(nIdx++)%cellsColored.length];
                                        cLast = c;
                                    }
                                    fnd = true;
                                    break;
                                }
                            }
                            if (!fnd) {
                                cLast = null;
                                content += cellGreen;
                            } else {

                                content += cellColored;
                            }
                            if (cLast!=null&&cLast.tag()!=null) {
                                content += cLast.tag();
                            }
                            content += cellClose;
                        }
                        content += close;
                    }
                    long lastEnd = 0;
//                    for (int j = 0; j < allChunks.size(); j++) {
//                        MemoryChunk c = allChunks.get(j);
//                        long freeSpace = c.offset-lastEnd;
//                        if (freeSpace > 0) {
//                            System.out.println("Free 0x"+Long.toHexString(lastEnd)+" to 0x"+Long.toHexString(c.offset));
//                        }
//                        lastEnd = c.offset+c.size;
//                        System.out.println(c);
//                    }
//                    break;
//                }
                content += close;
                content += close;
                response = header + nav + content + footer;
            }
            
        }
        resp.getHeaders().add("Content-Type", "text/html");
        resp.send(200, response);
        return 0;
    }
}
