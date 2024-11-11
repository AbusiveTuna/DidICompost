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
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.*;
import java.util.regex.Matcher;
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
	private DidICompostConfig config;

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
	private static final Collection<Integer> compostIds = Set.of(ItemID.COMPOST, ItemID.SUPERCOMPOST, ItemID.ULTRACOMPOST, ItemID.BOTTOMLESS_COMPOST_BUCKET_22997);

	int currentPatch = 0;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked menuClicked)
	{
		boolean isCompost = false;
		MenuAction action = menuClicked.getMenuAction();
		if(action == WIDGET_TARGET_ON_GAME_OBJECT)
		{
			Widget w = client.getSelectedWidget();
			if(w != null){
				isCompost = w.getId() == ComponentID.SPELLBOOK_FERTILE_SOIL || compostIds.contains(w.getItemId());
			}
		}
		if(action == GAME_OBJECT_FIFTH_OPTION)
		{
			isCompost = "Inspect".equals(menuClicked.getMenuOption());
		}

		if (!isCompost)
		{
			return;
		}

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
		String compostType = "";
		Matcher matcher;
		if((matcher = COMPOST_USED_ON_PATCH.matcher(messageString)).matches() ||
				(matcher = FERTILE_SOIL_CAST.matcher(messageString)).find() ||
				(matcher = ALREADY_TREATED.matcher(messageString)).matches() ||
				(matcher = INSPECT_PATCH.matcher(messageString)).matches() )
		{

				String compostGroup = matcher.group("compostType");

				switch(compostGroup){

					case "ultra":
						compostType = "ultra";
						break;

					case "super":
						compostType = "super";
						break;

					default:
						compostType = "compost";
						break;
				}

		}

		if(compostType == "ultra" || compostType == "super" || compostType == "compost")
		{
			addPatch(currentPatch);
		}

		if((matcher = CLEAR_PATCH.matcher(messageString)).matches() ||
				(matcher = CLEAR_HERB.matcher(messageString)).matches() ||
				(matcher = CLEAR_TREE.matcher(messageString)).matches() ||
				(matcher = INSPECT_PATCH_NONE.matcher(messageString)).matches() ||
				(matcher = CLEAR_ALLOTMENT.matcher(messageString)).matches() ||
				(matcher = CLEAR_SEAWEED.matcher(messageString)).matches() ||
				(matcher = CLEAR_MUSHROOM.matcher(messageString)).matches() ||
				(matcher = CLEAR_BELLA.matcher(messageString)).matches()){

			deletePatch(currentPatch);
			FarmingPatches patch = FarmingPatches.fromPatchId(currentPatch);
			if (patch != null) {
				patchOverlay.getNeedsCompostPoints().add(patch.getTile());
			}
		}
	}

	public void addPatch(int currentPatch)
	{
		FarmingPatches newPatch = FarmingPatches.fromPatchId(currentPatch);

		if(newPatch != null)
		{
			patchOverlay.getWorldPoints().add(newPatch.getTile());
			patchOverlay.getNeedsCompostPoints().remove(newPatch.getTile());
		}
	}

	public void deletePatch(int currentPatch)
	{
		FarmingPatches oldPatch = FarmingPatches.fromPatchId(currentPatch);
		if(oldPatch != null)
		{
			patchOverlay.getWorldPoints().remove(oldPatch.getTile());
		}
	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(patchOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(patchOverlay);
	}

	@Provides
	DidICompostConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DidICompostConfig.class);
	}
}
