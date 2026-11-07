package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/**
 * Sword of War, the red horseman: a sword that grows by itself.
 * Every kill made with it adds war stacks, which raise its damage without limit.
 * Scrolls of upgrade don't raise its damage directly; instead each level adds
 * +20% stacks per kill (and still lowers the strength requirement as usual).
 */
public class WarSword extends Sword {

	public float stacks = 0;
	public int kills = 0;

	{
		image = ItemSpriteSheet.ARTIFACT_WAR;
		tier = 3;
		unique = true;
		bones = false;
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

	@Override
	public int min(int lvl) {
		return tier + bonusMin();
	}

	@Override
	public int max(int lvl) {
		return 5 * (tier + 1) + bonusMax();
	}

	/** called when the hero kills an enemy while wielding this sword */
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
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(WarSword.class, "grow"));
			hero.sprite.emitter().burst(Speck.factory(Speck.UP), 3);
		}
		Item.updateQuickslot();
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", Math.round(stacks), kills,
				String.format(java.util.Locale.ENGLISH, "%.1f", stacksPerKill()), bonusMin(), bonusMax());
	}

	@Override
	public Item random() {
		cursed = false;
		enchantment = null;
		level(0);
		return this;
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
