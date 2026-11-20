package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AscentSprites;
import com.watabou.utils.PathFinder;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

/** Seven Sins: Azrael, the angel of death with the scroll of names (the ascent's angel boss of floor 15). */
public class Azrael extends AscentBoss {

	public static final int MARK_RANGE = 4;

	{
		spriteClass = AscentSprites.Azrael.class;
	}

	@Override protected int skillCount() { return 3; }

	@Override
	protected String skillKey(int s) {
		return s == 0 ? "mark" : s == 1 ? "harvest" : "scythe";
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == 0) return target.buff(DeathMark.class) == null ? new int[0] : null;
		if (s == 1) return Dungeon.level.distance(pos, target.pos) <= 3 ? donut(1, 2) : null;
		return cone(target.pos, 6);
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == 0) {
			if (Dungeon.hero != null && Dungeon.hero.isAlive()) {
				Buff.affect(Dungeon.hero, DeathMark.class, 4f).set(id(), 2 * skillMax());
				GLog.n(Messages.get(Azrael.class, "marked", MARK_RANGE));
			}
			return;
		}
		for (int c : cells) {
			Char ch = strike(c, skillMin(), skillMax());
			if (ch != null && s == 1 && isAlive()) HP = Math.min(HT, HP + HT / 33);
		}
	}

	@Override
	protected int cooldown(int s) {
		return s == 0 ? cd(10) : s == 1 ? cd(4) : cd(4);
	}

	/** the name written in the scroll: when it runs out, a hero still near Azrael takes a heavy blow */
	public static class DeathMark extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		private int source = -1;
		private int power = 0;

		public DeathMark set(int source, int power) {
			this.source = source;
			this.power = power;
			return this;
		}

		@Override
		public boolean act() {
			Actor a = Actor.findById(source);
			if (a instanceof Azrael && ((Azrael) a).isAlive() && target.isAlive()) {
				Azrael az = (Azrael) a;
				if (Dungeon.level.distance(az.pos, target.pos) <= MARK_RANGE) {
					target.damage(power, az);
					if (target.sprite != null) target.sprite.flash();
					if (target == Dungeon.hero && !target.isAlive()) Dungeon.fail(az);
				} else {
					GLog.p(Messages.get(Azrael.class, "escaped"));
				}
			}
			return super.act();
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", MARK_RANGE, dispTurns());
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("source", source);
			bundle.put("power", power);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			source = bundle.getInt("source");
			power = bundle.getInt("power");
		}
	}
}
