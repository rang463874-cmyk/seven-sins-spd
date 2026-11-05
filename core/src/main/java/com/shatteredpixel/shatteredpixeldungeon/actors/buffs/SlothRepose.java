package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Sloth class's repose.
 *
 * Torpor builds every turn the hero stays put and fades when they move. Defeating an enemy
 * with enough torpor built up grants sloth points, which raise stats without limit.
 * Points are only earned from kills, so waiting alone never grows the hero forever.
 * Also holds the Cozy Pillow's recharging charges.
 */
public class SlothRepose extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float sloth = 0;
	public int torpor = 0;
	public int lastPos = -1;
	public float regenTurns = 0;

	/** torpor spent by the Sleeping Lion's last pounce, still credited to kills that turn */
	public int pounceTorpor = 0;
	public float pounceTime = -1;

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
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.SLOTH_APATHY);
		return sloth * mult;
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

	/** armor from current torpor; the Hibernator gets much more */
	public int torporDR() {
		Hero h = hero();
		float per = 0.5f;
		if (h != null && h.subClass == HeroSubClass.HIBERNATOR) {
			per *= 1.5f + 0.25f * h.pointsInTalent(Talent.HIB_THICK);
		}
		return Math.round(torpor * per);
	}

	public int drRoll() {
		int max = drMax();
		return (max <= 0 ? 0 : Random.IntRange(max / 2, max)) + torporDR();
	}

	public void addSloth(float amount) {
		Hero h = hero();
		if (h != null) amount *= 1f + 0.15f * h.pointsInTalent(Talent.SLOTH_IDLE);
		if (amount <= 0) return;
		int oldHP = hpBonus();
		sloth += amount;
		if (h != null && hpBonus() != oldHP) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ torpor

	public int maxTorpor() {
		Hero h = hero();
		if (h == null) return 20;
		int max = 20 + 5 * h.pointsInTalent(Talent.SLOTH_COMFY) + (mirrored ? 10 : 0);
		if (h.subClass == HeroSubClass.HIBERNATOR) max += 20 + 10 * h.pointsInTalent(Talent.HIB_DEEP);
		return max;
	}

	public void addTorpor(int n) {
		torpor = Math.min(maxTorpor(), torpor + n);
		BuffIndicator.refreshHero();
	}

	/** torpor counted for a kill: includes torpor the lion just spent on a pounce */
	public int creditedTorpor() {
		if (pounceTime >= 0 && now() - pounceTime <= 1f) return Math.max(torpor, pounceTorpor);
		return torpor;
	}

	/** turns between each point of torpor regeneration, or 0 for none */
	public float regenDelay() {
		if (torpor < 10) return 0;
		Hero h = hero();
		float d = 6f - h.pointsInTalent(Talent.SLOTH_SNORE);
		if (h.subClass == HeroSubClass.HIBERNATOR) d -= 1f + h.pointsInTalent(Talent.HIB_DREAM);
		return Math.max(1f, d);
	}

	/** called when the hero changes position without rolling */
	public void disturb() {
		Hero h = hero();
		int linger = h == null ? 0 : h.pointsInTalent(Talent.SLOTH_LINGER);
		torpor = Math.round(torpor * 0.25f * linger);
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ charges

	public int maxCharges() {
		Hero h = hero();
		return 3 + (mirrored ? 1 : 0) + (h == null ? 0 : h.pointsInTalent(Talent.SLOTH_PILLOW));
	}

	public float turnsPerCharge() {
		Hero h = hero();
		return 30f / (1f + 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.SLOTH_RESTFUL)));
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

			if (lastPos == h.pos) {
				if (torpor < maxTorpor()) addTorpor(1);
			} else if (lastPos != -1) {
				disturb();
			}
			lastPos = h.pos;

			float delay = regenDelay();
			if (delay > 0 && h.HP < h.HT) {
				regenTurns += TICK;
				if (regenTurns >= delay) {
					regenTurns = 0;
					h.HP++;
				}
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
		icon.hardlight(0.45f, 0.6f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return 1f - torpor / (float) maxTorpor();
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(torpor);
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
		return Messages.get(SlothRepose.class, "desc_head", Math.round(sloth), torpor, maxTorpor(), torporDR(), charges, maxCharges())
				+ "\n\n" + Messages.get(SlothRepose.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("sloth", sloth);
		bundle.put("torpor", torpor);
		bundle.put("last_pos", lastPos);
		bundle.put("regen", regenTurns);
		bundle.put("charges", charges);
		bundle.put("partial", partialCharge);
		bundle.put("mirrored", mirrored);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sloth = bundle.getFloat("sloth");
		torpor = bundle.getInt("torpor");
		lastPos = bundle.getInt("last_pos");
		regenTurns = bundle.getFloat("regen");
		charges = bundle.getInt("charges");
		partialCharge = bundle.getFloat("partial");
		mirrored = bundle.getBoolean("mirrored");
	}

	public static SlothRepose of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(SlothRepose.class);
	}
}
