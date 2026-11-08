package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.elites.EliteHooks;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A region's named elite (one per region, see EliteHooks). Uses the same telegraph pattern as the
 * Seal Warden: a skill marks cells (or just announces itself), and is released on the elite's next
 * turn, so stepping out of the marks avoids it. Subclasses provide the skills.
 * Drops two sin essences and a guaranteed piece of equipment.
 */
public abstract class RegionElite extends Mob {

	{
		HUNTING = new Hunting();
		properties.add(Property.MINIBOSS);
	}

	protected static final int NONE = -1;

	protected int skill = NONE;
	protected int[] marked = new int[0];
	protected float skillCooldown = 2;
	private boolean greeted = false;

	/** how many skills this elite has; ids 0..n-1 */
	protected abstract int skillCount();

	/** marks for a skill against the target; null = can't use it now, empty = no marks (self skill) */
	protected abstract int[] prepare(int skill, Char target);

	protected abstract void release(int skill, int[] cells);

	protected abstract int cooldown(int skill);

	protected abstract String skillKey(int skill);

	/** whether this one drops the region reward when it dies (twins: only the last one) */
	protected boolean dropsReward() {
		return true;
	}

	@Override
	protected boolean act() {
		if (skill != NONE && (paralysed > 0 || state != HUNTING)) {
			skill = NONE;
			marked = new int[0];
		}
		if (paralysed <= 0 && skillCooldown > 0) skillCooldown--;
		return super.act();
	}

	protected class Hunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (skill != NONE) {
				int s = skill;
				int[] cells = marked;
				skill = NONE;
				marked = new int[0];
				skillCooldown = cooldown(s);
				release(s, cells);
				spend(TICK);
				return true;
			}
			if (skillCooldown <= 0 && enemyInFOV && enemy != null && !isCharmedBy(enemy)) {
				int start = Random.Int(skillCount());
				for (int i = 0; i < skillCount(); i++) {
					int s = (start + i) % skillCount();
					int[] cells = prepare(s, enemy);
					if (cells == null) continue;
					skill = s;
					marked = cells;
					for (int c : cells) GameScene.targetedCell(c, 1f);
					if (sprite != null) {
						sprite.turnTo(pos, enemy.pos);
						sprite.showStatus(CharSprite.WARNING, Messages.get(RegionElite.this, "skill_" + skillKey(s)));
					}
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE, 0.6f, 1.3f);
					spend(TICK);
					return true;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	// ---- helpers for subclasses ----

	protected void addIfOpen(ArrayList<Integer> cells, int c) {
		if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c] && !cells.contains(c)) cells.add(c);
	}

	protected static int[] toArray(ArrayList<Integer> list) {
		int[] out = new int[list.size()];
		for (int i = 0; i < out.length; i++) out[i] = list.get(i);
		return out;
	}

	/** hits whoever stands on a cell (not this elite's allies) for min..max minus armor; returns them */
	protected Char strike(int cell, int min, int max) {
		Char ch = Actor.findChar(cell);
		if (ch == null || ch == this || ch.alignment == alignment) return null;
		int dmg = Math.max(0, Random.NormalIntRange(min, max) - ch.drRoll());
		ch.damage(dmg, this);
		if (ch.sprite != null) ch.sprite.flash();
		if (ch == Dungeon.hero && !ch.isAlive()) {
			Dungeon.fail(this);
			GLog.n(Messages.get(RegionElite.class, "skill_kill", name()));
		}
		return ch;
	}

	@Override
	public void notice() {
		super.notice();
		if (!greeted) {
			greeted = true;
			yell(noticeLine());
		}
	}

	/** what it shouts the first time it notices the hero */
	protected String noticeLine() {
		return Messages.get(this, "notice");
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		if (dropsReward()) EliteHooks.regionReward(pos);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("skill", skill);
		bundle.put("marked", marked);
		bundle.put("skill_cd", skillCooldown);
		bundle.put("greeted", greeted);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		skill = bundle.getInt("skill");
		marked = bundle.getIntArray("marked");
		if (marked == null) marked = new int[0];
		skillCooldown = bundle.getFloat("skill_cd");
		greeted = bundle.getBoolean("greeted");
	}

}
