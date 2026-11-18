package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonSpells;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Seven Sins: Solomon's arcana (Nov 17 2026, owner: a demon-summoning mage who combines alchemical symbols).
 * Holds mana (spent by spells and summons), the symbols currently drawn in the circle and wisdom, the
 * permanent, unbounded growth: gained from the mana spent and from every demon recorded in the Lemegeton.
 */
public class SolomonArcana extends Buff {

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	public float wisdom = 0;
	public float mana = 0;
	private float regen = 0;
	/** symbols drawn in the circle (oldest first), at most MAX_SYMBOLS */
	public final ArrayList<SolomonSpells.Element> circle = new ArrayList<>();
	public static final int MAX_SYMBOLS = 3;
	/** the principles learned at levels 10 and 20 (solomon/SolomonPrinciples) */
	public final ArrayList<SolomonSpells.Element> principles = new ArrayList<>();
	/** how many principle choices have been announced */
	public int toldPrinciples = 0;

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	public float maxMana() {
		Hero h = hero();
		int lvl = h == null ? 1 : h.lvl;
		int insight = h == null ? 0 : h.pointsInTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SOL_INSIGHT);
		return 15 + lvl + wisdom * 0.2f + 3 * SinArtifact.bonusCharges(h) + 3 * insight;
	}

	public float turnsPerMana() {
		Hero h = hero();
		float t = 2f * SinArtifact.rechargeScale(h);
		//the Arch Magus draws mana 25% faster
		if (h != null && h.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.ARCH_MAGUS) t /= 1.25f;
		if (h != null) t /= 1f + 0.15f * h.pointsInTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.SOL_FOCUS);
		return t;
	}

	/** spell damage / healing / duration multiplier: +2% per wisdom, unbounded */
	public float spellPower() {
		Hero h = hero();
		float p = 1f + 0.02f * wisdom;
		//the Arch Magus's spells are 25% stronger
		if (h != null && h.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.ARCH_MAGUS) p *= 1.25f;
		if (h != null) p *= 1f + 0.08f * h.pointsInTalent(com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent.MAGUS_OVERLOAD);
		return p;
	}

	public int hpBonus() {
		return Math.round(wisdom * 0.15f);
	}

	/** evasion multiplier, capped at +30% like the sins */
	public float evasionMult() {
		return 1f + Math.min(0.30f, wisdom * 0.003f);
	}

	public void addWisdom(float amount) {
		if (amount <= 0) return;
		wisdom += amount;
		Hero h = hero();
		if (h != null) h.updateHT(false);
		BuffIndicator.refreshHero();
	}

	public boolean useMana(int n) {
		if (mana < n) return false;
		mana -= n;
		addWisdom(n * 0.25f);
		SinArtifact.onSpent(hero(), n / 3f);
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
		return true;
	}

	public void addMana(float n) {
		mana = Math.min(maxMana(), mana + n);
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
	}

	public void draw(SolomonSpells.Element e) {
		circle.add(e);
		while (circle.size() > MAX_SYMBOLS) circle.remove(0);
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
	}

	@Override
	public boolean act() {
		Hero h = hero();
		if (h != null && h.isAlive() && toldPrinciples < com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonPrinciples.allowed(h)
				&& principles.size() < com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonPrinciples.allowed(h)) {
			toldPrinciples = com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonPrinciples.allowed(h);
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(Messages.get(com.shatteredpixel.shatteredpixeldungeon.solomon.SolomonPrinciples.class, "can_learn"));
			com.watabou.noosa.Game.runOnRenderThread(new com.watabou.utils.Callback() {
				@Override
				public void call() {
					com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.syncSkillBarPos();
				}
			});
		}
		if (h != null && h.isAlive() && mana < maxMana()) {
			regen += TICK;
			float per = turnsPerMana();
			while (regen >= per) {
				regen -= per;
				mana = Math.min(maxMana(), mana + 1);
			}
			Item.updateQuickslot();
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.RECHARGING;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.7f, 0.4f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, 1f - mana / Math.max(1f, maxMana()));
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) mana);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", (int) mana, Math.round(maxMana()), SolomonSpells.circleText(this),
				Math.round(wisdom), Math.round((spellPower() - 1f) * 100), hpBonus(), Math.round((evasionMult() - 1f) * 100));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("wisdom", wisdom);
		bundle.put("mana", mana);
		bundle.put("regen", regen);
		ArrayList<String> c = new ArrayList<>();
		for (SolomonSpells.Element e : circle) c.add(e.name());
		bundle.put("circle", c.toArray(new String[0]));
		ArrayList<String> pr = new ArrayList<>();
		for (SolomonSpells.Element e : principles) pr.add(e.name());
		bundle.put("principles", pr.toArray(new String[0]));
		bundle.put("told_principles", toldPrinciples);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		wisdom = bundle.getFloat("wisdom");
		mana = bundle.getFloat("mana");
		regen = bundle.getFloat("regen");
		circle.clear();
		if (bundle.contains("circle")) {
			for (String n : bundle.getStringArray("circle")) {
				try { circle.add(SolomonSpells.Element.valueOf(n)); } catch (IllegalArgumentException ignored) {}
			}
		}
		principles.clear();
		if (bundle.contains("principles")) {
			for (String n : bundle.getStringArray("principles")) {
				try { principles.add(SolomonSpells.Element.valueOf(n)); } catch (IllegalArgumentException ignored) {}
			}
		}
		toldPrinciples = bundle.getInt("told_principles");
	}
}
