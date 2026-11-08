package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Sword of War, the red horseman's relic (replaces the old WarSword weapon). Like the warrior's broken
 * seal, it is fused onto a melee weapon (any, keeping that weapon's upgrades and enchantment) and adds
 * flat damage from its war stacks; kills made with that weapon feed the stacks. It can be taken back
 * off (detach on the weapon) and moved to another weapon, keeping its stacks. Scrolls of upgrade on it
 * add +20% stacks per kill per level, as before.
 */
public class WarEdge extends Item {

	public static final String AC_AFFIX = "AFFIX";

	public float stacks = 0;
	public int kills = 0;

	{
		image = ItemSpriteSheet.ARTIFACT_WAR;
		unique = true;
		bones = false;
		levelKnown = true;
		defaultAction = AC_AFFIX;
	}

	public float stacksPerKill() {
		return 1f + 0.2f * level();
	}

	public int bonusMin() {
		return (int) (stacks / 36f);
	}

	public int bonusMax() {
		return (int) (stacks / 12f);
	}

	public int roll() {
		int max = bonusMax();
		return max <= 0 ? 0 : Random.NormalIntRange(bonusMin(), max);
	}

	/** a kill with the weapon it is fused onto */
	public void onKill(Hero hero, Mob mob) {
		if (mob.EXP <= 0 && !mob.properties().contains(Char.Property.BOSS)
				&& !mob.properties().contains(Char.Property.MINIBOSS)) return;
		float gain = stacksPerKill();
		if (mob.properties().contains(Char.Property.BOSS)) gain *= 10;
		else if (mob.properties().contains(Char.Property.MINIBOSS)) gain *= 3;
		int oldMax = bonusMax();
		stacks += gain;
		kills++;
		if (bonusMax() > oldMax && hero.sprite != null) {
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(WarEdge.class, "grow"));
			hero.sprite.emitter().burst(Speck.factory(Speck.UP), 3);
		}
		Item.updateQuickslot();
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_AFFIX);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_AFFIX)) return Messages.get(this, "ac_affix");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_AFFIX)) {
			curItem = this;
			GameScene.selectItem(weaponSelector);
		}
	}

	public static boolean canHold(Item item) {
		return item instanceof MeleeWeapon && !(item instanceof com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarSword);
	}

	public void affixTo(Hero hero, Weapon weapon) {
		if (!weapon.cursedKnown) {
			GLog.w(Messages.get(WarEdge.class, "unknown_weapon"));
			return;
		}
		if (weapon.warEdge != null) {
			GLog.w(Messages.get(WarEdge.class, "already"));
			return;
		}
		detach(hero.belongings.backpack);
		weapon.warEdge = this;
		GLog.p(Messages.get(WarEdge.class, "affix", weapon.name()));
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.UP), 4);
		Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
		Item.updateQuickslot();
		hero.spendAndNext(1f);
	}

	private static final WndBag.ItemSelector weaponSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return Messages.get(WarEdge.class, "prompt");
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return canHold(item);
		}

		@Override
		public void onSelect(Item item) {
			if (item instanceof Weapon && curItem instanceof WarEdge) {
				((WarEdge) curItem).affixTo(Dungeon.hero, (Weapon) item);
			}
		}
	};

	/** the line a weapon with the sword fused onto it shows */
	public String weaponLine() {
		return Messages.get(WarEdge.class, "attached", bonusMin(), bonusMax(), Math.round(stacks));
	}

	@Override
	public String info() {
		return super.info() + "\n\n" + Messages.get(this, "stats_desc", Math.round(stacks), kills,
				String.format(java.util.Locale.ENGLISH, "%.1f", stacksPerKill()), bonusMin(), bonusMax());
	}

	@Override
	public boolean isUpgradable() {
		return true;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return bonusMax() > 0 ? new ItemSprite.Glowing(0xCC2222, 2f) : null;
	}

	@Override
	public int value() {
		return 0;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("war_stacks", stacks);
		bundle.put("war_kills", kills);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getFloat("war_stacks");
		kills = bundle.getInt("war_kills");
	}
}
