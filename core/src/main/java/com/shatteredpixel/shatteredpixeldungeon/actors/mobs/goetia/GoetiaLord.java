package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.elites.RegionElite;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GoetiaSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: a demon king of the Goetia, fought in the hidden room of a boss floor (5..25). Telegraphed
 * skills like the other big fights: Hellfire (ring of 2 around it), Lance (a line of 8 toward the hero),
 * Legion (two lesser demons), and below half HP Doom (3x3 on the hero). Its blows carry its kind's effect.
 * Death: codex entry, a scroll of upgrade, the region reward and two essences; the stairs back open.
 */
public class GoetiaLord extends RegionElite {

	private static final int HELLFIRE = 0, LANCE = 1, LEGION = 2, DOOM = 3;

	{
		spriteClass = GoetiaSprite.class;
		properties.add(Property.DEMONIC);
		properties.add(Property.BOSS);
	}

	public GoetiaDemon demon = GoetiaDemon.PURSON;
	public int depth = 5;

	{
		GoetiaDemon d = GoetiaDemon.lordOf(Math.max(5, Math.min(25, (Dungeon.depth + 4) / 5 * 5)));
		setup(d == null ? GoetiaDemon.PURSON : d, Math.max(5, Dungeon.depth));
	}

	public GoetiaLord setup(GoetiaDemon d, int depth) {
		demon = d;
		this.depth = Math.max(5, depth);
		HP = HT = 120 + 36 * this.depth;
		defenseSkill = 10 + this.depth;
		EXP = 10 + this.depth;
		maxLvl = 40;
		return this;
	}

	private int d() {
		return 4 + Math.round(depth * 0.9f);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(d(), d() * 2);
	}

	@Override
	public int attackSkill(Char target) {
		return 15 + Math.round(depth * 1.5f);
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(depth / 4, depth / 2);
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		GoetiaFiend.applyKind(this, enemy, demon.kind, depth, damage);
		return damage;
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(soften(dmg, softThreshold(HT)), src);
	}

	@Override
	protected int skillCount() {
		return HP < HT / 2 ? 4 : 3;
	}

	@Override
	protected int cooldown(int skill) {
		return HP < HT / 2 ? 2 : 3;
	}

	@Override
	protected String skillKey(int skill) {
		switch (skill) {
			case HELLFIRE: return "hellfire";
			case LANCE: return "lance";
			case LEGION: return "legion";
			default: return "doom";
		}
	}

	@Override
	protected int[] prepare(int s, Char target) {
		ArrayList<Integer> cells = new ArrayList<>();
		switch (s) {
			case HELLFIRE:
				if (Dungeon.level.distance(pos, target.pos) > 3) return null;
				for (int i = 0; i < Dungeon.level.length(); i++) {
					int dist = Dungeon.level.distance(pos, i);
					if (dist >= 1 && dist <= 2) addIfOpen(cells, i);
				}
				break;
			case LANCE:
				Ballistica b = new Ballistica(pos, target.pos, Ballistica.STOP_SOLID);
				for (int c : b.subPath(1, Math.min(8, b.dist))) addIfOpen(cells, c);
				break;
			case LEGION:
				if (minions() >= 2) return null;
				return new int[0];
			default:
				//doom: a fan of hellfire toward the target
				if (Dungeon.level.distance(pos, target.pos) > 7) return null;
				return cone(target.pos, 7);
		}
		return cells.isEmpty() ? null : toArray(cells);
	}

	@Override
	protected void release(int s, int[] cells) {
		int d = d();
		switch (s) {
			case HELLFIRE:
				for (int c : cells) {
					CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
					Char ch = strike(c, d, d * 2);
					if (ch != null && ch.isAlive() && !Dungeon.level.water[c]) Buff.affect(ch, Burning.class).reignite(ch);
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				break;
			case LANCE:
				for (int c : cells) {
					CellEmitter.center(c).burst(Speck.factory(Speck.STAR), 3);
					Char ch = strike(c, d, d * 2);
					if (ch != null) GoetiaFiend.applyKind(this, ch, demon.kind, depth, d);
				}
				Sample.INSTANCE.play(Assets.Sounds.RAY);
				break;
			case LEGION:
				summonMinions();
				break;
			default:
				for (int c : cells) {
					CellEmitter.get(c).burst(Speck.factory(Speck.ROCK), 4);
					Char ch = strike(c, d * 2, d * 3);
					if (ch != null) GoetiaFiend.applyKind(this, ch, demon.kind, depth, d);
				}
				PixelScene.shake(3, 0.5f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
		}
	}

	private int minions() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m instanceof GoetiaFiend && ((GoetiaFiend) m).minion && m.isAlive()) n++;
		return n;
	}

	private void summonMinions() {
		ArrayList<GoetiaDemon> pool = GoetiaDemon.ofRegion(Math.min(4, (depth - 1) / 5));
		for (int i = 0; i < 2; i++) {
			ArrayList<Integer> free = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) free.add(c);
			}
			if (free.isEmpty() || pool.isEmpty()) return;
			GoetiaFiend f = new GoetiaFiend().setup(Random.element(pool), depth);
			f.minion = true;
			f.HP = f.HT = f.HT / 2;
			f.EXP = 0;
			f.pos = Random.element(free);
			f.state = f.HUNTING;
			GameScene.add(f);
			CellEmitter.get(f.pos).burst(Speck.factory(Speck.SMOKE), 6);
		}
		if (sprite != null) sprite.showStatus(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.WARNING,
				Messages.get(this, "legion_call"));
	}

	@Override
	public void die(Object cause) {
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m instanceof GoetiaFiend && ((GoetiaFiend) m).minion) m.die(null);
		}
		super.die(cause);
		Dungeon.level.drop(new ScrollOfUpgrade(), pos).sprite.drop();
		Dungeon.level.drop(new DemonEssence().quantity(2), pos).sprite.drop();
		GoetiaHooks.onLordDefeated(this);
		yell(Messages.get(this, "defeated"));
	}

	@Override
	protected String noticeLine() {
		return Messages.get(GoetiaLord.class, "notice", demon.rankName(), demon.title());
	}

	@Override
	public String name() {
		return Messages.get(GoetiaFiend.class, "name", demon.rankName(), demon.title());
	}

	@Override
	public String description() {
		return Messages.get(GoetiaLord.class, "desc", demon.number(), demon.rankName(), demon.title())
				+ "\n\n" + demon.kindDesc();
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("demon", demon.name());
		bundle.put("depth", depth);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			demon = GoetiaDemon.valueOf(bundle.getString("demon"));
		} catch (Exception e) {
			demon = GoetiaDemon.PURSON;
		}
		depth = bundle.getInt("depth");
		int hp = HP, ht = HT;
		setup(demon, depth);
		HP = hp;
		HT = ht;
	}
}
