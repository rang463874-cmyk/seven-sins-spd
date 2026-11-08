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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import java.util.ArrayList;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.utils.Random;


public abstract class KindofMisc extends EquipableItem {

	// Seven Sins: the shared artifact/ring slots are misc, plus misc2 (mirror case) and misc3 (heavenly halo)
	// once unlocked; index 0..sharedCount-1

	private static int sharedCount(Belongings b) {
		int n = 1;
		if (b.extraSlot) n++;
		if (b.extraSlot2) n++;
		return n;
	}

	private static int sharedField(Belongings b, int i) {
		if (i == 0) return 0;
		if (i == 1 && b.extraSlot) return 1;
		return 2;
	}

	private static KindofMisc shared(Belongings b, int i) {
		switch (sharedField(b, i)) {
			case 0: return b.misc;
			case 1: return b.misc2;
			default: return b.misc3;
		}
	}

	private static void setShared(Belongings b, int i, KindofMisc item) {
		switch (sharedField(b, i)) {
			case 0: b.misc = item; break;
			case 1: b.misc2 = item; break;
			default: b.misc3 = item; break;
		}
	}

	/** tries to put this item into a free slot it fits; returns false if every fitting slot is taken */
	private boolean placeInFreeSlot(Hero hero) {
		Belongings b = hero.belongings;
		boolean isArtifact = this instanceof Artifact;
		if (isArtifact && b.artifact == null) {
			b.artifact = (Artifact) this;
			return true;
		}
		if (!isArtifact && this instanceof Ring && b.ring == null) {
			b.ring = (Ring) this;
			return true;
		}
		int n = sharedCount(b);
		for (int i = 0; i < n; i++) {
			if (shared(b, i) == null) {
				setShared(b, i, this);
				return true;
			}
		}
		//see if we can re-arrange items first: move a shared-slot item to its own dedicated slot
		for (int i = 0; i < n; i++) {
			KindofMisc other = shared(b, i);
			if (isArtifact && other instanceof Ring && b.ring == null) {
				b.ring = (Ring) other;
				setShared(b, i, this);
				return true;
			}
			if (!isArtifact && other instanceof Artifact && b.artifact == null) {
				b.artifact = (Artifact) other;
				setShared(b, i, this);
				return true;
			}
		}
		return false;
	}

	private boolean fitsSomewhere(Belongings b) {
		if (this instanceof Artifact && b.artifact == null) return true;
		if (this instanceof Ring && b.ring == null) return true;
		for (int i = 0; i < sharedCount(b); i++) {
			KindofMisc other = shared(b, i);
			if (other == null) return true;
			if (this instanceof Artifact && other instanceof Ring && b.ring == null) return true;
			if (this instanceof Ring && other instanceof Artifact && b.artifact == null) return true;
		}
		return false;
	}

	@Override
	public boolean doEquip(final Hero hero) {

		final Belongings b = hero.belongings;

		//every fitting slot is full: ask which item to take off
		boolean fits = fitsSomewhere(b);

		if (!fits) {

			//the slots this item could go into: its own dedicated slot and the shared ones
			final ArrayList<KindofMisc> choices = new ArrayList<>();
			choices.add(this instanceof Artifact ? b.artifact : b.ring);
			for (int i = 0; i < sharedCount(b); i++) choices.add(shared(b, i));

			String[] labels = new String[choices.size()];
			for (int i = 0; i < labels.length; i++) {
				labels[i] = choices.get(i) == null ? "---" : Messages.titleCase(choices.get(i).title());
			}

			GameScene.show(
					new WndOptions(new ItemSprite(this),
							Messages.get(KindofMisc.class, "unequip_title"),
							Messages.get(KindofMisc.class, "unequip_message"),
							labels) {

						@Override
						protected void onSelect(int index) {

							KindofMisc equipped = choices.get(index);
							if (equipped == null) return;
							//we directly remove the item because we want to have inventory capacity
							// to unequip the equipped one, but don't want to trigger any other
							// item detaching logic
							int slot = Dungeon.quickslot.getSlot(KindofMisc.this);
							slotOfUnequipped = -1;
							Dungeon.hero.belongings.backpack.items.remove(KindofMisc.this);
							if (equipped.doUnequip(hero, true, false)) {
								Dungeon.hero.belongings.backpack.items.add(KindofMisc.this);
								doEquip(hero);
							} else {
								Dungeon.hero.belongings.backpack.items.add(KindofMisc.this);
							}
							if (slot != -1) {
								Dungeon.quickslot.setSlot(slot, KindofMisc.this);
							} else if (slotOfUnequipped != -1 && defaultAction() != null){
								Dungeon.quickslot.setSlot(slotOfUnequipped, KindofMisc.this);
							}
							updateQuickslot();
						}

						@Override
						protected boolean enabled(int index) {
							return choices.get(index) != null;
						}
					});

			return false;

		} else {

			// 15/25% chance
			if (hero.heroClass != HeroClass.CLERIC && hero.hasTalent(Talent.HOLY_INTUITION)
					&& cursed && !cursedKnown
					&& Random.Int(20) < 1 + 2*hero.pointsInTalent(Talent.HOLY_INTUITION)){
				cursedKnown = true;
				GLog.p(Messages.get(this, "curse_detected"));
				return false;
			}

			placeInFreeSlot(hero);

			detach( hero.belongings.backpack );

			Talent.onItemEquipped(hero, this);
			activate( hero );

			cursedKnown = true;
			if (cursed) {
				equipCursed( hero );
				GLog.n( Messages.get(this, "equip_cursed", this) );
			}

			hero.spendAndNext( timeToEquip(hero) );
			return true;

		}

	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){

			if (hero.belongings.artifact == this) {
				hero.belongings.artifact = null;
			} else if (hero.belongings.misc == this) {
				hero.belongings.misc = null;
			} else if (hero.belongings.misc2 == this) {
				hero.belongings.misc2 = null;
			} else if (hero.belongings.misc3 == this) {
				hero.belongings.misc3 = null;
			} else if (hero.belongings.ring == this){
				hero.belongings.ring = null;
			}

			return true;

		} else {

			return false;

		}
	}

	@Override
	public boolean isEquipped( Hero hero ) {
		return hero != null && (hero.belongings.artifact() == this
				|| hero.belongings.misc() == this
				|| hero.belongings.misc2() == this
				|| hero.belongings.misc3() == this
				|| hero.belongings.ring() == this);
	}

}
