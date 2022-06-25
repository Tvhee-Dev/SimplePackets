package me.tvhee.simplepackets.channel;

import org.bukkit.event.Cancellable;

public final class SimpleCancellable implements Cancellable
{
	private boolean cancelled;

	@Override
	public boolean isCancelled()
	{
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel)
	{
		cancelled = cancel;
	}
}
