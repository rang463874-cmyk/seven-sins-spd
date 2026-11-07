/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.predator.GorgeBite;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.predator.IronGut;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.predator.Regurgitate;
import com.shatteredpixel.shatteredpixeldungeon.items.PredatorMaw;
import com.shatteredpixel.shatteredpixeldungeon.items.GoldenCard;
import com.shatteredpixel.shatteredpixeldungeon.items.WarDrum;
import com.shatteredpixel.shatteredpixeldungeon.items.PrideCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins.*;
import com.shatteredpixel.shatteredpixeldungeon.items.Perfume;
import com.shatteredpixel.shatteredpixeldungeon.items.EnvyMirror;
import com.shatteredpixel.shatteredpixeldungeon.items.SlothPillow;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth.AutoHunt;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth.NightmareLegion;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.sloth.Proxy;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.envy.Downfall;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.envy.Plunder;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.envy.Transference;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust.EnchantingDance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust.DanseMacabre;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust.BloodPact;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.pride.Aegis;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.pride.Judgement;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.pride.Reign;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.wrath.RageBurst;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.wrath.WarCry;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.wrath.WrathSpirits;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.greed.Bankruptcy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.greed.GoldRain;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.greed.Insurance;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.DeviceCompat;

public enum HeroClass {

	WARRIOR( HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR ),
	MAGE( HeroSubClass.BATTLEMAGE, HeroSubClass.WARLOCK ),
	ROGUE( HeroSubClass.ASSASSIN, HeroSubClass.FREERUNNER ),
	HUNTRESS( HeroSubClass.SNIPER, HeroSubClass.WARDEN ),
	DUELIST( HeroSubClass.CHAMPION, HeroSubClass.MONK ),
	CLERIC( HeroSubClass.PRIEST, HeroSubClass.PALADIN ),

	//Seven Sins
	GLUTTONY( HeroSubClass.GOURMAND, HeroSubClass.INHERITOR ),
	GREED( HeroSubClass.BANKER, HeroSubClass.USURER ),
	WRATH( HeroSubClass.INSTIGATOR, HeroSubClass.AVENGER ),
	PRIDE( HeroSubClass.MONARCH, HeroSubClass.PARAGON ),
	LUST( HeroSubClass.NIGHT_DANCER, HeroSubClass.CRIMSON_NOBLE ),
	ENVY( HeroSubClass.USURPER, HeroSubClass.COPYCAT ),
	SLOTH( HeroSubClass.DREAM_LORD, HeroSubClass.SLEEPWALKER );

	private HeroSubClass[] subClasses;

	HeroClass( HeroSubClass...subClasses ) {
		this.subClasses = subClasses;
	}

	//whether this class appears in hero selection (the cleric is hidden in this mod, but kept for old saves)
	public boolean selectable(){
		return this != CLERIC;
	}

	public void initHero( Hero hero ) {

		hero.heroClass = this;
		Talent.initClassTalents(hero);

		Item i = new ClothArmor().identify();
		if (!Challenges.isItemBlocked(i)) hero.belongings.armor = (ClothArmor)i;

		i = new Food();
		if (!Challenges.isItemBlocked(i)) i.collect();

		new VelvetPouch().collect();
		Dungeon.LimitedDrops.VELVET_POUCH.drop();

		Waterskin waterskin = new Waterskin();
		waterskin.collect();

		new ScrollOfIdentify().identify();

		switch (this) {
			case WARRIOR:
				initWarrior( hero );
				break;

			case MAGE:
				initMage( hero );
				break;

			case ROGUE:
				initRogue( hero );
				break;

			case HUNTRESS:
				initHuntress( hero );
				break;

			case DUELIST:
				initDuelist( hero );
				break;

			case CLERIC:
				initCleric( hero );
				break;

			case GLUTTONY:
				initGluttony( hero );
				break;

			case GREED:
				initGreed( hero );
				break;

			case WRATH:
				initWrath( hero );
				break;

			case PRIDE:
				initPride( hero );
				break;

			case LUST:
				initLust( hero );
				break;

			case ENVY:
				initEnvy( hero );
				break;

			case SLOTH:
				initSloth( hero );
				break;
		}

		if (SPDSettings.quickslotWaterskin()) {
			for (int s = 0; s < QuickSlot.SIZE; s++) {
				if (Dungeon.quickslot.getItem(s) == null) {
					Dungeon.quickslot.setSlot(s, waterskin);
					break;
				}
			}
		}

	}

	public Badges.Badge masteryBadge() {
		switch (this) {
			case WARRIOR:
				return Badges.Badge.MASTERY_WARRIOR;
			case MAGE:
				return Badges.Badge.MASTERY_MAGE;
			case ROGUE:
				return Badges.Badge.MASTERY_ROGUE;
			case HUNTRESS:
				return Badges.Badge.MASTERY_HUNTRESS;
			case DUELIST:
				return Badges.Badge.MASTERY_DUELIST;
			case CLERIC:
				return Badges.Badge.MASTERY_CLERIC;
		}
		return null;
	}

	private static void initWarrior( Hero hero ) {
		(hero.belongings.weapon = new WornShortsword()).identify();
		ThrowingStone stones = new ThrowingStone();
		stones.identify().collect();

		Dungeon.quickslot.setSlot(0, stones);

		if (hero.belongings.armor != null){
			hero.belongings.armor.affixSeal(new BrokenSeal());
			Catalog.setSeen(BrokenSeal.class); //as it's not added to the inventory
		}

		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	private static void initMage( Hero hero ) {
		MagesStaff staff;

		staff = new MagesStaff(new WandOfMagicMissile());

		(hero.belongings.weapon = staff).identify();
		hero.belongings.weapon.activate(hero);

		Dungeon.quickslot.setSlot(0, staff);

		new ScrollOfUpgrade().identify();
		new PotionOfLiquidFlame().identify();
	}

	private static void initRogue( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();

		CloakOfShadows cloak = new CloakOfShadows();
		(hero.belongings.artifact = cloak).identify();
		hero.belongings.artifact.activate( hero );

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify().collect();

		Dungeon.quickslot.setSlot(0, cloak);
		Dungeon.quickslot.setSlot(1, knives);

		new ScrollOfMagicMapping().identify();
		new PotionOfInvisibility().identify();
	}

	private static void initHuntress( Hero hero ) {

		(hero.belongings.weapon = new Gloves()).identify();
		SpiritBow bow = new SpiritBow();
		bow.identify().collect();

		Dungeon.quickslot.setSlot(0, bow);

		new PotionOfMindVision().identify();
		new ScrollOfLullaby().identify();
	}

	private static void initDuelist( Hero hero ) {

		(hero.belongings.weapon = new Rapier()).identify();
		hero.belongings.weapon.activate(hero);

		ThrowingSpike spikes = new ThrowingSpike();
		spikes.quantity(2).identify().collect(); //set quantity is 3, but Duelist starts with 2

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, spikes);

		new PotionOfStrength().identify();
		new ScrollOfMirrorImage().identify();
	}

	private static void initCleric( Hero hero ) {

		(hero.belongings.weapon = new Cudgel()).identify();
		hero.belongings.weapon.activate(hero);

		HolyTome tome = new HolyTome();
		(hero.belongings.artifact = tome).identify();
		hero.belongings.artifact.activate( hero );

		Dungeon.quickslot.setSlot(0, tome);

		new PotionOfPurity().identify();
		new ScrollOfRemoveCurse().identify();
	}

	private static void initGluttony( Hero hero ) {

		(hero.belongings.weapon = new GnawingFang()).identify();
		hero.belongings.weapon.activate(hero);

		PredatorMaw maw = new PredatorMaw();
		(hero.belongings.artifact = maw).identify();
		hero.belongings.artifact.activate( hero );
		Dungeon.quickslot.setSlot(0, maw);

		Buff.affect(hero, PredatorStomach.class);

		new PotionOfHealing().identify();
		new ScrollOfRemoveCurse().identify();
	}

	private static void initGreed( Hero hero ) {

		(hero.belongings.weapon = new CollectorRod()).identify();
		hero.belongings.weapon.activate(hero);

		GoldenCard card = new GoldenCard();
		(hero.belongings.artifact = card).identify();
		hero.belongings.artifact.activate( hero );
		Dungeon.quickslot.setSlot(0, card);

		Buff.affect(hero, GreedVault.class);

		new PotionOfHealing().identify();
		new ScrollOfMagicMapping().identify();
	}

	private static void initWrath( Hero hero ) {

		(hero.belongings.weapon = new TauntKnuckle()).identify();
		hero.belongings.weapon.activate(hero);

		WarDrum drum = new WarDrum();
		(hero.belongings.artifact = drum).identify();
		hero.belongings.artifact.activate( hero );
		Dungeon.quickslot.setSlot(0, drum);

		Buff.affect(hero, WrathGrudge.class);

		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	private static void initPride( Hero hero ) {

		(hero.belongings.weapon = new DuelingSword()).identify();
		hero.belongings.weapon.activate(hero);

		PrideCrown crown = new PrideCrown();
		(hero.belongings.artifact = crown).identify();
		hero.belongings.artifact.activate( hero );

		//pride fights from afar
		com.shatteredpixel.shatteredpixeldungeon.items.weapon.RoyalBow bow = new com.shatteredpixel.shatteredpixeldungeon.items.weapon.RoyalBow();
		bow.identify().collect();
		Dungeon.quickslot.setSlot(0, bow);
		Dungeon.quickslot.setSlot(1, crown);

		Buff.affect(hero, PrideGlory.class).bowGiven = true;

		new PotionOfHealing().identify();
		new ScrollOfMirrorImage().identify();
	}

	private static void initLust( Hero hero ) {

		(hero.belongings.weapon = new RoseWhip()).identify();
		hero.belongings.weapon.activate(hero);

		Perfume perfume = new Perfume();
		(hero.belongings.artifact = perfume).identify();
		hero.belongings.artifact.activate( hero );
		Dungeon.quickslot.setSlot(0, perfume);

		Buff.affect(hero, LustDesire.class);

		new PotionOfHealing().identify();
		new ScrollOfLullaby().identify();
	}

	private static void initEnvy( Hero hero ) {

		(hero.belongings.weapon = new PickpocketDagger()).identify();
		hero.belongings.weapon.activate(hero);

		EnvyMirror mirror = new EnvyMirror();
		(hero.belongings.artifact = mirror).identify();
		hero.belongings.artifact.activate( hero );
		Dungeon.quickslot.setSlot(0, mirror);

		Buff.affect(hero, EnvyCovet.class);

		new PotionOfHealing().identify();
		new ScrollOfTransmutation().identify();
	}

	private static void initSloth( Hero hero ) {

		(hero.belongings.weapon = new PillowMace()).identify();
		hero.belongings.weapon.activate(hero);

		SlothPillow pillow = new SlothPillow();
		(hero.belongings.artifact = pillow).identify();
		hero.belongings.artifact.activate( hero );
		Dungeon.quickslot.setSlot(0, pillow);

		Buff.affect(hero, SlothRepose.class);

		new PotionOfHealing().identify();
		new ScrollOfRecharging().identify();
	}

	public String title() {
		return Messages.get(HeroClass.class, name());
	}

	public String desc(){
		return Messages.get(HeroClass.class, name()+"_desc");
	}

	public String shortDesc(){
		return Messages.get(HeroClass.class, name()+"_desc_short");
	}

	public HeroSubClass[] subClasses() {
		return subClasses;
	}

	public ArmorAbility[] armorAbilities(){
		switch (this) {
			case WARRIOR: default:
				return new ArmorAbility[]{new HeroicLeap(), new Shockwave(), new Endure()};
			case MAGE:
				return new ArmorAbility[]{new ElementalBlast(), new WildMagic(), new WarpBeacon()};
			case ROGUE:
				return new ArmorAbility[]{new SmokeBomb(), new DeathMark(), new ShadowClone()};
			case HUNTRESS:
				return new ArmorAbility[]{new SpectralBlades(), new NaturesPower(), new SpiritHawk()};
			case DUELIST:
				return new ArmorAbility[]{new Challenge(), new ElementalStrike(), new Feint()};
			case CLERIC:
				return new ArmorAbility[]{new AscendedForm(), new Trinity(), new PowerOfMany()};
			case GLUTTONY:
				return new ArmorAbility[]{new GorgeBite(), new IronGut(), new Regurgitate()};
			case GREED:
				return new ArmorAbility[]{new GoldRain(), new Insurance(), new Bankruptcy()};
			case WRATH:
				return new ArmorAbility[]{new WarCry(), new WrathSpirits(), new RageBurst()};
			case PRIDE:
				return new ArmorAbility[]{new Reign(), new Aegis(), new Judgement()};
			case LUST:
				return new ArmorAbility[]{new EnchantingDance(), new BloodPact(), new DanseMacabre()};
			case ENVY:
				return new ArmorAbility[]{new Plunder(), new Transference(), new Downfall()};
			case SLOTH:
				return new ArmorAbility[]{new NightmareLegion(), new AutoHunt(), new Proxy()};
		}
	}

	public String spritesheet() {
		switch (this) {
			case WARRIOR: default:
				return Assets.Sprites.WARRIOR;
			case MAGE:
				return Assets.Sprites.MAGE;
			case ROGUE:
				return Assets.Sprites.ROGUE;
			case HUNTRESS:
				return Assets.Sprites.HUNTRESS;
			case DUELIST:
				return Assets.Sprites.DUELIST;
			case CLERIC:
				return Assets.Sprites.CLERIC;
			case GLUTTONY:
				return Assets.Sprites.GLUTTONY;
			case GREED:
				return Assets.Sprites.GREED;
			case WRATH:
				return Assets.Sprites.WRATH;
			case PRIDE:
				return Assets.Sprites.PRIDE;
			case LUST:
				return Assets.Sprites.LUST;
			case ENVY:
				return Assets.Sprites.ENVY;
			case SLOTH:
				return Assets.Sprites.SLOTH;
		}
	}

	public String splashArt(){
		switch (this) {
			case WARRIOR: default:
				return Assets.Splashes.WARRIOR;
			case MAGE:
				return Assets.Splashes.MAGE;
			case ROGUE:
				return Assets.Splashes.ROGUE;
			case HUNTRESS:
				return Assets.Splashes.HUNTRESS;
			case DUELIST:
				return Assets.Splashes.DUELIST;
			case CLERIC:
				return Assets.Splashes.CLERIC;
			case GLUTTONY:
				return Assets.Splashes.GLUTTONY;
			case GREED:
				return Assets.Splashes.GREED;
			case WRATH:
				return Assets.Splashes.WRATH;
			case PRIDE:
				return Assets.Splashes.PRIDE;
			case LUST:
				return Assets.Splashes.LUST;
			case ENVY:
				return Assets.Splashes.ENVY;
			case SLOTH:
				return Assets.Splashes.SLOTH;
		}
	}
	
	public boolean isUnlocked(){
		//always unlock on debug builds
		if (DeviceCompat.isDebug()) return true;

		switch (this){
			case WARRIOR: default:
				return true;
			case MAGE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE);
			case ROGUE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE);
			case HUNTRESS:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS);
			case DUELIST:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_DUELIST);
			case CLERIC:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_CLERIC);
			//the seven sins are available from the start
			case GLUTTONY:
			case GREED:
			case WRATH:
			case PRIDE:
			case LUST:
			case ENVY:
			case SLOTH:
				return true;
		}
	}
	
	public String unlockMsg() {
		return shortDesc() + "\n\n" + Messages.get(HeroClass.class, name()+"_unlock");
	}

}
