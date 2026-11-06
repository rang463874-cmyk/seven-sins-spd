package com.shatteredpixel.shatteredpixeldungeon.horsemen;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Prophet;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ConquestCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DeathScythe;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HorsemanArtifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.WarBlade;

/**
 * Game hooks for the four horsemen artifacts and the sewer prophet's quest.
 * Every method is a cheap pass-through when none of them is involved.
 */
public final class HorsemenHooks {

	private HorsemenHooks() {}

	/** the equipped, uncursed horseman artifact of a given type, or null */
	@SuppressWarnings("unchecked")
	private static <T extends HorsemanArtifact> T equipped(Hero hero, Class<T> cls) {
		if (hero == null || hero.buff(MagicImmune.class) != null) return null;
		for (Buff b : hero.buffs()) {
			if (b instanceof HorsemanArtifact.HorsemanBuff) {
				HorsemanArtifact.HorsemanBuff hb = (HorsemanArtifact.HorsemanBuff) b;
				if (cls.isInstance(hb.artifact()) && !hb.isCursed()) return (T) hb.artifact();
			}
		}
		return null;
	}

	/** Called from Hero.attackProc. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		WarBlade war = equipped(hero, WarBlade.class);
		return war == null ? dmg : war.modifyAttack(hero, dmg);
	}

	/** Called from Hero.defenseProc. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		WarBlade war = equipped(hero, WarBlade.class);
		if (war != null) war.onHurt(hero, dmg);
		return dmg;
	}

	/** Called from Hero.die before the ankh. Returns true if the hero was saved. */
	public static boolean cheatDeath(Hero hero) {
		DeathScythe death = equipped(hero, DeathScythe.class);
		return death != null && death.spare(hero);
	}

	/** Called from Mob.die. */
	public static void onMobDeath(Mob mob, Object cause) {
		Prophet.Quest.onMobDeath(mob);

		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive() || mob.alignment != Char.Alignment.ENEMY) return;

		DeathScythe death = equipped(hero, DeathScythe.class);
		if (death != null && Dungeon.level.heroFOV[mob.pos]) death.onNearbyDeath();

		ConquestCrown crown = equipped(hero, ConquestCrown.class);
		if (crown != null && cause == hero) crown.gainCharge(2f);
	}
}
