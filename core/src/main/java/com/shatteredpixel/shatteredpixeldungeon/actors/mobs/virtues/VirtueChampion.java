package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.RegularLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.VirtueSanctumRoom;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.elites.EliteHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinRewards;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueHooks;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A champion of one of the seven virtues (a paladin or a saint), come to judge the sinner. One waits on
 * the pedestal of its sanctum in each region from the prison on (see VirtueHooks); when the hero steps
 * inside, the door is walled up until one of them falls (or the hero gets out some other way). It never
 * leaves the sanctum and its skills stay inside it. Stats follow the floor it appears on; two
 * telegraphed skills each (RegionElite). The hero's nemesis (the virtue opposing its sin) has its own
 * lines, and the two hurt each other 25% more. Drops the region reward; a nemesis also gives sin points
 * and a scroll of upgrade.
 */
public abstract class VirtueChampion extends RegionElite implements com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueFoe {

	public abstract Virtue virtue();

	/** the floor its stats were made for */
	protected int level = 6;
	private boolean announced = false;

	/** the pedestal of its sanctum, or -1 for champions from older saves (they hunt the hero down) */
	public int home = -1;
	/** door cells walled up while the fight is on */
	private int[] sealedDoors = new int[0];

	{
		EXP = 20;
		maxLvl = 30;
		WANDERING = new Waiting();
		state = HUNTING;
	}

	public VirtueChampion() {
		super();
		setLevel(Math.max(6, Dungeon.depth));
	}

	protected void setLevel(int d) {
		level = d;
		HP = HT = 30 + 12 * d;
		defenseSkill = 5 + d;
		EXP = 10 + d;
	}

	public boolean nemesis() {
		return VirtueHooks.isNemesis(virtue());
	}

	@Override
	public CharSprite sprite() {
		return VirtueSprites.forVirtue(virtue());
	}

	@Override public int damageRoll() { return Random.NormalIntRange(level / 2 + 2, level + 6); }
	@Override public int attackSkill(Char target) { return 10 + 2 * level; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, level / 2); }

	/** skill damage */
	protected int sMin() { return level / 2 + 3; }
	protected int sMax() { return level + 8; }

	@Override
	public String name() {
		return Messages.get(VirtueChampion.class, "name_" + virtue().name().toLowerCase());
	}

	@Override
	public String description() {
		String d = Messages.get(VirtueChampion.class, "desc_" + virtue().name().toLowerCase());
		d += "\n\n" + Messages.get(VirtueChampion.class, "desc_common");
		if (nemesis()) d += "\n\n" + Messages.get(VirtueChampion.class, "desc_nemesis");
		return d;
	}

	private String line(String part) {
		String key = virtue().name().toLowerCase() + "_" + part + (nemesis() ? "_own" : "");
		return Messages.get(VirtueChampion.class, key);
	}

	@Override
	protected boolean act() {
		if (!announced && Dungeon.hero != null && Dungeon.level != null) {
			announced = true;
			if (home == -1) {
				GLog.n(Messages.get(VirtueChampion.class, nemesis() ? "arrives_nemesis" : "arrives", name()));
				//it came for the hero: it already knows where to look
				beckon(Dungeon.hero.pos);
			} else {
				GLog.n(Messages.get(VirtueChampion.class, nemesis() ? "awaits_nemesis" : "awaits", name()));
				state = WANDERING;
			}
		}
		Room arena = arena();
		if (arena != null && Dungeon.hero != null && Dungeon.hero.isAlive()) {
			boolean heroInside = inArena(arena, Dungeon.hero.pos);
			if (heroInside && state != HUNTING && paralysed <= 0) {
				//the hero stepped into the sanctum: the judgment begins
				aggro(Dungeon.hero);
				state = HUNTING;
				target = Dungeon.hero.pos;
				notice();
			}
			if (sealedDoors.length == 0 && heroInside && state == HUNTING) {
				sealArena(arena);
			} else if (sealedDoors.length > 0 && !heroInside) {
				unsealArena();
			}
		}
		return super.act();
	}

	// ------------------------------------------------------------------ the sanctum

	private Room arena() {
		if (home == -1 || !(Dungeon.level instanceof RegularLevel)) return null;
		Room r = ((RegularLevel) Dungeon.level).room(home);
		return r instanceof VirtueSanctumRoom ? r : null;
	}

	private boolean inArena(Room arena, int cell) {
		return arena != null && cell >= 0 && arena.inside(Dungeon.level.cellToPoint(cell));
	}

	/** skills only reach inside its sanctum and its doorway (always true for champions without one) */
	protected boolean canReach(int cell) {
		Room arena = arena();
		if (arena == null) return true;
		if (cell < 0 || cell >= Dungeon.level.length()) return false;
		Point p = Dungeon.level.cellToPoint(cell);
		return p.x >= arena.left && p.x <= arena.right && p.y >= arena.top && p.y <= arena.bottom;
	}

	private void sealArena(Room arena) {
		ArrayList<Integer> doors = new ArrayList<>();
		for (Room.Door d : arena.connected.values()) {
			int cell = Dungeon.level.pointToCell(d);
			//someone is standing in the doorway, try again next turn
			if (Actor.findChar(cell) != null || Dungeon.level.heaps.get(cell) != null) return;
			doors.add(cell);
		}
		if (doors.isEmpty()) return;

		sealedDoors = new int[doors.size()];
		for (int i = 0; i < sealedDoors.length; i++) {
			int cell = doors.get(i);
			sealedDoors[i] = cell;
			Level.set(cell, Terrain.WALL);
			GameScene.updateMap(cell);
			CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 6);
		}
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		GLog.w(Messages.get(VirtueChampion.class, "sealed"));
	}

	private void unsealArena() {
		if (sealedDoors.length == 0) return;
		for (int cell : sealedDoors) {
			Level.set(cell, Terrain.DOOR);
			GameScene.updateMap(cell);
			CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 4);
		}
		sealedDoors = new int[0];
		Dungeon.observe();
		Sample.INSTANCE.play(Assets.Sounds.ROCKS);
		GLog.i(Messages.get(VirtueChampion.class, "unsealed"));
	}

	@Override
	protected boolean getCloser(int target) {
		Room arena = arena();
		if (arena == null || inArena(arena, target)) return super.getCloser(target);

		//it never leaves its sanctum: head for the cell inside closest to the target
		int best = -1;
		Point p = new Point();
		for (int x = arena.left + 1; x < arena.right; x++) {
			for (int y = arena.top + 1; y < arena.bottom; y++) {
				p.set(x, y);
				int c = Dungeon.level.pointToCell(p);
				if (!Dungeon.level.passable[c] || (Actor.findChar(c) != null && c != pos)) continue;
				if (best == -1 || Dungeon.level.trueDistance(c, target) < Dungeon.level.trueDistance(best, target)) {
					best = c;
				}
			}
		}
		if (best == -1 || best == pos) return false;
		return super.getCloser(best);
	}

	/** in its sanctum it waits on the pedestal; without one it wanders like any mob */
	private class Waiting extends Mob.Wandering {
		@Override
		protected int randomDestination() {
			return arena() != null ? home : super.randomDestination();
		}
	}

	@Override
	protected String noticeLine() {
		return line("notice");
	}

	@Override
	protected boolean dropsReward() {
		return false;
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		unsealArena();
		yell(line("death"));
		EliteHooks.regionReward(pos);
		if (nemesis() && Dungeon.hero != null && Dungeon.hero.isAlive()) {
			SinRewards.grant(Dungeon.hero, 50);
			Dungeon.level.drop(new ScrollOfUpgrade(), pos).sprite.drop();
			GLog.p(Messages.get(VirtueChampion.class, "nemesis_slain"));
		}
	}

	// ------------------------------------------------------------------ shapes for the skills

	@Override
	protected void addIfOpen(ArrayList<Integer> cells, int c) {
		if (canReach(c)) super.addIfOpen(cells, c);
	}

	protected int[] plus(int center) {
		ArrayList<Integer> cells = new ArrayList<>();
		addIfOpen(cells, center);
		for (int n : PathFinder.NEIGHBOURS4) addIfOpen(cells, center + n);
		return toArray(cells);
	}

	protected int[] square(int center) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS9) addIfOpen(cells, center + n);
		return toArray(cells);
	}

	protected int[] ring() {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) addIfOpen(cells, pos + n);
		return toArray(cells);
	}

	protected int dist(Char target) {
		return Dungeon.level.distance(pos, target.pos);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("vlevel", level);
		bundle.put("announced", announced);
		bundle.put("home", home);
		bundle.put("sealed_doors", sealedDoors);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		level = bundle.getInt("vlevel");
		announced = bundle.getBoolean("announced");
		//champions from older saves have no sanctum
		home = bundle.contains("home") ? bundle.getInt("home") : -1;
		if (bundle.contains("sealed_doors")) sealedDoors = bundle.getIntArray("sealed_doors");
		//HT/defense come from the level it was made for
		int hp = HP;
		setLevel(level);
		HP = Math.min(hp, HT);
	}
}
