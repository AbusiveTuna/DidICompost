package com.compost;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("example")
public interface DidICompostConfig extends Config
{
	@ConfigItem(
		keyName = "compost",
		name = "Compost",
		description = "compost"
	)
	default String greeting()
	{
		return "compost";
	}
}
