package me.tvhee.simplepackets;

import java.util.ArrayList;
import java.util.List;
import me.tvhee.simplepackets.channel.PacketChannel;
import me.tvhee.simplepackets.handler.PacketHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

public final class SimplePackets implements Listener
{
	private static final List<PacketHandler> handlers = new ArrayList<>();
	private PacketChannel channel;
	private boolean injected = false;

	public static List<PacketHandler> getHandlers()
	{
		return new ArrayList<>(handlers);
	}

	public void addHandler(PacketHandler handler)
	{
		handlers.add(handler);
	}

	public void removeHandler(PacketHandler handler)
	{
		handlers.remove(handler);
	}

	public void onPluginLoad()
	{
		inject();

		if(injected)
			channel.addServerChannel();
	}

	public void onPluginEnable(Plugin plugin)
	{
		if(!injected)
		{
			plugin.getLogger().warning("Injection failed. Disabling...");
			Bukkit.getPluginManager().disablePlugin(plugin);
			return;
		}

		Bukkit.getPluginManager().registerEvents(this, plugin);

		for(Player player : Bukkit.getOnlinePlayers())
			this.channel.addChannel(player);
	}

	public void onPluginDisable()
	{
		if(!injected)
			return;

		for(Player player : Bukkit.getOnlinePlayers())
			channel.removeChannel(player);

		while(!getHandlers().isEmpty())
			removeHandler(getHandlers().get(0));
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e)
	{
		Player player = e.getPlayer();
		channel.addChannel(player);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e)
	{
		Player player = e.getPlayer();
		channel.removeChannel(player);
	}

	private void inject()
	{
		List<Exception> exceptions = new ArrayList<>();

		try
		{
			Class.forName("net.minecraft.util.io.netty.channel.Channel");
			throw new UnsupportedOperationException("Unsupported Server Version!");
		}
		catch(Exception e)
		{
			exceptions.add(e);
		}

		try
		{
			Class.forName("io.netty.channel.Channel");
			channel = new PacketChannel();
			this.injected = true;
			return;
		}
		catch(Exception e)
		{
			exceptions.add(e);
		}

		for(Exception e : exceptions)
			e.printStackTrace();

		this.injected = false;
	}
}
