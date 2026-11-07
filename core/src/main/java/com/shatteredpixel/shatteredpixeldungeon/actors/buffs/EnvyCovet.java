package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.envy.EnvyMimicry;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.UsurpersGauntlets;
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
 * granting their trait (and teaching their abilities to the mirror, see envy/EnvyMimicry).
 * Also holds the jealousy gauge that pays for mimicked skills, the mimic slots and the
 * imitated growth.
 */
public class EnvyCovet extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	public float envy = 0;
	/** jealousy gauge: fills slowly, faster with rivals in sight; pays for mimicked skills */
	public float jealousy = 0;
	public static final float MAX_JEALOUSY = 100f;
	/** skills currently held by the mirror */
	public final ArrayList<EnvyMimicry.Mimic> slots = new ArrayList<>();
	/** the other sin whose growth is being imitated */
	public EnvyMimicry.Growth growth = EnvyMimicry.Growth.NONE;
	/** the Doppelganger's second imitated growth */
	public EnvyMimicry.Growth growth2 = EnvyMimicry.Growth.NONE;
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
		float m = 1f;
		if (h != null && h.subClass == HeroSubClass.COPYCAT) m = 2f + 0.25f * h.pointsInTalent(Talent.COPY_STUDY);
		return mirrored ? m * 1.25f : m;
	}

	/** value of a trait if registered, 0 otherwise */
	public float trait(Trait t, float base) {
		return has(t) ? base * traitMult() : 0f;
	}

	// ------------------------------------------------------------------ permanent stats

	public float pts() {
		Hero h = hero();
		float mult = h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.ENVY_JEALOUSY);
		float p = envy;
		//imitating greed: the gold being carried counts, like it does for Greed
		if (imitating(EnvyMimicry.Growth.GREED)) {
			p += Dungeon.gold / 100f * efficiency(EnvyMimicry.Growth.GREED);
		}
		return p * mult;
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

	// ------------------------------------------------------------------ imitated growth

	public String growthTitle() {
		if (doppelganger() && growth2 != EnvyMimicry.Growth.NONE) return growth.title() + " + " + growth2.title();
		return growth.title();
	}

	public boolean doppelganger() {
		Hero h = hero();
		return h != null && h.subClass == HeroSubClass.USURPER;
	}

	/** true if this sin's growth is being imitated (the Doppelganger imitates two) */
	public boolean imitating(EnvyMimicry.Growth g) {
		return g != EnvyMimicry.Growth.NONE && (growth == g || (doppelganger() && growth2 == g));
	}

	/** efficiency of imitating this sin's growth (the Doppelganger's second one is weaker) */
	public float efficiency(EnvyMimicry.Growth g) {
		Hero h = hero();
		if (h == null) return 0;
		float eff = EnvyMimicry.growthEfficiency(h);
		if (growth == g) return eff;
		if (doppelganger() && growth2 == g) return secondEfficiency();
		return 0;
	}

	public float secondEfficiency() {
		Hero h = hero();
		if (h == null) return 0;
		return EnvyMimicry.growthEfficiency(h) * (0.5f + 0.15f * h.pointsInTalent(Talent.USURP_TYRANNY));
	}

	/** old robbed stacks (from before the Envy rework), kept only so old saves load; they fade at once */
	public static class Robbed extends Buff {
		public int stacks = 0;

		{
			type = buffType.NEGATIVE;
		}

		@Override
		public boolean act() {
			detach();
			return true;
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

	/** Petty talent: the next melee hit after using a copied skill is stronger */
	public static class Spite extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		public static float mult(Hero hero) {
			return 1f + 0.15f * hero.pointsInTalent(Talent.ENVY_PETTY);
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
			return Math.max(0, 1f - visualcooldown() / 10f);
		}

		@Override
		public String desc() {
			Hero h = Dungeon.hero;
			return Messages.get(this, "desc", h == null ? 0 : Math.round((mult(h) - 1f) * 100), dispTurns());
		}
	}

	/** mimicked Step: much harder to hit */
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
			icon.hardlight(0.4f, 1f, 0.5f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, 1f - visualcooldown() / 6f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	/** old Snatch buff (kept for saves) */
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

	// ------------------------------------------------------------------ jealousy & mimicry

	public int maxSlots() {
		Hero h = hero();
		if (h == null) return 3;
		return 3 + h.pointsInTalent(Talent.ENVY_MIRROR) + (h.subClass == HeroSubClass.COPYCAT ? 1 : 0)
				+ (mirrored ? 1 : 0);
	}

	/** a rival worth envying: a boss, a bestiary monster or anything sturdier than half the hero */
	private static boolean rival(Hero h, Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS)
				|| CODEX.containsKey(ch.getClass()) || ch.HT >= h.HT / 2;
	}

	/** jealousy gained per turn right now */
	public float jealousyRate() {
		Hero h = hero();
		if (h == null) return 0;
		float rate = (MAX_JEALOUSY / 300f) / SinArtifact.rechargeScale(h);
		rate *= 1f + 0.2f * h.pointsInTalent(Talent.ENVY_POLISH);
		if (mirrored) rate *= 1.25f;
		if (SinWeapons.wields(h, UsurpersGauntlets.class, HeroClass.ENVY)) rate *= 1.5f;
		boolean rivals = false;
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos] && rival(h, ch)) {
				rivals = true;
				break;
			}
		}
		if (rivals) rate *= 2f + 0.5f * h.pointsInTalent(Talent.ENVY_SNATCH);
		return rate;
	}

	public void addJealousy(float amount) {
		if (amount <= 0) return;
		jealousy = Math.min(MAX_JEALOUSY, jealousy + amount);
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
	}

	public boolean useJealousy(int n) {
		if (jealousy < n) return false;
		jealousy -= n;
		SinArtifact.onSpent(hero(), Math.max(1, n / 10));
		Item.updateQuickslot();
		BuffIndicator.refreshHero();
		return true;
	}

	/** drops slots that are no longer valid and fills empty ones with newly learned skills */
	public void refreshSlots() {
		Hero h = hero();
		if (h == null) return;
		while (slots.size() > maxSlots()) slots.remove(slots.size() - 1);
		for (EnvyMimicry.Mimic m : EnvyMimicry.Mimic.values()) {
			if (slots.size() >= maxSlots()) break;
			if (!slots.contains(m) && m.unlocked(h, this) && !known.contains(m)) {
				slots.add(m);
			}
		}
		for (EnvyMimicry.Mimic m : EnvyMimicry.Mimic.values()) {
			if (m.unlocked(h, this) && known.add(m)) {
				GLog.p(Messages.get(EnvyMimicry.class, "learned", m.title()));
			}
		}
	}

	/** mimicries the hero has already been told about (so newly learned ones fill free slots once) */
	public final java.util.HashSet<EnvyMimicry.Mimic> known = new java.util.HashSet<>();

	@Override
	public boolean act() {
		Hero h = hero();
		if (h != null && h.isAlive()) {
			if (jealousy < MAX_JEALOUSY) addJealousy(jealousyRate() * TICK);
			if (Random.Int(10) == 0) refreshSlots();
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
		return 1f - jealousy / MAX_JEALOUSY;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString((int) jealousy);
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
		return Messages.get(EnvyCovet.class, "desc_head", Math.round(envy), (int) jealousy, Math.round(MAX_JEALOUSY), growthTitle(), codex.size(), CODEX.size())
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
		bundle.put("jealousy", jealousy);
		bundle.put("growth", growth.name());
		bundle.put("growth2", growth2.name());
		ArrayList<String> sl = new ArrayList<>();
		for (EnvyMimicry.Mimic m : slots) sl.add(m.name());
		bundle.put("slots", sl.toArray(new String[0]));
		ArrayList<String> kn = new ArrayList<>();
		for (EnvyMimicry.Mimic m : known) kn.add(m.name());
		bundle.put("known", kn.toArray(new String[0]));
		bundle.put("mirrored", mirrored);
		bundle.put("regen", regenTurns);
		ArrayList<String> names = new ArrayList<>();
		for (Trait t : codex) names.add(t.name());
		bundle.put("codex", names.toArray(new String[0]));
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		envy = bundle.getFloat("envy");
		jealousy = bundle.getFloat("jealousy");
		try {
			growth = bundle.contains("growth") ? EnvyMimicry.Growth.valueOf(bundle.getString("growth")) : EnvyMimicry.Growth.NONE;
		} catch (IllegalArgumentException e) {
			growth = EnvyMimicry.Growth.NONE;
		}
		try {
			growth2 = bundle.contains("growth2") ? EnvyMimicry.Growth.valueOf(bundle.getString("growth2")) : EnvyMimicry.Growth.NONE;
		} catch (IllegalArgumentException e) {
			growth2 = EnvyMimicry.Growth.NONE;
		}
		slots.clear();
		known.clear();
		if (bundle.contains("slots")) {
			for (String n : bundle.getStringArray("slots")) {
				try { slots.add(EnvyMimicry.Mimic.valueOf(n)); } catch (IllegalArgumentException ignored) {}
			}
		}
		if (bundle.contains("known")) {
			for (String n : bundle.getStringArray("known")) {
				try { known.add(EnvyMimicry.Mimic.valueOf(n)); } catch (IllegalArgumentException ignored) {}
			}
		}
		mirrored = bundle.getBoolean("mirrored");
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
