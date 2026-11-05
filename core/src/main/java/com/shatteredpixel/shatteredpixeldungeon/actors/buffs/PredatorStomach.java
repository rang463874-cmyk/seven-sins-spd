package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
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
 * Permanent stat storage for the Predator class. Points are earned by devouring equipment
 * and converted into combat bonuses through the hooks in PredatorHooks.
 */
public class PredatorStomach extends Buff {

	public static final int CAT_WEAPON = 0;
	public static final int CAT_ARMOR = 1;
	public static final int CAT_TRINKET = 2;

	/** accumulated points per category */
	public float weaponPts = 0;
	public float armorPts = 0;
	public float trinketPts = 0;
	public int eaten = 0;
	/** permanent max HP from the Gourmand "Leftovers" talent */
	public int leftoverHP = 0;

	/** Inheritor: enchantments / glyphs kept from devoured gear (most recent last) */
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
		return (int) ((0.15f * weaponPts + 0.05f * trinketPts) * apex());
	}

	public float accuracyBonus() {
		return Math.min(0.40f, (0.005f * weaponPts + 0.002f * trinketPts) * apex());
	}

	public float evasionBonus() {
		return Math.min(0.30f, (0.003f * armorPts + 0.006f * trinketPts) * apex());
	}

	public int hpBonus() {
		return (int) ((1.0f * armorPts + 0.3f * trinketPts) * apex()) + leftoverHP;
	}

	public int drMax() {
		return (int) ((0.15f * armorPts + 0.05f * trinketPts) * apex());
	}

	public int drRoll() {
		int max = drMax();
		return max <= 0 ? 0 : Random.IntRange(max / 2, max);
	}

	public int maxInherit() {
		Hero h = hero();
		return 1 + (h == null ? 0 : h.pointsInTalent(Talent.INHERITOR_MEMORY));
	}

	// ------------------------------------------------------------------ devouring

	/** Category of an item, or -1 if it cannot be devoured. */
	public static int category(Hero hero, Item item) {
		if (item == null || item.unique) return -1;
		if (item instanceof EquipableItem && item.isEquipped(hero)) return -1;
		if (item instanceof MeleeWeapon) return CAT_WEAPON;
		if (item instanceof MissileWeapon) return hero.pointsInTalent(Talent.PREDATOR_SCRAPS) >= 1 ? CAT_WEAPON : -1;
		if (item instanceof ClassArmor) return -1;
		if (item instanceof Armor) return CAT_ARMOR;
		if (item instanceof Ring || item instanceof Artifact || item instanceof Trinket) return CAT_TRINKET;
		if (item instanceof Wand) return hero.pointsInTalent(Talent.PREDATOR_SCRAPS) >= 2 ? CAT_TRINKET : -1;
		return -1;
	}

	/** Raw points an item is worth before multipliers. */
	public static float baseScore(Item item) {
		int lvl = Math.max(0, item.level());
		float s;
		if (item instanceof MeleeWeapon) {
			s = ((MeleeWeapon) item).tier + 2 * lvl;
			if (((Weapon) item).hasGoodEnchant()) s += 1;
		} else if (item instanceof MissileWeapon) {
			s = Math.min(2f * ((MissileWeapon) item).tier, 0.4f * ((MissileWeapon) item).tier * item.quantity()) + lvl;
		} else if (item instanceof Armor) {
			Armor a = (Armor) item;
			s = a.tier + 2 * lvl;
			if (a.glyph != null && !a.glyph.curse()) s += 1;
		} else if (item instanceof Artifact) {
			int cap = Math.max(1, ((Artifact) item).levelCap());
			s = 3 + Math.round(6f * lvl / cap);
		} else if (item instanceof Trinket) {
			s = 3 + 2 * lvl;
		} else if (item instanceof Wand) {
			s = 2 + lvl;
		} else {
			s = 3 + 2 * lvl;
		}
		return Math.max(1, s);
	}

	/** Final points for an item, including curse handling and talents. */
	public static float score(Hero hero, Item item) {
		float s = baseScore(item);
		if (item.cursed) {
			int ce = hero.pointsInTalent(Talent.PREDATOR_CURSE_EATER);
			if (ce == 0) s *= 0.5f;
			else if (ce >= 2) s *= 1.5f;
			if (hero.pointsInTalent(Talent.INHERITOR_PURIFY) >= 3) s *= 1.5f;
		}
		float mult = 1f + 0.10f * hero.pointsInTalent(Talent.PREDATOR_EFFICIENCY);
		if (hero.subClass == HeroSubClass.GOURMAND) {
			mult += 0.30f + 0.10f * hero.pointsInTalent(Talent.GOURMAND_SECOND_STOMACH);
		}
		if (SinWeapons.wields(hero, GluttonScythe.class, com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass.GLUTTONY)) mult *= 1.5f;
		return s * mult;
	}

	public void addPoints(int cat, float pts) {
		if (cat == CAT_WEAPON) weaponPts += pts;
		else if (cat == CAT_ARMOR) armorPts += pts;
		else trinketPts += pts;
		Hero h = hero();
		if (h != null) h.updateHT(true);
		BuffIndicator.refreshHero();
	}

	/** Devour an item. Returns true if it was consumed. */
	public boolean devour(Hero hero, Item item) {
		int cat = category(hero, item);
		if (cat < 0) return false;

		float pts = score(hero, item);
		addPoints(cat, pts);

		// Adaptive digestion: some points also flow into the other combat category
		int adapt = hero.pointsInTalent(Talent.PREDATOR_ADAPT);
		if (adapt > 0) {
			if (cat == CAT_WEAPON) addPoints(CAT_ARMOR, pts * 0.10f * adapt);
			else if (cat == CAT_ARMOR) addPoints(CAT_WEAPON, pts * 0.10f * adapt);
		}

		// Gourmand: leftovers turn upgrades into max HP
		if (hero.subClass == HeroSubClass.GOURMAND) {
			int lo = hero.pointsInTalent(Talent.GOURMAND_LEFTOVERS);
			if (lo > 0) leftoverHP += lo * Math.max(0, item.level());
		}

		// Inheritor: keep enchantments and glyphs
		if (hero.subClass == HeroSubClass.INHERITOR) {
			inherit(hero, item);
		}

		eaten++;
		item.detachAll(hero.belongings.backpack);
		Item.updateQuickslot();

		afterDevour(hero, cat, pts);
		return true;
	}

	private void inherit(Hero hero, Item item) {
		int purify = hero.pointsInTalent(Talent.INHERITOR_PURIFY);
		if (item instanceof Weapon && ((Weapon) item).enchantment != null) {
			Weapon.Enchantment e = ((Weapon) item).enchantment;
			Class c = e.getClass();
			if (e.curse()) {
				if (purify >= 2) c = Weapon.Enchantment.random().getClass();
				else if (purify >= 1) c = null;
			}
			if (c != null) {
				enchants.remove(c);
				enchants.add(c);
				while (enchants.size() > maxInherit()) enchants.remove(0);
				GLog.p(Messages.get(PredatorStomach.class, "inherit_enchant"));
			}
		}
		if (item instanceof Armor && ((Armor) item).glyph != null) {
			Armor.Glyph g = ((Armor) item).glyph;
			Class c = g.getClass();
			if (g.curse()) {
				if (purify >= 2) c = Armor.Glyph.random().getClass();
				else if (purify >= 1) c = null;
			}
			if (c != null) {
				glyphs.remove(c);
				glyphs.add(c);
				while (glyphs.size() > maxInherit()) glyphs.remove(0);
				GLog.p(Messages.get(PredatorStomach.class, "inherit_glyph"));
			}
		}
	}

	private void afterDevour(Hero hero, int cat, float pts) {
		// tier 1
		int feast = hero.pointsInTalent(Talent.PREDATOR_FEAST);
		if (feast > 0) {
			int heal = Math.round(hero.HT * (feast == 1 ? 0.10f : 0.20f));
			heal = Math.min(heal, hero.HT - hero.HP);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
			}
		}
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
		int fang = hero.pointsInTalent(Talent.PREDATOR_FANG);
		if (fang > 0) {
			Buff.affect(hero, Fang.class).hits = 3;
		}
		int hide = hero.pointsInTalent(Talent.PREDATOR_HIDE);
		if (hide > 0) {
			Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * (hide == 1 ? 0.08f : 0.15f)));
		}
		// tier 2
		int glut = hero.pointsInTalent(Talent.PREDATOR_GLUTTONY);
		if (glut > 0) {
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(100f * glut);
		}
		// gourmand
		if (hero.subClass == HeroSubClass.GOURMAND && hero.pointsInTalent(Talent.GOURMAND_FOOD_COMA) > 0) {
			Buff.affect(hero, Coma.class);
		}

		String catName = Messages.get(PredatorStomach.class, "cat_" + cat);
		hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(PredatorStomach.class, "gain_status", catName, fmt(pts)));
		hero.sprite.emitter().burst(Speck.factory(Speck.BONE), 6);
		Sample.INSTANCE.play("sounds/eat.mp3");
	}

	public static String fmt(float f) {
		return String.format(java.util.Locale.ENGLISH, "%.1f", f);
	}

	// ------------------------------------------------------------------ inherited procs

	public int procEnchants(Hero hero, Char enemy, int damage) {
		if (enchants.isEmpty()) return damage;
		Weapon wep = hero.belongings.weapon() instanceof Weapon ? (Weapon) hero.belongings.weapon() : null;
		Weapon procWep = wep != null ? wep : new WornShortsword();
		int resonance = hero.pointsInTalent(Talent.INHERITOR_RESONANCE);
		for (Class c : new ArrayList<Class>(enchants)) {
			if (!enemy.isAlive()) break;
			if (wep != null && wep.enchantment != null && wep.enchantment.getClass() == c) continue;
			try {
				Weapon.Enchantment e = (Weapon.Enchantment) c.newInstance();
				damage = e.proc(procWep, hero, enemy, damage);
				if (resonance > 0 && enemy.isAlive() && Random.Float() < 0.15f * resonance) {
					damage = e.proc(procWep, hero, enemy, damage);
				}
			} catch (Throwable t) {
				// ignore broken enchantments
			}
		}
		return damage;
	}

	public int procGlyphs(Hero hero, Char attacker, int damage) {
		if (glyphs.isEmpty() || attacker == null) return damage;
		Armor arm = hero.belongings.armor();
		Armor procArm = arm != null ? arm : new ClothArmor();
		for (Class c : new ArrayList<Class>(glyphs)) {
			if (arm != null && arm.glyph != null && arm.glyph.getClass() == c) continue;
			try {
				Armor.Glyph g = (Armor.Glyph) c.newInstance();
				damage = g.proc(procArm, attacker, hero, damage);
			} catch (Throwable t) {
				// ignore
			}
		}
		return damage;
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
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.get(PredatorStomach.class, "desc_head", eaten)).append("\n\n");
		sb.append(Messages.get(PredatorStomach.class, "desc_points", fmt(weaponPts), fmt(armorPts), fmt(trinketPts))).append("\n\n");
		sb.append(Messages.get(PredatorStomach.class, "desc_stats",
				meleeBonus(),
				Math.round(accuracyBonus() * 100),
				hpBonus(),
				drMax(),
				Math.round(evasionBonus() * 100)));
		if (!enchants.isEmpty() || !glyphs.isEmpty()) {
			sb.append("\n\n").append(Messages.get(PredatorStomach.class, "desc_inherit"));
			for (Class c : enchants) sb.append("\n- ").append(Messages.get(c, "name", ""));
			for (Class c : glyphs) sb.append("\n- ").append(Messages.get(c, "name", ""));
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
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		weaponPts = bundle.getFloat(WEAPON);
		armorPts = bundle.getFloat(ARMOR);
		trinketPts = bundle.getFloat(TRINKET);
		eaten = bundle.getInt(EATEN);
		leftoverHP = bundle.getInt(LEFTOVER);
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
