package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;

/**
 * The Envy class's covetousness.
 *
 * Every hit robs the enemy a little (Robbed stacks); defeating a robbed enemy turns what was
 * taken into envy points, which raise stats without limit. Defeating special monsters
 * (rare variants, quest monsters and bosses) registers them in the bestiary, permanently
 * granting their trait. Also holds the Mirror of Envy's recharging charges.
 */
public class EnvyCovet extends Buff {

	public float envy = 0;
	public int charges = 2;
	public float partialCharge = 0;
	public float regenTurns = 0;

	/** registered bestiary entries, by trait */
	public final LinkedHashSet<Trait> codex = new LinkedHashSet<>();

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	// ------------------------------------------------------------------ bestiary

	public enum Trait {
		ALBINO, GNOLL_EXILE, HERMIT_CRAB, CAUSTIC_SLIME, BANDIT, SPECTRAL_NECRO, ARMORED_BRUTE,
		DM201, SENIOR, CHAOS_ELEMENTAL, ACIDIC, TORMENTED_SPIRIT, PHANTOM_PIRANHA, GOLDEN_MIMIC,
		FETID_RAT, GNOLL_TRICKSTER, GREAT_CRAB, NEWBORN_ELEMENTAL, ROT_HEART, GNOLL_GEOMANCER,
		CRYSTAL_GUARDIAN, FUNGAL_CORE,
		GOO, TENGU, DM300, DWARF_KING, YOG;

		public String title() {
			return Messages.get(EnvyCovet.class, "trait_" + name().toLowerCase() + "_name");
		}

		public String desc() {
			return Messages.get(EnvyCovet.class, "trait_" + name().toLowerCase());
		}
	}

	public static final LinkedHashMap<Class<? extends Mob>, Trait> CODEX = new LinkedHashMap<>();
	static {
		//rare variants
		CODEX.put(Albino.class,                       Trait.ALBINO);
		CODEX.put(GnollExile.class,                   Trait.GNOLL_EXILE);
		CODEX.put(HermitCrab.class,                   Trait.HERMIT_CRAB);
		CODEX.put(CausticSlime.class,                 Trait.CAUSTIC_SLIME);
		CODEX.put(Bandit.class,                       Trait.BANDIT);
		CODEX.put(SpectralNecromancer.class,          Trait.SPECTRAL_NECRO);
		CODEX.put(ArmoredBrute.class,                 Trait.ARMORED_BRUTE);
		CODEX.put(DM201.class,                        Trait.DM201);
		CODEX.put(Senior.class,                       Trait.SENIOR);
		CODEX.put(Elemental.ChaosElemental.class,     Trait.CHAOS_ELEMENTAL);
		CODEX.put(Acidic.class,                       Trait.ACIDIC);
		CODEX.put(TormentedSpirit.class,              Trait.TORMENTED_SPIRIT);
		CODEX.put(PhantomPiranha.class,               Trait.PHANTOM_PIRANHA);
		CODEX.put(GoldenMimic.class,                  Trait.GOLDEN_MIMIC);
		//quest monsters
		CODEX.put(FetidRat.class,                     Trait.FETID_RAT);
		CODEX.put(GnollTrickster.class,               Trait.GNOLL_TRICKSTER);
		CODEX.put(GreatCrab.class,                    Trait.GREAT_CRAB);
		CODEX.put(Elemental.NewbornFireElemental.class, Trait.NEWBORN_ELEMENTAL);
		CODEX.put(RotHeart.class,                     Trait.ROT_HEART);
		CODEX.put(GnollGeomancer.class,               Trait.GNOLL_GEOMANCER);
		CODEX.put(CrystalGuardian.class,              Trait.CRYSTAL_GUARDIAN);
		CODEX.put(FungalCore.class,                   Trait.FUNGAL_CORE);
		//bosses
		CODEX.put(Goo.class,                          Trait.GOO);
		CODEX.put(Tengu.class,                        Trait.TENGU);
		CODEX.put(DM300.class,                        Trait.DM300);
		CODEX.put(DwarfKing.class,                    Trait.DWARF_KING);
		CODEX.put(YogDzewa.class,                     Trait.YOG);
	}

	public boolean has(Trait t) {
		return codex.contains(t);
	}

	/** trait strength: the Copycat doubles every trait, and Studious Mimicry adds more */
	public float traitMult() {
		Hero h = hero();
		if (h == null || h.subClass != HeroSubClass.COPYCAT) return 1f;
		return 2f + 0.25f * h.pointsInTalent(Talent.COPY_STUDY);
	}

	/** value of a trait if registered, 0 otherwise */
	public float trait(Trait t, float base) {
		return has(t) ? base * traitMult() : 0f;
	}

	// ------------------------------------------------------------------ permanent stats

	public float pts() {
		Hero h = hero();
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.ENVY_JEALOUSY);
		return envy * mult;
	}

	public int meleeBonus() {
		return (int) (0.15f * pts());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, 0.004f * pts()) + trait(Trait.GNOLL_EXILE, 0.10f);
	}

	public float evasionBonus() {
		return Math.min(0.30f, 0.003f * pts())
				+ trait(Trait.SENIOR, 0.10f) + trait(Trait.PHANTOM_PIRANHA, 0.10f) + trait(Trait.TENGU, 0.15f);
	}

	public int hpBonus() {
		Hero h = hero();
		int trophy = h == null ? 0 : h.pointsInTalent(Talent.ENVY_TROPHY);
		return (int) (0.8f * pts())
				+ Math.round(trait(Trait.TORMENTED_SPIRIT, 10f) + trait(Trait.GNOLL_GEOMANCER, 15f))
				+ codex.size() * 2 * trophy;
	}

	/** percentage bonus to max HP from traits, applied after flat bonuses */
	public float hpMult() {
		return 1f + trait(Trait.YOG, 0.20f);
	}

	public int drMax() {
		return (int) (0.10f * pts());
	}

	/** flat armor from traits, always applied */
	public int traitDR() {
		return Math.round(trait(Trait.HERMIT_CRAB, 2f) + trait(Trait.ARMORED_BRUTE, 3f)
				+ trait(Trait.GREAT_CRAB, 4f) + trait(Trait.CRYSTAL_GUARDIAN, 3f) + trait(Trait.DM300, 5f));
	}

	public int drRoll() {
		int max = drMax();
		int r = max <= 0 ? 0 : Random.IntRange(max / 2, max);
		return r + traitDR();
	}

	/** melee damage multiplier from traits */
	public float meleeMult() {
		Hero h = hero();
		float m = 1f + trait(Trait.DM201, 0.10f) + trait(Trait.DWARF_KING, 0.15f) + trait(Trait.YOG, 0.15f);
		if (h != null && h.HP * 2 < h.HT) m += trait(Trait.GOO, 0.25f);
		return m;
	}

	public void addEnvy(float amount) {
		Hero h = hero();
		if (h != null) amount *= 1f + 0.15f * h.pointsInTalent(Talent.ENVY_COVET);
		if (amount <= 0) return;
		int oldHP = hpBonus();
		envy += amount;
		if (h != null && hpBonus() != oldHP) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	/** registers a special monster in the bestiary. Returns true if it was new. */
	public boolean register(Trait t) {
		if (!codex.add(t)) return false;
		Hero h = hero();
		if (h != null) h.updateHT(true);
		BuffIndicator.refreshHero();
		return true;
	}

	// ------------------------------------------------------------------ robbing

	public static int maxRobbed(Hero hero) {
		int base = hero.subClass == HeroSubClass.USURPER ? 10 : 5;
		return base + 2 * hero.pointsInTalent(Talent.ENVY_GRASP);
	}

	public static void rob(Hero hero, Char enemy, int stacks) {
		if (enemy == null || !enemy.isAlive() || enemy == hero) return;
		Robbed r = Buff.affect(enemy, Robbed.class);
		r.stacks = Math.min(maxRobbed(hero), r.stacks + stacks);
		BuffIndicator.refreshHero();
	}

	/** Enemies robbed by Envy are weaker; Petty talent strengthens the effect. */
	public static class Robbed extends Buff {
		public int stacks = 0;

		{
			type = buffType.NEGATIVE;
		}

		public float damageMult() {
			Hero h = Dungeon.hero;
			float per = 0.05f + 0.02f * (h == null ? 0 : h.pointsInTalent(Talent.ENVY_PETTY));
			return Math.max(0.25f, 1f - per * stacks);
		}

		@Override
		public int icon() {
			return BuffIndicator.WEAKNESS;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.3f, 0.9f, 0.3f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(stacks);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", stacks, Math.round((1f - damageMult()) * 100));
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("stacks", stacks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			stacks = bundle.getInt("stacks");
		}
	}

	/** Snatch: the hero is empowered by what was stolen. */
	public static class Stolen extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.3f, 0.9f, 0.3f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 16f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	// ------------------------------------------------------------------ charges

	public int maxCharges() {
		Hero h = hero();
		return 3 + (h == null ? 0 : h.pointsInTalent(Talent.ENVY_MIRROR));
	}

	public float turnsPerCharge() {
		Hero h = hero();
		return 30f / (1f + 0.2f * (h == null ? 0 : h.pointsInTalent(Talent.ENVY_POLISH)));
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
			float regen = trait(Trait.ROT_HEART, 1f) + trait(Trait.FUNGAL_CORE, 1f);
			if (regen > 0 && h.HP < h.HT) {
				regenTurns += TICK * regen;
				while (regenTurns >= 10f) {
					regenTurns -= 10f;
					if (h.HP < h.HT) h.HP++;
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
		icon.hardlight(0.3f, 0.9f, 0.3f);
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
		return Messages.get(EnvyCovet.class, "desc_head", Math.round(envy), charges, maxCharges(), codex.size(), CODEX.size())
				+ "\n\n" + Messages.get(EnvyCovet.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100));
	}

	public String codexText() {
		if (codex.isEmpty()) return Messages.get(EnvyCovet.class, "codex_empty");
		StringBuilder sb = new StringBuilder();
		for (Trait t : codex) {
			if (sb.length() > 0) sb.append("\n\n");
			sb.append("_").append(t.title()).append("_: ").append(t.desc());
		}
		return sb.toString();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("envy", envy);
		bundle.put("charges", charges);
		bundle.put("partial", partialCharge);
		bundle.put("regen", regenTurns);
		ArrayList<String> names = new ArrayList<>();
		for (Trait t : codex) names.add(t.name());
		bundle.put("codex", names.toArray(new String[0]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		envy = bundle.getFloat("envy");
		charges = bundle.getInt("charges");
		partialCharge = bundle.getFloat("partial");
		regenTurns = bundle.getFloat("regen");
		codex.clear();
		if (bundle.contains("codex")) {
			for (String n : bundle.getStringArray("codex")) {
				try {
					codex.add(Trait.valueOf(n));
				} catch (IllegalArgumentException e) {
					//trait removed in a later version
				}
			}
		}
	}

	public static EnvyCovet of() {
		return Dungeon.hero == null ? null : Dungeon.hero.buff(EnvyCovet.class);
	}
}
