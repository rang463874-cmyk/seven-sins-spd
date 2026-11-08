package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.DemonSprites;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A fly of the lord of flies: summoned as allies by Beelzebub's seal (they rot away after a while), or as
 * enemies by Beelzebub himself. Scales with depth, gives no exp or loot.
 */
public class DemonFly extends Mob {

	{
		spriteClass = DemonSprites.Fly.class;
		flying = true;
		EXP = 0;
		maxLvl = -2;
		properties.add(Property.DEMONIC);
		properties.add(Property.INORGANIC);
	}

	/** turns left for allied flies; -1 = forever */
	public int lifespan = -1;

	public DemonFly() {
		super();
		HP = HT = 8 + 2 * Math.max(1, Dungeon.depth);
		defenseSkill = 4 + Dungeon.depth;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(1 + Dungeon.depth / 2, 3 + Dungeon.depth);
	}

	@Override
	public int attackSkill(Char target) {
		return 10 + 2 * Dungeon.depth;
	}

	@Override
	public float attackDelay() {
		return super.attackDelay() * 0.5f;
	}

	@Override
	protected boolean act() {
		if (lifespan > 0 && --lifespan == 0) {
			die(null);
			return true;
		}
		return super.act();
	}

	public static DemonFly spawnNear(int pos, Alignment side, int lifespan) {
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (c >= 0 && c < Dungeon.level.length() && !Dungeon.level.solid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		if (cells.isEmpty()) return null;
		DemonFly f = new DemonFly();
		f.alignment = side;
		f.lifespan = lifespan;
		f.pos = Random.element(cells);
		f.state = f.HUNTING;
		GameScene.add(f);
		return f;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("life", lifespan);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		lifespan = bundle.getInt("life");
	}
}
