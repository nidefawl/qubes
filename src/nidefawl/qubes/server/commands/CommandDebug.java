package nidefawl.qubes.server.commands;

import java.util.Collection;
import java.util.Collections;
import nidefawl.qubes.biome.Biome;
import nidefawl.qubes.entity.Player;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.inventory.slots.SlotStack;
import nidefawl.qubes.item.Item;
import nidefawl.qubes.item.ItemStack;
import nidefawl.qubes.server.GameServer;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.StringUtil;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.world.WorldServer;
import nidefawl.qubes.world.biomes.HexBiome;
import nidefawl.qubes.world.biomes.HexBiomesServer;

public class CommandDebug extends Command {

    public CommandDebug() {
        super("debug");
    }

    public void execute(ICommandSource source, String cmd, String[] args, String line) {
        GameServer server = source.getServer();
        if (args.length > 0 && source instanceof PlayerServer) {
            source.sendMessage(args[0]);
            switch (args[0]) {
                case "regen":{
                    Vec3D pos = ((Player)source).pos;
                    HexBiome h = source.getWorld().getHex(GameMath.floor(pos.x), GameMath.floor(pos.z));
                    Collection<Long> chunks = h.getChunks();
                    int n = ((WorldServer)source.getWorld()).regenChunks(chunks);
                        source.sendMessage("Regenerating "+n+" chunks...");
                    }
                    return;
                case "deletechunks":
                    int n = ((WorldServer)source.getWorld()).deleteAllChunks();
                    source.sendMessage("Deleted "+n+" chunks");
                    return;
                case "give":
                    int id = StringUtil.parseInt(args[1], 0);
                    Item item = Item.get(id);
                    if (item == null)return;
                    source.sendMessage(""+item.getName());
                    ((PlayerServer)source).recvItem(new ItemStack(item));
                    ((PlayerServer)source).syncInventory();
                    return;
                case "pos":
                    Vec3D pos = ((Player)source).pos;
                    System.out.println(String.format("%.0f %.0f %.0f", pos.x, pos.y, pos.z));
                    return;
                case "wipeInv":
                    ((PlayerServer)source).getInventory().set(Collections.<SlotStack>emptyList());
                    ((PlayerServer)source).recvItem(new ItemStack(Item.axe));
                    ((PlayerServer)source).recvItem(new ItemStack(Item.pickaxe));
                    ((PlayerServer)source).syncInventory();
                    return;
                case "setbiome":
                {

                    pos = ((Player)source).pos;
                    WorldServer wserv = (WorldServer) ((Player)source).world;
                    HexBiome grid = ((HexBiomesServer)wserv.biomeManager).blockToHex((int)pos.x, (int)pos.z);
                    Biome b = Biome.get(Integer.parseInt(args[1]));
                    grid.biome = b;
                }
                   return;
                case "getbiome":
                {

                    pos = ((Player)source).pos;
                    WorldServer wserv = (WorldServer) ((Player)source).world;
                    HexBiome grid = ((HexBiomesServer)wserv.biomeManager).blockToHex((int)pos.x, (int)pos.z);
                    source.sendMessage(""+grid.biome.getClass().getSimpleName()+", "+(grid.subtype%2));
                }
                   return;
            }
        }
    }
}
