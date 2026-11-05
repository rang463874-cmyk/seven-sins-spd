package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Lust class's desire. Desire is earned by draining the life of charmed enemies and by
 * defeating them, and permanently raises stats without limit. Also holds the Perfume's charges.
 */
public class LustDesire extends Buff {

	public float desire = 0;
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
		return (int) (0.7f * pts());
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

	// ------------------------------------------------------------------ charm helpers

	/** true if this char is charmed by the hero */
	public static boolean charmedByHero(Char ch) {
		Hero h = Dungeon.hero;
		return h != null && ch.isCharmedBy(h);
	}

	public static float charmDuration(Hero hero, float base) {
		float d = base * (1f + 0.25f * hero.pointsInTalent(Talent.LUST_LINGER));
		if (hero.subClass == HeroSubClass.DREAMWEAVER) d += 2f * hero.pointsInTalent(Talent.DREAM_LULLABY);
		return d;
	}

	/** charms a mob for the hero, putting it to sleep as well for the Dreamweaver */
	public static void charm(Hero hero, Mob mob, float baseDuration) {
		Charm c = Buff.prolong(mob, Charm.class, charmDuration(hero, baseDuration));
		c.object = hero.id();
		c.ignoreHeroAllies = true;
		if (hero.subClass == HeroSubClass.DREAMWEAVER && !mob.isImmune(Sleep.class)) {
			mob.state = mob.SLEEPING;
		}
	}

	public static boolean charmImmune(Char ch) {
		return ch.isImmune(Charm.class)
				|| ch.properties().contains(Char.Property.BOSS)
				|| ch.properties().contains(Char.Property.MINIBOSS);
	}

	// ------------------------------------------------------------------ charges

	public int maxCharges() {
		Hero h = hero();
		return 3 + (h == null ? 0 : h.pointsInTalent(Talent.LUST_BOTTLE));
	}

	public float turnsPerCharge() {
		Hero h = hero();
		return 30f / (1f + 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.LUST_REFILL)));
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
		return BuffIndicator.HEART;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.35f, 0.65f);
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
		return Messages.get(LustDesire.class, "desc_head", Math.round(desire), charges, maxCharges())
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
		bundle.put("charges", charges);
		bundle.put("partial", partialCharge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		desire = bundle.getFloat("desire");
		charges = bundle.getInt("charges");
		partialCharge = bundle.getFloat("partial");
	}

	public static LustDesire of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(LustDesire.class);
	}
}
