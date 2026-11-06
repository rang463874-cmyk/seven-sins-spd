package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.SealFragment;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The Seal Warden, the sewer prophet's miniboss. It guards the apocalypse's seal, and each
 * time it loses a quarter of its health it takes on the aspect of the next horseman:
 *   Conquest (full HP): heavily armored.
 *   War (below 75%): attacks half again as fast.
 *   Famine (below 50%): its hits weaken the hero and drain satiety.
 *   Death (below 25%): hits much harder against a badly wounded hero.
 * It drops the seal when defeated.
 */
public class SealWarden extends Mob {

	public static final int CONQUEST = 0, WAR = 1, FAMINE = 2, DEATH = 3;
	private static final String[] ASPECTS = {"conquest", "war", "famine", "death"};

	{
		spriteClass = WardenSprite.class;

		HP = HT = 45;
		defenseSkill = 8;

		EXP = 10;
		maxLvl = 12;

		WANDERING = new Wandering();
		state = WANDERING;

		properties.add(Property.MINIBOSS);
		properties.add(Property.INORGANIC);
	}

	public int aspect = CONQUEST;

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2, 6);
	}

	@Override
	public int attackSkill(Char target) {
		return 12;
	}

	@Override
	public int drRoll() {
		int dr = super.drRoll() + Random.NormalIntRange(0, 2);
		//the conqueror's armor
		if (aspect == CONQUEST) dr += Random.NormalIntRange(1, 4);
		return dr;
	}

	@Override
	public float attackDelay() {
		float d = super.attackDelay();
		return aspect == WAR ? d / 1.5f : d;
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		if (aspect == FAMINE && enemy instanceof Hero) {
			Buff.prolong(enemy, Weakness.class, 3f);
			Hunger hunger = enemy.buff(Hunger.class);
			if (hunger != null && !hunger.isStarving()) hunger.affectHunger(-20);
		}
		if (aspect == DEATH && enemy.HP < enemy.HT / 2) {
			damage = Math.round(damage * 1.5f);
		}
		return damage;
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		if (!isAlive()) return;
		int next = HP < HT / 4 ? DEATH : HP < HT / 2 ? FAMINE : HP < HT * 3 / 4 ? WAR : CONQUEST;
		if (next > aspect) {
			aspect = next;
			if (sprite != null) {
				((WardenSprite) sprite).tint(aspect);
				sprite.emitter().burst(Speck.factory(Speck.STAR), 6);
				sprite.showStatus(CharSprite.WARNING, Messages.get(this, "aspect_" + ASPECTS[aspect]));
			}
			yell(Messages.get(this, "yell_" + ASPECTS[aspect]));
			GameScene.flash(0x40000000, false);
			Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		}
	}

	private boolean greeted = false;

	@Override
	public void notice() {
		super.notice();
		if (!greeted) {
			greeted = true;
			yell(Messages.get(this, "notice"));
		}
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		yell(Messages.get(this, "defeated"));
		if (Dungeon.level != null) Dungeon.level.drop(new SealFragment(), pos).sprite.drop();
	}

	@Override
	public String description() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "desc_" + ASPECTS[aspect]);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("aspect", aspect);
		bundle.put("greeted", greeted);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		aspect = bundle.getInt("aspect");
		greeted = bundle.getBoolean("greeted");
	}

	/** placeholder art: an armored statue, tinted by the current horseman's colour */
	public static class WardenSprite extends StatueSprite {

		private static final float[][] COLORS = {
				{1.0f, 1.0f, 1.0f},  //conquest: white
				{1.0f, 0.3f, 0.3f},  //war: red
				{0.35f, 0.35f, 0.4f},//famine: black
				{0.6f, 0.9f, 0.6f},  //death: pale green
		};

		private int aspect = CONQUEST;

		public WardenSprite() {
			super();
			setArmor(3);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			if (ch instanceof SealWarden) aspect = ((SealWarden) ch).aspect;
			tint(aspect);
		}

		public void tint(int aspect) {
			this.aspect = aspect;
			resetColor();
		}

		@Override
		public void resetColor() {
			super.resetColor();
			float[] c = COLORS[Math.max(0, Math.min(3, aspect))];
			hardlight(c[0], c[1], c[2]);
		}
	}
}
