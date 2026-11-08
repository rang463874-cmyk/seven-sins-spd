package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeavenSprites;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A priest of the heavens: spawns with other monsters (HeavenLevel adds them). While fighting it heals
 * the allies within 4 tiles and shields them (every 5 turns). Once it is left alone in a fight it
 * offers its own life and a lesser angel takes its place.
 */
public class Priest extends Mob {

	public static final int BLESS_RANGE = 4;
	public static final int ALONE_RANGE = 6;
	public static final int BLESS_COOLDOWN = 5;

	protected int d = 27;
	private int cooldown = 2;

	{
		spriteClass = HeavenSprites.Priest.class;
		EXP = 16;
		maxLvl = 32;
	}

	public Priest() {
		super();
		d = HeavenMobs.depth();
		HP = HT = 2 * d + 5;
		defenseSkill = d + 6;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(d / 4, d / 2);
	}

	@Override
	public int attackSkill(Char target) {
		return d + 8;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, d / 5);
	}

	@Override
	protected boolean act() {
		if (cooldown > 0) cooldown--;
		if (state == HUNTING && paralysed <= 0 && Dungeon.hero != null && Dungeon.hero.isAlive()) {
			ArrayList<Mob> allies = HeavenMobs.alliesNear(this, ALONE_RANGE);
			//alone in a fight (the hero in sight, no one left nearby): the last offering
			if (allies.isEmpty() && fieldOfView != null && fieldOfView[Dungeon.hero.pos]) {
				sacrifice();
				return true;
			}
			if (cooldown <= 0) {
				ArrayList<Mob> near = new ArrayList<>();
				for (Mob m : allies) {
					if (Dungeon.level.distance(pos, m.pos) <= BLESS_RANGE) near.add(m);
				}
				if (!near.isEmpty()) {
					bless(near);
					cooldown = BLESS_COOLDOWN;
					spend(TICK);
					return true;
				}
			}
		}
		return super.act();
	}

	private void bless(ArrayList<Mob> allies) {
		for (Mob m : allies) {
			int heal = Math.min(m.HT - m.HP, Math.max(1, m.HT / 6));
			if (heal > 0) {
				m.HP += heal;
				if (m.sprite != null) m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal),
						com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
			}
			Buff.affect(m, Barrier.class).setShield(5 + d / 2);
			if (m.sprite != null) m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
		}
		if (sprite != null) {
			sprite.operate(pos);
			sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
		}
		if (Dungeon.level.heroFOV[pos]) {
			Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.6f, 1.3f);
			GLog.w(Messages.get(Priest.class, "bless"));
		}
	}

	private void sacrifice() {
		int cell = pos;
		boolean seen = Dungeon.level.heroFOV[cell];
		if (seen) {
			GLog.n(Messages.get(Priest.class, "sacrifice"));
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
		}
		die(this);
		LesserAngel angel = new LesserAngel();
		angel.pos = cell;
		angel.state = angel.HUNTING;
		GameScene.add(angel);
		angel.aggro(Dungeon.hero);
		CellEmitter.get(cell).burst(Speck.factory(Speck.LIGHT), 12);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("depth", d);
		bundle.put("cooldown", cooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		if (bundle.contains("depth")) d = bundle.getInt("depth");
		cooldown = bundle.getInt("cooldown");
	}
}
