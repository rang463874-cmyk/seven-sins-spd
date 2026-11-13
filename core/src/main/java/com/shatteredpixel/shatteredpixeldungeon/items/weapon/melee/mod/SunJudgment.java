package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.mod;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShaftParticle;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * T6 20~44: hits charge the sun (CHARGE_PER_HIT + level each, to 100). Full: AC_SMITE (default action, 1 turn) fires a
 * beam of light up to RANGE tiles through every enemy: SMITE_MULT x a damage roll, ignoring armor, x1.5 against the
 * undead and demons, and blinds.
 */
public class SunJudgment extends ApocalypseWeapon {

	public static final String AC_SMITE = "SMITE";
	public static final float CHARGE_PER_HIT = 20f, SMITE_MULT = 2.5f;
	public static final int RANGE = 8;

	public float charge = 0;

	{
		image = ItemSpriteSheet.SUN_JUDGMENT; hitSound = Assets.Sounds.HIT_STAB; hitSoundPitch = 0.8f;
		baseMin = 20; baseMax = 44;
		usesTargeting = true;
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		if (attacker instanceof Hero && charge < 100) {
			charge = Math.min(100, charge + CHARGE_PER_HIT + buffedLvl());
			if (charge >= 100) GLog.p(Messages.get(SunJudgment.class, "ready"));
			updateQuickslot();
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String status() {
		return Math.round(charge) + "%";
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (hero.belongings.weapon == this) actions.add(AC_SMITE);
		return actions;
	}

	@Override
	public String defaultAction() {
		if (Dungeon.hero != null && Dungeon.hero.belongings.weapon == this && charge >= 100) return AC_SMITE;
		return super.defaultAction();
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_SMITE)) return Messages.get(this, "ac_smite");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_SMITE)) return;
		if (hero.belongings.weapon != this) { GLog.w(Messages.get(this, "need_wield")); return; }
		if (charge < 100) { GLog.w(Messages.get(this, "not_ready", Math.round(charge))); return; }
		curUser = hero;
		curItem = this;
		GameScene.selectCell(aimer);
	}

	private static final CellSelector.Listener aimer = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell != null && curItem instanceof SunJudgment && curUser != null && cell != curUser.pos) {
				((SunJudgment) curItem).smite(curUser, cell);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(SunJudgment.class, "prompt");
		}
	};

	public void smite(Hero hero, int target) {
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_SOLID);
		int last = Math.min(path.dist, RANGE);
		int end = path.path.get(last);
		charge = 0;
		Invisibility.dispel();
		hero.sprite.zap(end);
		hero.sprite.parent.add(new Beam.LightRay(hero.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(end)));
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		for (int i = 1; i <= last; i++) {
			Char ch = Actor.findChar(path.path.get(i));
			if (ch == null || ch == hero || ch.alignment == hero.alignment) continue;
			float mult = SMITE_MULT;
			if (ch.properties().contains(Char.Property.UNDEAD) || ch.properties().contains(Char.Property.DEMONIC)) mult *= 1.5f;
			ch.damage(Math.round(damageRoll(hero) * mult), this);
			if (ch.isAlive()) Buff.prolong(ch, Blindness.class, 3f);
			if (ch.sprite != null) ch.sprite.emitter().start(ShaftParticle.FACTORY, 0.2f, 3);
		}
		updateQuickslot();
		hero.spendAndNext(1f);
	}

	@Override protected Object[] statsArgs() { return new Object[]{Math.round(CHARGE_PER_HIT), Math.round(SMITE_MULT * 100), RANGE, Math.round(charge)}; }
	@Override public void storeInBundle(Bundle b) { super.storeInBundle(b); b.put("charge", charge); }
	@Override public void restoreFromBundle(Bundle b) { super.restoreFromBundle(b); charge = b.getFloat("charge"); }
}
