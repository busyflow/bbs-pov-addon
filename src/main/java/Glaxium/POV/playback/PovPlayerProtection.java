package Glaxium.POV.playback;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.IdentityHashMap;
import java.util.Map;

/** Reference-counted protection for the real player borrowed by BBS playback. */
public final class PovPlayerProtection
{
    private static final Map<ServerPlayerEntity, Integer> PLAYERS = new IdentityHashMap<>();

    private PovPlayerProtection() {}

    public static synchronized void acquire(ServerPlayerEntity player)
    {
        PLAYERS.merge(player, 1, Integer::sum);
    }

    public static synchronized void release(ServerPlayerEntity player)
    {
        Integer count = PLAYERS.get(player);

        if (count == null || count <= 1)
        {
            PLAYERS.remove(player);
        }
        else
        {
            PLAYERS.put(player, count - 1);
        }
    }

    public static synchronized boolean contains(ServerPlayerEntity player)
    {
        return PLAYERS.containsKey(player);
    }
}
