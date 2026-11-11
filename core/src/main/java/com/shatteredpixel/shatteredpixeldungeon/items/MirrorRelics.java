package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The Imp quest's vault mirror shows each sin what its signature item might have been.
 * Taking that reflection replaces the old item (all progress lives in the sin's buff,
 * so nothing is lost) and switches on the buff's "mirrored" bonuses.
 */
public final class MirrorRelics {

	private MirrorRelics() {}

	/** the mirror's reward for a sin, or null for other classes */
	public static Item rewardFor(HeroClass cls) {
		switch (cls) {
			case GLUTTONY: return new GoldenMaw();
			case GREED:    return new BlackCard();
			case WRATH:    return new BloodDrum();
			case PRIDE:    return new EternalCrown();
			case LUST:     return new ForbiddenPerfume();
			case ENVY:     return new ShatteredMirror();
			case SLOTH:    return new EternalPillow();
			default:       return null;
		}
	}

	/** switches on the sin buff's mirrored bonuses (the vault mirror's, and a +10 signature item's) */
	public static void markMirrored(Hero hero) {
		switch (hero.heroClass) {
			case GLUTTONY: Buff.affect(hero, PredatorStomach.class).mirrored = true; break;
			case GREED:    Buff.affect(hero, GreedVault.class).mirrored = true; break;
			case WRATH:    Buff.affect(hero, WrathGrudge.class).mirrored = true; break;
			case PRIDE:    Buff.affect(hero, PrideGlory.class).mirrored = true; break;
			case LUST:     Buff.affect(hero, LustDesire.class).mirrored = true; break;
			case ENVY:     Buff.affect(hero, EnvyCovet.class).mirrored = true; break;
			case SLOTH:    Buff.affect(hero, SlothRepose.class).mirrored = true; break;
			default: break;
		}
	}

	/** removes the old signature item, moving its quickslot to the relic */
	static void replaceOld(Hero hero, Item relic, Class<? extends Item> base) {
		Item old = null;
		for (Item it : hero.belongings) {
			if (base.isInstance(it) && it != relic && it.getClass() != relic.getClass()) {
				old = it;
				break;
			}
		}
		if (old == null) return;
		int slot = Dungeon.quickslot.getSlot(old);
		old.detachAll(hero.belongings.backpack);
		if (slot != -1) Dungeon.quickslot.setSlot(slot, relic);
		Item.updateQuickslot();
	}


	// ------------------------------------------------------------------ the seven reflections

	public static class GoldenMaw extends PredatorMaw {
		{
			image = ItemSpriteSheet.MIRROR_GLUTTONY;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, PredatorMaw.class);
			Buff.affect(hero, PredatorStomach.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}

	public static class BlackCard extends GoldenCard {
		{
			image = ItemSpriteSheet.MIRROR_GREED;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, GoldenCard.class);
			Buff.affect(hero, GreedVault.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}

	public static class BloodDrum extends WarDrum {
		{
			image = ItemSpriteSheet.MIRROR_WRATH;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, WarDrum.class);
			Buff.affect(hero, WrathGrudge.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}

	public static class EternalCrown extends PrideCrown {
		{
			image = ItemSpriteSheet.MIRROR_PRIDE;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, PrideCrown.class);
			Buff.affect(hero, PrideGlory.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}

	public static class ForbiddenPerfume extends Perfume {
		{
			image = ItemSpriteSheet.MIRROR_LUST;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, Perfume.class);
			Buff.affect(hero, LustDesire.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}

	public static class ShatteredMirror extends EnvyMirror {
		{
			image = ItemSpriteSheet.MIRROR_ENVY;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, EnvyMirror.class);
			Buff.affect(hero, EnvyCovet.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}

	public static class EternalPillow extends SlothPillow {
		{
			image = ItemSpriteSheet.MIRROR_SLOTH;
		}

		@Override
		public boolean doPickUp(Hero hero, int pos) {
			replaceOld(hero, this, SlothPillow.class);
			Buff.affect(hero, SlothRepose.class).mirrored = true;
			return super.doPickUp(hero, pos);
		}

		@Override
		public String desc() {
			return super.desc() + "\n\n" + Messages.get(this, "bonus");
		}
	}
}
