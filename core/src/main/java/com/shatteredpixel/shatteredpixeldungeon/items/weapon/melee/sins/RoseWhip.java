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

/** Lust's tier 1 weapon: a reach 2 whip whose hits can charm. */
public class RoseWhip extends WornShortsword {

	{
		image = ItemSpriteSheet.SIN_LUST_T1;
		RCH = 2;
	}

	public static final HeroClass OWNER = HeroClass.LUST;

	//reach 2 comes at the cost of damage, like the whip
	@Override
	public int max(int lvl) {
		return 4*(tier+1) + lvl*(tier+1);
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			Hero hero = (Hero) attacker;
			if (defender.isAlive() && defender instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob
					&& !LustDesire.charmImmune(defender) && !LustDesire.charmedByHero(defender)
					&& !SinWeapons.lethal(defender, damage) && Random.Float() < 0.15f) {
				LustDesire.charm(hero, (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob) defender, 4f);
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
