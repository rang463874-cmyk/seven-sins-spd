package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.effects.Chains;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GuardSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Prison (floors 6-9): the escaped chief jailer. Throws a chain along a marked line (whoever is
 * still on it is dragged next to it and crippled) and slams the ground around itself (roots).
 */
public class ChainWarden extends RegionElite {

	private static final int CHAIN = 0, SLAM = 1;

	{
		spriteClass = JailerSprite.class;
		HP = HT = 110;
		defenseSkill = 12;
		EXP = 20;
		maxLvl = 17;
	}

	@Override public int damageRoll() { return Random.NormalIntRange(5, 12); }
	@Override public int attackSkill(Char target) { return 20; }
	@Override public int drRoll() { return super.drRoll() + Random.NormalIntRange(0, 6); }

	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == CHAIN ? 5 : 4; }
	@Override protected String skillKey(int s) { return s == CHAIN ? "chain" : "slam"; }

	@Override
	protected int[] prepare(int s, Char target) {
		ArrayList<Integer> cells = new ArrayList<>();
		if (s == SLAM) {
			if (Dungeon.level.distance(pos, target.pos) > 1) return null;
			for (int n : PathFinder.NEIGHBOURS8) addIfOpen(cells, pos + n);
			return toArray(cells);
		}
		int d = Dungeon.level.distance(pos, target.pos);
		if (d < 2 || d > 6) return null;
		Ballistica line = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
		boolean through = false;
		for (int i = 1; i < line.path.size() && cells.size() < 7; i++) {
			int c = line.path.get(i);
			if (Dungeon.level.solid[c]) break;
			cells.add(c);
			if (c == target.pos) { through = true; break; }
		}
		return through ? toArray(cells) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == SLAM) {
			for (int c : cells) {
				CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 3);
				Char ch = strike(c, 4, 9);
				if (ch != null && ch.isAlive()) Buff.prolong(ch, Roots.class, 2f);
			}
			Sample.INSTANCE.play(Assets.Sounds.ROCKS);
			return;
		}
		//chain: the furthest character still on the line gets pulled next to the jailer
		Char victim = null;
		for (int i = cells.length - 1; i >= 0; i--) {
			Char ch = Actor.findChar(cells[i]);
			if (ch != null && ch != this && ch.alignment != alignment) { victim = ch; break; }
		}
		if (victim == null) return;
		int dest = -1;
		for (int c : cells) {
			if (Dungeon.level.adjacent(c, pos) && Dungeon.level.passable[c] && Actor.findChar(c) == null) { dest = c; break; }
		}
		if (sprite != null && sprite.parent != null) {
			sprite.parent.add(new Chains(sprite.center(), DungeonTilemap.raisedTileCenterToWorld(victim.pos), com.shatteredpixel.shatteredpixeldungeon.effects.Effects.Type.CHAIN, null));
		}
		strike(victim.pos, 3, 7);
		if (victim.isAlive()) {
			if (dest != -1 && !victim.properties().contains(Property.IMMOVABLE)) {
				Actor.add(new Pushing(victim, victim.pos, dest));
				victim.pos = dest;
				Dungeon.level.occupyCell(victim);
				if (victim == Dungeon.hero) Dungeon.observe();
			}
			Buff.prolong(victim, Cripple.class, 3f);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHAINS);
	}

	public static class JailerSprite extends GuardSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			hardlight(1f, 0.85f, 0.4f);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			hardlight(1f, 0.85f, 0.4f);
		}
	}
}
