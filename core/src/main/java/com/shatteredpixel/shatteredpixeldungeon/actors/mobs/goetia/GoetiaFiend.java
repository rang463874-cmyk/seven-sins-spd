package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.GoetiaSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: one of Solomon's demons roaming a regular floor (2-24). Much stronger than the floor's
 * monsters; its {@link GoetiaDemon.Kind} decides how it fights. Killing it fills the run's codex
 * ({@link GoetiaHooks}) and drops demon essences. Minions summoned by a lord don't count.
 */
public class GoetiaFiend extends Mob {

	{
		spriteClass = GoetiaSprite.class;
		properties.add(Property.DEMONIC);
		HUNTING = new Hunting();
	}

	public GoetiaDemon demon = GoetiaDemon.MARBAS;
	public int depth = 1;
	/** summoned by a lord: no codex entry, no essence */
	public boolean minion = false;
	private int cooldown = 0;
	private int healCooldown = 0;
	private int blinkCooldown = 0;

	{
		//made by reflection (dev summons, the bestiary): a random demon of the current floor's region
		java.util.ArrayList<GoetiaDemon> pool = GoetiaDemon.ofRegion(Math.min(4, Math.max(0, (Dungeon.depth - 1) / 5)));
		setup(pool.isEmpty() ? GoetiaDemon.MARBAS : Random.element(pool), Math.max(1, Dungeon.depth));
	}

	public GoetiaFiend setup(GoetiaDemon d, int depth) {
		demon = d;
		this.depth = Math.max(1, depth);
		HP = HT = 20 + 9 * this.depth;
		defenseSkill = 5 + this.depth;
		if (d.kind == GoetiaDemon.Kind.EVASIVE) defenseSkill *= 2;
		EXP = 4 + this.depth / 2;
		maxLvl = this.depth + 8;
		return this;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange(2 + depth / 2, 5 + Math.round(depth * 1.1f));
	}

	@Override
	public int attackSkill(Char target) {
		return 12 + Math.round(depth * 1.3f);
	}

	@Override
	public int drRoll() {
		int dr = super.drRoll() + Random.NormalIntRange(0, depth / 3);
		if (demon.kind == GoetiaDemon.Kind.ARMOR) dr += Random.NormalIntRange(depth / 3, depth / 2 + 2);
		return dr;
	}

	@Override
	protected boolean canAttack(Char enemy) {
		if (demon.kind.ranged()) {
			return new Ballistica(pos, enemy.pos, Ballistica.MAGIC_BOLT).collisionPos == enemy.pos
					&& Dungeon.level.distance(pos, enemy.pos) <= 6;
		}
		if (demon.kind == GoetiaDemon.Kind.LANCE && Dungeon.level.distance(pos, enemy.pos) <= 2) {
			return new Ballistica(pos, enemy.pos, Ballistica.PROJECTILE).collisionPos == enemy.pos;
		}
		return super.canAttack(enemy);
	}

	@Override
	protected boolean doAttack(Char enemy) {
		if (!demon.kind.ranged() || Dungeon.level.adjacent(pos, enemy.pos)) return super.doAttack(enemy);
		spend(attackDelay());
		final Char target = enemy;
		if (sprite != null && (sprite.visible || target.sprite.visible)) {
			sprite.turnTo(pos, target.pos);
			if (demon.kind == GoetiaDemon.Kind.LIGHTNING) {
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				arcs.add(new Lightning.Arc(sprite.center(), target.sprite.center()));
				sprite.parent.add(new Lightning(arcs, new Callback() {
					@Override
					public void call() {
						attack(target);
						next();
					}
				}));
			} else {
				MagicMissile.boltFromChar(sprite.parent, missileType(), sprite, target.pos, new Callback() {
					@Override
					public void call() {
						attack(target);
						next();
					}
				});
			}
			return false;
		}
		attack(target);
		return true;
	}

	private int missileType() {
		switch (demon.kind) {
			case FIRE: return MagicMissile.FIRE;
			case FROST: return MagicMissile.FROST;
			default: return MagicMissile.SHADOW;
		}
	}

	@Override
	public int attackProc(Char enemy, int damage) {
		damage = super.attackProc(enemy, damage);
		applyKind(this, enemy, demon.kind, depth, damage);
		return damage;
	}

	/** the on-hit effect of a demon kind (shared with the lords) */
	public static void applyKind(Char src, Char enemy, GoetiaDemon.Kind kind, int depth, int damage) {
		if (enemy == null || !enemy.isAlive()) return;
		switch (kind) {
			case POISON:
				if (Random.Int(2) == 0) Buff.affect(enemy, Poison.class).extend(2 + depth / 3f);
				break;
			case WEAKEN:
				if (Random.Int(3) == 0) Buff.prolong(enemy, Weakness.class, 5f);
				break;
			case CRIPPLE:
				if (Random.Int(3) == 0) Buff.prolong(enemy, Cripple.class, 3f);
				break;
			case BLEED: case ARROW:
				if (Random.Int(5) < 2) Buff.affect(enemy, Bleeding.class).set(Math.max(1, damage / 2f));
				break;
			case BURN: case FIRE:
				if (Random.Int(3) == 0 && !Dungeon.level.water[enemy.pos]) Buff.affect(enemy, Burning.class).reignite(enemy);
				break;
			case FROST:
				Buff.prolong(enemy, Chill.class, 3f);
				break;
			case LIGHTNING:
				if (Random.Int(4) == 0) Buff.prolong(enemy, Paralysis.class, 1f);
				break;
			case CHARM:
				if (Random.Int(5) == 0) {
					Charm c = Buff.affect(enemy, Charm.class, 3f);
					if (c != null) c.object = src.id();
				}
				break;
			case BLIND:
				if (Random.Int(4) == 0) Buff.prolong(enemy, Blindness.class, 3f);
				break;
			case CONFUSE:
				if (Random.Int(4) == 0) Buff.prolong(enemy, Vertigo.class, 3f);
				break;
			case FEAR:
				if (Random.Int(7) == 0) {
					Terror t = Buff.affect(enemy, Terror.class, 3f);
					if (t != null) t.object = src.id();
				}
				break;
			default:
				break;
		}
		if (kind == GoetiaDemon.Kind.CORRODE && Random.Int(10) < 3) {
			Buff.affect(enemy, Corrosion.class).set(3f, Math.max(1, depth / 3));
		}
	}

	@Override
	protected boolean act() {
		if (cooldown > 0) cooldown--;
		if (healCooldown > 0) healCooldown--;
		if (blinkCooldown > 0) blinkCooldown--;
		if (demon.kind == GoetiaDemon.Kind.HEAL && state == HUNTING && HP < HT / 2 && healCooldown <= 0) {
			HP = Math.min(HT, HP + HT / 4);
			healCooldown = 15;
			if (sprite != null) {
				sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);
				sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "heals"));
			}
		}
		return super.act();
	}

	@Override
	public void damage(int dmg, Object src) {
		super.damage(dmg, src);
		if (isAlive() && demon.kind == GoetiaDemon.Kind.BLINK && blinkCooldown <= 0 && Random.Int(10) < 3) {
			ArrayList<Integer> cells = new ArrayList<>();
			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (Dungeon.level.distance(pos, i) <= 4 && Dungeon.level.distance(pos, i) >= 2
						&& Dungeon.level.passable[i] && Actor.findChar(i) == null) cells.add(i);
			}
			if (!cells.isEmpty()) {
				blinkCooldown = 6;
				ScrollOfTeleportation.appear(this, Random.element(cells));
			}
		}
	}

	protected class Hunting extends Mob.Hunting {
		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (demon.kind == GoetiaDemon.Kind.SUMMON && enemyInFOV && cooldown <= 0 && summon()) {
				cooldown = 12;
				spend(TICK);
				return true;
			}
			return super.act(enemyInFOV, justAlerted);
		}
	}

	/** calls up to two of the floor's own monsters beside it */
	private boolean summon() {
		int alive = 0;
		for (Mob m : Dungeon.level.mobs) if (m.buff(Summoned.class) != null && ((Summoned) m.buff(Summoned.class)).by == id()) alive++;
		if (alive >= 2) return false;
		Mob m = Dungeon.level.createMob();
		if (m == null) return false;
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null) cells.add(c);
		}
		if (cells.isEmpty()) return false;
		m.pos = Random.element(cells);
		m.state = m.HUNTING;
		GameScene.add(m);
		Buff.affect(m, Summoned.class).by = id();
		CellEmitter.get(m.pos).burst(Speck.factory(Speck.WOOL), 6);
		if (sprite != null) sprite.showStatus(CharSprite.WARNING, Messages.get(this, "summons"));
		return true;
	}

	/** marks a demon's summon */
	public static class Summoned extends Buff {
		public int by = -1;

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("by", by);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			by = bundle.getInt("by");
		}
	}

	@Override
	public void die(Object cause) {
		super.die(cause);
		if (!minion) GoetiaHooks.onFiendKilled(this);
	}

	@Override
	public String name() {
		return Messages.get(GoetiaFiend.class, "name", demon.rankName(), demon.title());
	}

	@Override
	public String description() {
		return Messages.get(GoetiaFiend.class, "desc", demon.number(), demon.rankName(), demon.title())
				+ "\n\n" + demon.kindDesc()
				+ (GoetiaHooks.slain(demon) ? "\n\n" + Messages.get(GoetiaFiend.class, "known") : "");
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("demon", demon.name());
		bundle.put("depth", depth);
		bundle.put("minion", minion);
		bundle.put("cd", cooldown);
		bundle.put("heal_cd", healCooldown);
		bundle.put("blink_cd", blinkCooldown);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		try {
			demon = GoetiaDemon.valueOf(bundle.getString("demon"));
		} catch (Exception e) {
			demon = GoetiaDemon.MARBAS;
		}
		depth = bundle.getInt("depth");
		minion = bundle.getBoolean("minion");
		cooldown = bundle.getInt("cd");
		healCooldown = bundle.getInt("heal_cd");
		blinkCooldown = bundle.getInt("blink_cd");
		int hp = HP, ht = HT;
		setup(demon, depth);
		HP = hp;
		HT = ht;
	}
}
