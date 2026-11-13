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

/** Sloth's tier 5 weapon: hits harder for every puppet out, kills mend the puppets. */
public class DreamHammer extends WarHammer {

	{
		image = ItemSpriteSheet.SIN_SLOTH_T5;
	}

	public static final HeroClass OWNER = HeroClass.SLOTH;

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (SinWeapons.active(attacker, OWNER)) {
			damage = effect((Hero) attacker, this, defender, damage);
		}
		return super.proc(attacker, defender, damage);
	}

	/** the weapon's effect, also given by the sigil of sin on any melee weapon */
	public static int effect(Hero hero, com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon w, Char defender, int damage) {
		java.util.ArrayList<com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet> ps =
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet.active();
		damage = Math.round(damage * (1f + Math.min(0.5f, 0.10f * ps.size())));
		if (SinWeapons.lethal(defender, damage)) {
			for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet p : ps) {
				p.HP = Math.min(p.HT, p.HP + Math.round(p.HT * 0.20f));
			}
		}
		return damage;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect")
				+ "\n\n" + SinWeapons.ownerNote(OWNER, Dungeon.hero);
	}
}
