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

/** Lust's tier 5 weapon: hits on charmed enemies drain life and build desire without breaking the charm. */
public class RoseGlaive extends Glaive {

	{
		image = ItemSpriteSheet.SIN_LUST_T5;
	}

	public static final HeroClass OWNER = HeroClass.LUST;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			Hero hero = (Hero) attacker;
			if (LustDesire.charmedByHero(defender)) {
				Charm c = defender.buff(Charm.class);
				if (c != null) c.ignoreNextHit = true;
				int heal = Math.min(hero.HT - hero.HP, Math.round(damage * 0.3f));
				if (heal > 0) hero.HP += heal;
				Buff.affect(hero, LustDesire.class).addDesire(damage * 0.1f);
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
