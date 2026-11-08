package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
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
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A champion of one of the seven virtues (a paladin or a saint), come to judge the sinner. One hunts the
 * hero in each region from the prison on (see VirtueHooks). Stats follow the floor it appears on; two
 * telegraphed skills each (RegionElite). The hero's nemesis (the virtue opposing its sin) has its own
 * lines, and the two hurt each other 25% more. Drops the region reward; a nemesis also gives sin points
 * and a scroll of upgrade.
 */
public abstract class VirtueChampion extends RegionElite implements com.shatteredpixel.shatteredpixeldungeon.virtues.VirtueFoe {

	public abstract Virtue virtue();

	/** the floor its stats were made for */
	protected int level = 6;
	private boolean announced = false;

	{
		EXP = 20;
		maxLvl = 30;
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
			GLog.n(Messages.get(VirtueChampion.class, nemesis() ? "arrives_nemesis" : "arrives", name()));
			//it came for the hero: it already knows where to look
			beckon(Dungeon.hero.pos);
		}
		return super.act();
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
		yell(line("death"));
		EliteHooks.regionReward(pos);
		if (nemesis() && Dungeon.hero != null && Dungeon.hero.isAlive()) {
			SinRewards.grant(Dungeon.hero, 50);
			Dungeon.level.drop(new ScrollOfUpgrade(), pos).sprite.drop();
			GLog.p(Messages.get(VirtueChampion.class, "nemesis_slain"));
		}
	}

	// ------------------------------------------------------------------ shapes for the skills

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
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		level = bundle.getInt("vlevel");
		announced = bundle.getBoolean("announced");
		//HT/defense come from the level it was made for
		int hp = HP;
		setLevel(level);
		HP = Math.min(hp, HT);
	}
}
