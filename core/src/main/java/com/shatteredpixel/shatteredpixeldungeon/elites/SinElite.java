package com.shatteredpixel.shatteredpixeldungeon.elites;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Sin elites: an ordinary monster possessed by one of the seven sins. Built on the champion framework
 * (aura, damage/accuracy factors, attack procs are already wired in Char/Mob), plus +50% max HP,
 * double exp and a name prefix ("Wrathful rat"). One may appear on 25% of floors (see EliteHooks).
 * Killing one drops a sin essence; killing the hero's own sin also grants sin points.
 */
public abstract class SinElite extends ChampionEnemy {

	public static final Class<?>[] ALL = {
			Gluttonous.class, Greedy.class, Wrathful.class, Proud.class, Lustful.class, Envious.class, Slothful.class
	};
	private static final int[] COLORS = {0xAA44FF, 0xFFCC22, 0xFF3322, 0xFFFFFF, 0xFF66AA, 0x44DD66, 0x5588FF};

	/** sin index, same order as everywhere else (gluttony 0 ... sloth 6) */
	public abstract int sin();

	{
		rays = 5;
	}

	private boolean boosted = false;

	@Override
	public boolean attachTo(Char target) {
		color = COLORS[sin()];
		if (!super.attachTo(target)) return false;
		if (!boosted) {
			boosted = true;
			target.HT = Math.round(target.HT * 1.5f);
			target.HP = target.HT;
			if (target instanceof Mob) ((Mob) target).EXP *= 2;
		}
		return true;
	}

	public String prefix() {
		return Messages.get(SinElite.class, "prefix_" + sin());
	}

	@Override
	public String name() {
		return Messages.get(SinElite.class, "name", prefix());
	}

	@Override
	public String desc() {
		return Messages.get(SinElite.class, "desc_" + sin()) + "\n\n" + Messages.get(SinElite.class, "desc_common");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("boosted", boosted);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		boosted = bundle.getBoolean("boosted");
	}

	protected void heal(int amount) {
		amount = Math.min(amount, target.HT - target.HP);
		if (amount <= 0) return;
		target.HP += amount;
		if (target.sprite != null) {
			target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
		}
	}

	// ---- the seven ----

	/** devours: its hits steal satiety and heal it */
	public static class Gluttonous extends SinElite {
		public int sin() { return 0; }

		@Override
		public void onAttackProc(Char enemy) {
			Hunger h = enemy.buff(Hunger.class);
			if (h != null && !h.isStarving()) h.affectHunger(-25);
			heal(Math.max(1, Math.round(target.HT * 0.08f)));
		}

		@Override
		public float damageTakenFactor() {
			return 0.85f;
		}
	}

	/** steals gold on every hit and pays it back double when killed */
	public static class Greedy extends SinElite {
		public int sin() { return 1; }

		public int stolen = 0;

		@Override
		public void onAttackProc(Char enemy) {
			if (!(enemy instanceof Hero) || Dungeon.gold <= 0) return;
			int take = Math.min(Dungeon.gold, 10 + 5 * Dungeon.depth);
			Dungeon.gold -= take;
			stolen += take;
			if (target.sprite != null) target.sprite.emitter().burst(Speck.factory(Speck.COIN), 4);
			GLog.w(Messages.get(SinElite.class, "greed_steal", take));
		}

		/** what it drops on death */
		public Gold loot() {
			return new Gold(Math.max(20, stolen * 2));
		}

		@Override
		public float evasionAndAccuracyFactor() {
			return 1.25f;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("stolen", stolen);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stolen = bundle.getInt("stolen");
		}
	}

	/** hits harder the more it is hurt, and riles up nearby monsters when it notices you */
	public static class Wrathful extends SinElite {
		public int sin() { return 2; }

		private boolean roared = false;

		@Override
		public boolean act() {
			if (!roared && target instanceof Mob && ((Mob) target).state == ((Mob) target).HUNTING
					&& Dungeon.hero != null) {
				roared = true;
				for (Mob m : new ArrayList<>(Dungeon.level.mobs)) {
					if (m != target && m.alignment == Char.Alignment.ENEMY
							&& Dungeon.level.distance(m.pos, target.pos) <= 6) {
						m.beckon(Dungeon.hero.pos);
					}
				}
				if (target.sprite != null) {
					target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(SinElite.class, "wrath_roar"));
				}
			}
			spend(TICK);
			return true;
		}

		@Override
		public float meleeDamageFactor() {
			float missing = 1f - target.HP / (float) Math.max(1, target.HT);
			return 1.1f + 0.9f * missing;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("roared", roared);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			roared = bundle.getBoolean("roared");
		}
	}

	/** a barrier of pride (20% max HP) that comes back after 4 turns without being hit */
	public static class Proud extends SinElite {
		public int sin() { return 3; }

		private int calm = 0;
		private int lastHP = -1;
		private int lastShield = -1;

		@Override
		public boolean act() {
			int shield = target.shielding();
			if (lastHP != -1 && (target.HP < lastHP || shield < lastShield)) calm = 0;
			else calm++;
			if (calm >= 4) {
				int max = Math.max(1, Math.round(target.HT * 0.2f));
				Barrier b = target.buff(Barrier.class);
				if (b == null || b.shielding() < max) {
					Buff.affect(target, Barrier.class).setShield(max);
					if (target.sprite != null) target.sprite.showStatus(CharSprite.POSITIVE, Messages.get(SinElite.class, "pride_shield"));
				}
				calm = 0;
			}
			lastHP = target.HP;
			lastShield = target.shielding();
			spend(TICK);
			return true;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("calm", calm);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			calm = bundle.getInt("calm");
		}
	}

	/** its hits may charm you, and it drinks a little life from every hit */
	public static class Lustful extends SinElite {
		public int sin() { return 4; }

		@Override
		public void onAttackProc(Char enemy) {
			heal(Math.max(1, Math.round(target.HT * 0.05f)));
			if (enemy.buff(Charm.class) == null && Random.Int(3) == 0) {
				Charm c = Buff.affect(enemy, Charm.class, 2f);
				if (c != null) c.object = target.id();
				if (enemy.sprite != null) enemy.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
			}
		}
	}

	/** keener than its kind, and its hits strip one of your positive effects */
	public static class Envious extends SinElite {
		public int sin() { return 5; }

		@Override
		public void onAttackProc(Char enemy) {
			ArrayList<Buff> good = new ArrayList<>();
			for (Buff b : enemy.buffs()) {
				if (b.type == Buff.buffType.POSITIVE && b instanceof FlavourBuff) good.add(b);
			}
			if (good.isEmpty()) return;
			Buff b = Random.element(good);
			b.detach();
			if (target.sprite != null) target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(SinElite.class, "envy_steal"));
		}

		@Override
		public float evasionAndAccuracyFactor() {
			return 1.5f;
		}
	}

	/** found asleep; once woken it moves and strikes in a frenzy for a while */
	public static class Slothful extends SinElite {
		public int sin() { return 6; }

		private boolean woke = false;

		@Override
		public boolean attachTo(Char target) {
			if (!super.attachTo(target)) return false;
			if (!woke && target instanceof Mob) ((Mob) target).state = ((Mob) target).SLEEPING;
			return true;
		}

		@Override
		public boolean act() {
			if (!woke && target instanceof Mob && ((Mob) target).state != ((Mob) target).SLEEPING) {
				woke = true;
				Buff.affect(target, Adrenaline.class, 10f);
				if (target.sprite != null) target.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(SinElite.class, "sloth_wake"));
			}
			spend(TICK);
			return true;
		}

		@Override
		public float meleeDamageFactor() {
			return 1.2f;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("woke", woke);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			woke = bundle.getBoolean("woke");
		}
	}
}
