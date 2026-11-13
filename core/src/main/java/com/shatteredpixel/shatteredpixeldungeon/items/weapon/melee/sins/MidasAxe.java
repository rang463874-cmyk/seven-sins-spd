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

/** Greed's tier 5 weapon: hits deal bonus damage based on carried gold, kills pay out gold. */
public class MidasAxe extends Greataxe {

	{
		image = ItemSpriteSheet.SIN_GREED_T5;
	}

	public static final HeroClass OWNER = HeroClass.GREED;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			damage = effect((Hero) attacker, this, defender, damage);
		}
		return super.proc(attacker, defender, damage);
	}

	/** the weapon's effect, also given by the sigil of sin on any melee weapon */
	public static int effect(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon w, Char defender, int damage) {
		
		GreedVault v = Buff.affect(hero, GreedVault.class);
		damage += (int) Math.min(Math.max(0, Dungeon.gold) / 200, 10 + 2L * w.buffedLvl());
		if (SinWeapons.lethal(defender, damage)) {
			v.earnGold(hero, 5 + 2 * Dungeon.depth);
		}
		return damage;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
