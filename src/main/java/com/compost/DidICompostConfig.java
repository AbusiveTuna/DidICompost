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

    @ConfigItem(
            keyName = "showNeedsCompost",
            name = "Show Needs Compost",
            description = "Shows text over empty patches that need composting",
            position = 2
    )
    default boolean showNeedsCompost()
    {
        return false;
    }

    @ConfigItem(
            keyName = "showAppliedCompost",
            name = "Show Applied Compost",
            description = "Shows icon over patches that already have compost applied",
            position = 3
    )
    default boolean showAppliedCompost()
    {
        return true;
    }
}
