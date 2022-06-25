package me.tvhee.simplepackets.handler;

import java.util.Objects;
import me.tvhee.simplereflection.MCReflectionUtil;
import me.tvhee.simplereflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class PacketHandler
{
    private final Class<?> classEntityPlayer = MCReflectionUtil.getClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer");
    private final Class<?> classPlayerConnection = MCReflectionUtil.getClass("{nms}.PlayerConnection", "{nms}.server.network.PlayerConnection");
    private final Plugin plugin;

    public PacketHandler(Plugin plugin)
    {
		if(plugin == null)
			throw new IllegalArgumentException("plugin == null!");

        this.plugin = plugin;
    }

	public Plugin getPlugin()
	{
		return this.plugin;
	}

    public abstract void onSend(SimplePacket packet);

    public abstract void onReceive(SimplePacket packet);

    public final void sendPacket(Player player, Object packet)
    {
        if(player == null || packet == null)
            throw new NullPointerException();

        try
        {
            Reflection handle = new Reflection(player).method(classEntityPlayer);
			Reflection connection;

            try
            {
                connection = handle.field(classPlayerConnection);
            }
            catch(Exception e)
            {
                connection = handle.field("playerConnection");
            }

            if(connection == null)
                throw new IllegalStateException("failed to find the playerConnection field");

            connection.method("sendPacket", packet);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

	@Override
	public final boolean equals(Object object)
	{
		if(this == object)
            return true;

		if(object == null || getClass() != object.getClass())
            return false;

		PacketHandler that = (PacketHandler) object;
		return Objects.equals(plugin, that.plugin);
	}

	@Override
	public String toString()
	{
		return "PacketHandler{plugin=" + plugin + '}';
	}
}
