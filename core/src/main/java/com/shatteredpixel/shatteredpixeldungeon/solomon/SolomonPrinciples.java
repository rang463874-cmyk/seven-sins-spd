package com.shatteredpixel.shatteredpixeldungeon.solomon;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
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
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.elite.StoneDust;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: the three alchemical principles (Nov 18 2026, owner): sulfur (soul, combustion), mercury (spirit, melting and
 * volatility) and salt (body, the incombustible and fixed). Solomon learns one at level 10 and another at level 20 (a choice
 * among those left). A principle only works in a two-symbol pair (with an element, itself or the other learned principle).
 */
public final class SolomonPrinciples {

	private SolomonPrinciples() {}

	public static final int LEVEL_1 = 10;
	public static final int LEVEL_2 = 20;

	public static boolean isPrinciple(SolomonSpells.Element e) {
		return e == SolomonSpells.Element.SULFUR || e == SolomonSpells.Element.MERCURY || e == SolomonSpells.Element.SALT;
	}

	/** how many principles the hero may know at its level */
	public static int allowed(Hero hero) {
		return hero == null ? 0 : (hero.lvl >= LEVEL_2 ? 2 : hero.lvl >= LEVEL_1 ? 1 : 0);
	}

	public static boolean pending(Hero hero) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		return a != null && a.principles.size() < allowed(hero);
	}

	public enum PSpell {
		SOULFIRE(SolomonSpells.Element.SULFUR, SolomonSpells.Element.FIRE, 10, SinSkill.TARGET_ENEMY),
		BRIMSTONE(SolomonSpells.Element.SULFUR, SolomonSpells.Element.WATER, 9, SinSkill.TARGET_NONE),
		SOUL_ARROWS(SolomonSpells.Element.SULFUR, SolomonSpells.Element.AIR, 10, SinSkill.TARGET_NONE),
		BRIMSTONE_FIELD(SolomonSpells.Element.SULFUR, SolomonSpells.Element.EARTH, 10, SinSkill.TARGET_ENEMY),
		SOUL_HARVEST(SolomonSpells.Element.SULFUR, SolomonSpells.Element.SULFUR, 12, SinSkill.TARGET_NONE),
		VOLATILE(SolomonSpells.Element.MERCURY, SolomonSpells.Element.FIRE, 10, SinSkill.TARGET_PROJECTILE),
		DISSOLVE(SolomonSpells.Element.MERCURY, SolomonSpells.Element.WATER, 9, SinSkill.TARGET_ENEMY),
		QUICKSILVER(SolomonSpells.Element.MERCURY, SolomonSpells.Element.AIR, 7, SinSkill.TARGET_NONE),
		EXCHANGE(SolomonSpells.Element.MERCURY, SolomonSpells.Element.EARTH, 8, SinSkill.TARGET_ENEMY),
		PANACEA(SolomonSpells.Element.MERCURY, SolomonSpells.Element.MERCURY, 12, SinSkill.TARGET_NONE),
		FIREPROOF(SolomonSpells.Element.SALT, SolomonSpells.Element.FIRE, 9, SinSkill.TARGET_NONE),
		SALT_WARD(SolomonSpells.Element.SALT, SolomonSpells.Element.WATER, 10, SinSkill.TARGET_NONE),
		SALT_STORM(SolomonSpells.Element.SALT, SolomonSpells.Element.AIR, 10, SinSkill.TARGET_NONE),
		SALT_FORTRESS(SolomonSpells.Element.SALT, SolomonSpells.Element.EARTH, 10, SinSkill.TARGET_NONE),
		IMMUTABLE(SolomonSpells.Element.SALT, SolomonSpells.Element.SALT, 12, SinSkill.TARGET_NONE),
		GREAT_WORK(SolomonSpells.Element.SULFUR, SolomonSpells.Element.MERCURY, 13, SinSkill.TARGET_ENEMY),
		SOUL_REND(SolomonSpells.Element.SULFUR, SolomonSpells.Element.SALT, 12, SinSkill.TARGET_ENEMY),
		COAGULATE(SolomonSpells.Element.MERCURY, SolomonSpells.Element.SALT, 11, SinSkill.TARGET_NONE);

		public final SolomonSpells.Element a, b;
		public final int baseCost;
		public final int targeting;

		PSpell(SolomonSpells.Element a, SolomonSpells.Element b, int cost, int targeting) {
			this.a = a;
			this.b = b;
			this.baseCost = cost;
			this.targeting = targeting;
		}

		public boolean picksCell() {
			return targeting != SinSkill.TARGET_NONE || this == QUICKSILVER;
		}

		public String title() {
			return Messages.get(SolomonPrinciples.class, "p_" + name().toLowerCase());
		}

		public String desc() {
			return Messages.get(SolomonPrinciples.class, "p_" + name().toLowerCase() + "_desc");
		}

		public int cost(Hero hero) {
			return SolomonHooks.adjustCost(hero, baseCost, false);
		}
	}

	/** the principle spell of a two-symbol circle, or null */
	public static PSpell current(SolomonArcana a) {
		if (a == null || a.circle.size() != 2) return null;
		SolomonSpells.Element x = a.circle.get(0), y = a.circle.get(1);
		if (!isPrinciple(x) && !isPrinciple(y)) return null;
		for (PSpell s : PSpell.values()) {
			if ((s.a == x && s.b == y) || (s.a == y && s.b == x)) return s;
		}
		return null;
	}

	/** a circle with a principle that makes no spell (one symbol, or three) */
	public static boolean hasPrinciple(SolomonArcana a) {
		if (a == null) return false;
		for (SolomonSpells.Element e : a.circle) if (isPrinciple(e)) return true;
		return false;
	}

	// ------------------------------------------------------------------ learning

	public static void choose(final Hero hero) {
		final SolomonArcana a = SolomonSpells.arcana(hero);
		if (a == null || !pending(hero)) return;
		final ArrayList<SolomonSpells.Element> options = new ArrayList<>();
		for (SolomonSpells.Element e : SolomonSpells.Element.values()) {
			if (isPrinciple(e) && !a.principles.contains(e)) options.add(e);
		}
		String[] labels = new String[options.size()];
		for (int i = 0; i < labels.length; i++) labels[i] = options.get(i).title();
		GameScene.show(new WndOptions(new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(
				com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SOLOMON_RING),
				Messages.get(SolomonPrinciples.class, "choose_title"),
				Messages.get(SolomonPrinciples.class, "choose_msg"), labels) {
			@Override
			protected void onSelect(int index) {
				if (!pending(hero)) return;
				SolomonSpells.Element e = options.get(index);
				a.principles.add(e);
				GLog.p(Messages.get(SolomonPrinciples.class, "learned", e.title()));
				hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
				Sample.INSTANCE.play(Assets.Sounds.LEVELUP);
				com.shatteredpixel.shatteredpixeldungeon.items.Item.updateQuickslot();
				GameScene.syncSkillBarPos();
				if (pending(hero)) choose(hero);
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				SolomonSpells.Element e = options.get(index);
				GameScene.show(new WndTitledMessage(new com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite(
						com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.SOLOMON_RING), e.title(),
						Messages.get(SolomonPrinciples.class, "pr_" + e.name().toLowerCase() + "_desc")));
			}
		});
	}

	// ------------------------------------------------------------------ casting

	private static PSpell pending;

	public static void cast(Hero hero) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		PSpell s = current(a);
		if (s == null) {
			GLog.w(Messages.get(SolomonSpells.class, "no_spell"));
			return;
		}
		if (a.mana < s.cost(hero)) {
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
			PSpell s = pending;
			pending = null;
			if (cell == null || s == null) return;
			Hero hero = Dungeon.hero;
			SolomonArcana a = SolomonSpells.arcana(hero);
			if (current(a) != s || a.mana < s.cost(hero) || cell == hero.pos) return;
			aimed(hero, s, cell);
		}

		@Override
		public String prompt() {
			return Messages.get(SolomonSpells.class, "prompt");
		}
	};

	private static void finish(Hero hero, PSpell s, int pos) {
		SolomonHooks.afterCast(hero, s.cost(hero));
		if (pos != hero.pos) hero.sprite.zap(pos);
		else hero.sprite.operate(hero.pos);
		hero.sprite.showStatus(0xCC99FF, s.title());
		com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	private static float power(Hero hero) {
		SolomonArcana a = SolomonSpells.arcana(hero);
		return a == null ? 1f : a.spellPower();
	}

	private static int roll(Hero hero, float mult) {
		int lvl = Math.max(1, hero.lvl);
		return Math.max(1, Math.round(Random.NormalIntRange(3 + lvl / 2, 8 + lvl) * mult * power(hero)));
	}

	private static boolean foe(Char ch) {
		return ch != null && ch != Dungeon.hero && ch.alignment != Char.Alignment.ALLY && ch.isAlive();
	}

	private static boolean boss(Char ch) {
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	private static boolean unholy(Char ch) {
		return Char.hasProp(ch, Char.Property.DEMONIC) || Char.hasProp(ch, Char.Property.UNDEAD);
	}

	private static ArrayList<Char> foesInSight(Hero hero, int radius) {
		ArrayList<Char> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (foe(m) && Dungeon.level.heroFOV[m.pos] && (radius <= 0 || Dungeon.level.distance(hero.pos, m.pos) <= radius)) out.add(m);
		}
		return out;
	}

	private static void heal(Char ch, int amount) {
		amount = Math.min(amount, ch.HT - ch.HP);
		if (amount <= 0) return;
		ch.HP += amount;
		if (ch.sprite != null) ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	/** damage dealt to a char (HP lost) */
	private static int strike(Char ch, int dmg) {
		int before = ch.HP;
		ch.damage(dmg, SolomonPrinciples.class);
		return Math.max(0, before - Math.max(0, ch.HP));
	}

	private static void cleanse(Hero hero) {
		for (Buff b : hero.buffs()) {
			if (b.type == Buff.buffType.NEGATIVE && !(b instanceof Hunger)) b.detach();
		}
	}

	private static boolean instant(Hero hero, PSpell s) {
		switch (s) {
			case BRIMSTONE: {
				int lvl = Math.max(1, hero.lvl);
				for (Char ch : foesInSight(hero, 3)) {
					strike(ch, roll(hero, 0.6f));
					if (!ch.isAlive()) continue;
					Buff.affect(ch, Ooze.class).set(Ooze.DURATION);
					Buff.affect(ch, Poison.class).set(Math.round((3 + lvl / 2f) * power(hero)));
					CellEmitter.center(ch.pos).burst(Speck.factory(Speck.STENCH), 3);
				}
				Sample.INSTANCE.play(Assets.Sounds.GAS);
				return true;
			}
			case SOUL_ARROWS: {
				ArrayList<Char> targets = foesInSight(hero, 8);
				if (targets.isEmpty()) {
					GLog.w(Messages.get(SolomonSpells.class, "no_target"));
					return false;
				}
				int dealt = 0;
				for (int i = 0; i < 5; i++) {
					Char ch = targets.get(i % targets.size());
					if (!ch.isAlive()) continue;
					MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SHADOW, hero.sprite, ch.pos, null);
					dealt += strike(ch, roll(hero, 0.8f));
				}
				heal(hero, Math.round(dealt * 0.2f));
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
				return true;
			}
			case SOUL_HARVEST: {
				int reaped = 0;
				for (Char ch : foesInSight(hero, 0)) {
					if (!boss(ch) && ch.HP <= ch.HT * 0.3f) {
						ch.sprite.emitter().burst(ShadowParticle.UP, 8);
						ch.die(SolomonPrinciples.class);
						reaped++;
					} else {
						strike(ch, roll(hero, 0.8f));
					}
				}
				if (reaped > 0) {
					heal(hero, Math.round(hero.HT * 0.1f * reaped));
					SolomonArcana a = SolomonSpells.arcana(hero);
					if (a != null) a.addMana(3 * reaped);
				}
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				return true;
			}
			case PANACEA: {
				cleanse(hero);
				heal(hero, Math.round(hero.HT * 0.15f * power(hero)));
				Buff.affect(hero, Elixir.class, 15f);
				hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 8);
				Sample.INSTANCE.play(Assets.Sounds.DRINK);
				return true;
			}
			case FIREPROOF: {
				Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.25f * power(hero)));
				Buff.affect(hero, FireImbue.class).set(15f);
				Buff.detach(hero, Burning.class);
				hero.sprite.emitter().burst(Speck.factory(Speck.FORGE), 8);
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				return true;
			}
			case SALT_WARD: {
				cleanse(hero);
				Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.15f * power(hero)));
				for (Char ch : foesInSight(hero, 4)) {
					if (unholy(ch)) {
						strike(ch, roll(hero, 2.0f));
						CellEmitter.center(ch.pos).burst(Speck.factory(Speck.LIGHT), 4);
					}
				}
				hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
				Sample.INSTANCE.play(Assets.Sounds.READ);
				return true;
			}
			case SALT_STORM: {
				for (Char ch : foesInSight(hero, 3)) {
					strike(ch, roll(hero, unholy(ch) ? 1.6f : 0.8f));
					if (ch.isAlive()) Buff.prolong(ch, Blindness.class, 5f);
					CellEmitter.get(ch.pos).burst(Speck.factory(Speck.DUST), 5);
				}
				Sample.INSTANCE.play(Assets.Sounds.PUFF);
				return true;
			}
			case SALT_FORTRESS: {
				int raised = 0;
				int w = Dungeon.level.width();
				int hx = hero.pos % w, hy = hero.pos / w;
				for (int y = hy - 2; y <= hy + 2; y++) {
					for (int x = hx - 2; x <= hx + 2; x++) {
						if (Math.max(Math.abs(x - hx), Math.abs(y - hy)) != 2) continue;
						if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
						if (StoneDust.raise(Dungeon.level, x + y * w, 10)) raised++;
					}
				}
				if (raised == 0) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return false;
				}
				Dungeon.observe();
				GameScene.updateFog();
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				return true;
			}
			case IMMUTABLE: {
				Buff.affect(hero, Immutable.class, 8f);
				hero.sprite.emitter().burst(Speck.factory(Speck.ROCK), 8);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS, 1f, 0.5f);
				return true;
			}
			case COAGULATE: {
				for (Char ch : foesInSight(hero, 0)) {
					Buff.prolong(ch, Slow.class, 6f);
					Buff.prolong(ch, Cripple.class, 6f);
				}
				Buff.prolong(hero, Haste.class, 4f);
				Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
				return true;
			}
			default:
				return false;
		}
	}

	private static void aimed(final Hero hero, final PSpell s, final int cell) {
		switch (s) {
			case QUICKSILVER: {
				if (!Dungeon.level.heroFOV[cell] || !Dungeon.level.passable[cell] || Actor.findChar(cell) != null
						|| Dungeon.level.distance(hero.pos, cell) > 8) {
					GLog.w(Messages.get(SolomonSpells.class, "bad_cell"));
					return;
				}
				ScrollOfTeleportation.appear(hero, cell);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
				Buff.prolong(hero, Haste.class, 6f);
				finish(hero, s, hero.pos);
				return;
			}
			case VOLATILE: {
				final int end = new Ballistica(hero.pos, cell, Ballistica.MAGIC_BOLT).collisionPos;
				SolomonHooks.afterCast(hero, s.cost(hero));
				hero.sprite.zap(end);
				hero.busy();
				MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.SHAMAN_PURPLE, hero.sprite, end, new Callback() {
					@Override
					public void call() {
						int w = Dungeon.level.width();
						int ex = end % w, ey = end / w;
						for (int y = ey - 2; y <= ey + 2; y++) {
							for (int x = ex - 2; x <= ex + 2; x++) {
								if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
								int c = x + y * w;
								if (Dungeon.level.solid[c]) continue;
								CellEmitter.get(c).burst(Speck.factory(Speck.STEAM), 2);
								Char ch = Actor.findChar(c);
								if (foe(ch)) strike(ch, roll(hero, 1.6f));
							}
						}
						SolomonSpells.igniteArea(end, 2);
						PixelScene.shake(2, 0.4f);
						Sample.INSTANCE.play(Assets.Sounds.BLAST);
						hero.sprite.showStatus(0xCC99FF, s.title());
						com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility.dispel();
						hero.spendAndNext(1f);
					}
				});
				return;
			}
			default:
		}

		//single-target spells
		Char ch = Actor.findChar(cell);
		if (!foe(ch) || !Dungeon.level.heroFOV[cell]) {
			GLog.w(Messages.get(SolomonSpells.class, "no_target"));
			return;
		}
		switch (s) {
			case SOULFIRE: {
				strike(ch, roll(hero, 2.2f));
				CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 10);
				SolomonSpells.ignite(ch.pos);
				if (ch.isAlive()) {
					Buff.affect(ch, Burning.class).reignite(ch);
				} else {
					SolomonArcana a = SolomonSpells.arcana(hero);
					if (a != null) a.addMana(5);
				}
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				break;
			}
			case BRIMSTONE_FIELD: {
				int w = Dungeon.level.width();
				int cx = cell % w, cy = cell / w;
				for (int y = cy - 2; y <= cy + 2; y++) {
					for (int x = cx - 2; x <= cx + 2; x++) {
						if (x < 0 || y < 0 || x >= w || y >= Dungeon.level.height()) continue;
						int c = x + y * w;
						if (Dungeon.level.solid[c]) continue;
						CellEmitter.get(c).burst(FlameParticle.FACTORY, 3);
						Char t = Actor.findChar(c);
						if (!foe(t)) continue;
						strike(t, roll(hero, 1.2f));
						if (!t.isAlive()) continue;
						Buff.affect(t, Burning.class).reignite(t);
						Buff.prolong(t, Roots.class, 2f);
					}
				}
				SolomonSpells.igniteArea(cell, 2);
				Sample.INSTANCE.play(Assets.Sounds.BURNING);
				break;
			}
			case DISSOLVE: {
				strike(ch, roll(hero, 1.0f));
				if (ch.isAlive()) {
					Buff.prolong(ch, Vulnerable.class, 10f);
					Buff.prolong(ch, Weakness.class, 10f);
					Buff.prolong(ch, Hex.class, 10f);
				}
				CellEmitter.get(ch.pos).burst(Speck.factory(Speck.STEAM), 6);
				Sample.INSTANCE.play(Assets.Sounds.GAS);
				break;
			}
			case EXCHANGE: {
				if (Char.hasProp(ch, Char.Property.IMMOVABLE) || Char.hasProp(ch, Char.Property.LARGE)) {
					GLog.w(Messages.get(SolomonPrinciples.class, "no_swap"));
					return;
				}
				int from = hero.pos, to = ch.pos;
				Actor.add(new Pushing(ch, to, from));
				Actor.add(new Pushing(hero, from, to));
				ch.pos = from;
				hero.pos = to;
				Dungeon.level.occupyCell(ch);
				Dungeon.level.occupyCell(hero);
				Dungeon.observe();
				GameScene.updateFog();
				if (boss(ch)) Buff.prolong(ch, Cripple.class, 4f);
				else Buff.prolong(ch, Paralysis.class, 2f);
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				break;
			}
			case GREAT_WORK: {
				strike(ch, roll(hero, 2.5f));
				if (ch.isAlive()) {
					Buff.affect(ch, Burning.class).reignite(ch);
					Buff.prolong(ch, Vulnerable.class, 8f);
				}
				CellEmitter.get(ch.pos).burst(Speck.factory(Speck.LIGHT), 10);
				CellEmitter.get(ch.pos).burst(FlameParticle.FACTORY, 10);
				SolomonSpells.ignite(ch.pos);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				break;
			}
			case SOUL_REND: {
				int dmg = Math.round(ch.HP * (boss(ch) ? 0.10f : 0.25f)) + roll(hero, 1.0f);
				heal(hero, strike(ch, dmg));
				ch.sprite.emitter().burst(ShadowParticle.UP, 8);
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				break;
			}
			default:
				return;
		}
		finish(hero, s, cell);
	}

	// ------------------------------------------------------------------ buffs

	/** Panacea: spells cost half their mana */
	public static class Elixir extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.RECHARGING;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.8f, 0.8f, 0.9f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (15f - visualcooldown()) / 15f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}

	/** Immutable: damage taken -60% (SolomonHooks.modifyDamage) */
	public static class Immutable extends FlavourBuff {
		{
			type = buffType.POSITIVE;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.9f, 0.9f, 0.9f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (8f - visualcooldown()) / 8f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
