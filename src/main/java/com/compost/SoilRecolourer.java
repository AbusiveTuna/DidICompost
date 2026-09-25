package com.compost;

import java.awt.Color;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.JagexColor;
import net.runelite.api.Model;
import net.runelite.api.Renderable;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.Scene;
import net.runelite.api.SceneTilePaint;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.hooks.DrawCallbacks;
import net.runelite.client.callback.RenderCallback;

/**
 * Colours the soil of composted patches.
 * <p>
 * What you see of most patches' soil is a dirt mound decoration on each of their tiles; a few
 * patches show soil in their own model instead. Those models are shared by every object in the
 * same state, so they can't simply be recoloured. Instead, as the renderer is about to read one
 * for a composted patch, the soil faces of the shared model are given that patch's colour, and
 * the model's own colours are put back as soon as the renderer moves on. The soil keeps any
 * texture the renderer gives it.
 * <p>
 * Renderers read models on several threads at once, so another thread could read a model while it
 * holds a composted patch's colours. Nothing waits for that; instead, such reads are noticed, and
 * the patches read are read again.
 * <p>
 * Once planted, the soil of patches without mounds is the terrain, which is recoloured directly,
 * except where 117HD would blend it away: there, a flat quad of the soil's colour is laid over it.
 */
@Slf4j
@Singleton
class SoilRecolourer implements RenderCallback
{
	/**
	 * The dirt mounds on every tile of most patches.
	 */
	private static final int MOUNDS = 7517;
	/**
	 * Hue and saturation (hsl >> 7) of soil faces, and the lightness of that soil: the usual soil,
	 * and the soil mound of the Troll Stronghold and Weiss herb patches.
	 */
	private static final short SOIL = 8724;
	private static final int[][] SOIL_SHADES = {{SOIL >> 7, SOIL & 127}, {7050 >> 7, 7050 & 127}};

	/**
	 * Patches without mounds, whose soil is drawn by the patch's own model.
	 */
	private static final Set<FarmingPatches> SOIL_IN_PATCH_MODEL = EnumSet.of(
		FarmingPatches.TROLL_HERB,
		FarmingPatches.WEISS_HERB,
		FarmingPatches.VARROCK_TREE,
		FarmingPatches.AUBURNVALE_BELL,
		FarmingPatches.HARMONY_ALLOTMENT
	);
	/**
	 * Patches without mounds whose planted soil is the terrain.
	 */
	private static final Set<FarmingPatches> SOIL_IN_TERRAIN = EnumSet.of(FarmingPatches.VARROCK_TREE);
	/**
	 * Patches without mounds whose planted soil is terrain 117HD blends with its surroundings when
	 * the scene loads, so can't be recoloured, and is covered with a flat quad instead.
	 */
	private static final Set<FarmingPatches> SOIL_UNDER_QUAD = EnumSet.of(
		FarmingPatches.AUBURNVALE_BELL,
		FarmingPatches.HARMONY_ALLOTMENT
	);
	/**
	 * Roughly the middle of the lightness of soil terrain, which varies from corner to corner.
	 */
	private static final int TERRAIN_LIGHTNESS = 11;
	/**
	 * The raked soil model covering one tile, lifted just clear of the terrain, and kept within its
	 * tile; one that reaches into neighbouring tiles can end up not drawn at all.
	 */
	private static final int QUAD_MODEL = 8223;
	private static final int QUAD_LIFT = 3;
	private static final int QUAD_RADIUS = 60;

	/**
	 * How long a model may keep a composted patch's colours. The renderer reads it as soon as it's
	 * coloured, but its thread only comes back to put it right at its next callback, which can be
	 * much later; after this long they're put back for it.
	 */
	private static final long STALE_NANOS = 20_000_000;

	/**
	 * How far from a patch's tile to look for the objects making it up.
	 */
	private static final int SEARCH_RADIUS = 8;
	private static final int EXTENDED_OFFSET = (Constants.EXTENDED_SCENE_SIZE - Constants.SCENE_SIZE) / 2;

	private final Client client;

	private final Map<FarmingPatches, Soil> soils = new EnumMap<>(FarmingPatches.class);
	/**
	 * The scene tiles {@link #soils} was found in; a new scene has fresh tiles.
	 */
	private Tile[][][] sceneTiles;
	private boolean coloursChanged;

	// used by the renderers, off the client thread

	/**
	 * The soil colours (compost, supercompost, ultracompost) as HSL.
	 */
	private volatile short[] soilColours;
	/**
	 * Compost (1-3) of each composted mound tile, keyed by packed world point.
	 */
	private volatile Map<Integer, Integer> compostByTile = Collections.emptyMap();
	/**
	 * Compost (1-3) of each composted patch whose soil is in its own model, keyed by object id.
	 */
	private volatile Map<Integer, Integer> compostByPatchObject = Collections.emptyMap();
	/**
	 * Each model seen, with its own colours and its colours for each compost.
	 */
	private final Map<Model, ModelColours> models = Collections.synchronizedMap(new WeakHashMap<>());
	/**
	 * Object ids of every patch, any of whose models might be coloured.
	 */
	private static final Set<Integer> PATCH_OBJECTS = new HashSet<>();

	static
	{
		for (FarmingPatches patch : FarmingPatches.values())
		{
			PATCH_OBJECTS.add(patch.getPatchId());
		}
	}

	/**
	 * Set while a model holds a composted patch's colours; only one may at a time.
	 */
	private final AtomicBoolean colouring = new AtomicBoolean();
	/**
	 * Odd while a model holds a composted patch's colours, and changed whenever that starts or ends.
	 */
	private final AtomicLong version = new AtomicLong();
	/**
	 * The model holding a composted patch's colours, its own colours, and when it was coloured.
	 */
	private final Object colouredLock = new Object();
	private Model colouredModel;
	private Colours colouredOriginal;
	private long colouredAt;
	/**
	 * What this thread last read, to check once the renderer has finished with it.
	 */
	private final ThreadLocal<Read> lastRead = new ThreadLocal<>();
	/**
	 * Zones the renderer may have read another patch's colours into, to have it read them again.
	 */
	private final Set<Integer> suspectZones = ConcurrentHashMap.newKeySet();

	/**
	 * A model read by the renderer: either coloured by this thread, or read while it might have
	 * been holding another patch's colours (at {@code version}).
	 */
	private static final class Read
	{
		final int zone;
		final long version;
		final boolean coloured;

		Read(int zone, long version, boolean coloured)
		{
			this.zone = zone;
			this.version = version;
			this.coloured = coloured;
		}
	}

	private static final class Soil
	{
		/**
		 * The patch's mound tiles, as packed world points.
		 */
		final Set<Integer> tiles = new HashSet<>();
		/**
		 * The zones the patch is in, packed as {@code zx << 16 | zz}.
		 */
		final Set<Integer> zones = new HashSet<>();
		/**
		 * The patch's soil terrain, and its own corner colours.
		 */
		final Map<SceneTilePaint, int[]> terrain = new IdentityHashMap<>();
		/**
		 * Tiles to cover with a flat quad, with their plane, and the quads covering them.
		 */
		final Map<LocalPoint, Integer> quadTiles = new HashMap<>();
		final Map<LocalPoint, RuneLiteObject> quads = new HashMap<>();
		int compost;
	}

	/**
	 * A model's face colours.
	 */
	private static final class Colours
	{
		final int[] colours1;
		final int[] colours2;
		final int[] colours3;
		final short[] unlit;

		Colours(Model model)
		{
			this(model.getFaceColors1().clone(), model.getFaceColors2().clone(), model.getFaceColors3().clone(),
				model.getUnlitFaceColors() == null ? null : model.getUnlitFaceColors().clone());
		}

		private Colours(int[] colours1, int[] colours2, int[] colours3, short[] unlit)
		{
			this.colours1 = colours1;
			this.colours2 = colours2;
			this.colours3 = colours3;
			this.unlit = unlit;
		}

		void writeTo(Model model)
		{
			copy(colours1, model.getFaceColors1());
			copy(colours2, model.getFaceColors2());
			copy(colours3, model.getFaceColors3());
			short[] u = model.getUnlitFaceColors();
			if (unlit != null && u != null)
			{
				System.arraycopy(unlit, 0, u, 0, Math.min(unlit.length, u.length));
			}
		}

		/**
		 * These colours with every soil face recoloured, keeping its shading. Null if there are no
		 * soil faces.
		 */
		Colours withSoil(short colour)
		{
			int[] c1 = colours1.clone();
			int[] c2 = colours2.clone();
			int[] c3 = colours3.clone();
			short[] u = unlit == null ? null : unlit.clone();
			boolean any = false;
			for (int face = 0; face < c1.length; face++)
			{
				int[] shade = soilShade(c1[face]);
				if (c3[face] == -2 || shade == null)
				{
					continue;
				}
				any = true;
				c1[face] = recolour(c1[face], shade[1], colour);
				if (c3[face] != -1)
				{
					c2[face] = recolour(c2[face], shade[1], colour);
					c3[face] = recolour(c3[face], shade[1], colour);
				}
				if (u != null)
				{
					u[face] = (short) recolour(u[face] & 0xFFFF, shade[1], colour);
				}
			}
			return any ? new Colours(c1, c2, c3, u) : null;
		}

		private static int[] soilShade(int hsl)
		{
			for (int[] shade : SOIL_SHADES)
			{
				if (hsl >= 0 && hsl >> 7 == shade[0])
				{
					return shade;
				}
			}
			return null;
		}

		private static void copy(int[] from, int[] to)
		{
			System.arraycopy(from, 0, to, 0, Math.min(from.length, to.length));
		}
	}

	/**
	 * A model's own colours, and those colours with its soil recoloured for each compost.
	 */
	private static final class ModelColours
	{
		final Colours original;
		final Colours[] byCompost = new Colours[3];
		final short[] soilColours;

		ModelColours(Model model, short[] soilColours)
		{
			this.original = new Colours(model);
			this.soilColours = soilColours;
			for (int c = 0; c < 3; c++)
			{
				byCompost[c] = original.withSoil(soilColours[c]);
			}
		}
	}

	@Inject
	SoilRecolourer(Client client)
	{
		this.client = client;
	}

	/**
	 * Set the soil colour for compost, supercompost and ultracompost. Must run on the client thread.
	 */
	void setColours(Color compost, Color supercompost, Color ultracompost)
	{
		soilColours = new short[]{hsl(compost), hsl(supercompost), hsl(ultracompost)};
		coloursChanged = true;
	}

	private static short hsl(Color colour)
	{
		return JagexColor.rgbToHSL(colour.getRGB(), 1.0);
	}

	/**
	 * A colour, lightened or darkened as much as {@code hsl} is from {@code lightness}.
	 */
	private static int recolour(int hsl, int lightness, short colour)
	{
		int l = Math.max(0, Math.min(127, (colour & 127) + (hsl & 127) - lightness));
		return (colour & 0xFF80) | l;
	}

	/**
	 * Bring each patch's soil in line with its compost. Must run on the client thread.
	 */
	void update(boolean enabled)
	{
		WorldView wv = client.getTopLevelWorldView();
		if (wv == null || soilColours == null)
		{
			return;
		}

		if (wv.getScene().getTiles() != sceneTiles)
		{
			// a new scene, uploaded without our colours
			soils.values().forEach(SoilRecolourer::removeQuads);
			soils.clear();
			sceneTiles = wv.getScene().getTiles();
		}

		boolean changed = false;
		Set<Integer> zones = new HashSet<>();
		Map<Integer, Integer> byPatchObject = new HashMap<>();
		for (FarmingPatches patch : FarmingPatches.values())
		{
			int compost = enabled ? client.getVarbitValue(patch.getCompostVarbit()) : 0;
			if (compost != 0 && SOIL_IN_PATCH_MODEL.contains(patch))
			{
				byPatchObject.put(patch.getPatchId(), compost);
			}

			Soil soil = soils.get(patch);
			if (soil == null)
			{
				if (compost == 0 || (soil = find(wv, patch)) == null)
				{
					continue;
				}
				soils.put(patch, soil);
			}

			if (soil.compost != compost || coloursChanged)
			{
				soil.compost = compost;
				colourTerrain(soil);
				placeQuads(soil);
				zones.addAll(soil.zones);
				changed = true;
			}
		}
		coloursChanged = false;

		if (changed)
		{
			Map<Integer, Integer> byTile = new HashMap<>();
			for (Soil soil : soils.values())
			{
				if (soil.compost != 0)
				{
					for (int tile : soil.tiles)
					{
						byTile.put(tile, soil.compost);
					}
				}
			}
			compostByTile = byTile;
			compostByPatchObject = byPatchObject;
			// have the renderer read the patches again
			invalidate(wv.getScene(), zones);
		}
	}

	/**
	 * Put back the original soil. Must run on the client thread.
	 */
	void clear()
	{
		compostByTile = Collections.emptyMap();
		compostByPatchObject = Collections.emptyMap();
		WorldView wv = client.getTopLevelWorldView();
		if (wv != null && wv.getScene().getTiles() == sceneTiles)
		{
			Set<Integer> zones = new HashSet<>();
			for (Soil soil : soils.values())
			{
				soil.compost = 0;
				colourTerrain(soil);
				zones.addAll(soil.zones);
			}
			invalidate(wv.getScene(), zones);
		}
		soils.values().forEach(SoilRecolourer::removeQuads);
		soils.clear();
		sceneTiles = null;

		// a renderer thread may not have come back to put its colours right
		putBack(0);
		suspectZones.clear();
	}

	/**
	 * Give a patch's soil terrain its compost's colour, or its own colours back.
	 */
	private void colourTerrain(Soil soil)
	{
		short colour = soil.compost == 0 ? 0 : soilColours[soil.compost - 1];
		soil.terrain.forEach((paint, original) ->
		{
			paint.setSwColor(terrainColour(original[0], soil.compost, colour));
			paint.setSeColor(terrainColour(original[1], soil.compost, colour));
			paint.setNeColor(terrainColour(original[2], soil.compost, colour));
			paint.setNwColor(terrainColour(original[3], soil.compost, colour));
		});
	}

	private static int terrainColour(int original, int compost, short colour)
	{
		return compost == 0 || original < 0 || original > 0xFFFF ? original : recolour(original, TERRAIN_LIGHTNESS, colour);
	}

	/**
	 * Cover a patch's soil with flat quads of its compost's colour, or remove them.
	 */
	private void placeQuads(Soil soil)
	{
		removeQuads(soil);
		if (soil.compost == 0)
		{
			return;
		}
		short colour = soilColours[soil.compost - 1];
		soil.quadTiles.forEach((location, plane) ->
		{
			// every quad gets its own model; the renderers keep per-model state
			Model model = client.loadModelData(QUAD_MODEL)
				.cloneVertices()
				.cloneColors()
				// the model faces down, so would be lit from below; mirroring turns it over
				.scale(-128, 128, 128)
				.recolor(SOIL, colour)
				.light();
			model.calculateBoundsCylinder();

			RuneLiteObject quad = client.createRuneLiteObject();
			quad.setModel(model);
			quad.setLocation(location, plane);
			quad.setZ(quad.getZ() - QUAD_LIFT);
			quad.setRadius(QUAD_RADIUS);
			quad.setActive(true);
			soil.quads.put(location, quad);
		});
	}

	private static void removeQuads(Soil soil)
	{
		soil.quads.values().forEach(quad -> quad.setActive(false));
		soil.quads.clear();
	}

	private void invalidate(Scene scene, Set<Integer> zones)
	{
		DrawCallbacks dc = client.getDrawCallbacks();
		if (dc == null)
		{
			return;
		}
		for (int zone : zones)
		{
			dc.invalidateZone(scene, zone >> 16, zone & 0xFFFF);
		}
	}

	/**
	 * Put back colours a renderer thread has left in a model too long, and have zones that may
	 * have been read with the wrong colours read again. Must run on the client thread.
	 */
	void tick()
	{
		synchronized (colouredLock)
		{
			if (colouredModel != null && System.nanoTime() - colouredAt > STALE_NANOS)
			{
				putBack(0);
			}
		}

		WorldView wv = client.getTopLevelWorldView();
		if (wv != null && !suspectZones.isEmpty())
		{
			Set<Integer> zones = new HashSet<>(suspectZones);
			suspectZones.removeAll(zones);
			invalidate(wv.getScene(), zones);
		}
	}

	@Override
	public boolean drawTile(Scene scene, Tile tile)
	{
		// the renderer has finished with the previous tile's objects
		finishRead();
		return true;
	}

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		finishRead();

		Integer compost;
		Renderable renderable;
		if (object instanceof GameObject && PATCH_OBJECTS.contains(object.getId()))
		{
			compost = compostByPatchObject.get(object.getId());
			renderable = ((GameObject) object).getRenderable();
		}
		else if (object instanceof GroundObject && object.getId() == MOUNDS)
		{
			LocalPoint lp = object.getLocalLocation();
			compost = compostByTile.get(pack(scene.getBaseX() + lp.getSceneX(), scene.getBaseY() + lp.getSceneY(), object.getPlane()));
			renderable = ((GroundObject) object).getRenderable();
		}
		else
		{
			return true;
		}

		// never getModel() off the client thread: for a patch it rebuilds the model from the
		// patch's state, which isn't safe to read here
		Model model = renderable instanceof Model ? (Model) renderable
			: renderable instanceof DynamicObject ? ((DynamicObject) renderable).getModelZbuf()
			: null;
		short[] soilColours = this.soilColours;
		if (model == null || soilColours == null)
		{
			return true;
		}

		LocalPoint lp = object.getLocalLocation();
		int zone = (((lp.getSceneX() + EXTENDED_OFFSET) >> 3) << 16) | ((lp.getSceneY() + EXTENDED_OFFSET) >> 3);
		if (compost == null)
		{
			// check afterwards whether another patch's colours could have been in the model
			lastRead.set(new Read(zone, version.get(), false));
		}
		else if (colouring.compareAndSet(false, true))
		{
			colour(model, compost, soilColours, zone);
		}
		else
		{
			// another patch's colours are in a model: this one will have to be read again
			suspectZones.add(zone);
		}
		return true;
	}

	/**
	 * Colour the soil faces of a model the renderer is about to read. Only called by the thread
	 * that has set {@link #colouring}, so no model holds any other patch's colours.
	 */
	private void colour(Model model, int compost, short[] soilColours, int zone)
	{
		ModelColours colours;
		synchronized (models)
		{
			colours = models.get(model);
			if (colours == null || colours.soilColours != soilColours)
			{
				colours = new ModelColours(model, soilColours);
				models.put(model, colours);
			}
		}

		Colours soil = colours.byCompost[compost - 1];
		if (soil == null)
		{
			colouring.set(false);
			return;
		}

		long v;
		synchronized (colouredLock)
		{
			colouredModel = model;
			colouredOriginal = colours.original;
			colouredAt = System.nanoTime();
			// odd from before the colours are written until after they're put back
			v = version.incrementAndGet();
			soil.writeTo(model);
		}
		lastRead.set(new Read(zone, v, true));
	}

	/**
	 * Now the renderer has finished with what this thread last read: put back any colours this
	 * thread wrote, or if another patch's colours could have been read, have it read again.
	 */
	private void finishRead()
	{
		Read read = lastRead.get();
		if (read == null)
		{
			return;
		}
		lastRead.remove();

		if (read.coloured)
		{
			putBack(read.version);
		}
		else if ((read.version & 1) != 0 || version.get() != read.version)
		{
			suspectZones.add(read.zone);
		}
	}

	/**
	 * Put back the colours of the coloured model, if it was coloured at {@code at} or {@code at} is 0.
	 */
	private void putBack(long at)
	{
		synchronized (colouredLock)
		{
			if (colouredModel == null || at != 0 && version.get() != at)
			{
				return;
			}
			colouredOriginal.writeTo(colouredModel);
			colouredModel = null;
			colouredOriginal = null;
			version.incrementAndGet();
			colouring.set(false);
		}
	}

	private static int pack(int x, int y, int plane)
	{
		return (plane << 28) | ((x & 0x3FFF) << 14) | (y & 0x3FFF);
	}

	/**
	 * Find the zones a patch is in (it's sometimes several objects sharing an id), with its mound
	 * tiles, or for a patch without mounds, its soil terrain.
	 */
	private static Soil find(WorldView wv, FarmingPatches patch)
	{
		LocalPoint centre = LocalPoint.fromWorld(wv, patch.getTile());
		if (centre == null || patch.getTile().getPlane() != wv.getPlane())
		{
			return null;
		}

		int plane = patch.getTile().getPlane();
		Tile[][] tiles = wv.getScene().getTiles()[plane];
		short[][] overlays = wv.getScene().getOverlayIds()[plane];
		Soil soil = new Soil();
		boolean found = false;
		int cx = centre.getSceneX();
		int cy = centre.getSceneY();
		for (int x = Math.max(0, cx - SEARCH_RADIUS); x <= Math.min(tiles.length - 1, cx + SEARCH_RADIUS); x++)
		{
			for (int y = Math.max(0, cy - SEARCH_RADIUS); y <= Math.min(tiles[x].length - 1, cy + SEARCH_RADIUS); y++)
			{
				Tile tile = tiles[x][y];
				if (tile == null || !hasObject(tile, patch.getPatchId()))
				{
					continue;
				}
				found = true;
				int zone = (((x + EXTENDED_OFFSET) >> 3) << 16) | ((y + EXTENDED_OFFSET) >> 3);

				if (SOIL_IN_PATCH_MODEL.contains(patch))
				{
					soil.zones.add(zone);
					SceneTilePaint paint = tile.getSceneTilePaint();
					boolean dirt = overlays[x + EXTENDED_OFFSET][y + EXTENDED_OFFSET] != 0;
					if (SOIL_UNDER_QUAD.contains(patch) && dirt)
					{
						soil.quadTiles.put(tile.getLocalLocation(), plane);
					}
					else if (SOIL_IN_TERRAIN.contains(patch) && dirt && paint != null)
					{
						soil.terrain.put(paint, new int[]{paint.getSwColor(), paint.getSeColor(), paint.getNeColor(), paint.getNwColor()});
					}
					continue;
				}

				GroundObject ground = tile.getGroundObject();
				if (ground != null && ground.getId() == MOUNDS)
				{
					WorldPoint wp = tile.getWorldLocation();
					soil.tiles.add(pack(wp.getX(), wp.getY(), wp.getPlane()));
					soil.zones.add(zone);
				}
			}
		}
		log.debug("{}: {} mound tiles, {} terrain tiles, {} quad tiles, in {} zones", patch, soil.tiles.size(), soil.terrain.size(),
			soil.quadTiles.size(), soil.zones.size());
		// not found means not loaded yet, so look again later
		return found ? soil : null;
	}

	private static boolean hasObject(Tile tile, int id)
	{
		for (GameObject go : tile.getGameObjects())
		{
			if (go != null && go.getId() == id)
			{
				return true;
			}
		}
		return false;
	}
}
