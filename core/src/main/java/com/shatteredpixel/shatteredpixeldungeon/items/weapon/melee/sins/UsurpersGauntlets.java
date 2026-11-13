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

/** Envy's tier 5 weapon: the jealousy gauge fills 50% faster while wielded (see EnvyCovet.jealousyRate), and every hit adds a little more. */
public class UsurpersGauntlets extends Gauntlet {

	{
		image = ItemSpriteSheet.SIN_ENVY_T5;
	}

	public static final HeroClass OWNER = HeroClass.ENVY;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			damage = effect((Hero) attacker, this, defender, damage);
		}
		return super.proc(attacker, defender, damage);
	}

	/** the weapon's effect, also given by the sigil of sin on any melee weapon */
	public static int effect(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon w, Char defender, int damage) {
		
		Buff.affect(hero, EnvyCovet.class).addJealousy(0.5f);
		return damage;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
