package com.compost;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.ArrayList;
import java.util.Arrays;
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
	private static final int MAX_DRAW_DISTANCE = 30;
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

	private static final ImmutableSet<Integer> COMPOST_ITEMS = ImmutableSet.of(
			ItemID.COMPOST,
			ItemID.SUPERCOMPOST,
			ItemID.ULTRACOMPOST,
			ItemID.BOTTOMLESS_COMPOST_BUCKET_22997
	);

	//final Map<FarmingPatch, PendingCompost> pendingCompostActions = new HashMap<>();

	private static final ArrayList<Integer> compostIds = new ArrayList<>(Arrays.asList(ItemID.COMPOST, ItemID.SUPERCOMPOST, ItemID.ULTRACOMPOST, ItemID.BOTTOMLESS_COMPOST_BUCKET_22997));

	int currentPatch = 0;
	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked menuClicked)
	{

		Boolean isCompost = false;
		MenuAction test = menuClicked.getMenuAction();
		if(test == WIDGET_TARGET_ON_GAME_OBJECT){
			Widget w = client.getSelectedWidget();
			if(w != null){
				if(compostIds.contains(w.getItemId())){
					isCompost = true;
				}
				if(w.getId() == WidgetInfo.SPELL_LUNAR_FERTILE_SOIL.getPackedId()){
					isCompost = true;
				}

			}
		}
		if(test == GAME_OBJECT_FIFTH_OPTION){
				isCompost = "Inspect".equals(menuClicked.getMenuOption());
		}


		ObjectComposition patchDef = client.getObjectDefinition(menuClicked.getId());
		if(patchDef.getId() == 8555){

			currentPatch = patchDef.getId();
			//update something with patch ID
		}

		//regionID 10548 location 44,43


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

		if(compostType == "ultra" || compostType == "super" || compostType == "compost"){
			updatePatch(currentPatch,compostType);
		}




	}

	public void updatePatch(int currentPatch, String compostType){

		System.out.println(currentPatch);
		System.out.println(compostType);
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

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.greeting(), null);
		}
	}

	@Provides
	DidICompostConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DidICompostConfig.class);
	}
}
