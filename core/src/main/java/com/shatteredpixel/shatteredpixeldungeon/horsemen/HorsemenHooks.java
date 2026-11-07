package com.shatteredpixel.shatteredpixeldungeon.horsemen;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Prophet;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DeathScythe;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.FamineScales;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HorsemanArtifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.WarBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WarSword;

/**
 * Game hooks for the four horsemen relics and the sewer prophet's quest.
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

	private static WarSword warSword(Hero hero) {
		if (hero == null) return null;
		return hero.belongings.weapon() instanceof WarSword ? (WarSword) hero.belongings.weapon() : null;
	}

	/** Called from Hero.attackProc. */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		//the old Blade of War artifact, kept for saves
		WarBlade war = equipped(hero, WarBlade.class);
		if (war != null) dmg = war.modifyAttack(hero, dmg);

		FamineScales famine = equipped(hero, FamineScales.class);
		if (famine != null) dmg = famine.modifyAttack(hero, dmg);

		DeathScythe death = equipped(hero, DeathScythe.class);
		if (death != null) death.mark(enemy, dmg);
		return dmg;
	}

	/** Called from Hero.defenseProc. */
	public static int onDefense(Hero hero, Char attacker, int dmg) {
		WarBlade war = equipped(hero, WarBlade.class);
		if (war != null) war.onHurt(hero, dmg);
		return dmg;
	}

	/** Called from Char.damage after HP is lowered: death marks. Returns extra damage dealt. */
	public static int afterDamage(Char ch) {
		if (ch instanceof Hero || ch.buff(DeathScythe.Mark.class) == null) return 0;
		if (equipped(Dungeon.hero, DeathScythe.class) == null) return 0;
		return DeathScythe.reap(ch);
	}

	/** Called from Hero.speed. */
	public static float moveSpeed(Hero hero) {
		FamineScales famine = equipped(hero, FamineScales.class);
		return famine == null ? 1f : famine.moveSpeedMult(hero);
	}

	/** Called from Hero.attackDelay (divides the delay). */
	public static float attackSpeed(Hero hero) {
		FamineScales famine = equipped(hero, FamineScales.class);
		return famine == null ? 1f : famine.attackSpeedMult(hero);
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

		WarSword sword = warSword(hero);
		if (sword != null && cause == hero) sword.onKill(hero, mob);
	}
}
