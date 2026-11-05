package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.sins;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.*;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/** Greed's tier 1 weapon: hits sometimes pay gold into the card. */
public class CollectorRod extends Cudgel {

	{
		image = ItemSpriteSheet.SIN_GREED_T1;
	}

	public static final HeroClass OWNER = HeroClass.GREED;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			Hero hero = (Hero) attacker;
			if (Random.Float() < 0.30f) {
				Buff.affect(hero, GreedVault.class).addBalance(1 + Dungeon.depth);
			}
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
