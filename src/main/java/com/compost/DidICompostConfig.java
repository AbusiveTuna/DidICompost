package com.compost;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("didICompost")
public interface DidICompostConfig extends Config
{
    @ConfigItem(
            keyName = "displayMode",
            name = "Display Mode",
            description = "Mark composted patches with an icon, by colouring their soil like the compost used, or both",
            position = 0
    )
    default DisplayMode displayMode()
    {
        return DisplayMode.ICON;
    }

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
            description = "Shows an icon over patches that have no compost applied",
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

    @ConfigSection(
            name = "Soil Colours",
            description = "The colour of composted soil in the soil colour display mode",
            position = 4
    )
    String soilColours = "soilColours";

    @ConfigItem(
            keyName = "compostColour",
            name = "Compost",
            description = "Soil colour of patches treated with compost",
            section = soilColours,
            position = 5
    )
    default Color compostColour()
    {
        return new Color(0x4C5337);
    }

    @ConfigItem(
            keyName = "supercompostColour",
            name = "Supercompost",
            description = "Soil colour of patches treated with supercompost",
            section = soilColours,
            position = 6
    )
    default Color supercompostColour()
    {
        return new Color(0x293120);
    }

    @ConfigItem(
            keyName = "ultracompostColour",
            name = "Ultracompost",
            description = "Soil colour of patches treated with ultracompost",
            section = soilColours,
            position = 7
    )
    default Color ultracompostColour()
    {
        return new Color(0x141519);
    }
}
