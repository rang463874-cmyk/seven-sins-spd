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

/** Sloth's tier 1 weapon: hits build torpor. */
public class PillowMace extends Cudgel {

	{
		image = ItemSpriteSheet.SIN_SLOTH_T1;
	}

	public static final HeroClass OWNER = HeroClass.SLOTH;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			Hero hero = (Hero) attacker;
			Buff.affect(hero, SlothRepose.class).addTorpor(1);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
