package me.tvhee.simplepackets.channel;

import java.net.SocketAddress;

public abstract class ChannelWrapper<T>
{
	private final T channel;

	public ChannelWrapper(T channel)
	{
		this.channel = channel;
	}

	public T channel()
	{
		return this.channel;
	}

	public abstract SocketAddress getRemoteAddress();

	public abstract SocketAddress getLocalAddress();
}
