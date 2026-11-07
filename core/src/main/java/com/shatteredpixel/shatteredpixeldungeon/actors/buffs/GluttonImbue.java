package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;

/**
 * Gluttony: a harmful potion or scroll swallowed by the glutton turns into one of these.
 * Each one empowers melee hits (see PredatorHooks.onMeleeAttack) and grants an immunity.
 */
public abstract class GluttonImbue extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	public static final float DURATION = 20f;

	/** called on every melee hit while active; returns the new damage */
	public int proc(Hero hero, Char enemy, int dmg) {
		return dmg;
	}

	protected abstract int color();

	@Override
	public int icon() {
		return BuffIndicator.IMBUE;
	}

	@Override
	public void tintIcon(Image icon) {
		int c = color();
		icon.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - visualcooldown() / 30f);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}

	/** toxic gas: hits poison, immune to poison */
	public static class Venom extends GluttonImbue {
		{
			immunities.add(Poison.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas.class);
		}

		@Override
		public int proc(Hero hero, Char enemy, int dmg) {
			if (enemy.isAlive()) Buff.affect(enemy, Poison.class).set(3f + hero.lvl / 3f);
			return dmg;
		}

		@Override
		protected int color() {
			return 0x60D040;
		}
	}

	/** paralytic gas: hits may paralyze, immune to paralysis */
	public static class Numbing extends GluttonImbue {
		{
			immunities.add(Paralysis.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ParalyticGas.class);
		}

		@Override
		public int proc(Hero hero, Char enemy, int dmg) {
			if (enemy.isAlive() && Random.Float() < 0.25f) Buff.prolong(enemy, Paralysis.class, 2f);
			return dmg;
		}

		@Override
		protected int color() {
			return 0xE0E060;
		}
	}

	/** corrosive gas: hits corrode, immune to corrosion */
	public static class Acid extends GluttonImbue {
		{
			immunities.add(Corrosion.class);
			immunities.add(com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas.class);
		}

		@Override
		public int proc(Hero hero, Char enemy, int dmg) {
			if (enemy.isAlive()) Buff.affect(enemy, Corrosion.class).set(3f, 1 + hero.lvl / 6);
			return dmg;
		}

		@Override
		protected int color() {
			return 0xB0A030;
		}
	}

	/** scroll of rage: +35% melee damage */
	public static class Frenzy extends GluttonImbue {
		@Override
		public int proc(Hero hero, Char enemy, int dmg) {
			return Math.round(dmg * 1.35f);
		}

		@Override
		protected int color() {
			return 0xE04030;
		}
	}
}
