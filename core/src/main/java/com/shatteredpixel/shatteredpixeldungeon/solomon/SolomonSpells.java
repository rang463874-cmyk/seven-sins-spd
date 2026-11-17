package com.shatteredpixel.shatteredpixeldungeon.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaFiend;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Lightning;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaHooks;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;

/**
 * Seven Sins: Solomon's magic (Nov 17 2026). Tapping an element symbol on the skill bar draws it into the
 * circle (free, no turn; at most two, the oldest drops out); Cast fires the spell of the drawn symbols
 * (one symbol = a basic spell, two = a combined one) and clears the circle. Summon calls one of the demons
 * recorded in this run's Lemegeton as a timed ally.
 */
public final class SolomonSpells {

	private SolomonSpells() {}

	public enum Element {
		FIRE, WATER, AIR, EARTH;

		/** skill_icons.png row 19 */
		public int icon() {
			return 152 + ordinal();
		}

		public String title() {
			return Messages.get(SolomonSpells.class, "el_" + name().toLowerCase());
		}
	}

	public enum Spell {
		FIRE_BOLT(Element.FIRE, null, 3, SinSkill.TARGET_PROJECTILE),
		WATER_LASH(Element.WATER, null, 3, SinSkill.TARGET_PROJECTILE),
		WIND_BLADE(Element.AIR, null, 3, SinSkill.TARGET_ENEMY),
		STONE_SKIN(Element.EARTH, null, 3, SinSkill.TARGET_NONE),
		FIREBALL(Element.FIRE, Element.FIRE, 7, SinSkill.TARGET_PROJECTILE),
		SPRING(Element.WATER, Element.WATER, 8, SinSkill.TARGET_NONE),
		GUST(Element.AIR, Element.AIR, 5, SinSkill.TARGET_NONE),
		EARTH_ARMOR(Element.EARTH, Element.EARTH, 6, SinSkill.TARGET_NONE),
		STEAM(Element.FIRE, Element.WATER, 6, SinSkill.TARGET_PROJECTILE),
		CHAIN_LIGHTNING(Element.FIRE, Element.AIR, 7, SinSkill.TARGET_ENEMY),
		MAGMA(Element.FIRE, Element.EARTH, 7, SinSkill.TARGET_ENEMY),
		BLIZZARD(Element.WATER, Element.AIR, 7, SinSkill.TARGET_NONE),
		VINES(Element.WATER, Element.EARTH, 6, SinSkill.TARGET_ENEMY),
		SANDSTORM(Element.AIR, Element.EARTH, 6, SinSkill.TARGET_NONE);

		public final Element a, b;
		public final int cost;
		public final int targeting;

		Spell(Element a, Element b, int cost, int targeting) {
			this.a = a;
			this.b = b;
			this.cost = cost;
			this.targeting = targeting;
		}

		public String title() {
			return Messages.get(SolomonSpells.class, "sp_" + name().toLowerCase());
		}

		public String desc() {
			return Messages.get(SolomonSpells.class, "sp_" + name().toLowerCase() + "_desc");
		}

		/** needs a cell even though it doesn't aim at an enemy */
		public boolean picksCell() {
			return this == GUST || targeting != SinSkill.TARGET_NONE;
		}
	}

	public static final int SUMMON_COST = 12;
	public static final float SUMMON_TURNS = 30f;
	public static final int SUMMON_CHOICES = 6;

	public static SolomonArcana arcana(Hero hero) {
		return hero == null ? null : hero.buff(SolomonArcana.class);
	}

	/** the spell of the drawn symbols, or null */
	public static Spell current(SolomonArcana a) {
		if (a == null || a.circle.isEmpty()) return null;
		Element x = a.circle.get(0);
		Element y = a.circle.size() > 1 ? a.circle.get(1) : null;
		for (Spell s : Spell.values()) {
			if (y == null) {
				if (s.b == null && s.a == x) return s;
			} else if (s.b != null && ((s.a == x && s.b == y) || (s.a == y && s.b == x))) {
				return s;
			}
		}
		return null;
	}

	public static String circleText(SolomonArcana a) {
		if (a.circle.isEmpty()) return Messages.get(SolomonSpells.class, "circle_empty");
		StringBuilder sb = new StringBuilder();
		for (Element e : a.circle) {
			if (sb.length() > 0) sb.append(" + ");
			sb.append(e.title());
		}
		Spell s = current(a);
		if (s != null) sb.append(" = ").append(s.title());
		return sb.toString();
	}

	public static int cost(Hero hero, Spell s) {
		return s == null ? 0 : s.cost;
	}

	// ------------------------------------------------------------------ drawing and casting

	public static void draw(Hero hero, Element e) {
		SolomonArcana a = arcana(hero);
		if (a == null) return;
		a.draw(e);
		Spell s = current(a);
		hero.sprite.showStatus(0xCC99FF, s == null ? e.title() : s.title());
		Sample.INSTANCE.play(Assets.Sounds.CHARGEUP, 0.5f, 1.3f + 0.1f * e.ordinal());
	}

	private static Spell pending;

	public static void cast(Hero hero) {
		SolomonArcana a = arcana(hero);
		Spell s = current(a);
		if (s == null) {
			GLog.w(Messages.get(SolomonSpells.class, "no_spell"));
			return;
		}
		if (a.mana < cost(hero, s)) {
			GLog.w(Messages.get(SolomonSpells.class, "no_mana"));
			return;
		}
		if (s.picksCell()) {
			pending = s;
			GameScene.selectCell(targeter);
			return;
		}
		if (instant(hero, s)) finish(hero, s, hero.pos);
	}

	private static final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Spell s = pending;
			pending = null;
			if (cell == null || s == null) return;
			Hero hero = Dungeon.hero;
			SolomonArcana a = arcana(hero);
			if (a == null || current(a) != s || a.mana < cost(hero, s)) return;
			if (cell == hero.pos) return;
			aimed(hero, s, cell);
		}

		@Override
		public String prompt() {
			return Messages.get(SolomonSpells.class, "prompt");
		}
	};

	private static void finish(Hero hero, Spell s, int pos) {
		SolomonArcana a = arcana(hero);
		if (a != null) {
			a.useMana(cost(hero, s));
			a.circle.clear();
		}
		if (pos != hero.pos) hero.sprite.zap(pos);
		else hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	private static float power(Hero hero) {
		SolomonArcana a = arcana(hero);
		return a == null ? 1f : a.spellPower();
	}

	/** a basic spell damage roll before the spell's own multiplier */
	private static int roll(Hero hero, float mult) {
		int lvl = Math.max(1, hero.lvl);
		return Math.max(1, Math.round(Random.NormalIntRange(2 + lvl / 2, 6 + lvl) * mult * power(hero)));
	}

	private static boolean foe(Char ch) {
		return ch != null && ch != Dungeon.hero && ch.alignment != Char.Alignment.ALLY && ch.isAlive();
	}

	private static boolean boss(Char ch) {
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	private static ArrayList<Char> foesAround(int cell, int radius) {
		ArrayList<Char> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (foe(m) && Dungeon.level.distance(cell, m.pos) <= radius) out.add(m);
		}
		return out;
	}

	private static ArrayList<Char> foesInSight(Hero hero, int radius) {
		ArrayList<Char> out = new ArrayList<>();
		for (Char ch : foesAround(hero.pos, radius)) if (Dungeon.level.heroFOV[ch.pos]) out.add(ch);
		return out;
	}

	private static void heal(Hero hero, int amount) {
		amount = Math.min(amount, hero.HT - hero.HP);
		if (amount <= 0) return;
		hero.HP += amount;
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	/** spells without a target; false = nothing happened */
	private static boolean instant(Hero hero, Spell s) {
		switch (s) {
			case STONE_SKIN: {
				int sh = Math.round((4 + hero.lvl) * power(hero));
				Buff.affect(hero, Barrier.class).incShield(sh);
				hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(sh));
				CellEmitter.get(hero.pos).burst(Speck.factory(Speck.ROCK), 6);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 0.5f);
				return true;
			}
			case SPRING: {
				heal(hero, Math.round(hero.HT * 0.2f * power(hero)));
				Buff.detach(hero, Poison.class);
				Buff.detach(hero, Burning.class);
				Buff.detach(hero, Ooze.class);
				Buff.detach(hero, Bleeding.class);
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 6);
				Sample.INSTANCE.play(Assets.Sounds.DRINK);
				return true;
			}
			case EARTH_ARMOR: {
				int lvl = Math.round((2 + hero.lvl / 2f) * power(hero));
				Buff.affect(hero, Barkskin.class).set(lvl, 3);
				CellEmitter.bottom(hero.pos).burst(LeafParticle.GENERAL, 10);
				Sample.INSTANCE.play(Assets.Sounds.PLANT);
				return true;
			}
			case BLIZZARD: {
				for (Char ch : foesInSight(hero, 2)) {
					ch.damage(roll(hero, 0.7f), SolomonSpells.class);
					if (!ch.isAlive()) continue;
					if (boss(ch)) Buff.prolong(ch, Chill.class, 3f);
					else Buff.affect(ch, Frost.class, 2f);
				}
				CellEmitter.get(hero.pos).burst(Speck.factory(Speck.STEAM), 12);
				Sample.INSTANCE.play(Assets.Sounds.SHATTER);
				return true;
			}
			case SANDSTORM: {
				for (Char ch : foesInSight(hero, 3)) {
					Buff.prolong(ch, Blindness.class, 4f);
					Buff.prolong(ch, Cripple.class, 4f);
					CellEmitter.get(ch.pos).burst(Speck.factory(Speck.DUST), 4);
				}
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
				return true;
			}
			default:
				return false;
		}
	}

	/** spells aimed at a cell */
	private static void aimed(final Hero hero, final Spell s, final int cell) {
		switch (s) {
			case GUST: {
				if (!Dungeon.level.heroFOV[cell] || !Dungeon.level.passable[cell] || Actor.findChar(cell) != null
						|| Dungeon.level.distance(hero.pos, cell) > 6) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				ScrollOfTeleportation.appear(hero, cell);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
				finish(hero, s, hero.pos);
				return;
			}
			case FIRE_BOLT: case WATER_LASH: case FIREBALL: case STEAM: {
				final Ballistica path = new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT);
				final int end = path.collisionPos;
				int type = s == Spell.WATER_LASH ? MagicMissile.FROST : s == Spell.STEAM ? MagicMissile.MAGIC_MISSILE : MagicMissile.FIRE;
				SolomonArcana a = arcana(hero);
				a.useMana(cost(hero, s));
				a.circle.clear();
				hero.sprite.zap(end);
				hero.busy();
				MagicMissile.boltFromChar(hero.sprite.parent, type, hero.sprite, end, new Callback() {
					@Override
					public void call() {
						bolt(hero, s, end);
						Invisibility.dispel();
						hero.spendAndNext(1f);
					}
				});
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
				return;
			}
			case WIND_BLADE: {
				Ballistica line = new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID);
				int max = Math.min(line.dist, 6);
				for (int c : line.subPath(1, max)) {
					Char ch = Actor.findChar(c);
					if (foe(ch)) ch.damage(roll(hero, 0.7f), SolomonSpells.class);
					CellEmitter.get(c).burst(Speck.factory(Speck.JET), 1);
				}
				Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 0.8f);
				finish(hero, s, line.path.get(Math.max(1, max)));
				return;
			}
			case MAGMA: {
				Ballistica line = new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID);
				int max = Math.min(line.dist, 7);
				for (int c : line.subPath(1, max)) {
					Char ch = Actor.findChar(c);
					if (foe(ch)) {
						ch.damage(roll(hero, 1.3f), SolomonSpells.class);
						if (ch.isAlive()) Buff.affect(ch, Burning.class).reignite(ch);
					}
					CellEmitter.get(c).burst(FlameParticle.FACTORY, 4);
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				finish(hero, s, line.path.get(Math.max(1, max)));
				return;
			}
			case CHAIN_LIGHTNING: {
				Char first = Actor.findChar(cell);
				if (!foe(first) || !Dungeon.level.heroFOV[cell]) {
					GLog.w(Messages.get(SolomonSpells.class, "no_target"));
					return;
				}
				ArrayList<Char> hit = new ArrayList<>();
				hit.add(first);
				Char last = first;
				for (int i = 0; i < 2; i++) {
					Char next = null;
					for (Char ch : foesAround(last.pos, 2)) {
						if (!hit.contains(ch) && Dungeon.level.heroFOV[ch.pos]) { next = ch; break; }
					}
					if (next == null) break;
					hit.add(next);
					last = next;
				}
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				int from = hero.pos;
				for (Char ch : hit) {
					arcs.add(new Lightning.Arc(from, ch.pos));
					from = ch.pos;
				}
				hero.sprite.parent.addToFront(new Lightning(arcs, null));
				for (Char ch : hit) ch.damage(roll(hero, 1.1f), SolomonSpells.class);
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				finish(hero, s, first.pos);
				return;
			}
			case VINES: {
				if (!Dungeon.level.heroFOV[cell] || Dungeon.level.solid[cell]) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				for (int n : PathFinder.NEIGHBOURS9) {
					int c = cell + n;
					if (!Dungeon.level.insideMap(c) || Dungeon.level.solid[c]) continue;
					CellEmitter.get(c).burst(LeafParticle.GENERAL, 4);
					Char ch = Actor.findChar(c);
					if (foe(ch)) {
						ch.damage(roll(hero, 0.5f), SolomonSpells.class);
						if (ch.isAlive()) Buff.prolong(ch, Roots.class, 4f);
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.PLANT);
				finish(hero, s, cell);
				return;
			}
			default:
		}
	}

	/** a bolt spell reaching its end cell */
	private static void bolt(Hero hero, Spell s, int end) {
		switch (s) {
			case FIRE_BOLT: {
				Char ch = Actor.findChar(end);
				if (foe(ch)) {
					ch.damage(roll(hero, 1f), SolomonSpells.class);
					if (ch.isAlive() && Random.Int(2) == 0) Buff.affect(ch, Burning.class).reignite(ch);
				}
				return;
			}
			case WATER_LASH: {
				Char ch = Actor.findChar(end);
				if (foe(ch)) {
					ch.damage(roll(hero, 0.8f), SolomonSpells.class);
					if (ch.isAlive()) Buff.prolong(ch, Chill.class, 3f);
				}
				return;
			}
			case FIREBALL: case STEAM: {
				for (int n : PathFinder.NEIGHBOURS9) {
					int c = end + n;
					if (!Dungeon.level.insideMap(c)) continue;
					if (s == Spell.FIREBALL) CellEmitter.get(c).burst(FlameParticle.FACTORY, 5);
					else CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), 3);
					Char ch = Actor.findChar(c);
					if (!foe(ch)) continue;
					ch.damage(roll(hero, s == Spell.FIREBALL ? 1.2f : 0.8f), SolomonSpells.class);
					if (!ch.isAlive()) continue;
					if (s == Spell.FIREBALL) Buff.affect(ch, Burning.class).reignite(ch);
					else Buff.prolong(ch, Blindness.class, 3f);
				}
				Sample.INSTANCE.play(s == Spell.FIREBALL ? Assets.Sounds.BLAST : Assets.Sounds.GAS);
				return;
			}
			default:
		}
	}

	// ------------------------------------------------------------------ summoning

	/** the strongest demons of this run's Lemegeton, kings first */
	public static ArrayList<GoetiaDemon> summonable() {
		ArrayList<GoetiaDemon> out = new ArrayList<>();
		for (GoetiaDemon d : GoetiaDemon.values()) if (GoetiaHooks.slain(d)) out.add(d);
		Collections.sort(out, (x, y) -> x.rank != y.rank ? y.rank.ordinal() - x.rank.ordinal() : x.number() - y.number());
		while (out.size() > SUMMON_CHOICES) out.remove(out.size() - 1);
		return out;
	}

	/** the Master of Demons keeps two at once and its pacts last 50% longer */
	public static int maxSummons(Hero hero) {
		return hero != null && hero.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.DEMON_MASTER ? 2 : 1;
	}

	public static float summonTurns(Hero hero) {
		float t = SUMMON_TURNS;
		if (hero != null && hero.subClass == com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass.DEMON_MASTER) t *= 1.5f;
		return t;
	}

	public static int activeSummons() {
		int n = 0;
		for (Mob m : Dungeon.level.mobs) if (m.buff(SummonPact.class) != null) n++;
		return n;
	}

	public static void summon(final Hero hero) {
		final SolomonArcana a = arcana(hero);
		if (a == null) return;
		final ArrayList<GoetiaDemon> list = summonable();
		if (list.isEmpty()) {
			GLog.w(Messages.get(SolomonSpells.class, "no_demons"));
			return;
		}
		if (activeSummons() >= maxSummons(hero)) {
			GLog.w(Messages.get(SolomonSpells.class, "too_many"));
			return;
		}
		String[] labels = new String[list.size()];
		for (int i = 0; i < labels.length; i++) labels[i] = list.get(i).rankName() + " " + list.get(i).title();
		GameScene.show(new WndOptions(new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(
				com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SOLOMON_RING),
				Messages.get(SolomonSpells.class, "summon_title"),
				Messages.get(SolomonSpells.class, "summon_msg", SUMMON_COST, Math.round(summonTurns(hero))), labels) {
			@Override
			protected void onSelect(int index) {
				summonDemon(hero, list.get(index));
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				GoetiaDemon d = list.get(index);
				GameScene.show(new WndTitledMessage(new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(
						com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SOLOMON_RING),
						d.rankName() + " " + d.title(), d.kindDesc()));
			}
		});
	}

	private static void summonDemon(Hero hero, GoetiaDemon d) {
		SolomonArcana a = arcana(hero);
		if (a == null || a.mana < SUMMON_COST || activeSummons() >= maxSummons(hero)) {
			GLog.w(Messages.get(SolomonSpells.class, "no_mana"));
			return;
		}
		ArrayList<Integer> cells = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null) cells.add(c);
		}
		if (cells.isEmpty()) {
			GLog.w(Messages.get(SolomonSpells.class, "no_room"));
			return;
		}
		a.useMana(SUMMON_COST);
		GoetiaFiend f = new GoetiaFiend();
		f.setup(d, Math.max(Dungeon.depth, Dungeon.scalingDepth()));
		f.minion = true;
		f.pos = Random.element(cells);
		f.state = f.WANDERING;
		GameScene.add(f);
		Buff.affect(f, SummonPact.class).set(summonTurns(hero));
		CellEmitter.get(f.pos).burst(Speck.factory(Speck.WOOL), 8);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		GLog.p(Messages.get(SolomonSpells.class, "summoned", d.rankName(), d.title()));
		hero.sprite.operate(hero.pos);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	/** a demon bound by Solomon's seal: an ally until the pact runs out */
	public static class SummonPact extends AllyBuff {

		{
			type = buffType.POSITIVE;
		}

		private float left = SUMMON_TURNS;

		public SummonPact set(float turns) {
			left = turns;
			return this;
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0) {
				if (target.sprite != null) CellEmitter.get(target.pos).burst(Speck.factory(Speck.WOOL), 6);
				target.die(null);
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator.CORRUPT;
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", (int) Math.ceil(left));
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat("left");
		}
	}
}
