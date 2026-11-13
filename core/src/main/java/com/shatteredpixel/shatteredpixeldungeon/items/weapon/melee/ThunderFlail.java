package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

/**
 * Seven Sins: a tier 4 combo weapon. Every hit within MOMENTUM_TURNS of the last one builds a charge (+8% damage each);
 * the hit after the fifth charge is a thunderclap: +50% damage, half of it arcing into every enemy next to the target,
 * which are stunned for a turn, and the charges reset.
 */
public class ThunderFlail extends MeleeWeapon {

	public static final int MAX_CHARGES = 5;
	public static final float PER_CHARGE = 0.08f;
	public static final float MOMENTUM_TURNS = 3f;
	public static final float CLAP_MULT = 1.5f, ARC_SHARE = 0.5f;

	{
		image = ItemSpriteSheet.THUNDER_FLAIL;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 0.9f;
		tier = 4;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (attacker instanceof Hero) {
			Momentum m = attacker.buff(Momentum.class);
			int charges = m == null ? 0 : m.charges;
			if (charges >= MAX_CHARGES) {
				damage = Math.round(damage * CLAP_MULT);
				thunderclap(attacker, defender, Math.round(damage * ARC_SHARE));
				charges = 0;
			} else {
				damage = Math.round(damage * (1f + PER_CHARGE * charges));
				charges++;
			}
			m = Buff.prolong(attacker, Momentum.class, MOMENTUM_TURNS);
			m.charges = charges;
			BuffIndicator.refreshHero();
		}
		return super.proc(attacker, defender, damage);
	}

	private void thunderclap(Char attacker, Char defender, int arcDmg) {
		Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
		if (defender.sprite != null) {
			defender.sprite.centerEmitter().burst(SparkParticle.FACTORY, 12);
			defender.sprite.showStatus(CharSprite.WARNING, Messages.get(ThunderFlail.class, "clap"));
		}
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(defender.pos + n);
			if (ch == null || ch == attacker || ch.alignment == attacker.alignment || !ch.isAlive()) continue;
			if (attacker.sprite != null && attacker.sprite.parent != null && Dungeon.level.heroFOV[ch.pos]) {
				attacker.sprite.parent.addToFront(new Lightning(defender.pos, ch.pos, null));
			}
			ch.damage(arcDmg, this);
			if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
		}
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", MAX_CHARGES, Math.round(PER_CHARGE * 100), Math.round((CLAP_MULT - 1) * 100),
				Math.round(ARC_SHARE * 100), (int) MOMENTUM_TURNS);
	}

	public static class Momentum extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public int charges;

		@Override
		public int icon() {
			return BuffIndicator.COMBO;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(charges >= MAX_CHARGES ? 0x66CCFF : 0xFFDD44);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(charges);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (MOMENTUM_TURNS - visualcooldown()) / MOMENTUM_TURNS);
		}

		@Override
		public String desc() {
			return Messages.get(this, charges >= MAX_CHARGES ? "desc_ready" : "desc", charges,
					Math.round(PER_CHARGE * 100 * charges), dispTurns());
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("charges", charges);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			charges = bundle.getInt("charges");
		}
	}
}
