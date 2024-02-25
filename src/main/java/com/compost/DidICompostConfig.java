package com.compost;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("didICompost")
public interface DidICompostConfig extends Config
{
        @ConfigItem(
                keyName = "iconSize",
                name = "Compost Icon Size",
                description = "Choose the size of the bucket icon",
                position = 1
        )
    default CompostIconSize iconSize()
        {
            return CompostIconSize.MEDIUM;
        }

}
