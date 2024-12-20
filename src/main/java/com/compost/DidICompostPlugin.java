package com.compost;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.regex.Pattern;

import static net.runelite.api.MenuAction.GAME_OBJECT_FIFTH_OPTION;
import static net.runelite.api.MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;

@Slf4j
@PluginDescriptor(
	name = "Did I Compost?"
)
public class DidICompostPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private PatchOverlay patchOverlay;

	@Inject
	private OverlayManager overlayManager;

	private static final Pattern COMPOST_USED_ON_PATCH = Pattern.compile(
			"You treat the .+ with (?<compostType>ultra|super|)compost\\.");
	private static final Pattern FERTILE_SOIL_CAST = Pattern.compile(
			"^The .+ has been treated with (?<compostType>ultra|super|)compost");
	private static final Pattern ALREADY_TREATED = Pattern.compile(
			"This .+ has already been (treated|fertilised) with (?<compostType>ultra|super|)compost(?: - the spell can't make it any more fertile)?\\.");
	private static final Pattern INSPECT_PATCH = Pattern.compile(
			"This is an? .+\\. The soil has been treated with (?<compostType>ultra|super|)compost\\..*");

	private static final Pattern INSPECT_PATCH_NONE = Pattern.compile(
			"This is an? .+\\. The soil has not been treated.*");

	private static final Pattern CLEAR_HERB = Pattern.compile("The herb patch is now empty.*");
	private static final Pattern CLEAR_PATCH = Pattern.compile("You have successfully cleared this patch for new crops.*");
	private static final Pattern CLEAR_TREE = Pattern.compile("You examine the tree for signs of disease and find that it is in perfect health.*");
	private static final Pattern CLEAR_ALLOTMENT = Pattern.compile("The allotment is now empty.*");
	private static final Pattern CLEAR_SEAWEED = Pattern.compile("You pick some giant seaweed.*");
	private static final Pattern CLEAR_BELLA = Pattern.compile("You pick some deadly nightshade.*");
	private static final Pattern CLEAR_MUSHROOM = Pattern.compile("You pick a Bittercap mushroom.*");
	
	private static final ImmutableSet<Integer> COMPOST_ITEMS = ImmutableSet.of(
			ItemID.COMPOST,
			ItemID.SUPERCOMPOST,
			ItemID.ULTRACOMPOST,
			ItemID.BOTTOMLESS_COMPOST_BUCKET_22997
	);

	private int currentPatch = 0;

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getNewValue() == null || !"didICompost".equals(event.getGroup()))
		{
			return;
		}

		if ("iconSize".equals(event.getKey()))
		{
			patchOverlay.updateImages();
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked menuClicked)
	{
		ObjectComposition patchDef = client.getObjectDefinition(menuClicked.getId());
		//avoids swapping the id to random objects
		FarmingPatches patch = FarmingPatches.fromPatchId(patchDef.getId());
		if (patch != null)
		{
			currentPatch = patch.getPatchId();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage message)
	{
		String messageString = message.getMessage();
		if (COMPOST_USED_ON_PATCH.matcher(messageString).matches() ||
				FERTILE_SOIL_CAST.matcher(messageString).find() ||
				ALREADY_TREATED.matcher(messageString).matches() ||
				INSPECT_PATCH.matcher(messageString).matches())
		{
			addPatch(currentPatch);
			return;
		}

		if (CLEAR_PATCH.matcher(messageString).matches() ||
				CLEAR_HERB.matcher(messageString).matches() ||
				CLEAR_TREE.matcher(messageString).matches() ||
				INSPECT_PATCH_NONE.matcher(messageString).matches() ||
				CLEAR_ALLOTMENT.matcher(messageString).matches() ||
				CLEAR_SEAWEED.matcher(messageString).matches() ||
				CLEAR_MUSHROOM.matcher(messageString).matches() ||
				CLEAR_BELLA.matcher(messageString).matches()) {
			deletePatch(currentPatch);
			FarmingPatches patch = FarmingPatches.fromPatchId(currentPatch);
			if (patch != null) {
				patchOverlay.getNeedsCompostPoints().add(patch.getTile());
			}
		}
	}

	private void addPatch(int currentPatch)
	{
		FarmingPatches newPatch = FarmingPatches.fromPatchId(currentPatch);

		if(newPatch != null)
		{
			patchOverlay.getWorldPoints().add(newPatch.getTile());
			patchOverlay.getNeedsCompostPoints().remove(newPatch.getTile());
		}
	}

	private void deletePatch(int currentPatch)
	{
		FarmingPatches oldPatch = FarmingPatches.fromPatchId(currentPatch);
		if(oldPatch != null)
		{
			patchOverlay.getWorldPoints().remove(oldPatch.getTile());
		}
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(patchOverlay);
		patchOverlay.updateImages();
	}

	@Override
	protected void shutDown()
	{
		currentPatch = 0;
		overlayManager.remove(patchOverlay);
		patchOverlay.reset();
	}

	@Provides
	DidICompostConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DidICompostConfig.class);
	}
}
