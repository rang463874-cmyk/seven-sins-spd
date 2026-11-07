package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.WarlockSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * City (floors 16-19): the fallen priest. Blesses every monster it can see (heal 15% + barrier),
 * and curses a marked 3x3 area around the hero (damage, weakness, vulnerability).
 */
public class FallenPriest extends RegionElite {

	private static final int BLESS = 0, CURSE = 1;

	{
		spriteClass = PriestSprite.class;
		HP = HT = 190;
		defenseSkill = 20;
		EXP = 32;
		maxLvl = 27;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(10, 20); }
	@Override public int attackSkill(Char target) { return 32; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 10); }

	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == BLESS ? 7 : 4; }
	@Override protected String skillKey(int s) { return s == BLESS ? "bless" : "curse"; }

	private ArrayList<Mob> flock() {
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs) {
			if (m.alignment == alignment && m.isAlive() && (m == this || fieldOfView[m.pos])
					&& (m.HP < m.HT || m == this)) out.add(m);
		}
		return out;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == BLESS) {
			//only worth it if someone (itself included) is hurt
			for (Mob m : flock()) if (m.HP < m.HT * 0.8f) return new int[0];
			return null;
		}
		ArrayList<Integer> cells = new ArrayList<>();
		addIfOpen(cells, target.pos);
		for (int n : PathFinder.NEIGHBOURS8) addIfOpen(cells, target.pos + n);
		cells.remove((Integer) pos);
		return toArray(cells);
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == BLESS) {
			for (Mob m : flock()) {
				int heal = Math.min(m.HT - m.HP, Math.round(m.HT * 0.15f));
				if (heal > 0) {
					m.HP += heal;
					if (m.sprite != null) m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
				}
				Buff.affect(m, Barrier.class).setShield(Math.round(m.HT * 0.1f));
				if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
			}
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
			return;
		}
		for (int c : cells) {
			CellEmitter.get(c).burst(ShadowParticle.CURSE, 5);
			Char ch = strike(c, 10, 18);
			if (ch != null && ch.isAlive()) {
				Buff.prolong(ch, Weakness.class, 6f);
				Buff.prolong(ch, Vulnerable.class, 6f);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
	}

	public static class PriestSprite extends WarlockSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(0.75f, 0.45f, 1f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(0.75f, 0.45f, 1f);
		}
	}
}
