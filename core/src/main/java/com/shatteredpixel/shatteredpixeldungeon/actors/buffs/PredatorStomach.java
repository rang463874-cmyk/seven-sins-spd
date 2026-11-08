package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.Brew;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.Elixir;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.Trinket;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.GluttonScythe;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.SinWeapons;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Permanent stat storage for the Gluttony class. Points are earned by using consumables
 * (potions, food and seeds, scrolls and runestones), and new kinds fill the gourmet codex,
 * and converted into combat bonuses through the hooks in PredatorHooks.
 */
public class PredatorStomach extends Buff {

	/** set once the Imp quest's vault mirror reflection of the signature item is taken */
	public boolean mirrored = false;

	/** accumulated points per category */
	public float weaponPts = 0;
	public float armorPts = 0;
	public float trinketPts = 0;
	public int eaten = 0;
	/** permanent max HP from the Gourmand "Leftovers" talent */
	public int leftoverHP = 0;

	/** consumables tasted so far (class names), for the gourmet codex */
	public final java.util.HashSet<String> codex = new java.util.HashSet<>();
	/** last potion or scroll used, for Rumination */
	public Class lastUsed = null;

	/** old Inheritor saves: enchantments / glyphs kept from devoured gear (no longer used) */
	public final ArrayList<Class> enchants = new ArrayList<Class>();
	public final ArrayList<Class> glyphs = new ArrayList<Class>();

	{
		type = buffType.POSITIVE;
		revivePersists = true;
	}

	// ------------------------------------------------------------------ effects

	private float apex() {
		Hero h = hero();
		return h == null ? 1f : 1f + 0.10f * h.pointsInTalent(Talent.PREDATOR_APEX);
	}

	private Hero hero() {
		return target instanceof Hero ? (Hero) target : null;
	}

	public int meleeBonus() {
		return (int) ((0.075f * weaponPts + 0.025f * trinketPts) * apex());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, (0.005f * weaponPts + 0.002f * trinketPts) * apex());
	}

	public float evasionBonus() {
		return Math.min(0.30f, (0.003f * armorPts + 0.006f * trinketPts) * apex());
	}

	public int hpBonus() {
		return (int) ((0.25f * armorPts + 0.075f * trinketPts) * apex()) + leftoverHP;
	}

	public int drMax() {
		return (int) ((0.075f * armorPts + 0.025f * trinketPts) * apex());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	// ------------------------------------------------------------------ consuming

	/** potions (and elixirs) feed the weapon pool: melee damage and accuracy */
	public static final int CAT_POTION = 0;
	/** food and seeds feed the armor pool: max HP and armor */
	public static final int CAT_FOOD = 1;
	/** scrolls and runestones feed the trinket pool: a bit of everything, mostly evasion */
	public static final int CAT_SCROLL = 2;

	/** codex milestones: number of different consumables tasted to unlock each perk */
	public static final int CODEX_HEAL     = 5;
	public static final int CODEX_SEEDS    = 10;
	public static final int CODEX_LINGER   = 20;
	public static final int CODEX_PRESERVE = 30;
	public static final int CODEX_FRUGAL   = 40;
	public static final int CODEX_FEAST    = 50;
	public static final int CODEX_ECHO     = 60;
	public static final int[] MILESTONES = {CODEX_HEAL, CODEX_SEEDS, CODEX_LINGER, CODEX_PRESERVE,
			CODEX_FRUGAL, CODEX_FEAST, CODEX_ECHO};

	/** points for the first taste of a new kind of consumable */
	public static final float CODEX_BONUS = 5f;

	public boolean codex(int milestone) {
		return codex.size() >= milestone;
	}

	public static boolean hasCodex(Hero hero, int milestone) {
		PredatorStomach st = hero == null ? null : hero.buff(PredatorStomach.class);
		return st != null && st.codex(milestone);
	}

	/** Category of a consumable, or -1 if it doesn't feed the stomach. */
	public static int category(Item item) {
		if (item instanceof Potion) return CAT_POTION;
		if (item instanceof Food || item instanceof Plant.Seed) return CAT_FOOD;
		if (item instanceof Scroll || item instanceof Runestone) return CAT_SCROLL;
		return -1;
	}

	/** Raw points an item is worth before multipliers. */
	public static float baseScore(Item item) {
		if (item instanceof Food) return Math.max(1f, ((Food) item).energy / 100f);
		if (item instanceof Plant.Seed) return 1f;
		if (item instanceof Runestone) return 1f;
		if (item instanceof ExoticPotion || item instanceof Elixir || item instanceof Brew) return 4f;
		if (item instanceof ExoticScroll) return 4f;
		return 2f;
	}

	/** Final points for an item, including talents and subclass bonuses. */
	public static float score(Hero hero, Item item, boolean swallowed) {
		int cat = category(item);
		float s = baseScore(item);
		float mult = 1f + 0.10f * hero.pointsInTalent(Talent.PREDATOR_EFFICIENCY);
		if (hero.subClass == HeroSubClass.GOURMAND) {
			mult += 0.10f * hero.pointsInTalent(Talent.GOURMAND_SECOND_STOMACH);
			if (cat == CAT_FOOD) mult += 1f;
		} else if (hero.subClass == HeroSubClass.INHERITOR) {
			if (cat == CAT_POTION || cat == CAT_SCROLL) mult += 0.5f;
		}
		if (SinWeapons.wields(hero, GluttonScythe.class, com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.GLUTTONY)) mult *= 1.5f;
		PredatorStomach st = hero.buff(PredatorStomach.class);
		if (st != null && st.mirrored) mult *= 1.25f;
		mult *= SinArtifact.devourScale(hero);
		if (swallowed) mult *= 3f + 1.5f * hero.pointsInTalent(Talent.PREDATOR_SCRAPS);
		return s * mult;
	}

	public void addPoints(int cat, float pts) {
		if (cat == CAT_POTION) weaponPts += pts;
		else if (cat == CAT_FOOD) armorPts += pts;
		else trinketPts += pts;
		Hero h = hero();
		if (h != null) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	/**
	 * Called whenever the glutton uses (or swallows) a consumable. Adds points, registers it in
	 * the codex and runs the talents. Returns the points gained.
	 */
	public float consume(Hero hero, Item item, boolean swallowed) {
		int cat = category(item);
		if (cat < 0) return 0;

		//the glutton knows what it swallowed by taste
		if (swallowed && !item.isIdentified()) item.identify();
		float pts = score(hero, item, swallowed);
		String key = item.getClass().getName();
		boolean fresh = codex.add(key);
		if (fresh) pts += CODEX_BONUS;
		addPoints(cat, pts);

		// Adaptive digestion: some points also flow into the other two pools
		int adapt = hero.pointsInTalent(Talent.PREDATOR_ADAPT);
		if (adapt > 0) {
			for (int c = 0; c < 3; c++) if (c != cat) addPoints(c, pts * 0.05f * adapt);
		}

		if (!swallowed && (item instanceof Potion || item instanceof Scroll)) {
			lastUsed = item.getClass();
		}

		eaten++;
		SinArtifact.onSpent(hero, 1);
		Item.updateQuickslot();

		String catName = Messages.get(PredatorStomach.class, "cat_" + cat);
		hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(PredatorStomach.class, "gain_status", catName, fmt(pts)));
		if (fresh) {
			GLog.p(Messages.get(PredatorStomach.class, "codex_new", item.trueName(), codex.size()));
			for (int m : MILESTONES) {
				if (codex.size() == m) {
					GLog.h(Messages.get(PredatorStomach.class, "codex_unlock", Messages.get(PredatorStomach.class, "codex_" + m)));
					hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 8);
				}
			}
		}

		afterConsume(hero, item, cat);
		return pts;
	}

	private void afterConsume(Hero hero, Item item, int cat) {
		// codex: a little health back from everything
		if (codex(CODEX_HEAL)) heal(hero, Math.round(hero.HT * 0.03f));
		// codex: every meal is a feast
		if (codex(CODEX_FEAST)) {
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(30f);
			Buff.affect(hero, Barrier.class).incShield(Math.max(1, Math.round(hero.HT * 0.05f)));
		}

		// tier 1
		int feast = hero.pointsInTalent(Talent.PREDATOR_FEAST);
		if (feast > 0 && cat == CAT_FOOD) heal(hero, Math.round(hero.HT * (feast == 1 ? 0.10f : 0.20f)));
		int taste = hero.pointsInTalent(Talent.PREDATOR_TASTE);
		if (taste > 0) {
			for (Item it : hero.belongings.backpack.items) {
				if (it instanceof EquipableItem && (!it.isIdentified())) {
					if (taste >= 2) {
						it.identify();
					} else if (!it.cursedKnown) {
						it.cursedKnown = true;
					} else {
						continue;
					}
					GLog.i(Messages.get(PredatorStomach.class, "taste", it.name()));
					break;
				}
			}
		}
		//hungry fang: every consumable also fills the belly a little
		int fang = hero.pointsInTalent(Talent.PREDATOR_FANG);
		if (fang > 0) {
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(FANG_CONSUME * fang);
		}
		int hide = hero.pointsInTalent(Talent.PREDATOR_HIDE);
		if (hide > 0) {
			Buff.affect(hero, Barrier.class).incShield(Math.max(1, Math.round(hero.HT * (hide == 1 ? 0.04f : 0.08f))));
		}

		// gourmand: food makes you sturdier
		if (hero.subClass == HeroSubClass.GOURMAND && cat == CAT_FOOD) {
			int lo = hero.pointsInTalent(Talent.GOURMAND_LEFTOVERS);
			if (lo > 0) {
				leftoverHP += lo;
				hero.updateHT(true);
			}
			if (hero.pointsInTalent(Talent.GOURMAND_FOOD_COMA) > 0) Buff.affect(hero, Coma.class);
		}

		// alchemic glutton: potions resonate, scrolls cleanse
		if (hero.subClass == HeroSubClass.INHERITOR) {
			int res = hero.pointsInTalent(Talent.INHERITOR_RESONANCE);
			if (item instanceof Potion && res > 0 && Random.Float() < 0.15f * res) {
				switch (Random.Int(3)) {
					case 0: Buff.prolong(hero, Haste.class, 5f); break;
					case 1: Buff.prolong(hero, Bless.class, 10f); break;
					default: Buff.affect(hero, Barrier.class).incShield(Math.round(hero.HT * 0.10f)); break;
				}
				hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
			}
			int purify = hero.pointsInTalent(Talent.INHERITOR_PURIFY);
			if (item instanceof Scroll && purify > 0) {
				int cleansed = 0;
				for (Buff b : hero.buffs()) {
					if (b.type == buffType.NEGATIVE && !(b instanceof AllyBuff) && !(b instanceof Hunger)
							&& !(b instanceof LostInventory) && (purify >= 2 || cleansed == 0)) {
						b.detach();
						cleansed++;
					}
				}
				if (purify >= 3) Buff.affect(hero, Barrier.class).incShield(Math.round(hero.HT * 0.10f));
			}
		}

		hero.sprite.emitter().burst(Speck.factory(Speck.BONE), 3);
	}

	private static void heal(Hero hero, int amount) {
		amount = Math.min(amount, hero.HT - hero.HP);
		if (amount <= 0) return;
		hero.HP += amount;
		hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
	}

	public static String fmt(float f) {
		return String.format(java.util.Locale.ENGLISH, "%.1f", f);
	}

	// ------------------------------------------------------------------ acid stomach talent

	private static final Class[] ACIDS = {Poison.class, Ooze.class, Corrosion.class};

	@Override
	public java.util.HashSet<Class> resistances() {
		java.util.HashSet<Class> r = super.resistances();
		Hero h = hero();
		if (h != null && h.pointsInTalent(Talent.PREDATOR_CURSE_EATER) == 1) r.addAll(java.util.Arrays.asList(ACIDS));
		return r;
	}

	@Override
	public java.util.HashSet<Class> immunities() {
		java.util.HashSet<Class> r = super.immunities();
		Hero h = hero();
		if (h != null && h.pointsInTalent(Talent.PREDATOR_CURSE_EATER) >= 2) r.addAll(java.util.Arrays.asList(ACIDS));
		return r;
	}

	// ------------------------------------------------------------------ buff ui

	@Override
	public boolean act() {
		diactivate();
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.FURY;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.75f, 0.2f, 0.9f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(eaten);
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
		sb.append(Messages.get(PredatorStomach.class, "desc_head", eaten)).append("\n\n");
		sb.append(Messages.get(PredatorStomach.class, "desc_points", fmt(weaponPts), fmt(armorPts), fmt(trinketPts))).append("\n\n");
		sb.append(Messages.get(PredatorStomach.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100)));
		sb.append("\n\n").append(Messages.get(PredatorStomach.class, "desc_codex", codex.size()));
		for (int m : MILESTONES) {
			sb.append("\n").append(codex(m) ? "_" : "").append(m).append(": ")
					.append(Messages.get(PredatorStomach.class, "codex_" + m)).append(codex(m) ? "_" : "");
		}
		return sb.toString();
	}

	private static final String WEAPON = "weapon_pts";
	private static final String ARMOR = "armor_pts";
	private static final String TRINKET = "trinket_pts";
	private static final String EATEN = "eaten";
	private static final String LEFTOVER = "leftover_hp";
	private static final String ENCHANTS = "inherit_enchants";
	private static final String GLYPHS = "inherit_glyphs";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(WEAPON, weaponPts);
		bundle.put(ARMOR, armorPts);
		bundle.put(TRINKET, trinketPts);
		bundle.put(EATEN, eaten);
		bundle.put(LEFTOVER, leftoverHP);
		bundle.put(ENCHANTS, enchants.toArray(new Class[0]));
		bundle.put(GLYPHS, glyphs.toArray(new Class[0]));
		bundle.put("mirrored", mirrored);
		bundle.put("codex", codex.toArray(new String[0]));
		if (lastUsed != null) bundle.put("last_used", lastUsed);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		weaponPts = bundle.getFloat(WEAPON);
		armorPts = bundle.getFloat(ARMOR);
		trinketPts = bundle.getFloat(TRINKET);
		eaten = bundle.getInt(EATEN);
		leftoverHP = bundle.getInt(LEFTOVER);
		mirrored = bundle.getBoolean("mirrored");
		codex.clear();
		if (bundle.contains("codex")) {
			for (String c : bundle.getStringArray("codex")) if (c != null) codex.add(c);
		}
		lastUsed = bundle.contains("last_used") ? bundle.getClass("last_used") : null;
		enchants.clear();
		glyphs.clear();
		if (bundle.contains(ENCHANTS)) {
			for (Class c : bundle.getClassArray(ENCHANTS)) if (c != null) enchants.add(c);
		}
		if (bundle.contains(GLYPHS)) {
			for (Class c : bundle.getClassArray(GLYPHS)) if (c != null) glyphs.add(c);
		}
	}

	// ------------------------------------------------------------------ helper buffs

	/** Hungry Fang talent: next few melee hits deal bonus damage. */
	/** hungry fang: satiety per point for every consumable, and for every kill (PredatorHooks.onMobDeath) */
	public static final int FANG_CONSUME = 15;
	public static final int FANG_KILL = 10;

	/** old hungry fang buff, kept for saves (no longer applied) */
	public static class Fang extends Buff {
		public int hits = 3;

		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.9f, 0.3f, 0.3f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(hits);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", hits);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("hits", hits);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			hits = bundle.getInt("hits");
		}
	}

	/** Gourmand Food Coma talent: the next hit taken is reduced. */
	public static class Coma extends Buff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.6f, 0.4f, 0.9f);
		}

		@Override
		public String desc() {
			Hero h = Dungeon.hero;
			int pts = h == null ? 1 : h.pointsInTalent(Talent.GOURMAND_FOOD_COMA);
			return Messages.get(this, "desc", comaPercent(pts));
		}

		public static int comaPercent(int pts) {
			return pts >= 3 ? 50 : pts == 2 ? 35 : 20;
		}
	}
}
