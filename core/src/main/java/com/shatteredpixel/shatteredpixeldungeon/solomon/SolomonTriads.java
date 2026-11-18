package com.shatteredpixel.shatteredpixeldungeon.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.elite.StoneDust;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: Solomon's three-symbol spells (Nov 18 2026, owner). The order matters:
 * - the LAST symbol is the family: fire = blasts and melting (damage), air = projectiles, spread and mobility,
 *   earth = terrain, barriers and battlefield control, water = status effects, cleansing and wide control;
 * - the MIDDLE symbol picks the core spell of that family (4 x 4 = 16 cores);
 * - the FIRST symbol shapes it: fire = amplified (x1.4 power, +3 mana), air = spread (+1 radius / target / range),
 *   earth = lasting (x1.5 durations), water = cleansing (hit enemies become vulnerable, the caster is cleansed).
 * 16 cores x 4 forms = 64 spells.
 */
public final class SolomonTriads {

	private SolomonTriads() {}

	public enum Core {
		//fire family
		INFERNO(SolomonSpells.Element.FIRE, SolomonSpells.Element.FIRE, 12, SinSkill.TARGET_PROJECTILE),
		MELTDOWN(SolomonSpells.Element.WATER, SolomonSpells.Element.FIRE, 11, SinSkill.TARGET_ENEMY),
		METEOR(SolomonSpells.Element.AIR, SolomonSpells.Element.FIRE, 13, SinSkill.TARGET_ENEMY),
		VOLCANO(SolomonSpells.Element.EARTH, SolomonSpells.Element.FIRE, 12, SinSkill.TARGET_ENEMY),
		//air family
		PHOENIX(SolomonSpells.Element.FIRE, SolomonSpells.Element.AIR, 11, SinSkill.TARGET_ENEMY),
		ICE_VOLLEY(SolomonSpells.Element.WATER, SolomonSpells.Element.AIR, 11, SinSkill.TARGET_NONE),
		TAILWIND(SolomonSpells.Element.AIR, SolomonSpells.Element.AIR, 9, SinSkill.TARGET_NONE),
		SKYFALL(SolomonSpells.Element.EARTH, SolomonSpells.Element.AIR, 10, SinSkill.TARGET_NONE),
		//earth family
		OBSIDIAN(SolomonSpells.Element.FIRE, SolomonSpells.Element.EARTH, 11, SinSkill.TARGET_ENEMY),
		BOG(SolomonSpells.Element.WATER, SolomonSpells.Element.EARTH, 10, SinSkill.TARGET_ENEMY),
		EARTHQUAKE(SolomonSpells.Element.AIR, SolomonSpells.Element.EARTH, 12, SinSkill.TARGET_NONE),
		GREAT_WALL(SolomonSpells.Element.EARTH, SolomonSpells.Element.EARTH, 10, SinSkill.TARGET_NONE),
		//water family
		FEVER(SolomonSpells.Element.FIRE, SolomonSpells.Element.WATER, 10, SinSkill.TARGET_NONE),
		CLEANSING_RAIN(SolomonSpells.Element.WATER, SolomonSpells.Element.WATER, 12, SinSkill.TARGET_NONE),
		TIDAL_WAVE(SolomonSpells.Element.AIR, SolomonSpells.Element.WATER, 11, SinSkill.TARGET_NONE),
		PETRIFY(SolomonSpells.Element.EARTH, SolomonSpells.Element.WATER, 12, SinSkill.TARGET_ENEMY);

		public final SolomonSpells.Element middle, last;
		public final int cost;
		public final int targeting;

		Core(SolomonSpells.Element middle, SolomonSpells.Element last, int cost, int targeting) {
			this.middle = middle;
			this.last = last;
			this.cost = cost;
			this.targeting = targeting;
		}

		/** cast on a chosen tile even without an enemy aim */
		public boolean picksCell() {
			return targeting != SinSkill.TARGET_NONE || this == SKYFALL || this == GREAT_WALL;
		}

		public String title() {
			return Messages.get(SolomonTriads.class, "c_" + name().toLowerCase());
		}

		public static Core of(SolomonSpells.Element middle, SolomonSpells.Element last) {
			for (Core c : values()) if (c.middle == middle && c.last == last) return c;
			return null;
		}
	}

	/** the core and the shaping first symbol */
	public static class Triad {
		public final SolomonSpells.Element form;
		public final Core core;

		Triad(SolomonSpells.Element form, Core core) {
			this.form = form;
			this.core = core;
		}

		public String title() {
			return Messages.get(SolomonTriads.class, "title", core.title(), Messages.get(SolomonTriads.class, "f_" + form.name().toLowerCase()));
		}

		public String desc() {
			return Messages.get(SolomonTriads.class, "c_" + core.name().toLowerCase() + "_desc") + "\n\n"
					+ Messages.get(SolomonTriads.class, "f_" + form.name().toLowerCase() + "_desc");
		}

		public int cost() {
			return core.cost + (form == SolomonSpells.Element.FIRE ? 3 : 0);
		}

		public int targeting() {
			return core.targeting;
		}

		float pow() { return form == SolomonSpells.Element.FIRE ? 1.4f : 1f; }
		int rad() { return form == SolomonSpells.Element.AIR ? 1 : 0; }
		float dur() { return form == SolomonSpells.Element.EARTH ? 1.5f : 1f; }
		boolean cleansing() { return form == SolomonSpells.Element.WATER; }
	}

	public static Triad current(SolomonArcana a) {
		if (a == null || a.circle.size() < 3) return null;
		Core core = Core.of(a.circle.get(1), a.circle.get(2));
		return core == null ? null : new Triad(a.circle.get(0), core);
	}

	// ------------------------------------------------------------------ casting

	private static Triad pending;

	public static void cast(Hero hero) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		Triad t = current(a);
		if (t == null) return;
		if (a.mana < t.cost()) {
			GLog.w(Messages.get(SolomonSpells.class, "no_mana"));
			return;
		}
		if (t.core.picksCell()) {
			pending = t;
			GameScene.selectCell(targeter);
			return;
		}
		if (instant(hero, t)) finish(hero, t, hero.pos);
	}

	private static final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Triad t = pending;
			pending = null;
			if (cell == null || t == null) return;
			Hero hero = Dungeon.hero;
			SolomonArcana a = SolomonSpells.arcana(hero);
			Triad now = current(a);
			if (now == null || now.core != t.core || now.form != t.form || a.mana < t.cost() || cell == hero.pos) return;
			aimed(hero, t, cell);
		}

		@Override
		public String prompt() {
			return Messages.get(SolomonSpells.class, "prompt");
		}
	};

	private static void pay(Hero hero, Triad t) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		if (a != null) {
			a.useMana(t.cost());
			a.circle.clear();
		}
		if (t.cleansing()) {
			for (Buff b : hero.buffs()) {
				if (b.type == Buff.buffType.NEGATIVE && !(b instanceof Hunger)) b.detach();
			}
		}
	}

	private static void finish(Hero hero, Triad t, int pos) {
		pay(hero, t);
		if (pos != hero.pos) hero.sprite.zap(pos);
		else hero.sprite.operate(hero.pos);
		hero.sprite.showStatus(0xCC99FF, t.title());
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	private static float power(Hero hero) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		return a == null ? 1f : a.spellPower();
	}

	private static int roll(Hero hero, Triad t, float mult) {
		int lvl = Math.max(1, hero.lvl);
		return Math.max(1, Math.round(Random.NormalIntRange(3 + lvl / 2, 8 + lvl) * mult * t.pow() * power(hero)));
	}

	/** damages an enemy; the cleansing form leaves it vulnerable */
	private static void hit(Hero hero, Triad t, Char ch, float mult) {
		ch.damage(roll(hero, t, mult), SolomonTriads.class);
		affected(t, ch);
	}

	private static void affected(Triad t, Char ch) {
		if (t.cleansing() && ch.isAlive()) Buff.prolong(ch, Vulnerable.class, 5f);
	}

	private static boolean foe(Char ch) {
		return ch != null && ch != Dungeon.hero && ch.alignment != Char.Alignment.ALLY && ch.isAlive();
	}

	private static boolean boss(Char ch) {
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	private static ArrayList<Char> foesAround(int cell, int radius, boolean seen) {
		ArrayList<Char> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (!foe(m) || Dungeon.level.distance(cell, m.pos) > radius) continue;
			if (seen && !Dungeon.level.heroFOV[m.pos]) continue;
			out.add(m);
		}
		return out;
	}

	private static void heal(Char ch, int amount) {
		amount = Math.min(amount, ch.HT - ch.HP);
		if (amount <= 0) return;
		ch.HP += amount;
		if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	private static boolean instant(Hero hero, Triad t) {
		switch (t.core) {
			case ICE_VOLLEY: {
				ArrayList<Char> targets = foesAround(hero.pos, 8, true);
				if (targets.isEmpty()) {
					GLog.w(Messages.get(SolomonSpells.class, "no_target"));
					return false;
				}
				final Hero h = hero;
				java.util.Collections.sort(targets, (x, y) -> Dungeon.level.distance(h.pos, x.pos) - Dungeon.level.distance(h.pos, y.pos));
				int n = Math.min(targets.size(), 3 + t.rad());
				for (int i = 0; i < n; i++) {
					Char ch = targets.get(i);
					MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.FROST, hero.sprite, ch.pos, null);
					hit(hero, t, ch, 1.2f);
					if (ch.isAlive()) Buff.prolong(ch, Chill.class, 4f * t.dur());
				}
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
				return true;
			}
			case TAILWIND: {
				Buff.prolong(hero, Haste.class, 8f * t.dur());
				Buff.prolong(hero, Levitation.class, 8f * t.dur());
				if (t.rad() > 0) Buff.affect(hero, Invisibility.class, 3f);
				hero.sprite.emitter().burst(Speck.factory(Speck.JET), 8);
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
				return true;
			}
			case EARTHQUAKE: {
				for (Char ch : foesAround(hero.pos, 4 + t.rad(), true)) {
					hit(hero, t, ch, 1.0f);
					if (!ch.isAlive()) continue;
					if (boss(ch)) Buff.prolong(ch, Cripple.class, 4f * t.dur());
					else Buff.prolong(ch, Paralysis.class, 2f * t.dur());
				}
				CellEmitter.get(hero.pos).burst(Speck.factory(Speck.ROCK), 16);
				PixelScene.shake(3, 0.6f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				return true;
			}
			case FEVER: {
				int lvl = Math.max(1, hero.lvl);
				for (Char ch : foesAround(hero.pos, 5 + t.rad(), true)) {
					Buff.affect(ch, Poison.class).set(Math.round((3 + lvl / 2f) * t.pow() * power(hero)));
					Buff.prolong(ch, Weakness.class, 8f * t.dur());
					affected(t, ch);
					CellEmitter.center(ch.pos).burst(Speck.factory(Speck.STENCH), 3);
				}
				Sample.INSTANCE.play(Assets.Sounds.GAS);
				return true;
			}
			case CLEANSING_RAIN: {
				heal(hero, Math.round(hero.HT * 0.3f * t.pow() * power(hero)));
				for (Buff b : hero.buffs()) {
					if (b.type == Buff.buffType.NEGATIVE && !(b instanceof Hunger)) b.detach();
				}
				Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.15f * t.dur()));
				for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
					if (m.alignment == Char.Alignment.ALLY && Dungeon.level.heroFOV[m.pos]
							&& Dungeon.level.distance(hero.pos, m.pos) <= 4 + t.rad()) {
						heal(m, Math.round(m.HT * 0.3f * t.pow()));
					}
				}
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 10);
				Sample.INSTANCE.play(Assets.Sounds.DRINK);
				return true;
			}
			case TIDAL_WAVE: {
				for (Char ch : foesAround(hero.pos, 3 + t.rad(), true)) {
					hit(hero, t, ch, 0.6f);
					if (!ch.isAlive()) continue;
					Buff.prolong(ch, Chill.class, 4f * t.dur());
					if (Char.hasProp(ch, Char.Property.IMMOVABLE)) continue;
					int dx = Integer.signum(Dungeon.level.cellToPoint(ch.pos).x - Dungeon.level.cellToPoint(hero.pos).x);
					int dy = Integer.signum(Dungeon.level.cellToPoint(ch.pos).y - Dungeon.level.cellToPoint(hero.pos).y);
					int far = ch.pos + (dx + dy * Dungeon.level.width()) * 4;
					if (far < 0 || far >= Dungeon.level.length() || far == ch.pos) continue;
					WandOfBlastWave.throwChar(ch, new Ballistica(ch.pos, far, Ballistica.MAGIC_BOLT), 3, true, true, SolomonTriads.class);
				}
				CellEmitter.get(hero.pos).burst(Speck.factory(Speck.STEAM), 14);
				Sample.INSTANCE.play(Assets.Sounds.BLAST, 0.8f, 0.7f);
				return true;
			}
			default:
				return false;
		}
	}

	private static void aimed(final Hero hero, final Triad t, final int cell) {
		switch (t.core) {
			case INFERNO: {
				final int end = new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT).collisionPos;
				pay(hero, t);
				hero.sprite.zap(end);
				hero.busy();
				MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.FIRE, hero.sprite, end, new Callback() {
					@Override
					public void call() {
						for (int c : area(end, 1 + t.rad())) {
							CellEmitter.get(c).burst(FlameParticle.FACTORY, 6);
							Char ch = Actor.findChar(c);
							if (!foe(ch)) continue;
							hit(hero, t, ch, 2.0f);
							if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
						}
						PixelScene.shake(2, 0.4f);
						Sample.INSTANCE.play(Assets.Sounds.BLAST);
						hero.sprite.showStatus(0xCC99FF, t.title());
						Invisibility.dispel();
						hero.spendAndNext(1f);
					}
				});
				return;
			}
			case MELTDOWN: {
				Char ch = target(cell);
				if (ch == null) return;
				hit(hero, t, ch, 2.4f);
				if (ch.isAlive()) {
					Buff.prolong(ch, Vulnerable.class, 6f * t.dur());
					Buff.prolong(ch, Weakness.class, 6f * t.dur());
				}
				CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 10);
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				finish(hero, t, ch.pos);
				return;
			}
			case METEOR: case VOLCANO: case BOG: {
				if (!Dungeon.level.heroFOV[cell] || Dungeon.level.solid[cell]) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				if (t.core == Core.METEOR) {
					ArrayList<Integer> spots = area(cell, 2 + t.rad());
					int n = 4 + t.rad();
					for (int i = 0; i < n && !spots.isEmpty(); i++) {
						int at = i == 0 ? cell : Random.element(spots);
						for (int c : area(at, 1)) {
							CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
							Char ch = Actor.findChar(c);
							if (!foe(ch)) continue;
							hit(hero, t, ch, 0.9f);
							if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
						}
					}
					PixelScene.shake(3, 0.6f);
					Sample.INSTANCE.play(Assets.Sounds.BLAST);
				} else if (t.core == Core.VOLCANO) {
					for (int c : area(cell, 2 + t.rad())) {
						CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
						Char ch = Actor.findChar(c);
						if (!foe(ch)) continue;
						hit(hero, t, ch, 1.5f);
						if (!ch.isAlive()) continue;
						Buff.affect(ch, Burning.class).reignite(ch);
						Buff.prolong(ch, Cripple.class, 4f * t.dur());
					}
					PixelScene.shake(2, 0.5f);
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
				} else {
					for (int c : area(cell, 1 + t.rad())) {
						CellEmitter.get(c).burst(LeafParticle.GENERAL, 4);
						Char ch = Actor.findChar(c);
						if (!foe(ch)) continue;
						hit(hero, t, ch, 0.6f);
						if (!ch.isAlive()) continue;
						Buff.prolong(ch, Roots.class, 5f * t.dur());
						Buff.prolong(ch, Cripple.class, 10f * t.dur());
					}
					Sample.INSTANCE.play(Assets.Sounds.PLANT);
				}
				finish(hero, t, cell);
				return;
			}
			case PHOENIX: {
				Ballistica line = new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID);
				int max = Math.min(line.dist, 8 + 2 * t.rad());
				for (int c : line.subPath(1, max)) {
					CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
					Char ch = Actor.findChar(c);
					if (!foe(ch)) continue;
					hit(hero, t, ch, 1.6f);
					if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				finish(hero, t, line.path.get(Math.max(1, max)));
				return;
			}
			case SKYFALL: {
				if (!Dungeon.level.heroFOV[cell] || !Dungeon.level.passable[cell] || Actor.findChar(cell) != null
						|| Dungeon.level.distance(hero.pos, cell) > 7 + 2 * t.rad()) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				ScrollOfTeleportation.appear(hero, cell);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
				for (Char ch : foesAround(hero.pos, 1 + t.rad(), false)) {
					hit(hero, t, ch, 1.2f);
					if (ch.isAlive() && !boss(ch)) Buff.prolong(ch, Paralysis.class, 1f * t.dur());
				}
				CellEmitter.get(hero.pos).burst(Speck.factory(Speck.ROCK), 10);
				PixelScene.shake(2, 0.3f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				finish(hero, t, hero.pos);
				return;
			}
			case OBSIDIAN: {
				Char ch = target(cell);
				if (ch == null) return;
				hit(hero, t, ch, 1.0f);
				if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
				int raised = 0;
				for (int n : PathFinder.NEIGHBOURS8) {
					int c = ch.pos + n;
					if (c == hero.pos) continue;
					if (StoneDust.raise(Dungeon.level, c, Math.round(10 * t.dur()))) raised++;
				}
				if (raised > 0) {
					Dungeon.observe();
					GameScene.updateFog();
				}
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				finish(hero, t, ch.pos);
				return;
			}
			case GREAT_WALL: {
				if (!Dungeon.level.heroFOV[cell]) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				int dx = Dungeon.level.cellToPoint(cell).x - Dungeon.level.cellToPoint(hero.pos).x;
				int dy = Dungeon.level.cellToPoint(cell).y - Dungeon.level.cellToPoint(hero.pos).y;
				//across the line from the caster: vertical for a sideways aim, horizontal otherwise
				int step = Math.abs(dx) >= Math.abs(dy) ? Dungeon.level.width() : 1;
				int half = 2 + t.rad();
				int raised = 0;
				for (int i = -half; i <= half; i++) {
					int c = cell + i * step;
					if (c < 0 || c >= Dungeon.level.length()) continue;
					if (StoneDust.raise(Dungeon.level, c, Math.round(20 * t.dur()))) raised++;
				}
				if (raised == 0) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				Dungeon.observe();
				GameScene.updateFog();
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				finish(hero, t, cell);
				return;
			}
			case PETRIFY: {
				Char first = target(cell);
				if (first == null) return;
				for (Char ch : foesAround(first.pos, t.rad(), false)) {
					if (boss(ch)) {
						Buff.prolong(ch, Slow.class, 5f * t.dur());
						Buff.prolong(ch, Weakness.class, 5f * t.dur());
					} else {
						Buff.prolong(ch, Paralysis.class, 6f * t.dur());
					}
					affected(t, ch);
					CellEmitter.get(ch.pos).burst(Speck.factory(Speck.ROCK), 6);
				}
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.6f);
				finish(hero, t, first.pos);
				return;
			}
			default:
		}
	}

	private static Char target(int cell) {
		Char ch = Actor.findChar(cell);
		if (!foe(ch) || !Dungeon.level.heroFOV[cell]) {
			GLog.w(Messages.get(SolomonSpells.class, "no_target"));
			return null;
		}
		return ch;
	}

	/** open tiles within a radius of a cell */
	private static ArrayList<Integer> area(int center, int radius) {
		ArrayList<Integer> out = new ArrayList<>();
		int w = Dungeon.level.width();
		int cx = center % w, cy = center / w;
		for (int y = cy - radius; y <= cy + radius; y++) {
			for (int x = cx - radius; x <= cx + radius; x++) {
				if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
				int c = x + y * w;
				if (Dungeon.level.solid[c]) continue;
				out.add(c);
			}
		}
		return out;
	}
}
