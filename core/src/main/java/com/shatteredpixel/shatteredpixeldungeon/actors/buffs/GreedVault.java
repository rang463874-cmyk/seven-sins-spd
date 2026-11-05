package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Greed class's bank account, held by the Golden Card.
 *
 * Stats scale with the lifetime amount deposited ("credit"), which never goes down,
 * so withdrawing or spending the balance on payment skills keeps every stat bonus.
 */
public class GreedVault extends Buff {

	/** gold currently in the account, spendable on payment skills or withdrawable */
	public long balance = 0;
	/** lifetime gold credited to the account; drives the permanent stats */
	public long credited = 0;

	/** deepest floor that has already paid out banker interest */
	public int interestDepth = 0;
	/** turns since the last banker dividend */
	public float dividendTurns = 0;

	public static final long MAX_BALANCE = 2_000_000_000L;

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	// ------------------------------------------------------------------ permanent stats

	/** credit points: one per 100 gold credited, unbounded */
	public float creditPts() {
		Hero h = hero();
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.GREED_PRESTIGE);
		return credited / 100f * mult;
	}

	public int meleeBonus() {
		return (int) (0.20f * creditPts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.005f * creditPts());
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.004f * creditPts());
	}

	public int hpBonus() {
		return (int) (1.0f * creditPts());
	}

	public int drMax() {
		return (int) (0.12f * creditPts());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	// ------------------------------------------------------------------ money flow

	/** moves gold from the hero's wallet into the account */
	public void deposit(Hero hero, int amount) {
		amount = Math.min(amount, Dungeon.gold);
		if (amount <= 0) return;
		Dungeon.gold -= amount;
		addBalance(amount);
		credit(amount * (1f + 0.10f * hero.pointsInTalent(Talent.GREED_HOARD)));

		int skin = hero.pointsInTalent(Talent.GREED_GILDED_SKIN);
		if (skin > 0) {
			int shield = Math.min(Math.round(amount * 0.04f * skin), Math.round(hero.HT * 0.15f * skin));
			if (shield > 0) Buff.affect(hero, Barrier.class).setShield(shield);
		}

		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.GOLD);
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
	}

	/** moves gold from the account back to the hero's wallet */
	public void withdraw(Hero hero, int amount) {
		amount = (int) Math.min(amount, balance);
		if (amount <= 0) return;
		balance -= amount;
		Dungeon.gold += amount;
		hero.sprite.showStatusWithIcon(CharSprite.NEUTRAL, Integer.toString(amount), FloatingText.GOLD);
		Sample.INSTANCE.play(Assets.Sounds.GOLD);
		BuffIndicator.refreshHero();
	}

	/** adds lifetime credit, which permanently raises stats */
	public void credit(float amount) {
		if (amount <= 0) return;
		credited = Math.min(MAX_BALANCE, credited + Math.round(amount));
		Hero h = hero();
		if (h != null) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	public void addBalance(long amount) {
		balance = Math.min(MAX_BALANCE, balance + Math.max(0, amount));
		BuffIndicator.refreshHero();
	}

	/** pays a cost out of the balance. Returns false (and spends nothing) if it can't be afforded */
	public boolean spend(int cost) {
		if (cost > balance) return false;
		balance -= cost;
		BuffIndicator.refreshHero();
		return true;
	}

	// ------------------------------------------------------------------ banker interest

	public float interestRate() {
		Hero h = hero();
		return 0.05f + 0.03f * (h == null ? 0 : h.pointsInTalent(Talent.BANKER_RATE));
	}

	private void payInterest(Hero h, float rate) {
		long interest = (long) Math.floor(balance * rate);
		if (interest <= 0) return;
		addBalance(interest);
		int cap = h.pointsInTalent(Talent.BANKER_CAPITALIZE);
		if (cap > 0) credit(interest * 0.25f * cap);
		h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Long.toString(interest), FloatingText.GOLD);
		GLog.p(Messages.get(GreedVault.class, "interest", interest));
	}

	@Override
	public boolean act() {
		Hero h = hero();
		if (h != null && h.isAlive()) {
			if (Dungeon.depth > interestDepth) {
				//interest is paid once per new floor, and only to bankers
				if (h.subClass == HeroSubClass.BANKER && interestDepth > 0) {
					payInterest(h, interestRate());
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
	public String iconTextDisplay() {
		if (balance >= 1_000_000) return (balance / 1_000_000) + "M";
		if (balance >= 10_000) return (balance / 1000) + "K";
		return Long.toString(balance);
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
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.get(GreedVault.class, "desc_money", balance, credited)).append("\n\n");
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

	private static final String BALANCE = "balance";
	private static final String CREDITED = "credited";
	private static final String INTEREST_DEPTH = "interest_depth";
	private static final String DIVIDEND = "dividend_turns";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(BALANCE, balance);
		bundle.put(CREDITED, credited);
		bundle.put(INTEREST_DEPTH, interestDepth);
		bundle.put(DIVIDEND, dividendTurns);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		balance = bundle.getLong(BALANCE);
		credited = bundle.getLong(CREDITED);
		interestDepth = bundle.getInt(INTEREST_DEPTH);
		dividendTurns = bundle.getFloat(DIVIDEND);
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
			long owed = Math.round(d.principal * repayMultiplier(hero));
			v.addBalance(owed);
			d.principal = 0;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Long.toString(owed), FloatingText.GOLD);
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
