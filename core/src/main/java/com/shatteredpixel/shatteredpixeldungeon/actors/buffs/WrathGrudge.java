package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.WrathGreatsword;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Wrath class's grudge. Grudge points are earned whenever enemies hurt or kill each other
 * and permanently raise stats without limit. Also holds the War Drum's recharging charges.
 */
public class WrathGrudge extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float grudge = 0;
	public int charges = 2;
	public float partialCharge = 0;

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
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.WRATH_FURY);
		return grudge * mult;
	}

	public int meleeBonus() {
		return (int) (0.10f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.003f * pts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.002f * pts());
	}

	public int hpBonus() {
		return (int) (0.5f * pts());
	}

	public int drMax() {
		return (int) (0.06f * pts());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	public void addGrudge(float amount) {
		Hero h = hero();
		if (h != null) amount *= 1f + 0.15f * h.pointsInTalent(Talent.WRATH_SPITE);
		if (SinWeapons.wields(h, WrathGreatsword.class, HeroClass.WRATH)) amount *= 1.5f;
		if (amount <= 0) return;
		int oldHP = hpBonus();
		grudge += amount;
		if (h != null && hpBonus() != oldHP) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ charges

	public int maxCharges() {
		Hero h = hero();
		return 3 + (mirrored ? 1 : 0) + (h == null ? 0 : h.pointsInTalent(Talent.WRATH_DRUMMER));
	}

	public float turnsPerCharge() {
		Hero h = hero();
		float t = 35f / (1f + 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.WRATH_TEMPO)));
		return mirrored ? t / 1.25f : t;
	}

	public void gainCharge(float amount) {
		if (charges >= maxCharges()) {
			partialCharge = 0;
			return;
		}
		partialCharge += amount;
		while (partialCharge >= 1f && charges < maxCharges()) {
			partialCharge -= 1f;
			charges++;
		}
		if (charges >= maxCharges()) partialCharge = 0;
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
	}

	public boolean useCharges(int n) {
		if (charges < n) return false;
		charges -= n;
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
		return true;
	}

	/** standard duration for amok/discord inflicted by the hero */
	public static float rageDuration(Hero hero, float base) {
		float d = base * (1f + 0.25f * hero.pointsInTalent(Talent.WRATH_LONG_GRUDGE));
		if (hero.subClass == HeroSubClass.INSTIGATOR) d *= 1.5f;
		return d;
	}

	@Override
	public boolean act() {
		Hero h = hero();
		if (h != null && h.isAlive() && charges < maxCharges()) {
			gainCharge(TICK / turnsPerCharge());
		}
		spend(TICK);
		return true;
	}

	// ------------------------------------------------------------------ ui

	@Override
	public int icon() {
		return BuffIndicator.FURY;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.25f, 0.1f);
	}

	@Override
	public float iconFadePercent() {
		return charges >= maxCharges() ? 0 : 1f - partialCharge;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(charges);
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
		return Messages.get(WrathGrudge.class, "desc_head", Math.round(grudge), charges, maxCharges())
				+ "\n\n" + Messages.get(WrathGrudge.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("grudge", grudge);
		bundle.put("charges", charges);
		bundle.put("partial", partialCharge);
		bundle.put("mirrored", mirrored);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		grudge = bundle.getFloat("grudge");
		charges = bundle.getInt("charges");
		partialCharge = bundle.getFloat("partial");
		mirrored = bundle.getBoolean("mirrored");
	}

	// ------------------------------------------------------------------ debuffs on enemies

	/** Two enemies set against each other: each will only hunt its rival. */
	public static class Discord extends FlavourBuff {
		public int object = 0;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.AMOK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.5f, 0f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 15f);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("object", object);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt("object");
		}
	}

	/** Amok that spreads: anything this enemy damages is also driven to rage. */
	public static class Contagion extends FlavourBuff {
		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.AMOK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.6f, 1f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 15f);
		}
	}

	/** First Blood talent: the provoked enemy's next hit on another enemy is stronger. */
	public static class FirstBlood extends Buff {
		{
			type = buffType.NEUTRAL;
		}
	}

	/** Avenger's Empower talent: stacking melee bonus from watching enemies fight. */
	public static class Vengeance extends Buff {
		public int stacks = 0;
		public float left = 0;

		{
			type = buffType.POSITIVE;
		}

		public void add(int max) {
			stacks = Math.min(max, stacks + 1);
			left = 10f;
			BuffIndicator.refreshHero();
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0) detach();
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.2f, 0.1f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(stacks);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - left / 10f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", stacks, (int) left);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("stacks", stacks);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt("stacks");
			left = bundle.getFloat("left");
		}
	}

	/** true if this char is currently enraged by the hero (amok, discord or contagion) */
	public static boolean enraged(Char ch) {
		return ch.buff(Amok.class) != null || ch.buff(Discord.class) != null || ch.buff(Contagion.class) != null;
	}

	public static boolean immune(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS)
				|| ch.isImmune(Amok.class);
	}

	/** current hero's grudge, or null */
	public static WrathGrudge of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(WrathGrudge.class);
	}
}
