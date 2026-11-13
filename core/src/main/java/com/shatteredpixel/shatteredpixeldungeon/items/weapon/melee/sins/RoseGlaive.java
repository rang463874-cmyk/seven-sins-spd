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

/** Lust's tier 5 weapon: every hit steals a little life, and hits on charmed enemies keep them charmed. */
public class RoseGlaive extends Glaive {

	{
		image = ItemSpriteSheet.SIN_LUST_T5;
	}

	public static final HeroClass OWNER = HeroClass.LUST;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			damage = effect((Hero) attacker, this, defender, damage);
		}
		return super.proc(attacker, defender, damage);
	}

	/** the weapon's effect, also given by the sigil of sin on any melee weapon */
	public static int effect(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon w, Char defender, int damage) {
		
		//every hit drinks a little; hits on the charmed don't shake the charm off
		if (LustDesire.charmedByHero(defender)) {
			Charm c = defender.buff(Charm.class);
			if (c != null) c.ignoreNextHit = true;
		}
		Buff.affect(hero, LustDesire.class).drain(Math.min(damage, defender.HP + defender.shielding()), 0.15f);
		return damage;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
