package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaLord;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.utils.Random;

/**
 * Seven Sins (ritual since Nov 13 2026, owner: like the wandmaker's fire elemental): the four ALTARS (pedestals) around
 * the circle each need a demon essence thrown or dropped on them (checkRitual, from GoetiaHooks.tick); then the essences
 * burn away, the king rises in the centre and the stairs seal until it falls. Before that the stairs stay open.
 * The hidden room of a boss floor (branch GoetiaHooks.ARENA_BRANCH, same depth): a ritual hall in
 * the floor's own tileset with a summoning circle of embers, where the floor's demon king waits. The way
 * back (stairs to the boss floor's exit) stays sealed until the king falls.
 */
public class GoetiaArenaLevel extends Level {

	private static final int W = 17, H = 17;
	private static final int LEFT = 2, TOP = 2, RIGHT = 14, BOTTOM = 14;

	{
		color1 = 0x401040;
		color2 = 0x9020c0;
		viewDistance = 8;
	}

	public static int arrivalCell() {
		return BOTTOM * W + W / 2;
	}

	public static int portalCell() {
		return (BOTTOM + 1) * W + W / 2;
	}

	private static int[] altars() {
		int cx = W / 2, cy = (TOP + BOTTOM) / 2 - 1;
		return new int[]{(cy - 3) * W + cx, cy * W + cx - 3, cy * W + cx + 3, (cy + 3) * W + cx};
	}

	private boolean hinted = false;

	private boolean lordPresent() {
		for (Mob m : mobs) if (m instanceof GoetiaLord) return true;
		return false;
	}

	/** every hero turn while here: four essences on the four altars summon the king */
	public void checkRitual() {
		if (GoetiaHooks.lordsBeaten.contains(Dungeon.depth) || lordPresent()) return;
		GoetiaDemon d = GoetiaDemon.lordOf(Dungeon.depth);
		if (d == null) return;
		if (!hinted) {
			hinted = true;
			GLog.w(Messages.get(GoetiaArenaLevel.class, "ritual_hint", d.rankName(), d.title(), GoetiaHooks.RITUAL_ESSENCES));
		}
		for (int a : altars()) {
			Heap h = heaps.get(a);
			if (h == null || h.type != Heap.Type.HEAP || essenceIn(h) == null) return;
		}
		for (int a : altars()) {
			Heap h = heaps.get(a);
			Item e = essenceIn(h);
			if (e.quantity() > 1) e.quantity(e.quantity() - 1);
			else h.remove(e);
			CellEmitter.get(a).burst(ElmoParticle.FACTORY, 12);
		}
		GoetiaLord lord = new GoetiaLord().setup(d, Dungeon.depth);
		lord.pos = centerCell();
		if (Actor.findChar(lord.pos) != null) {
			for (int n : PathFinder.NEIGHBOURS8) {
				if (Actor.findChar(lord.pos + n) == null && passable[lord.pos + n]) { lord.pos += n; break; }
			}
		}
		lord.state = lord.HUNTING;
		GameScene.add(lord);
		lord.beckon(Dungeon.hero.pos);
		CellEmitter.get(lord.pos).burst(ElmoParticle.FACTORY, 30);
		Camera.main.shake(3, 0.7f);
		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		set(portalCell(), Terrain.LOCKED_EXIT);
		GameScene.updateMap(portalCell());
		GLog.n(Messages.get(GoetiaArenaLevel.class, "ritual_done", d.rankName(), d.title()));
	}

	private static Item essenceIn(Heap h) {
		for (Item i : h.items) if (i instanceof com.shatteredpixel.shatteredpixeldungeon.items.DemonEssence) return i;
		return null;
	}

	private static int centerCell() {
		return ((TOP + BOTTOM) / 2 - 1) * W + W / 2;
	}

	@Override
	public String tilesTex() {
		switch (Math.min(5, Math.max(1, (Dungeon.depth + 4) / 5))) {
			case 1: return Assets.Environment.TILES_SEWERS;
			case 2: return Assets.Environment.TILES_PRISON;
			case 3: return Assets.Environment.TILES_CAVES;
			case 4: return Assets.Environment.TILES_CITY;
			default: return Assets.Environment.TILES_HALLS;
		}
	}

	@Override
	public String waterTex() {
		switch (Math.min(5, Math.max(1, (Dungeon.depth + 4) / 5))) {
			case 1: return Assets.Environment.WATER_SEWERS;
			case 2: return Assets.Environment.WATER_PRISON;
			case 3: return Assets.Environment.WATER_CAVES;
			case 4: return Assets.Environment.WATER_CITY;
			default: return Assets.Environment.WATER_HALLS;
		}
	}

	@Override
	protected boolean build() {
		setSize(W, H);
		for (int i = 0; i < length(); i++) map[i] = Terrain.WALL;
		int cx = W / 2, cy = (TOP + BOTTOM) / 2 - 1;
		for (int y = TOP; y <= BOTTOM; y++) {
			for (int x = LEFT; x <= RIGHT; x++) {
				int dx = x - cx, dy = y - cy;
				int r2 = dx * dx + dy * dy;
				//a rough round hall
				if (r2 > 46) continue;
				map[y * W + x] = Random.Int(7) == 0 ? Terrain.EMPTY_DECO : Terrain.EMPTY;
				//the summoning circle
				if (r2 >= 8 && r2 <= 10) map[y * W + x] = Terrain.EMBERS;
			}
		}
		//the corridor to the stairs
		for (int y = cy; y <= BOTTOM; y++) if (map[y * W + cx] == Terrain.WALL) map[y * W + cx] = Terrain.EMPTY;
		int[] pillars = {(cy - 4) * W + cx - 4, (cy - 4) * W + cx + 4, (cy + 4) * W + cx - 4, (cy + 4) * W + cx + 4};
		for (int p : pillars) map[p] = Terrain.STATUE;
		map[centerCell()] = Terrain.EMPTY_SP;
		for (int a : altars()) map[a] = Terrain.PEDESTAL;

		//the stairs stay open until the ritual summons the king
		map[portalCell()] = Terrain.ENTRANCE;
		transitions.add(new LevelTransition(this, portalCell(), LevelTransition.Type.BRANCH_ENTRANCE,
				Dungeon.depth, 0, LevelTransition.Type.REGULAR_EXIT));
		return true;
	}

	/** the king fell: the stairs back open */
	public void unlockPortal() {
		set(portalCell(), Terrain.ENTRANCE);
		GameScene.updateMap(portalCell());
	}

	@Override
	public Mob createMob() {
		return null;
	}

	@Override
	protected void createMobs() {
		//the king only rises through the ritual (checkRitual)
	}

	@Override
	public Actor addRespawner() {
		return null;
	}

	@Override
	protected void createItems() {
	}

	@Override
	public int randomRespawnCell(Char ch) {
		return arrivalCell() - W;
	}
}
