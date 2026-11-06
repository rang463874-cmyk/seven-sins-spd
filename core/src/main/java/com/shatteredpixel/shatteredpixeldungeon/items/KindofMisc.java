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
		if (b.misc == null) {
			b.misc = this;
			return true;
		}
		if (b.extraSlot && b.misc2 == null) {
			b.misc2 = this;
			return true;
		}
		//see if we can re-arrange items first: move a shared-slot item to its own dedicated slot
		if (isArtifact) {
			if (b.misc instanceof Ring && b.ring == null) {
				b.ring = (Ring) b.misc;
				b.misc = this;
				return true;
			}
			if (b.extraSlot && b.misc2 instanceof Ring && b.ring == null) {
				b.ring = (Ring) b.misc2;
				b.misc2 = this;
				return true;
			}
		} else {
			if (b.misc instanceof Artifact && b.artifact == null) {
				b.artifact = (Artifact) b.misc;
				b.misc = this;
				return true;
			}
			if (b.extraSlot && b.misc2 instanceof Artifact && b.artifact == null) {
				b.artifact = (Artifact) b.misc2;
				b.misc2 = this;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean doEquip(final Hero hero) {

		final Belongings b = hero.belongings;

		//every fitting slot is full: ask which item to take off
		boolean fits = (this instanceof Artifact && b.artifact == null)
				|| (this instanceof Ring && b.ring == null)
				|| b.misc == null
				|| (b.extraSlot && b.misc2 == null)
				|| (this instanceof Artifact && (b.misc instanceof Ring || (b.extraSlot && b.misc2 instanceof Ring)) && b.ring == null)
				|| (this instanceof Ring && (b.misc instanceof Artifact || (b.extraSlot && b.misc2 instanceof Artifact)) && b.artifact == null);

		if (!fits) {

			//the slots this item could go into: its own dedicated slot and the shared ones
			final ArrayList<KindofMisc> choices = new ArrayList<>();
			choices.add(this instanceof Artifact ? b.artifact : b.ring);
			choices.add(b.misc);
			if (b.extraSlot) choices.add(b.misc2);

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
				|| hero.belongings.ring() == this);
	}

}
