package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * Seven Sins: base of the owner's weapon list (Nov 13 2026). Damage at +0 is baseMin..baseMax (the owner's numbers);
 * each level adds minScale / maxScale (vanilla: +1 / +(tier+1)). Effects go in proc(); statsInfo() fills the weapon's
 * stats_desc with statsArgs().
 */
public abstract class ModMeleeWeapon extends MeleeWeapon {

	protected int baseMin, baseMax;
	protected int minScale = 1;
	protected int maxScale = -1; //-1: tier+1

	@Override
	public int min(int lvl) {
		return baseMin + lvl * minScale;
	}

	@Override
	public int max(int lvl) {
		return baseMax + lvl * (maxScale < 0 ? tier + 1 : maxScale);
	}

	protected Object[] statsArgs() {
		return new Object[0];
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", statsArgs());
	}

	/** the multiplier of the target's armor roll for this weapon's hits (1 = normal), see Char.attack */
	public float armorFactor(Char attacker, Char defender) {
		return 1f;
	}

	/** from Char.attack: the hero's equipped mod weapon may reduce the target's armor roll */
	public static int armorPierce(Char attacker, Char defender, int dr) {
		if (!(attacker instanceof Hero)) return dr;
		Object w = ((Hero) attacker).belongings.attackingWeapon();
		if (w instanceof ModMeleeWeapon) {
			return Math.round(dr * ((ModMeleeWeapon) w).armorFactor(attacker, defender));
		}
		return dr;
	}

	public static boolean lethal(Char defender, int damage) {
		return damage >= defender.HP + defender.shielding();
	}

	public static boolean big(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS)
				|| ch.properties().contains(Char.Property.LARGE);
	}

	/** splash damage (armor applies) to enemies of the attacker within `radius` of `center`, except `skip` */
	public static void splash(Char attacker, int center, int radius, int dmg, Char skip, Object src) {
		if (dmg <= 0) return;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m == skip || m == attacker || !m.isAlive() || m.alignment == attacker.alignment) continue;
			if (Dungeon.level.distance(m.pos, center) > radius) continue;
			int d = Math.max(0, dmg - m.drRoll());
			if (d > 0) m.damage(d, src);
		}
	}
}
