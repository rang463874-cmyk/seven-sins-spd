package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Greed class's purse, tied to the Golden Card.
 *
 * Stats scale with the gold the hero is carrying right now, so spending gold in a shop
 * lowers them again. Payment skills use a separate "payment limit" gauge, which is
 * filled by picking up gold (the gold itself still goes into the wallet).
 */
public class GreedVault extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	/** payment limit gauge, spent by the Golden Card's payment skills */
	public float charge = 0;

	/** deepest floor that has already paid out banker interest */
	public int interestDepth = 0;
	/** turns since the last banker dividend */
	public float dividendTurns = 0;

	/** gold seen on the last tick, so max HP follows the wallet */
	private int lastGold = -1;
	/** card balance from saves made before the rework, returned to the wallet on the next tick */
	private long legacyRefund = 0;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	// ------------------------------------------------------------------ stats from carried gold

	/** wealth points: one per 100 gold carried */
	public float pts() {
		Hero h = hero();
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.GREED_PRESTIGE);
		return Math.max(0, Dungeon.gold) / 100f * mult;
	}

	public int meleeBonus() {
		return (int) (0.20f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.005f * pts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.004f * pts());
	}

	public int hpBonus() {
		return (int) (0.5f * pts());
	}

	public int drMax() {
		return (int) (0.12f * pts());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	/** re-syncs max HP with the wallet; safe to call any time gold changes */
	public void goldChanged() {
		Hero h = hero();
		lastGold = Dungeon.gold;
		if (h != null) h.updateHT(true);
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
	}

	// ------------------------------------------------------------------ payment limit gauge

	public float maxCharge() {
		Hero h = hero();
		int hoard = h == null ? 0 : h.pointsInTalent(Talent.GREED_HOARD);
		return (100 + 50 * Math.max(1, Dungeon.depth)) * (1f + 0.25f * hoard) * SinArtifact.gaugeScale(h);
	}

	public void addCharge(float amount) {
		if (amount <= 0) return;
		charge = Math.min(maxCharge(), charge + amount);
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
	}

	/** pays a cost out of the gauge. Returns false (and spends nothing) if it can't be afforded */
	public boolean spend(int cost) {
		if (cost > charge) return false;
		charge -= cost;
		SinArtifact.onSpent(hero(), cost / 20f);
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
		return true;
	}

	public int chargeInt() {
		return (int) charge;
	}

	/** called when gold is picked up from the floor (not from selling items) */
	public void onGoldPickup(Hero hero, int amount) {
		if (amount <= 0) return;
		addCharge(amount * SinArtifact.gaugeScale(hero));

		int skin = hero.pointsInTalent(Talent.GREED_GILDED_SKIN);
		if (skin > 0) {
			int shield = Math.min(Math.round(amount * 0.04f * skin), Math.round(hero.HT * 0.15f * skin));
			if (shield > 0) Buff.affect(hero, Barrier.class).setShield(shield);
		}
		goldChanged();
	}

	/** gold earned by talents, weapons or debts: goes to the wallet and fills the gauge like a pickup */
	public void earnGold(Hero hero, int amount) {
		if (amount <= 0) return;
		Dungeon.gold += amount;
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.GOLD);
		onGoldPickup(hero, amount);
	}

	// ------------------------------------------------------------------ banker interest

	public float interestRate() {
		Hero h = hero();
		return 0.05f + 0.03f * (h == null ? 0 : h.pointsInTalent(Talent.BANKER_RATE));
	}

	private void payInterest(Hero h, float rate) {
		int interest = (int) Math.min(Integer.MAX_VALUE / 4, (long) Math.floor(Math.max(0, Dungeon.gold) * (double) rate));
		if (interest <= 0) return;
		Dungeon.gold += interest;
		int cap = h.pointsInTalent(Talent.BANKER_CAPITALIZE);
		if (cap > 0) addCharge(interest * 0.5f * cap);
		goldChanged();
		h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(interest), FloatingText.GOLD);
		GLog.p(Messages.get(GreedVault.class, "interest", interest));
	}

	@Override
	public boolean act() {
		Hero h = hero();
		if (h != null && h.isAlive()) {
			if (legacyRefund > 0) {
				Dungeon.gold += (int) Math.min(Integer.MAX_VALUE / 4, legacyRefund);
				legacyRefund = 0;
			}
			if (Dungeon.gold != lastGold) goldChanged();

			if (Dungeon.depth > interestDepth) {
				//interest is paid once per new floor, to bankers and to holders of the black card
				float rate = (h.subClass == HeroSubClass.BANKER ? interestRate() : 0f) + (mirrored ? 0.02f : 0f);
				if (rate > 0 && interestDepth > 0) {
					payInterest(h, rate);
				}
				interestDepth = Dungeon.depth;
			}

			int div = h.pointsInTalent(Talent.BANKER_DIVIDEND);
			if (h.subClass == HeroSubClass.BANKER && div > 0) {
				dividendTurns += TICK;
				if (dividendTurns >= 400 - 100 * div) {
					dividendTurns = 0;
					payInterest(h, interestRate() / 2f);
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
		icon.hardlight(1f, 0.85f, 0.2f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - charge / maxCharge());
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(chargeInt());
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
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.get(GreedVault.class, "desc_money", Dungeon.gold, chargeInt(), Math.round(maxCharge()))).append("\n\n");
		sb.append(Messages.get(GreedVault.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100)));
		Hero h = hero();
		if (h != null && h.subClass == HeroSubClass.BANKER) {
			sb.append("\n\n").append(Messages.get(GreedVault.class, "desc_interest", Math.round(interestRate() * 100)));
		}
		return sb.toString();
	}

	private static final String CHARGE = "charge";
	private static final String INTEREST_DEPTH = "interest_depth";
	private static final String DIVIDEND = "dividend_turns";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(CHARGE, charge);
		bundle.put(INTEREST_DEPTH, interestDepth);
		bundle.put(DIVIDEND, dividendTurns);
		bundle.put("mirrored", mirrored);
		if (legacyRefund > 0) bundle.put("balance", legacyRefund);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getFloat(CHARGE);
		interestDepth = bundle.getInt(INTEREST_DEPTH);
		dividendTurns = bundle.getFloat(DIVIDEND);
		mirrored = bundle.getBoolean("mirrored");
		//saves from before the rework kept gold in the card; hand it back
		legacyRefund = bundle.contains("balance") ? bundle.getLong("balance") : 0;
	}

	// ------------------------------------------------------------------ usurer debt

	/** Forced loan placed on an enemy by the Usurer. Collected (with interest) when it dies. */
	public static class Debt extends Buff {

		public int principal = 0;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public static float repayMultiplier(Hero hero) {
			return 2f + 0.5f * hero.pointsInTalent(Talent.USURER_GOUGE);
		}

		/** bonus damage the hero deals to this debtor on every hit */
		public int bonusDamage(Hero hero) {
			return Math.round(principal * 0.08f * (1f + 0.25f * hero.pointsInTalent(Talent.USURER_COLLECTOR)));
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.8f, 0.1f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(principal);
		}

		@Override
		public String desc() {
			Hero h = Dungeon.hero;
			int owed = h == null ? principal * 2 : Math.round(principal * repayMultiplier(h));
			return Messages.get(this, "desc", principal, owed);
		}

		@Override
		public void fx(boolean on) {
			if (on) target.sprite.add(CharSprite.State.MARKED);
			else target.sprite.remove(CharSprite.State.MARKED);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("principal", principal);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			principal = bundle.getInt("principal");
		}

		/** called when the debtor dies: the hero collects principal x repay multiplier */
		public static void collect(Hero hero, Char debtor) {
			Debt d = debtor.buff(Debt.class);
			GreedVault v = hero.buff(GreedVault.class);
			if (d == null || v == null || d.principal <= 0) return;
			int owed = Math.round(d.principal * repayMultiplier(hero));
			d.principal = 0;
			v.earnGold(hero, owed);
			GLog.p(Messages.get(Debt.class, "collected", owed));
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
		}
	}

	/** Insurance armor ability: the next lethal blow is paid off instead. */
	public static class Insurance extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.85f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 150f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
