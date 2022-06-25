package me.tvhee.simplepackets.handler;

import me.tvhee.simplereflection.MCReflectionUtil;
import me.tvhee.simplereflection.Reflection;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import me.tvhee.simplepackets.channel.ChannelWrapper;

public final class SimplePacket
{
	private final Class<?> classPacket = MCReflectionUtil.getClass("{nms}.Packet", "{nms}.network.protocol.Packet");
	private final Cancellable cancellable;
	private final PacketType packetType;
	private Reflection packetReflected;
	private Player player;
	private ChannelWrapper<?> channelWrapper;
	private Object packet;

	public SimplePacket(Object packet, Cancellable cancellable, PacketType packetType, Player player)
	{
		this.packetType = packetType;
		this.player = player;
		this.packet = packet;
		this.cancellable = cancellable;
		this.packetReflected = new Reflection(packet);
	}

	public SimplePacket(Object packet, Cancellable cancellable, PacketType packetType, ChannelWrapper<?> channelWrapper)
	{
		this.packetType = packetType;
		this.channelWrapper = channelWrapper;
		this.packet = packet;
		this.cancellable = cancellable;
		this.packetReflected = new Reflection(packet);
	}

	public PacketType getPacketType()
	{
		return packetType;
	}

	public void setPacketValue(String field, Object value)
	{
		packetReflected.field(field, value);
	}

	public Object getPacketValue(String field)
	{
		return packetReflected.field(field).object();
	}

	public Reflection getPacketReflected()
	{
		return packetReflected;
	}

	public void setCancelled(boolean cancel)
	{
		this.cancellable.setCancelled(cancel);
	}

	public boolean isCancelled()
	{
		return this.cancellable.isCancelled();
	}

	public Player getPlayer()
	{
		return this.player;
	}

	public boolean hasPlayer()
	{
		return this.player != null;
	}

	public ChannelWrapper<?> getChannel()
	{
		return this.channelWrapper;
	}

	public boolean hasChannel()
	{
		return this.channelWrapper != null;
	}

	public String getPlayerName()
	{
		if(!this.hasPlayer())
			return null;

		return this.player.getName();
	}

	public void setPacket(Object packet)
	{
		if(packet instanceof Reflection)
			packet = ((Reflection) packet).object();

		if(!classPacket.isInstance(packet))
			throw new IllegalArgumentException("Packet is not an instance of " + classPacket.getName() + "!");

		this.packet = packet;
		this.packetReflected = new Reflection(packet);
	}

	public Object getPacket()
	{
		return this.packet;
	}

	public String getPacketName()
	{
		return this.packet.getClass().getSimpleName();
	}

	@Override
	public String toString()
	{
		return "Packet{ " + (packetType == PacketType.SENT_BY_SERVER ? "[> OUT >]" : "[< IN <]") + " " + this.getPacketName() + " " + (this.hasPlayer() ? this.getPlayerName() : this.hasChannel() ? this.getChannel().channel() : "#server#") + " }";
	}
}
