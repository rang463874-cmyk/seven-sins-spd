package com.shatteredpixel.shatteredpixeldungeon.levels;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.Believer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.Priest;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.Seraph;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.CityPainter;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.StandardRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.BlazingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.CorrosionTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.DisintegrationTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ExplosiveTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.FlashingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.FrostTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrippingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GuardianTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ShockingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.StormTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.TeleportationTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.WeakeningTrap;
import com.watabou.noosa.audio.Music;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: the heavens, floors 27-30, opened below the amulet once the archangel falls.
 * Big floors (10-13 standard rooms) that mix the rooms of the sewers, prison, caves and city (the
 * room chance tables have rows 27-30 for this), drawn with the city's tiles. Monsters: believers
 * (in groups of 2-3), priests (always with company), lesser angels (only from a priest's
 * sacrifice) and now and then a seraph (2x2) guarding one big room. Floor 30's stairs lead to the holy
 * giant (HolyGiantLevel, floor 31); its chasms are filled in.
 */
public class HeavenLevel extends RegularLevel {

	public static final int FIRST = 27;
	public static final int LAST = 30;
	/** chance per floor of a seraph */
	public static final float SERAPH_CHANCE = 0.35f;

	{
		color1 = 0xf0e6c0;
		color2 = 0xffffff;
	}

	@Override
	public void playLevelMusic() {
		Music.INSTANCE.playTracks(CityLevel.CITY_TRACK_LIST, CityLevel.CITY_TRACK_CHANCES, false);
	}

	@Override
	protected int standardRooms(boolean forceMax) {
		if (forceMax) return 13;
		//10 to 13
		return 10 + Random.chances(new float[]{1, 2, 2, 1});
	}

	@Override
	protected int specialRooms(boolean forceMax) {
		if (forceMax) return 4;
		return 3 + Random.chances(new float[]{1, 1});
	}

	@Override
	public int mobLimit() {
		//groups are added on top of this (see createMobs)
		return 5 + Random.Int(3);
	}

	@Override
	public String tilesTex() {
		return Assets.Environment.TILES_HEAVEN;
	}

	@Override
	public String waterTex() {
		return Assets.Environment.WATER_HEAVEN;
	}

	@Override
	protected Painter painter() {
		return new HeavenPainter()
				.setWater(feeling == Feeling.WATER ? 0.90f : 0.30f, 4)
				.setGrass(feeling == Feeling.GRASS ? 0.80f : 0.20f, 3)
				.setTraps(nTraps(), trapClasses(), trapChances());
	}

	@Override
	protected int nTraps() {
		return Random.NormalIntRange(4, 7);
	}

	@Override
	protected Class<?>[] trapClasses() {
		return new Class[]{
				GrippingTrap.class, TeleportationTrap.class, PoisonDartTrap.class, ExplosiveTrap.class,
				ShockingTrap.class, FlashingTrap.class, FrostTrap.class, StormTrap.class, CorrosionTrap.class,
				BlazingTrap.class, DisintegrationTrap.class, GuardianTrap.class, WeakeningTrap.class};
	}

	@Override
	protected float[] trapChances() {
		return new float[]{2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 1};
	}

	@Override
	protected boolean build() {
		if (!super.build()) return false;
		if (Dungeon.depth >= LAST) {
			//the stairs lead down to the holy giant; no falling into its arena
			for (int i = 0; i < length(); i++) {
				if (map[i] == Terrain.CHASM) map[i] = Terrain.EMPTY_SP;
			}
		}
		com.shatteredpixel.shatteredpixeldungeon.tiles.custom.HeavenSky.addTo(this);
		return true;
	}

	@Override
	public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		super.restoreFromBundle(bundle);
		//levels made before the sky existed get it too
		com.shatteredpixel.shatteredpixeldungeon.tiles.custom.HeavenSky.addTo(this);
	}

	// ------------------------------------------------------------------ monsters

	@Override
	protected void createMobs() {
		super.createMobs();
		//believers walk in groups of 2-3, priests never come alone
		for (Mob m : new ArrayList<>(mobs)) {
			int extra = 0;
			if (m instanceof Believer) extra = Random.IntRange(1, 2);
			else if (m instanceof Priest) extra = Random.IntRange(1, 2);
			for (int i = 0; i < extra; i++) addCompanion(m.pos, new Believer());
		}
		if (Random.Float() < SERAPH_CHANCE) placeSeraph();
	}

	@Override
	public boolean spawnMob(int disLimit) {
		java.util.HashSet<Mob> before = new java.util.HashSet<>(mobs);
		boolean ok = super.spawnMob(disLimit);
		if (!ok) return false;
		//respawns come in groups too
		for (Mob m : new ArrayList<>(mobs)) {
			if (before.contains(m) || !(m instanceof Believer || m instanceof Priest)) continue;
			int extra = Random.IntRange(1, 2);
			for (int i = 0; i < extra; i++) {
				int cell = freeNear(m.pos);
				if (cell == -1) break;
				Believer b = new Believer();
				b.pos = cell;
				b.state = b.WANDERING;
				com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.add(b);
			}
		}
		return true;
	}

	private int freeNear(int cell) {
		ArrayList<Integer> cands = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = cell + n;
			if (c < 0 || c >= length()) continue;
			if (passable[c] && !solid[c] && findMob(c) == null && c != entrance() && c != exit()
					&& traps.get(c) == null && (Dungeon.hero == null || Dungeon.hero.pos != c)) {
				cands.add(c);
			}
		}
		return cands.isEmpty() ? -1 : Random.element(cands);
	}

	private void addCompanion(int near, Mob m) {
		int cell = freeNear(near);
		if (cell == -1) return;
		m.pos = cell;
		mobs.add(m);
	}

	/** a seraph in one of the bigger standard rooms (not the entrance) */
	private void placeSeraph() {
		ArrayList<Room> cands = new ArrayList<>();
		for (Room r : rooms) {
			if (r instanceof StandardRoom && r != roomEntrance && r != roomExit
					&& ((StandardRoom) r).mobSpawnWeight() > 0
					&& r.width() >= 7 && r.height() >= 7) cands.add(r);
		}
		Random.shuffle(cands);
		for (Room r : cands) {
			Seraph s = new Seraph();
			s.left = r.left + 1;
			s.top = r.top + 1;
			s.right = r.right - 1;
			s.bottom = r.bottom - 1;
			ArrayList<Integer> spots = new ArrayList<>();
			for (int x = s.left; x < s.right; x++) {
				for (int y = s.top; y < s.bottom; y++) {
					int c = x + y * width();
					if (fitsAtBuild(s, c)) spots.add(c);
				}
			}
			if (spots.isEmpty()) continue;
			s.place(this, Random.element(spots));
			return;
		}
	}

	private boolean fitsAtBuild(Seraph s, int c) {
		int w = width();
		int cx = c % w, cy = c / w;
		if (cx < s.left || cy < s.top || cx + 1 > s.right || cy + 1 > s.bottom) return false;
		for (int b : new int[]{c, c + 1, c + w, c + w + 1}) {
			if (!passable[b] || solid[b] || findMob(b) != null || b == entrance() || b == exit()
					|| traps.get(b) != null) return false;
		}
		return true;
	}

	/** the city painter with a fixed wall-decoration rate (the city's own one depends on the depth) */
	public static class HeavenPainter extends CityPainter {
		@Override
		protected void decorate(Level level, ArrayList<Room> rooms) {
			int[] map = level.map;
			int w = level.width();
			int l = level.length();
			for (int i = 0; i < l - w; i++) {
				if (map[i] == Terrain.EMPTY && Random.Int(10) == 0) {
					map[i] = Terrain.EMPTY_DECO;
				} else if (map[i] == Terrain.WALL
						&& !com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTileSheet.wallStitcheable(map[i + w])
						&& Random.Int(6) == 0) {
					map[i] = Terrain.WALL_DECO;
				}
			}
		}
	}
}
