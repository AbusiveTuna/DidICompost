package com.compost;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.coords.WorldPoint;

import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum FarmingPatches
{
    ARDY_ALLOTMENT_NORTH(8555, new WorldPoint(2667,3371,0)),
    ARDY_ALLOTMENT_SOUTH(8554, new WorldPoint(2667,3378,0)),
    ARDY_FLOWER(7849, new WorldPoint(2667,3374,0)),
    ARDY_HERB(8152, new WorldPoint(2670,3374,0)),

    FALLY_ALLOTMENT_EAST(8551, new WorldPoint(3055,3304,0)),
    FALLY_ALLOTMENT_WEST(8550, new WorldPoint(3051,3307,0)),
    FALLY_FLOWER(7847, new WorldPoint(3054,3307,0)),
    FALLY_HERB(8150, new WorldPoint(3058,3311,0)),

    MORY_ALLOTMENT_EAST(8557, new WorldPoint(3602,3522,0)),
    MORY_ALLOTMENT_WEST(8556, new WorldPoint(3598,3525,0)),
    MORY_FLOWER(7850, new WorldPoint(3602,3525,0)),
    MORY_HERB(8153, new WorldPoint(3605,3529,0)),

    CATHERBY_ALLOTMENT_NORTH(8552, new WorldPoint(2806,3466,0)),
    CATHERBY_ALLOTMENT_SOUTH(8553, new WorldPoint(2806,3461,0)),
    CATHERBY_FLOWER(7848, new WorldPoint(2810,3463,0)),
    CATHERBY_HERB(8151, new WorldPoint(2813,3463,0)),

    HOSIDIUS_ALLOTMENT_EAST(27113, new WorldPoint(1738,3554,0)),
    HOSIDIUS_ALLOTMENT_WEST(27114, new WorldPoint(1735,3551,0)),
    HOSIDIUS_FLOWER(27111, new WorldPoint(1735,3554,0)),
    HOSIDIUS_HERB(27115, new WorldPoint(1738,3551,0)),

    VARLAMORE_ALLOTMENT_NORTH(50696, new WorldPoint(1582,3100,0)),
    VARLAMORE_ALLOTMENT_SOUTH(50695, new WorldPoint(1586,3095,0)),
    VARLAMORE_FLOWER(50693, new WorldPoint(1585,3098,0)),
    VARLAMORE_HERB(50697, new WorldPoint(1582,3095,0)),

    HARMONY_HERB(9372, new WorldPoint(3790,2838,0)),
    HARMONY_ALLOTMENT(21950, new WorldPoint(3794,2837,0)),

    FARMING_GUILD_ALLOTMENT_NORTH(33694, new WorldPoint(1267,3732,0)),
    FARMING_GUILD_ALLOTMENT_SOUTH(33693, new WorldPoint(1267,3727,0)),
    FARMING_GUILD_FLOWER(33649, new WorldPoint(1260,3726,0)),
    FARMING_GUILD_HERB(33979, new WorldPoint(1239,3727,0)),

    PRIFF_ALLOTMENT_NORTH(34922, new WorldPoint(3289,6101,0)),
    PRIFF_ALLOTMENT_SOUTH(34921, new WorldPoint(3289,6098,0)),
    PRIFF_FLOWER(34919, new WorldPoint(3293,6100,0)),

    TROLL_HERB(18816, new WorldPoint(2827,3694,0)),
    WEISS_HERB(33176, new WorldPoint(2848,3935,0)),

    //Hops
    CHAMPION_GUILD_HOP(8175, new WorldPoint(3231,3317,0)),
    MCGRUBOR_HOP(8176, new WorldPoint(2669,3523,0)),
    YANILLE_HOP(8173, new WorldPoint(2577,3106,0)),
    ENTRANA_HOP(8174, new WorldPoint(2812,3338,0)),
    VARLAMORE_HOP(55341, new WorldPoint(1366,2940,0)),

    //Bushes
    FARMING_GUILD_BUSH(34006, new WorldPoint(1260,3733,0)),
    CHAMPION_GUILD_BUSH(7577, new WorldPoint(3182,3358,0)),
    RIMMINGTON_BUSH(7578, new WorldPoint(2941,3222,0)),
    ARDY_BUSH(7580, new WorldPoint(2617,3226,0)),
    ETCETERA_BUSH(7579, new WorldPoint(2592,3863,0)),

    //Trees
    FALLY_TREE(8389, new WorldPoint(3003,3374,0)),
    TALVERY_TREE(8388, new WorldPoint(2935,3439,0)),
    FARMING_GUILD_TREE(33732, new WorldPoint(1233,3735,0)),
    LUMBRIDGE_TREE(8391, new WorldPoint(3194,3232,0)),
    VARROCK_TREE(8390, new WorldPoint(3228,3458,0)),
    GNOME_STRONGHOLD_TREE(19147, new WorldPoint(2437,3416,0)),

    //Fruit trees
    CATHERBY_FRUIT(7965, new WorldPoint(2860,3433,0)),
    FARMING_GUILD_FRUIT(34007, new WorldPoint(1243,3758,0)),
    GNOME_STRONGHOLD_FRUIT(7962, new WorldPoint(2475,3446,0)),
    GNOME_VILLIAGE_FRUIT(7963, new WorldPoint(2490,3180,0)),
    BRIMHAVEN_FRUIT(7964, new WorldPoint(2765,3212,0)),
    LLETYA_FRUIT(26579, new WorldPoint(2346,3162,0)),

    //Spirit trees
    FARMING_GUILD_SPIRIT(33733, new WorldPoint(1252,3751,0)),
    ETCETERA_SPIRIT(8382, new WorldPoint(2612,3857,0)),
    PORT_SARIM_SPIRIT(8338, new WorldPoint(3059,3257,0)),
    BRIMHAVEN_SPIRIT(8383, new WorldPoint(2801,3204,0)),
    HOSIDIUS_SPIRIT(27116, new WorldPoint(1692,3541,0)),

    //Seaweed
    SEAWEED_NORTH(30500, new WorldPoint(3733,10273,1)),
    SEAWEED_SOUTH(30501, new WorldPoint(3733,10268,1)),

    //Hard(Kappa)wood
    HARDWOOD_WEST(30481, new WorldPoint(3703,3836,0)),
    HARDWOOD_SOUTH(30480, new WorldPoint(3707,3834,0)),
    HARDWOOD_EAST(30482, new WorldPoint(3714,3836,0)),
    VARLAMORE_HARDWOOD(50692, new WorldPoint(1687,2973,0)),

    //Special
    ALKHARID_CACTUS(7771, new WorldPoint(3315,3203,0)),
    FARMING_GUILD_CACTUS(33761, new WorldPoint(1264,3747,0)),
    FARMING_GUILD_CELSTRUS(34629, new WorldPoint(1245,3751,0)),
    FARMING_GUILD_REDWOOD(34059, new WorldPoint(1232,3753,0)),
    PRIFF_CRYSTAL_TREE(34906, new WorldPoint(3291,6118,0)),
    CANFIS_MUSHROOM(8337, new WorldPoint(3452,3472,0)),
    DRAYNOR_BELL(7572, new WorldPoint(3087,3354,0)),
    TAIBWO_CALQUAT(7807, new WorldPoint(2796,3100,0));

    private final int patchId;
    private final WorldPoint tile;

    private static final Map<Integer, FarmingPatches> PATCH_MAP = new HashMap<>();

    static {
        for (FarmingPatches e: values()) {
            PATCH_MAP.put(e.patchId, e);
        }
    }

    public static FarmingPatches fromPatchId(int itemId) {
        return PATCH_MAP.get(itemId);
    }
}
