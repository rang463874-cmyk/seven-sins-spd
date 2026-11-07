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

/** Pride's tier 5 weapon: glory gains are 50% higher and maximum dignity is increased. */
public class KingsAegis extends Greatshield {

	{
		image = ItemSpriteSheet.SIN_PRIDE_T5;
	}

	public static final HeroClass OWNER = HeroClass.PRIDE;

	/** while held, pride's arrows and thrown weapons hit this much harder (see PrideHooks) */
	public static final float RANGED_BONUS = 0.30f;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			Hero hero = (Hero) attacker;
			//passive: +30% ranged damage (PrideHooks) and higher maximum dignity (PrideGlory)
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
