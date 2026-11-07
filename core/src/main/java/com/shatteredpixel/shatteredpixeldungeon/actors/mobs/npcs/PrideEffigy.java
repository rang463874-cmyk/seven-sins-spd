package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.StatueSprite;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * The Paragon's effigy of pride: a golden statue that can't move or fight but draws every enemy
 * within a few tiles to attack it instead. Enemies hitting it don't become insolent, so the hero's
 * kills stay flawless. It doesn't count as an ally for the Paragon's "fighting alone" bonus.
 * Crumbles after its lifespan runs out; only one stands at a time.
 */
public class PrideEffigy extends Mob {

	public static final int TAUNT_RADIUS = 4;

	{
		spriteClass = EffigySprite.class;
		alignment = Alignment.ALLY;
		state = PASSIVE;
		HP = HT = 10;
		defenseSkill = 0;
		EXP = 0;
		properties.add(Property.IMMOVABLE);
		properties.add(Property.INORGANIC);
		immunities.add(AllyBuff.class);
		actPriority = MOB_PRIO - 1;
	}

	public float lifespan = 20;

	public static PrideEffigy create(int heroLvl, int glory, float lifespan) {
		PrideEffigy e = new PrideEffigy();
		e.HT = e.HP = 15 + 5 * heroLvl + glory / 5;
		e.lifespan = lifespan;
		return e;
	}

	public static ArrayList<PrideEffigy> active() {
		ArrayList<PrideEffigy> out = new ArrayList<>();
		for (Char ch : Actor.chars()) if (ch instanceof PrideEffigy && ch.isAlive()) out.add((PrideEffigy) ch);
		return out;
	}

	@Override
	protected boolean act() {
		lifespan -= 1f;
		if (lifespan <= 0) {
			if (sprite != null) sprite.emitter().burst(Speck.factory(Speck.ROCK), 6);
			die(null);
			return true;
		}
		//keep everything nearby fixated on the statue
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Alignment.ENEMY && ch.isAlive()
					&& com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.distance(pos, ch.pos) <= TAUNT_RADIUS) {
				SlothRepose.Taunted.apply(ch, this, 2f);
			}
		}
		spend(TICK);
		return true;
	}

	@Override
	public int defenseSkill(Char enemy) {
		return 0;
	}

	@Override
	public boolean canInteract(Char c) {
		return false;
	}

	@Override
	protected boolean getCloser(int target) {
		return false;
	}

	@Override
	protected boolean getFurther(int target) {
		return false;
	}

	@Override
	public String description() {
		return Messages.get(this, "desc", Math.max(0, Math.round(lifespan)));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("lifespan", lifespan);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		lifespan = bundle.getFloat("lifespan");
	}

	/** placeholder art: a gilded statue */
	public static class EffigySprite extends StatueSprite {
		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(1f, 0.85f, 0.35f);
		}

		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.85f, 0.35f);
		}
	}

}
