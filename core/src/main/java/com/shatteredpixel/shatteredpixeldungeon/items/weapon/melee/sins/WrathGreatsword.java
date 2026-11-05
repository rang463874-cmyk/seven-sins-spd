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

/** Wrath's tier 5 weapon: struck enemies briefly go amok, and grudge from infighting is 50% higher (see WrathGrudge.addGrudge). */
public class WrathGreatsword extends Greatsword {

	{
		image = ItemSpriteSheet.SIN_WRATH_T5;
	}

	public static final HeroClass OWNER = HeroClass.WRATH;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			Hero hero = (Hero) attacker;
			if (defender.isAlive() && !WrathGrudge.immune(defender) && !SinWeapons.lethal(defender, damage)) {
				Buff.prolong(defender, Amok.class, 2f);
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
