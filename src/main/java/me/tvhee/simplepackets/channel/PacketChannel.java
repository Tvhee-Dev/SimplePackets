package me.tvhee.simplepackets.channel;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import me.tvhee.simplepackets.SimplePackets;
import me.tvhee.simplepackets.handler.PacketHandler;
import me.tvhee.simplepackets.handler.PacketType;
import me.tvhee.simplepackets.handler.SimplePacket;
import me.tvhee.simplereflection.MCReflectionUtil;
import me.tvhee.simplereflection.Reflection;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class PacketChannel
{
	private final Executor addChannelExecutor = Executors.newSingleThreadExecutor();
	private final Executor removeChannelExecutor = Executors.newSingleThreadExecutor();
	private final String KEY_HANDLER = "packet_handler";
	private final String KEY_PLAYER = "packet_listener_player";
	private final String KEY_SERVER = "packet_listener_server";

	public void addChannel(Player player)
	{
		try
		{
			final io.netty.channel.Channel channel = getChannel(player);

			addChannelExecutor.execute(() ->
			{
				try
				{
					if(channel.pipeline().get(KEY_HANDLER) != null)
						channel.pipeline().addBefore(KEY_HANDLER, KEY_PLAYER, new ChannelHandler(player));
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			});
		}
		catch(ReflectiveOperationException e)
		{
			throw new RuntimeException("Failed to add channel for " + player, e);
		}
	}

	public void removeChannel(Player player)
	{
		try
		{
			final io.netty.channel.Channel channel = getChannel(player);

			removeChannelExecutor.execute(() ->
			{
				try
				{
					if(channel.pipeline().get(KEY_PLAYER) != null)
						channel.pipeline().remove(KEY_PLAYER);
				}
				catch(Exception e)
				{
					throw new RuntimeException(e);
				}
			});
		}
		catch(ReflectiveOperationException e)
		{
			throw new RuntimeException("Failed to remove channel for " + player, e);
		}
	}

	public void addServerChannel()
	{
		try
		{
			Class<?> classDedicatedServer = MCReflectionUtil.getClass("{nms}.DedicatedServer", "{nms}.server.dedicated.DedicatedServer");
			Class<?> classServerConnection = MCReflectionUtil.getClass("{nms}.ServerConnection", "{nms}.server.network.ServerConnection");
			Reflection serverConnection = new Reflection(Bukkit.getServer()).field(classDedicatedServer).field(classServerConnection);
			List<?> currentList = serverConnection.field(1, List.class).object();

			if(!currentList.isEmpty())
			{
				try
				{
					Reflection list = new Reflection(currentList).field("list").object();

					if(list.clazz().getName().startsWith("me.tvhee.packetlistener"))
						return;
				}
				catch(Exception ignored)
				{

				}
			}
			
			serverConnection.field(1, List.class, Collections.synchronizedList(new ListenerList<>(currentList)));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	private Channel getChannel(Player player) throws ReflectiveOperationException
	{
		Class<?> classNetworkManager = MCReflectionUtil.getClass("{nms}.NetworkManager", "{nms}.network.NetworkManager");
		Class<?> classPlayerConnection = MCReflectionUtil.getClass("{nms}.PlayerConnection", "{nms}.server.network.PlayerConnection");
		Class<?> classEntityPlayer = MCReflectionUtil.getClass("{nms}.EntityPlayer", "{nms}.server.level.EntityPlayer");
		Reflection handle = new Reflection(player).method(classEntityPlayer);
		Reflection connection = handle.field(classPlayerConnection);
		return connection.field(classNetworkManager).field(Channel.class).object();
	}

	private class ListenerList<E> extends ArrayList<E>
	{
		public ListenerList(Collection<E> collection)
		{
			super(collection);
		}

		@Override
		public boolean add(E paramE)
		{
			try
			{
				final E a = paramE;
				addChannelExecutor.execute(() ->
				{
					try
					{
						io.netty.channel.Channel channel = null;

						while(channel == null)
							channel = new Reflection(a).field(io.netty.channel.Channel.class).object();

						if(channel.pipeline().get(KEY_SERVER) == null)
							channel.pipeline().addBefore(KEY_HANDLER, KEY_SERVER, new ChannelHandler(new NettyChannelWrapper(channel)));

					}
					catch(Exception ignored) {}
				});
			}
			catch(Exception ignored) {}

			return super.add(paramE);
		}

		@Override
		public boolean remove(Object arg0)
		{
			try
			{
				final Object a = arg0;
				removeChannelExecutor.execute(() ->
				{
					try
					{
						io.netty.channel.Channel channel = null;

						while(channel == null)
							channel = new Reflection(a).field(io.netty.channel.Channel.class).object();

						channel.pipeline().remove(KEY_SERVER);
					}
					catch(Exception ignored) {}
				});
			}
			catch(Exception ignored) {}

			return super.remove(arg0);
		}
	}

	private static class ChannelHandler extends ChannelDuplexHandler
	{
		private final Class<?> classPacket = MCReflectionUtil.getClass("{nms}.Packet", "{nms}.network.protocol.Packet");
		private final Object owner;

		public ChannelHandler(Player player)
		{
			this.owner = player;
		}

		public ChannelHandler(ChannelWrapper<?> channelWrapper)
		{
			this.owner = channelWrapper;
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
		{
			SimpleCancellable cancellable = new SimpleCancellable();
			Object packet = msg;

			if(classPacket.isAssignableFrom(msg.getClass()))
			{
				SimplePacket sentPacket;

				if(this.owner instanceof Player)
					sentPacket = new SimplePacket(packet, cancellable, PacketType.SENT_BY_SERVER, (Player) this.owner);
				else
					sentPacket = new SimplePacket(packet, cancellable, PacketType.SENT_BY_SERVER, (ChannelWrapper<?>) this.owner);

				for(PacketHandler packetHandler : SimplePackets.getHandlers())
				{
					try
					{
						packetHandler.onSend(sentPacket);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}

				if(sentPacket.getPacket() != null)
					packet = sentPacket.getPacket();
			}

			if(cancellable.isCancelled())
				return;

			super.write(ctx, packet, promise);
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
		{
			SimpleCancellable cancellable = new SimpleCancellable();
			Object packet = msg;

			if(classPacket.isAssignableFrom(msg.getClass()))
			{
				SimplePacket receivedPacket;

				if(this.owner instanceof Player)
					receivedPacket = new SimplePacket(packet, cancellable, PacketType.RECEIVED_BY_SERVER, (Player) this.owner);
				else
					receivedPacket = new SimplePacket(packet, cancellable, PacketType.RECEIVED_BY_SERVER, (ChannelWrapper<?>) this.owner);

				for(PacketHandler packetHandler : SimplePackets.getHandlers())
				{
					try
					{
						packetHandler.onReceive(receivedPacket);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}

				if(receivedPacket.getPacket() != null)
					packet = receivedPacket.getPacket();
			}

			if(cancellable.isCancelled())
				return;

			super.channelRead(ctx, packet);
		}

		@Override
		public String toString()
		{
			return "PacketChannel$ChannelHandler@" + hashCode() + " (" + this.owner + ")";
		}
	}

	private static class NettyChannelWrapper extends ChannelWrapper<io.netty.channel.Channel>
	{
		public NettyChannelWrapper(io.netty.channel.Channel channel)
		{
			super(channel);
		}

		@Override
		public SocketAddress getRemoteAddress()
		{
			return this.channel().remoteAddress();
		}

		@Override
		public SocketAddress getLocalAddress()
		{
			return this.channel().localAddress();
		}
	}
}
