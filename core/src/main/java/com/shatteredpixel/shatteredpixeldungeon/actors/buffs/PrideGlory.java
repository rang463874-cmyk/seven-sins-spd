package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.DuelingSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.KingsAegis;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Pride class's glory.
 *
 * Glory is earned by killing enemies that never managed to hurt the hero, and permanently
 * raises stats without limit. Dignity builds while the hero goes unhurt and shatters when hit.
 * Also holds the Crown of Hubris's recharging charges.
 */
public class PrideGlory extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float glory = 0;

	public int dignity = 0;
	public float calmTurns = 0;

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
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.PRIDE_MAJESTY);
		return glory * mult;
	}

	public int meleeBonus() {
		return (int) (0.15f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.004f * pts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.003f * pts());
	}

	public int hpBonus() {
		return (int) (0.8f * pts());
	}

	public int drMax() {
		return (int) (0.10f * pts());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	public void addGlory(float amount) {
		Hero h = hero();
		if (h != null) amount *= 1f + 0.15f * h.pointsInTalent(Talent.PRIDE_VANITY);
		if (SinWeapons.wields(h, KingsAegis.class, HeroClass.PRIDE)) amount *= 1.5f;
		if (amount <= 0) return;
		int oldHP = hpBonus();
		glory += amount;
		if (h != null && hpBonus() != oldHP) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	/** glory for a flawless kill of this char */
	public static float gloryFor(Char ch) {
		float g = 1f + ch.HT / 15f;
		if (ch.properties().contains(Char.Property.BOSS)) g *= 5f;
		else if (ch.properties().contains(Char.Property.MINIBOSS)) g *= 3f;
		return g;
	}

	// ------------------------------------------------------------------ dignity

	public int maxDignity() {
		Hero h = hero();
		int max = 10 + 3 * (h == null ? 0 : h.pointsInTalent(Talent.PRIDE_COMPOSURE));
		if (SinWeapons.wields(h, KingsAegis.class, HeroClass.PRIDE)) max += 5;
		if (mirrored) max += 5;
		return max;
	}

	public float turnsPerDignity() {
		Hero h = hero();
		float t = 3f - 0.5f * (h == null ? 0 : h.pointsInTalent(Talent.PRIDE_POISE));
		if (SinWeapons.wields(h, DuelingSword.class, HeroClass.PRIDE)) t -= 1f;
		return Math.max(1f, t);
	}

	/** multiplier on melee damage from dignity: +4% per stack */
	public float dignityMult() {
		return 1f + 0.04f * dignity;
	}

	public void addDignity(int n) {
		dignity = Math.min(maxDignity(), dignity + n);
		BuffIndicator.refreshHero();
	}

	/** being hurt shatters the hero's dignity */
	public void shatter() {
		Hero h = hero();
		calmTurns = 0;
		if (dignity <= 0) return;
		int grace = h == null ? 0 : h.pointsInTalent(Talent.PRIDE_GRACE);
		int kept = Math.round(dignity * 0.25f * grace);
		if (h != null && h.sprite != null && kept < dignity) {
			h.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(PrideGlory.class, "shattered"));
		}
		dignity = kept;
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ charges

	public int maxCharges() {
		Hero h = hero();
		return 3 + (mirrored ? 1 : 0) + (h == null ? 0 : h.pointsInTalent(Talent.PRIDE_REGALIA));
	}

	public float turnsPerCharge() {
		Hero h = hero();
		return 30f / (1f + 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.PRIDE_SWIFT_CROWN)));
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
		if (h != null && h.isAlive()) {
			if (charges < maxCharges()) gainCharge(TICK / turnsPerCharge());
			calmTurns += TICK;
			if (calmTurns >= turnsPerDignity()) {
				calmTurns = 0;
				if (dignity < maxDignity()) addDignity(1);
			}
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
		icon.hardlight(0.75f, 0.45f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - dignity / (float) maxDignity();
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(dignity);
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
		return Messages.get(PrideGlory.class, "desc_head", Math.round(glory), dignity, maxDignity(),
				Math.round((dignityMult() - 1f) * 100), charges, maxCharges())
				+ "\n\n" + Messages.get(PrideGlory.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("glory", glory);
		bundle.put("dignity", dignity);
		bundle.put("calm", calmTurns);
		bundle.put("charges", charges);
		bundle.put("partial", partialCharge);
		bundle.put("mirrored", mirrored);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		glory = bundle.getFloat("glory");
		dignity = bundle.getInt("dignity");
		calmTurns = bundle.getFloat("calm");
		charges = bundle.getInt("charges");
		partialCharge = bundle.getFloat("partial");
		mirrored = bundle.getBoolean("mirrored");
	}

	// ------------------------------------------------------------------ marks on enemies

	/** This enemy has dared to hurt the hero: killing it no longer brings glory. */
	public static class Insolent extends Buff {
		{
			type = buffType.NEUTRAL;
		}
	}

	/** Decree: this enemy's next attack(s) on the hero do nothing. */
	public static class Decree extends Buff {
		public int uses = 1;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.WEAKNESS;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.75f, 0.45f, 1f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(uses);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", uses);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("uses", uses);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			uses = bundle.getInt("uses");
		}
	}

	/** Duel: on the hero. Only the challenged enemy can hurt the hero. */
	public static class Duel extends FlavourBuff {
		public int object = 0;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.DUEL_EVASIVE;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 12f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
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

	/** Aegis armor ability: the next few blows are turned aside entirely. */
	public static class Aegis extends Buff {
		public int blocks = 3;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.75f, 0.45f, 1f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(blocks);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", blocks);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("blocks", blocks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			blocks = bundle.getInt("blocks");
		}
	}

	/** Monarch: an enemy that knelt and now serves. Unlike corruption, it does not decay. */
	public static class Vassal extends AllyBuff {
		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HEART;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.75f, 0.45f, 1f);
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.GLOWING);
			else target.sprite.remove(CharSprite.State.GLOWING);
		}
	}

	public static boolean royalImmune(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS);
	}

	public static PrideGlory of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(PrideGlory.class);
	}
}
