package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GhostSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Sloth's dream puppet: an ally that does the fighting so the Sloth doesn't have to.
 * Its stats follow the hero's level and sloth points. Nightmares are temporary puppets
 * (from the Dream Lord and the Nightmare Legion ability) that fade after a while.
 */
public class DreamPuppet extends DirectableAlly {

	{
		spriteClass = PuppetSprite.class;

		HP = HT = 10;

		immunities.add(AllyBuff.class);
		properties.add(Property.INORGANIC);

		//puppets roam the level on their own instead of trailing the hero, and hunt anything
		//they come across, sleeping or wandering enemies included
		intelligentAlly = false;
		WANDERING = new Roaming();
		state = WANDERING;
	}

	/** nightmares are temporary and weaker */
	public boolean nightmare = false;
	public float lifespan = 0;

	public static float statMult(DreamPuppet p) {
		return p != null && p.nightmare ? 0.7f : 1f;
	}

	private static SlothRepose repose() {
		return SlothRepose.of();
	}

	private static float pts() {
		SlothRepose r = repose();
		return r == null ? 0 : r.pts();
	}

	private static int lvl() {
		return Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
	}

	private static int talent(Talent t) {
		return Dungeon.hero == null ? 0 : Dungeon.hero.pointsInTalent(t);
	}

	/** max HP for a puppet right now */
	public int targetHT() {
		float ht = 8 + 4 * lvl() + 0.8f * pts();
		ht *= 1f + 0.15f * talent(Talent.SLOTH_SNOOZE);
		ht *= com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.slothPuppetStats(); //Seven Sins: awakening
		return Math.max(1, Math.round(ht * statMult(this)));
	}

	/** keeps max HP in line with the hero, preserving the current HP ratio */
	public void rescale() {
		int newHT = targetHT();
		if (newHT == HT) return;
		float ratio = HT <= 0 ? 1f : HP / (float) HT;
		HT = newHT;
		HP = Math.max(1, Math.min(HT, Math.round(HT * ratio)));
	}

	public static DreamPuppet create(boolean nightmare, float lifespan) {
		DreamPuppet p = new DreamPuppet();
		p.nightmare = nightmare;
		p.lifespan = lifespan;
		p.HT = p.targetHT();
		p.HP = p.HT;
		return p;
	}

	/** every puppet and nightmare on the current level */
	public static ArrayList<DreamPuppet> active() {
		ArrayList<DreamPuppet> out = new ArrayList<>();
		if (Dungeon.level == null) return out;
		for (Char ch : Actor.chars()) {
			if (ch instanceof DreamPuppet && ch.isAlive()) out.add((DreamPuppet) ch);
		}
		return out;
	}

	public static ArrayList<DreamPuppet> puppets(boolean nightmares) {
		ArrayList<DreamPuppet> out = new ArrayList<>();
		for (DreamPuppet p : active()) if (p.nightmare == nightmares) out.add(p);
		return out;
	}

	public static DreamPuppet nearestTo(int pos) {
		DreamPuppet best = null;
		for (DreamPuppet p : active()) {
			if (best == null || Dungeon.level.distance(pos, p.pos) < Dungeon.level.distance(pos, best.pos)) best = p;
		}
		return best;
	}

	// ------------------------------------------------------------------ behaviour

	@Override
	protected boolean act() {
		if (nightmare) {
			lifespan -= 1f;
			if (lifespan <= 0) {
				sprite.emitter().burst(Speck.factory(Speck.NOTE), 3);
				die(null);
				return true;
			}
		}
		rescale();
		//an Order from the pillow: the dream link tells every puppet where the marked enemy is
		Char mark = orderedTarget();
		if (mark != null && paralysed <= 0) {
			enemy = mark;
			target = mark.pos;
			defendingPos = -1;
			movingToDefendPos = false;
			if (state != HUNTING) state = HUNTING;
		}
		int oldPos = pos;
		boolean result = super.act();
		if ((pos == target || oldPos == pos) && sprite != null && sprite.looping()) {
			sprite.idle();
		}
		return result;
	}

	private static Char orderedTarget() {
		Char m = SlothRepose.DreamMark.marked();
		return m != null && m.alignment == Alignment.ENEMY ? m : null;
	}

	@Override
	protected Char chooseEnemy() {
		Char mark = orderedTarget();
		if (mark != null) return mark;
		return super.chooseEnemy();
	}

	@Override
	public int attackSkill(Char target) {
		SlothRepose r = repose();
		float acc = 9 + lvl();
		if (r != null) acc *= 1f + r.accuracyBonus();
		return Math.round(acc);
	}

	@Override
	public int defenseSkill(Char enemy) {
		SlothRepose r = repose();
		float ev = 4 + lvl();
		if (r != null) ev *= 1f + r.evasionBonus();
		return Math.round(ev);
	}

	@Override
	public int damageRoll() {
		int lvl = lvl();
		float dmg = Random.NormalIntRange(1 + lvl / 3, 4 + lvl * 2 / 3) + 0.15f * pts();
		if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.DREAM_LORD) {
			dmg *= 1f + 0.10f * talent(Talent.LORD_COMMAND);
		}
		return Math.max(1, Math.round(dmg * statMult(this) * com.shatteredpixel.shatteredpixeldungeon.sins.AwakeningHooks.slothPuppetStats()));
	}

	/** dream puppets have no armor at all: they are only dreams */
	@Override
	public int drRoll() {
		return 0;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (enemy.buff(SlothRepose.DreamMark.class) != null) {
			damage = Math.round(damage * (1f + SlothRepose.DreamMark.DAMAGE_BONUS));
		}
		int yawn = talent(Talent.SLOTH_YAWN);
		if (yawn > 0 && enemy.isAlive() && Random.Float() < 0.15f * yawn) {
			Buff.affect(enemy, Slow.class, 2f);
		}
		return damage;
	}

	@Override
	public String name() {
		return Messages.get(this, nightmare ? "name_nightmare" : "name");
	}

	@Override
	public String description() {
		String d = Messages.get(this, "desc");
		if (nightmare) d += "\n\n" + Messages.get(this, "desc_nightmare", Math.max(0, Math.round(lifespan)));
		return d;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("nightmare", nightmare);
		bundle.put("lifespan", lifespan);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		nightmare = bundle.getBoolean("nightmare");
		lifespan = bundle.getFloat("lifespan");
	}

	/** wanders between random spots on the level, switching to hunting as soon as it sees an enemy */
	private class Roaming extends Mob.Wandering {

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (enemyInFOV) {
				enemySeen = true;
				notice();
				alerted = true;
				state = HUNTING;
				target = enemy.pos;
				return true;
			}
			enemySeen = false;
			if (target == -1 || target == pos || !Dungeon.level.passable[target]) {
				target = Dungeon.level.randomDestination(DreamPuppet.this);
			}
			int oldPos = pos;
			if (target != -1 && getCloser(target)) {
				spend(1 / speed());
				return moveSprite(oldPos, pos);
			}
			//stuck: pick somewhere else next time
			target = Dungeon.level.randomDestination(DreamPuppet.this);
			spend(TICK);
			return true;
		}
	}

	/** placeholder art: a blue-tinted ghost (nightmares are purple) */
	public static class PuppetSprite extends GhostSprite {

		@Override
		public void link(Char ch) {
			super.link(ch);
			if (ch instanceof DreamPuppet && ((DreamPuppet) ch).nightmare) {
				hardlight(0.7f, 0.4f, 1f);
			} else {
				hardlight(0.5f, 0.7f, 1f);
			}
		}

		@Override
		public void resetColor() {
			super.resetColor();
			if (ch instanceof DreamPuppet && ((DreamPuppet) ch).nightmare) {
				hardlight(0.7f, 0.4f, 1f);
			} else {
				hardlight(0.5f, 0.7f, 1f);
			}
		}
	}
}
