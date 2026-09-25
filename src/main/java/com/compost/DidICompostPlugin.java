package com.compost;

import com.google.inject.Provides;
import javax.inject.Inject;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Did I Compost?"
)
public class DidICompostPlugin extends Plugin
{
	@Inject
	private PatchOverlay patchOverlay;

	@Inject
	private OverlayManager overlayManager;

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

	@Override
	protected void startUp()
	{
		overlayManager.add(patchOverlay);
		patchOverlay.updateImages();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(patchOverlay);
		patchOverlay.reset();
	}

	@Provides
	DidICompostConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DidICompostConfig.class);
	}
}
