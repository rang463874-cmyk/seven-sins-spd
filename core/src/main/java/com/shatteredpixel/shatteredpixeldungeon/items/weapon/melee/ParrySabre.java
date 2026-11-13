package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: a tier 3 parrying weapon. While wielded, "parry" (1 turn) takes a stance for STANCE_TURNS: the next melee
 * attack from an adjacent enemy is turned aside and answered at once with a sure riposte for RIPOSTE_DMG damage.
 * The hooks are in Hero.defenseSkill / defenseVerb.
 */
public class ParrySabre extends MeleeWeapon {

	public static final String AC_PARRY = "PARRY";
	public static final float STANCE_TURNS = 3f;
	public static final float RIPOSTE_DMG = 1.5f;

	{
		image = ItemSpriteSheet.PARRY_SABRE;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.1f;
		tier = 3;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (hero.belongings.weapon == this) actions.add(AC_PARRY);
		return actions;
	}

	@Override
	public String defaultAction() {
		if (Dungeon.hero != null && Dungeon.hero.belongings.weapon == this) return AC_PARRY;
		return super.defaultAction();
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_PARRY)) return Messages.get(this, "ac_parry");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_PARRY)) {
			if (hero.belongings.weapon != this) {
				GLog.w(Messages.get(this, "need_wield"));
				return;
			}
			Buff.prolong(hero, Stance.class, STANCE_TURNS + 1f);
			hero.sprite.operate(hero.pos);
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 0.6f, 1.2f);
			GLog.i(Messages.get(this, "stance"));
			hero.spendAndNext(1f);
		}
	}

	/** from Hero.defenseSkill: true if the attack is parried (a riposte is queued) */
	public static boolean tryParry(Hero hero, Char enemy) {
		Stance s = hero.buff(Stance.class);
		if (s == null || enemy == null || !Dungeon.level.adjacent(hero.pos, enemy.pos)) return false;
		if (!(hero.belongings.weapon instanceof ParrySabre)) {
			s.detach();
			return false;
		}
		s.parried = true;
		if (hero.canAttack(enemy) && !hero.isCharmedBy(enemy)) {
			Buff.affect(hero, Riposte.class).enemy = enemy;
		}
		return true;
	}

	/** from Hero.defenseVerb: the parry text, or null */
	public static String parryVerb(Hero hero) {
		Stance s = hero.buff(Stance.class);
		if (s == null || !s.parried) return null;
		s.detach();
		Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1, Random.Float(0.96f, 1.05f));
		return Messages.get(ParrySabre.class, "parried");
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", (int) STANCE_TURNS, Math.round(RIPOSTE_DMG * 100));
	}

	public static class Stance extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public boolean parried;

		@Override
		public int icon() {
			return BuffIndicator.DUEL_GUARD;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (STANCE_TURNS + 1f - visualcooldown()) / (STANCE_TURNS + 1f));
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.round(RIPOSTE_DMG * 100), dispTurns());
		}
	}

	public static class Riposte extends Buff {

		{
			actPriority = VFX_PRIO;
		}

		public Char enemy;

		@Override
		public boolean act() {
			final Hero hero = (Hero) target;
			detach();
			if (enemy == null || !enemy.isAlive() || hero.sprite == null) return true;
			hero.sprite.attack(enemy.pos, new Callback() {
				@Override
				public void call() {
					AttackIndicator.target(enemy);
					hero.attack(enemy, RIPOSTE_DMG, 0, Char.INFINITE_ACCURACY);
					next();
				}
			});
			return false;
		}
	}
}
