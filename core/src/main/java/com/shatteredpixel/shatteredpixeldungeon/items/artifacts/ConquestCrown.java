package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Gun of Conquest, the white horseman (the class keeps its old name for saves).
 * It devours weapons, thrown weapons, armor and rings to load bullets, then fires them at anything in sight.
 * Loading an enchanted weapon makes the bullets carry that enchantment.
 * It levels up (0..10) as it fires: more damage. There is no magazine limit.
 */
public class ConquestCrown extends HorsemanArtifact {

	public static final String AC_LOAD = "LOAD";

	/** last good enchantment taken from a loaded weapon */
	public Class<? extends Weapon.Enchantment> enchant = null;

	{
		image = ItemSpriteSheet.ARTIFACT_CONQUEST;
		//aims at the last or nearest enemy from the quickslot, like a thrown weapon
		usesTargeting = true;
		charge = 0;
		chargeCap = maxAmmo();
	}

	@Override
	protected float rechargeTurns() {
		return 100f;
	}

	@Override
	protected boolean timeCharges() {
		return false;
	}

	/** no magazine limit any more: load as many bullets as you like */
	public int maxAmmo() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int expToLevel() {
		return 10 + 5 * level();
	}

	@Override
	public Item upgrade() {
		super.upgrade();
		chargeCap = maxAmmo();
		return this;
	}

	@Override
	public void resetForTrinity(int visibleLevel) {
		super.resetForTrinity(visibleLevel);
		//the base version fills to the cap, which no longer exists
		charge = 20;
	}

	@Override
	public void charge(Hero target, float amount) {
		//recharging doesn't make bullets
	}

	public int minDamage() {
		int lvl = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		return 2 + level() + lvl / 3;
	}

	public int maxDamage() {
		int lvl = Dungeon.hero == null ? 1 : Dungeon.hero.lvl;
		return Math.round(6 + 1.5f * level() + 0.8f * lvl);
	}

	/** bullets an item loads, or 0 if it can't be loaded */
	public static int bulletsFor(Hero hero, Item item) {
		if (item == null || item.unique) return 0;
		if (item instanceof EquipableItem && item.isEquipped(hero)) return 0;
		int lvl = Math.max(0, item.level());
		float b;
		if (item instanceof MeleeWeapon) {
			b = ((MeleeWeapon) item).tier + 2 * lvl;
			if (((Weapon) item).hasGoodEnchant()) b += 1;
		} else if (item instanceof Armor && !(item instanceof ClassArmor)) {
			Armor a = (Armor) item;
			b = a.tier + 2 * lvl;
			if (a.glyph != null && !a.glyph.curse()) b += 1;
		} else if (item instanceof MissileWeapon) {
			//the whole stack is loaded: half of tier + 2*level per thrown weapon
			MissileWeapon m = (MissileWeapon) item;
			b = (m.tier + 2 * lvl) * 0.5f * m.quantity();
			if (m.hasGoodEnchant()) b += 1;
		} else if (item instanceof Ring) {
			b = 4 + 3 * lvl;
		} else {
			return 0;
		}
		if (item.cursed) b *= 0.5f;
		return Math.max(1, Math.round(b));
	}

	// ------------------------------------------------------------------ actions

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_LOAD);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_LOAD)) return Messages.get(this, "ac_load");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		chargeCap = maxAmmo();
		if (action.equals(AC_USE)) {
			//firing only needs one bullet, not a full charge
			if (hero.buff(MagicImmune.class) != null) return;
			if (!isEquipped(hero)) {
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			} else if (cursed) {
				GLog.i(Messages.get(HorsemanArtifact.class, "cursed"));
			} else if (charge <= 0) {
				GLog.w(Messages.get(this, "empty"));
			} else {
				usePower(hero);
			}
			return;
		}
		super.execute(hero, action);
		if (action.equals(AC_LOAD)) {
			if (hero.buff(MagicImmune.class) != null) return;
			if (!isEquipped(hero)) {
				GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			} else if (!cursed) {
				selectLoad(hero);
			}
		}
	}

	private void selectLoad(final Hero hero) {
		GameScene.selectItem(new WndBag.ItemSelector() {
			@Override
			public String textPrompt() {
				return Messages.get(ConquestCrown.class, "prompt_load");
			}

			@Override
			public boolean itemSelectable(Item item) {
				return bulletsFor(hero, item) > 0;
			}

			@Override
			public void onSelect(Item item) {
				if (item == null) return;
				int bullets = bulletsFor(hero, item);
				if (bullets <= 0) return;
				if (item instanceof Weapon && ((Weapon) item).hasGoodEnchant()) {
					enchant = ((Weapon) item).enchantment.getClass();
					GLog.p(Messages.get(ConquestCrown.class, "enchanted", ((Weapon) item).enchantment.name()));
				}
				int loaded = (int) Math.min((long) bullets, (long) maxAmmo() - charge);
				charge += loaded;
				item.detachAll(hero.belongings.backpack);
				Item.updateQuickslot();
				hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(ConquestCrown.class, "loaded", loaded));
				hero.sprite.operate(hero.pos);
				Sample.INSTANCE.play(Assets.Sounds.EVOKE);
				Invisibility.dispel();
				hero.spendAndNext(1f);
			}
		});
	}

	@Override
	protected boolean usePower(Hero hero) {
		Char last = com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget;
		if (last == null || !last.isAlive() || !Dungeon.level.heroFOV[last.pos] || last.alignment == Char.Alignment.ALLY) {
			com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.lastTarget =
					com.shatteredpixel.shatteredpixeldungeon.ui.SkillBar.autoTarget();
		}
		GameScene.selectCell(shooter);
		return true;
	}

	private void shoot(final Hero hero, int cell) {
		final Ballistica shot = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
		final int target = shot.collisionPos;
		charge--;
		gainExp(1);
		Item.updateQuickslot();
		Invisibility.dispel();
		hero.sprite.zap(target);
		Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.6f, 1.6f);
		hero.busy();
		Char aimed = Actor.findChar(target);
		com.watabou.utils.PointF to = aimed != null && aimed.sprite != null
				? aimed.sprite.center()
				: com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.raisedTileCenterToWorld(target);
		com.shatteredpixel.shatteredpixeldungeon.effects.Bullet.fire(hero.sprite.parent, hero.sprite.center(), to, () -> {
			Char ch = Actor.findChar(target);
			if (ch != null && ch != hero) {
				com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton.target(ch);
				if (Char.hit(hero, ch, false)) {
					int dmg = Random.NormalIntRange(minDamage(), maxDamage());
					dmg = Math.max(0, dmg - ch.drRoll());
					if (enchant != null) {
						Weapon w = new WornShortsword();
						try {
							w.enchant(enchant.newInstance());
							dmg = w.enchantment.proc(w, hero, ch, dmg);
						} catch (Exception e) {
							enchant = null;
						}
					}
					ch.damage(dmg, ConquestCrown.this);
					Sample.INSTANCE.play(Assets.Sounds.HIT_ARROW);
				} else if (ch.sprite != null) {
					ch.sprite.showStatus(CharSprite.NEUTRAL, ch.defenseVerb());
				}
			}
			hero.spendAndNext(1f);
		});
	}

	private final CellSelector.Listener shooter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			if (cell == hero.pos || charge <= 0) return;
			shoot(hero, cell);
		}

		@Override
		public String prompt() {
			return Messages.get(ConquestCrown.class, "prompt");
		}
	};

	// ------------------------------------------------------------------ info

	@Override
	public String status() {
		if (cursed) return null;
		return Integer.toString(charge);
	}

	@Override
	protected String statsDesc() {
		String s = Messages.get(this, "stats", charge, minDamage(), maxDamage());
		if (enchant != null) {
			try {
				s += "\n" + Messages.get(this, "stats_enchant", enchant.newInstance().name());
			} catch (Exception ignored) {}
		}
		return s;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (enchant != null) bundle.put("gun_enchant", enchant);
		bundle.put("gun_v2", true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("gun_enchant")) enchant = bundle.getClass("gun_enchant");
		if (!bundle.contains("gun_v2")) {
			//old crown saves: its charge was a percentage, not bullets
			charge = 0;
		}
		chargeCap = maxAmmo();
	}

	/** a surrendered enemy fighting for the hero (the crown's old power, kept for saves) */
	public static class Subjugated extends AllyBuff {

		public float left = 10f;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0) {
				target.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(ConquestCrown.class, "collapse"));
				target.die(null);
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.CORRUPT;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 1f, 0.8f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(Math.max(0, Math.round(left)));
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.max(0, Math.round(left)));
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat("left");
		}
	}
}
