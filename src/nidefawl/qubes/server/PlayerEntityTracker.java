/**
 * 
 */
package nidefawl.qubes.server;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import nidefawl.qubes.entity.Entity;
import nidefawl.qubes.entity.PlayerServer;
import nidefawl.qubes.network.packet.PacketSEntityMove;
import nidefawl.qubes.network.packet.PacketSEntityTrack;
import nidefawl.qubes.network.packet.PacketSEntityUnTrack;
import nidefawl.qubes.util.GameMath;
import nidefawl.qubes.util.Side;
import nidefawl.qubes.util.SideOnly;
import nidefawl.qubes.vec.Vec3D;
import nidefawl.qubes.world.WorldServer;

/**
 * @author Michael Hept 2015
 * Copyright: Michael Hept
 */
@SideOnly(value = Side.SERVER)
public class PlayerEntityTracker {
    //TODO: use list + map to speed up update iteration
    Map<Integer, Entry> map = Maps.newHashMap();
    private PlayerServer player;
    static final class Entry {
        public Entry(Entity entity) {
            this.entity = entity;
            this.pos = new Vec3D(this.entity.pos);
            this.yaw = this.entity.yaw;
            this.pitch = this.entity.pitch;
            this.yawBodyOffset = this.entity.yawBodyOffset;
        }
        Entity entity;
        public Vec3D pos;
        public float yaw, pitch;
        public float yawBodyOffset;
    }
    /**
     * @param player 
     * 
     */
    public PlayerEntityTracker(PlayerServer player) {
        this.player = player;
    }
    /**
     * 
     */
    public void leaveWorld() {
        map.clear();
    }

    public void joinWorld(WorldServer worldServer) {
        //TODO: add range check or do chunk based
        List<Entity> ents = worldServer.entityList;
        int size = ents.size();
        for (int i = 0; i < size; i++) {
            Entity e = ents.get(i);
            if (e != this.player) {
                track(ents.get(i));    
            }
        }
    }
    
    public void update() {
        if (this.map.isEmpty()) {
            return;
        }
        Iterator<Entry> iterator = this.map.values().iterator();
        
        while (iterator.hasNext()) {
            Entry entry = iterator.next();
            Entity entity = entry.entity;
            double d = entry.pos.distanceSq(entity.pos);
            float dyaw = Math.abs(GameMath.wrapAngle(entry.yaw-entity.yaw));
            float dyawB = Math.abs(entry.yawBodyOffset-entity.yawBodyOffset);
            float dpitch = Math.abs(entry.pitch-entity.pitch);
            int flags = 0;
            if (d > 1.0E-4) {
                entry.pos.set(entity.pos);
                flags |= 1;
            }
            if (dyaw > 1.0E-4 || dpitch > 1.0E-4 || dyawB > 1.0E-4) {
//                System.out.println("rot update "+dyaw);
                entry.yaw = entity.yaw;
                entry.pitch = entity.pitch;
                entry.yawBodyOffset = entity.yawBodyOffset;
                flags |= 2;
            }
            if (flags > 0) {
                PacketSEntityMove p = new PacketSEntityMove(entity.id, flags, entry.pos, entry.pitch, entry.yaw, entry.yawBodyOffset);
                this.player.sendPacket(p);
            }
        }
    }

    public void track(Entity ent) {
        if (ent == this.player)
            return;
        Entry tNew = new Entry(ent);
        Entry t = map.put(ent.id, tNew);
        if (t != null) { //must not happen
            System.err.println("Entity is already tracked");
            return;
        }
        //TODO: buffer add/remove/updates
        PacketSEntityTrack p = getPacket(tNew);
        if (p != null)
            this.player.sendPacket(p);
    }

    /**
     * @param t
     * @return
     */
    private PacketSEntityTrack getPacket(Entry t) {
        System.out.println(t.pos);
        
        PacketSEntityTrack packet = new PacketSEntityTrack();
        packet.entId = t.entity.id;
        packet.entType = t.entity.getEntityType().id;
        packet.pos = new Vec3D(t.pos);
        packet.yaw = t.yaw;
        packet.yawbody = t.yawBodyOffset;
        packet.pitch = t.pitch;
        //TODO: compress data
        packet.data = t.entity.writeClientData(false);
        return packet;
    }
    public void untrack(Entity ent) {
        if (ent == this.player)
            return;
        Entry t = map.remove(ent.id);
        if (t == null) { //must not happen
            System.err.println("Entity is not tracked");
            return;
        }
        //TODO: buffer add/remove/updates
        PacketSEntityUnTrack p = new PacketSEntityUnTrack(ent.id);
        this.player.sendPacket(p);
    }

}
