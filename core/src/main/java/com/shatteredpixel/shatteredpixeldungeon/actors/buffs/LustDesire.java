package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Lust class's desire: a dancer who drinks life.
 *
 * Hits on charmed enemies steal life; every point of life stolen feeds desire, which raises
 * stats without limit. Dodging an attack charms the attacker. The Perfume's skills are paid
 * for in HP instead of charges.
 */
public class LustDesire extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float desire = 0;

	/** base share of damage stolen from charmed enemies */
	public static final float BASE_LIFESTEAL = 0.25f;
	/** desire gained per point of life stolen */
	public static final float DESIRE_PER_HP = 0.25f;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	// ------------------------------------------------------------------ permanent stats

	public float pts() {
		Hero h = hero();
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.LUST_OBSESSION);
		return desire * mult;
	}

	public int meleeBonus() {
		return (int) (0.12f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.003f * pts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.004f * pts());
	}

	public int hpBonus() {
		return (int) (0.35f * pts());
	}

	public int drMax() {
		return (int) (0.08f * pts());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	public void addDesire(float amount) {
		Hero h = hero();
		if (h != null) amount *= 1f + 0.15f * h.pointsInTalent(Talent.LUST_YEARNING);
		if (amount <= 0) return;
		int oldHP = hpBonus();
		desire += amount;
		if (h != null && hpBonus() != oldHP) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ lifesteal

	/**
	 * Steals life equal to dmg x rate. Life stolen beyond max HP becomes shielding for the
	 * Crimson Noble. All of it feeds desire. Returns the amount stolen.
	 */
	public int drain(int dmg, float rate) {
		Hero h = hero();
		if (h == null || dmg <= 0 || rate <= 0) return 0;

		if (h.subClass == HeroSubClass.CRIMSON_NOBLE) {
			//the noble drinks deeper the closer it is to death
			float missing = 1f - h.HP / (float) Math.max(1, h.HT);
			rate *= 1f + (0.5f + 0.25f * h.pointsInTalent(Talent.NOBLE_THIRST)) * Math.max(0, missing);
		}

		int stolen = Math.round(dmg * rate);
		if (stolen <= 0) return 0;
		int heal = Math.min(h.HT - h.HP, stolen);
		if (heal > 0) {
			h.HP += heal;
			h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		int overflow = stolen - heal;
		if (overflow > 0 && h.subClass == HeroSubClass.CRIMSON_NOBLE) {
			int shield = Math.round(overflow * (0.5f + 0.25f * h.pointsInTalent(Talent.NOBLE_BLOOD)));
			if (shield > 0) Buff.affect(h, Barrier.class).incShield(shield);
		}
		addDesire(stolen * DESIRE_PER_HP);
		return stolen;
	}

	/** lifesteal share for a melee hit on this enemy, 0 if none applies */
	public float lifestealFor(Char enemy) {
		Hero h = hero();
		if (h == null || enemy == null) return 0;
		float rate = 0;
		boolean charmed = charmedByHero(enemy);
		if (charmed) rate += BASE_LIFESTEAL + 0.05f * h.pointsInTalent(Talent.LUST_SWEET);
		if (h.buff(Rapture.class) != null) rate += BASE_LIFESTEAL;
		BloodPact pact = h.buff(BloodPact.class);
		if (pact != null) rate += 0.25f + 0.10f * h.pointsInTalent(Talent.PACT_THIRST);
		return rate;
	}

	// ------------------------------------------------------------------ skill costs

	/** HP cost of a perfume skill that costs this share of max HP */
	public int hpCost(float share) {
		Hero h = hero();
		if (h == null) return 0;
		float mult = 1f - 0.15f * h.pointsInTalent(Talent.LUST_BOTTLE);
		if (mirrored) mult *= 0.75f;
		if (h.subClass == HeroSubClass.CRIMSON_NOBLE) mult *= 0.5f;
		mult *= SinArtifact.costScale(h);
		return Math.max(1, Math.round(h.HT * share * mult));
	}

	public boolean canPay(int cost) {
		Hero h = hero();
		return h != null && h.HP > cost;
	}

	/** pays a skill's HP cost; the Noble's Price talent hands part of it back as shielding */
	public boolean pay(int cost) {
		Hero h = hero();
		if (h == null || h.HP <= cost) return false;
		h.HP -= cost;
		h.sprite.showStatus(CharSprite.NEGATIVE, Integer.toString(cost));
		SinArtifact.onSpent(h, 1);
		int price = h.subClass == HeroSubClass.CRIMSON_NOBLE ? h.pointsInTalent(Talent.NOBLE_PRICE) : 0;
		if (price > 0) Buff.affect(h, Barrier.class).incShield(Math.round(cost * 0.2f * price));
		return true;
	}

	// ------------------------------------------------------------------ charm helpers

	/** true if this char is charmed by the hero */
	public static boolean charmedByHero(Char ch) {
		Hero h = Dungeon.hero;
		return h != null && ch != null && ch.isCharmedBy(h);
	}

	public static float charmDuration(Hero hero, float base) {
		float d = base * (1f + 0.25f * hero.pointsInTalent(Talent.LUST_LINGER));
		LustDesire ld = hero.buff(LustDesire.class);
		if (ld != null && ld.mirrored) d *= 1.25f;
		return d;
	}

	public static void charm(Hero hero, Mob mob, float baseDuration) {
		Charm c = Buff.prolong(mob, Charm.class, charmDuration(hero, baseDuration));
		if (c == null) return;
		c.object = hero.id();
		c.ignoreHeroAllies = true;
	}

	public static boolean charmImmune(Char ch) {
		return ch.isImmune(Charm.class)
				|| ch.properties().contains(Char.Property.BOSS)
				|| ch.properties().contains(Char.Property.MINIBOSS);
	}

	// ------------------------------------------------------------------ ui

	@Override
	public int icon() {
		return BuffIndicator.HEART;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.25f, 0.4f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(Math.round(desire));
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return statsText();
	}

	public String statsText() {
		return statsTextBase() + com.shatteredpixel.shatteredpixeldungeon.sins.UniqueStats.line(target);
	}

	private String statsTextBase() {
		return Messages.get(LustDesire.class, "desc_head", Math.round(desire))
				+ "\n\n" + Messages.get(LustDesire.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("desire", desire);
		bundle.put("mirrored", mirrored);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		desire = bundle.getFloat("desire");
		mirrored = bundle.getBoolean("mirrored");
	}

	public static LustDesire of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(LustDesire.class);
	}

	// ------------------------------------------------------------------ skill buffs

	/** Step: evasion is greatly raised for a few turns */
	public static class Sway extends FlavourBuff {

		public static final float EVASION_MULT = 1.6f;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.HASTE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.4f, 0.7f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 8f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	/** Rapture: every hit steals life */
	public static class Rapture extends FlavourBuff {

		public static final float DURATION = 10f;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.9f, 0.1f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	/** Blood Pact armor ability: strong lifesteal on every hit */
	public static class BloodPact extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.SACRIFICE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.8f, 0.05f, 0.15f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 20f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
