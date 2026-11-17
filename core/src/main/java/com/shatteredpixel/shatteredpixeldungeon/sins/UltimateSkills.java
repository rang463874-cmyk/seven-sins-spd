package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.UltimateBook;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfSirensSong;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sloth.SlothHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: ultimate skills (Nov 17 2026, owner). The choice at level 10 / 20, the skill bar entries
 * (in the extra row via SkillBar.extraSkills), what each ultimate does and its hooks
 * (Char.damage, Hero.attackProc / attackSkill / defenseSkill, Mob.die, PrideGlory.shatter, GoldenCard.discounted).
 */
public final class UltimateSkills {

	private UltimateSkills() {}

	public static boolean isSin(Hero hero) {
		return hero != null && hero.heroClass.ordinal() >= HeroClass.GLUTTONY.ordinal()
				&& hero.heroClass.ordinal() <= HeroClass.SLOTH.ordinal();
	}

	public static UltimateBook book(Hero hero) {
		return hero == null ? null : hero.buff(UltimateBook.class);
	}

	/** tier whose choice is open right now, or 0 */
	public static int pendingTier(Hero hero) {
		UltimateBook b = book(hero);
		if (b == null) return 0;
		if (hero.lvl >= Ultimate.TIER1_LEVEL && b.first == null) return 1;
		if (hero.lvl >= Ultimate.TIER2_LEVEL && b.second == null) return 2;
		return 0;
	}

	/** from Hero.act: gives sin heroes the book and announces open choices */
	public static void tick(Hero hero) {
		if (!isSin(hero)) return;
		UltimateBook b = book(hero);
		if (b == null) {
			b = Buff.affect(hero, UltimateBook.class);
			resyncBar();
		}
		int t = pendingTier(hero);
		if (t == 1 && !b.told1) {
			b.told1 = true;
			GLog.p(Messages.get(UltimateSkills.class, "can_learn", Ultimate.TIER1_LEVEL));
			resyncBar();
		} else if (t == 2 && !b.told2) {
			b.told2 = true;
			GLog.p(Messages.get(UltimateSkills.class, "can_learn", Ultimate.TIER2_LEVEL));
			resyncBar();
		}
	}

	/** Hero.act runs on the actor thread: the skill bar is laid out on the render thread */
	private static void resyncBar() {
		com.watabou.noosa.Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.syncSkillBarPos();
			}
		});
	}

	// ------------------------------------------------------------------ skill bar

	public static ArrayList<SinSkill> skills(Hero hero) {
		ArrayList<SinSkill> out = new ArrayList<>();
		UltimateBook b = book(hero);
		if (b == null || !isSin(hero)) return out;
		if (b.first != null) out.add(new UltimateSkill(b.first));
		if (b.second != null) out.add(new UltimateSkill(b.second));
		if (pendingTier(hero) > 0) out.add(new LearnSkill());
		return out;
	}

	public static class UltimateSkill extends SinSkill {
		public final Ultimate u;

		public UltimateSkill(Ultimate u) {
			super(Ultimate.class, "ult_" + u.id(), u.icon);
			this.u = u;
		}

		@Override
		public String name(Hero hero) {
			return u.title();
		}

		@Override
		public String desc(Hero hero) {
			return u.desc(UltimateSkills.descArgs(hero, u));
		}

		@Override
		public CostType costType() {
			return CostType.VARIES;
		}

		@Override
		public String costShort(Hero hero) {
			UltimateBook b = book(hero);
			int cd = b == null ? 0 : (int) Math.ceil(b.cooldown(u));
			return cd > 0 ? Integer.toString(cd) : "";
		}

		@Override
		public String costLabel(Hero hero) {
			UltimateBook b = book(hero);
			int cd = b == null ? 0 : (int) Math.ceil(b.cooldown(u));
			return Messages.get(UltimateSkills.class, "cooldown", u.cooldown, cd);
		}

		@Override
		public boolean usable(Hero hero) {
			UltimateBook b = book(hero);
			return b != null && b.cooldown(u) <= 0;
		}

		@Override
		public int targeting() {
			return u.targeting;
		}

		@Override
		public void use(Hero hero) {
			UltimateSkills.use(hero, u);
		}
	}

	public static class LearnSkill extends SinSkill {
		public LearnSkill() {
			super(Ultimate.class, "ult_learn", Ultimate.LEARN_ICON);
		}

		@Override
		public String name(Hero hero) {
			return Messages.get(UltimateSkills.class, "learn");
		}

		@Override
		public String desc(Hero hero) {
			return Messages.get(UltimateSkills.class, "learn_desc", Ultimate.TIER1_LEVEL, Ultimate.TIER2_LEVEL);
		}

		@Override
		public CostType costType() {
			return CostType.NONE;
		}

		@Override
		public boolean usable(Hero hero) {
			return pendingTier(hero) > 0;
		}

		@Override
		public void use(Hero hero) {
			choose(hero);
		}
	}

	/** the level 10 / 20 choice window */
	public static void choose(final Hero hero) {
		final int tier = pendingTier(hero);
		if (tier == 0) return;
		final ArrayList<Ultimate> options = Ultimate.choices(hero.heroClass, tier);
		String[] labels = new String[options.size()];
		for (int i = 0; i < labels.length; i++) labels[i] = options.get(i).title();
		GameScene.show(new WndOptions(new SinSkillIcon(Ultimate.LEARN_ICON).image(),
				Messages.get(UltimateSkills.class, "choose_title", tier == 1 ? Ultimate.TIER1_LEVEL : Ultimate.TIER2_LEVEL),
				Messages.get(UltimateSkills.class, "choose_msg"), labels) {
			@Override
			protected void onSelect(int index) {
				UltimateBook b = book(hero);
				if (b == null || pendingTier(hero) != tier) return;
				Ultimate u = options.get(index);
				if (tier == 1) b.first = u;
				else b.second = u;
				GLog.p(Messages.get(UltimateSkills.class, "learned", u.title()));
				hero.sprite.emitter().burst(Speck.factory(Speck.STAR), 10);
				Sample.INSTANCE.play(Assets.Sounds.LEVELUP);
				GameScene.syncSkillBarPos();
				if (pendingTier(hero) > 0) choose(hero);
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				Ultimate u = options.get(index);
				GameScene.show(new WndTitledMessage(new SinSkillIcon(u.icon).image(), u.title(),
						u.desc(descArgs(hero, u)) + "\n\n" + Messages.get(UltimateSkills.class, "cooldown_info", u.cooldown)));
			}
		});
	}

	/** an icon of skill_icons.png for windows */
	private static class SinSkillIcon extends SinSkill {
		SinSkillIcon(int icon) {
			super(Ultimate.class, "icon", icon);
		}

		com.watabou.noosa.Image image() {
			return iconImage();
		}

		@Override
		public CostType costType() {
			return CostType.NONE;
		}

		@Override
		public boolean usable(Hero hero) {
			return false;
		}

		@Override
		public void use(Hero hero) {
		}
	}

	// ------------------------------------------------------------------ numbers

	private static int lvl(Hero h) {
		return Math.max(1, h.lvl);
	}

	/** tier 1 area damage */
	private static int small(Hero h) {
		return Random.NormalIntRange(lvl(h), 2 * lvl(h) + 5);
	}

	/** tier 2 heavy damage */
	private static int big(Hero h) {
		return Random.NormalIntRange(2 * lvl(h), 3 * lvl(h) + 10);
	}

	public static Object[] descArgs(Hero h, Ultimate u) {
		int l = lvl(h);
		switch (u) {
			case ACID_RAIN: case GOLD_STORM: case QUAKE: case BLOOD_FEAST: case ENVY_STORM: case NIGHTMARE:
				return new Object[]{l, 2 * l + 5};
			case DEVOUR: return new Object[]{Math.round(l * 1.5f), Math.round((2 * l + 5) * 1.5f)};
			case ABYSS_MAW: case JUDGMENT: case ETERNAL_SLEEP:
				return new Object[]{2 * l, 3 * l + 10};
			case BLADE_RAIN: return new Object[]{l + 5, 2 * l + 10};
			case ERUPTION: return new Object[]{l, 2 * l};
			case BOUNTY: return new Object[]{30 * Math.max(1, Dungeon.depth)};
			case MIDAS: return new Object[]{15 * Math.max(1, Dungeon.depth)};
			case COMPOUND: return new Object[]{100 * Math.max(1, Dungeon.depth)};
			case FATAL_KISS: return new Object[]{Math.round(h.HT * 0.4f)};
			default: return new Object[0];
		}
	}

	// ------------------------------------------------------------------ using

	private static Ultimate pending;

	public static void use(Hero hero, Ultimate u) {
		UltimateBook b = book(hero);
		if (b == null || b.cooldown(u) > 0) return;
		if (u.targeting != SinSkill.TARGET_NONE) {
			pending = u;
			GameScene.selectCell(targeter);
			return;
		}
		if (instant(hero, u)) finish(hero, u, hero.pos);
	}

	private static final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Ultimate u = pending;
			pending = null;
			if (cell == null || u == null) return;
			Hero hero = Dungeon.hero;
			UltimateBook b = book(hero);
			if (b == null || b.cooldown(u) > 0) return;
			Char ch = Actor.findChar(cell);
			if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[cell]) {
				GLog.w(Messages.get(UltimateSkills.class, "no_target"));
				return;
			}
			boolean adjacentOnly = u == Ultimate.DEVOUR || u == Ultimate.FATAL_KISS;
			if (adjacentOnly && !Dungeon.level.adjacent(hero.pos, ch.pos)) {
				GLog.w(Messages.get(UltimateSkills.class, "not_adjacent"));
				return;
			}
			if (targeted(hero, u, ch)) finish(hero, u, ch.pos);
		}

		@Override
		public String prompt() {
			return Messages.get(UltimateSkills.class, "prompt");
		}
	};

	private static void finish(Hero hero, Ultimate u, int pos) {
		UltimateBook b = book(hero);
		if (b != null) b.startCooldown(u);
		if (pos != hero.pos) hero.sprite.zap(pos);
		else hero.sprite.operate(hero.pos);
		hero.sprite.showStatus(CharSprite.POSITIVE, u.title());
		Invisibility.dispel();
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
		hero.spendAndNext(1f);
	}

	private static boolean boss(Char ch) {
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	/** visible enemies within range (0 = anywhere in sight) */
	private static ArrayList<Mob> enemies(Hero hero, int range) {
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			if (range > 0 && Dungeon.level.distance(hero.pos, m.pos) > range) continue;
			out.add(m);
		}
		return out;
	}

	private static void heal(Hero hero, int amount) {
		amount = Math.min(amount, hero.HT - hero.HP);
		if (amount <= 0) return;
		hero.HP += amount;
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	private static void effect(Hero hero, Ultimate u, float turns) {
		Buff.append(hero, UltimateBook.Effect.class, turns).set(u, turns, u == Ultimate.AEGIS ? 3 : 0);
	}

	private static void kill(Char ch) {
		ch.die(Ultimate.class);
	}

	/** untargeted ultimates; false = nothing happened (no cooldown) */
	private static boolean instant(Hero hero, Ultimate u) {
		switch (u) {
			case ACID_RAIN: {
				Splash.at(hero.pos, 0x66AA22, 20);
				for (Mob m : enemies(hero, 3)) {
					m.damage(small(hero), Ultimate.class);
					if (m.isAlive()) {
						Buff.affect(m, Ooze.class).set(Ooze.DURATION);
						Buff.prolong(m, Weakness.class, 10f);
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.GAS);
				return true;
			}
			case IRON_STOMACH: effect(hero, u, 20f); return true;
			case ABYSS_MAW: {
				for (Mob m : enemies(hero, 5)) {
					pull(hero, m);
					m.damage(big(hero), Ultimate.class);
					if (m.isAlive() && !boss(m) && m.HP <= m.HT / 4) kill(m);
				}
				hero.sprite.emitter().burst(Speck.factory(Speck.BONE), 10);
				Sample.INSTANCE.play(Assets.Sounds.EAT);
				return true;
			}
			case FEAST: {
				Hunger h = hero.buff(Hunger.class);
				if (h != null) h.satisfy(Hunger.STARVING);
				heal(hero, hero.HT);
				for (Buff bf : hero.buffs()) if (bf.type == Buff.buffType.NEGATIVE && !(bf instanceof Hunger)) bf.detach();
				Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.25f));
				Sample.INSTANCE.play(Assets.Sounds.EAT);
				return true;
			}
			case GLUTTON_AVATAR: effect(hero, u, 20f); return true;

			case GOLD_STORM: {
				int bonus = Math.min(Dungeon.gold / 100, 3 * lvl(hero));
				for (Mob m : enemies(hero, 4)) {
					m.damage(small(hero) + bonus, Ultimate.class);
					CellEmitter.center(m.pos).burst(Speck.factory(Speck.COIN), 4);
				}
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				return true;
			}
			case GOLD_ARMOR: effect(hero, u, 20f); return true;
			case COMPOUND: {
				GreedVault v = hero.buff(GreedVault.class);
				if (v != null) v.addCharge(v.maxCharge());
				int gain = Math.min(Dungeon.gold / 10, 100 * Math.max(1, Dungeon.depth));
				if (gain > 0) {
					Dungeon.gold += gain;
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(gain), FloatingText.GOLD);
					if (v != null) v.goldChanged();
				}
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				return true;
			}
			case GOLDEN_AGE: effect(hero, u, 25f); return true;

			case QUAKE: {
				for (Mob m : enemies(hero, 2)) {
					m.damage(small(hero), Ultimate.class);
					if (m.isAlive() && !boss(m)) Buff.prolong(m, Paralysis.class, 2f);
				}
				CellEmitter.get(hero.pos).burst(Speck.factory(Speck.ROCK), 12);
				com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(3, 0.5f);
				Sample.INSTANCE.play(Assets.Sounds.ROCKS);
				return true;
			}
			case BLOOD_RAGE: {
				WrathGrudge g = hero.buff(WrathGrudge.class);
				if (g != null) g.rage = g.maxRage();
				effect(hero, u, 20f);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				return true;
			}
			case BERSERK: effect(hero, u, 15f); Sample.INSTANCE.play(Assets.Sounds.CHALLENGE); return true;
			case ERUPTION: {
				WrathGrudge g = hero.buff(WrathGrudge.class);
				float rage = g == null ? 0 : g.rage;
				if (g != null) g.rage = 0;
				for (Mob m : enemies(hero, 6)) {
					m.damage(Random.NormalIntRange(lvl(hero), 2 * lvl(hero)) + Math.round(rage * 0.6f), Ultimate.class);
					CellEmitter.center(m.pos).burst(Speck.factory(Speck.STEAM), 6);
				}
				com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(2, 0.5f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				return true;
			}
			case WAR_DRUMS: {
				for (Mob m : enemies(hero, 8)) if (!boss(m)) Buff.prolong(m, Amok.class, 15f);
				hero.sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.3f, 3);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				return true;
			}

			case KNEEL: {
				for (Mob m : enemies(hero, 0)) {
					if (!boss(m)) Buff.prolong(m, Paralysis.class, 3f);
					else Buff.prolong(m, Weakness.class, 5f);
				}
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				return true;
			}
			case AEGIS: effect(hero, u, 30f); return true;
			case BLADE_RAIN: {
				for (Mob m : enemies(hero, 0)) {
					m.damage(Random.NormalIntRange(lvl(hero) + 5, 2 * lvl(hero) + 10), Ultimate.class);
					CellEmitter.center(m.pos).burst(Speck.factory(Speck.STAR), 3);
				}
				Sample.INSTANCE.play(Assets.Sounds.HIT_STAB);
				return true;
			}
			case ABSOLUTE: {
				PrideGlory g = hero.buff(PrideGlory.class);
				if (g != null) g.addDignity(g.maxDignity());
				effect(hero, u, 25f);
				return true;
			}
			case DIVINE_RIGHT: effect(hero, u, 20f); return true;

			case DANCE: {
				for (Mob m : enemies(hero, 3)) {
					if (boss(m)) continue;
					Charm c = Buff.affect(m, Charm.class, 6f);
					c.object = hero.id();
					m.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 3);
				}
				Sample.INSTANCE.play(Assets.Sounds.CHARMS);
				return true;
			}
			case BLOOD_FEAST: {
				int total = 0;
				for (Mob m : enemies(hero, 0)) {
					if (m.buff(Charm.class) == null) continue;
					int before = m.HP;
					m.damage(small(hero), Ultimate.class);
					total += Math.max(0, before - Math.max(0, m.HP));
					m.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
				}
				if (total == 0) {
					GLog.w(Messages.get(UltimateSkills.class, "no_charmed"));
					return false;
				}
				heal(hero, total);
				return true;
			}
			case SHADOW_DANCE: effect(hero, u, 15f); return true;
			case CRIMSON_NIGHT: effect(hero, u, 20f); return true;

			case FLOOD: {
				EnvyCovet c = hero.buff(EnvyCovet.class);
				if (c != null) c.addJealousy(c.maxJealousy());
				hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
				return true;
			}
			case MIRROR_IMAGES: return puppets(hero, 2, 25f);
			case REFLECTION: effect(hero, u, 15f); return true;
			case ENVY_STORM: {
				EnvyCovet c = hero.buff(EnvyCovet.class);
				float mult = 1f + 0.03f * (c == null ? 0 : c.codex.size());
				for (Mob m : enemies(hero, 0)) {
					m.damage(Math.round(small(hero) * mult), Ultimate.class);
					CellEmitter.center(m.pos).burst(Speck.factory(Speck.LIGHT), 4);
				}
				Sample.INSTANCE.play(Assets.Sounds.LIGHTNING);
				return true;
			}

			case SLUMBER: {
				for (Mob m : enemies(hero, 0)) if (!boss(m)) Buff.affect(m, MagicalSleep.class);
				Sample.INSTANCE.play(Assets.Sounds.LULLABY);
				return true;
			}
			case LEGION: return puppets(hero, 3, 30f);
			case REVERIE: {
				heal(hero, Math.round(hero.HT * 0.4f));
				Buff.affect(hero, Barrier.class).setShield(Math.round(hero.HT * 0.2f));
				UltimateBook b = book(hero);
				if (b != null) b.startCooldown(u);
				hero.sprite.showStatus(CharSprite.POSITIVE, u.title());
				BuffIndicator.refreshHero();
				SlothRepose.doze(hero, 3);
				return false;
			}
			case NIGHTMARE: {
				for (Mob m : enemies(hero, 0)) {
					m.damage(small(hero), Ultimate.class);
					if (m.isAlive() && !boss(m)) {
						Terror t = Buff.affect(m, Terror.class, 10f);
						t.object = hero.id();
					}
				}
				Sample.INSTANCE.play(Assets.Sounds.GHOST);
				return true;
			}
			case DAYDREAM: effect(hero, u, 25f); return true;
			default:
				return false;
		}
	}

	private static boolean puppets(Hero hero, int n, float life) {
		int made = 0;
		for (int i = 0; i < n; i++) {
			DreamPuppet p = DreamPuppet.create(true, life);
			if (SlothHooks.spawnNear(hero.pos, p)) made++;
		}
		if (made == 0) {
			GLog.w(Messages.get(UltimateSkills.class, "no_room"));
			return false;
		}
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
		return true;
	}

	/** drags an enemy next to the hero */
	private static void pull(Hero hero, Mob mob) {
		if (Char.hasProp(mob, Char.Property.IMMOVABLE) || Dungeon.level.adjacent(hero.pos, mob.pos)) return;
		int to = -1;
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = hero.pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null
					&& (!Char.hasProp(mob, Char.Property.LARGE) || Dungeon.level.openSpace[c])
					&& (to == -1 || Dungeon.level.distance(mob.pos, c) < Dungeon.level.distance(mob.pos, to))) {
				to = c;
			}
		}
		if (to == -1) return;
		Actor.add(new Pushing(mob, mob.pos, to));
		mob.pos = to;
		Dungeon.level.occupyCell(mob);
	}

	/** targeted ultimates; false = nothing happened */
	private static boolean targeted(final Hero hero, Ultimate u, final Char ch) {
		switch (u) {
			case DEVOUR:
				if (!boss(ch) && ch.HP <= ch.HT * 0.35f) {
					kill(ch);
					heal(hero, Math.round(hero.HT * 0.2f));
					Hunger h = hero.buff(Hunger.class);
					if (h != null) h.satisfy(150);
				} else {
					ch.damage(Math.round(small(hero) * 1.5f), Ultimate.class);
				}
				Sample.INSTANCE.play(Assets.Sounds.EAT);
				return true;
			case BOUNTY:
				Buff.append(ch, UltimateBook.Mark.class, 30f).set(u);
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				return true;
			case MIDAS:
				if (!boss(ch)) {
					int pos = ch.pos;
					kill(ch);
					Dungeon.level.drop(new Gold(15 * Math.max(1, Dungeon.depth)), pos).sprite.drop();
				} else {
					ch.damage(Math.round(ch.HT * 0.15f), Ultimate.class);
				}
				CellEmitter.center(ch.pos).burst(Speck.factory(Speck.COIN), 12);
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				return true;
			case VENDETTA:
				Buff.append(ch, UltimateBook.Mark.class, 25f).set(u);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				return true;
			case JUDGMENT: {
				PrideGlory g = hero.buff(PrideGlory.class);
				float mult = g == null ? 1f : g.dignityMult();
				final int dmg = Math.round(big(hero) * mult);
				MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.BEACON, hero.sprite, ch.pos, new Callback() {
					@Override
					public void call() {
						if (ch.isAlive()) ch.damage(dmg, Ultimate.class);
					}
				});
				Sample.INSTANCE.play(Assets.Sounds.ZAP);
				return true;
			}
			case THRALL:
				if (boss(ch) || !(ch instanceof Mob)) {
					GLog.w(Messages.get(UltimateSkills.class, "no_boss"));
					return false;
				}
				AllyBuff.affectAndLoot((Mob) ch, hero, ScrollOfSirensSong.Enthralled.class);
				ch.sprite.centerEmitter().start(Speck.factory(Speck.HEART), 0.2f, 5);
				Sample.INSTANCE.play(Assets.Sounds.CHARMS);
				return true;
			case FATAL_KISS: {
				int dmg = Math.round(hero.HT * 0.4f);
				int before = ch.HP;
				ch.damage(dmg, Ultimate.class);
				heal(hero, Math.max(0, before - Math.max(0, ch.HP)));
				ch.sprite.emitter().burst(Speck.factory(Speck.HEART), 6);
				Sample.INSTANCE.play(Assets.Sounds.CHARMS);
				return true;
			}
			case EVIL_EYE:
				Buff.prolong(ch, Weakness.class, 15f);
				Buff.prolong(ch, Vulnerable.class, 15f);
				Buff.prolong(ch, Hex.class, 15f);
				ch.sprite.emitter().burst(Speck.factory(Speck.DUST), 6);
				Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
				return true;
			case USURP:
				if (!boss(ch)) {
					int shield = Math.min(ch.HT / 2, hero.HT / 2);
					kill(ch);
					Buff.affect(hero, Barrier.class).setShield(Math.max(1, shield));
				} else {
					ch.damage(Math.round(ch.HT * 0.15f), Ultimate.class);
				}
				Sample.INSTANCE.play(Assets.Sounds.DEBUFF);
				return true;
			case ETERNAL_SLEEP: {
				boolean asleep = ch.buff(MagicalSleep.class) != null
						|| (ch instanceof Mob && ((Mob) ch).state == ((Mob) ch).SLEEPING);
				if (asleep && !boss(ch)) {
					kill(ch);
				} else {
					ch.damage(big(hero), Ultimate.class);
				}
				Sample.INSTANCE.play(Assets.Sounds.LULLABY);
				return true;
			}
			default:
				return false;
		}
	}

	// ------------------------------------------------------------------ hooks

	public static UltimateBook.Effect active(Char ch, Ultimate u) {
		if (ch == null) return null;
		for (UltimateBook.Effect e : ch.buffs(UltimateBook.Effect.class)) if (e.kind == u) return e;
		return null;
	}

	private static boolean marked(Char ch, Ultimate u) {
		for (UltimateBook.Mark m : ch.buffs(UltimateBook.Mark.class)) if (m.kind == u) return true;
		return false;
	}

	/** from Char.damage */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0) return dmg;
		Hero hero = Dungeon.hero;
		if (hero == null) return dmg;

		//the hero's own hits
		if (src == hero) {
			float m = 1f;
			if (active(hero, Ultimate.GLUTTON_AVATAR) != null) m += 0.5f;
			if (active(hero, Ultimate.BLOOD_RAGE) != null) m += 0.4f;
			if (active(hero, Ultimate.BERSERK) != null) m += 0.5f;
			if (active(hero, Ultimate.GOLDEN_AGE) != null) m += 0.3f;
			if (active(hero, Ultimate.DIVINE_RIGHT) != null) m += 0.4f;
			dmg = Math.round(dmg * m);
		}
		//allies while Daydream lasts
		if (src instanceof Mob && ((Mob) src).alignment == Char.Alignment.ALLY && active(hero, Ultimate.DAYDREAM) != null) {
			dmg *= 2;
		}
		if (target instanceof Mob && target.alignment == Char.Alignment.ALLY && active(hero, Ultimate.DAYDREAM) != null) {
			dmg = Math.max(1, dmg / 2);
		}

		if (target instanceof Mob) {
			if (marked(target, Ultimate.VENDETTA)) dmg = Math.round(dmg * 1.5f);
			if (marked(target, Ultimate.BOUNTY)) dmg = Math.round(dmg * 1.3f);
			return dmg;
		}

		if (target != hero) return dmg;

		UltimateBook.Effect aegis = active(hero, Ultimate.AEGIS);
		if (aegis != null && aegis.count > 0 && src instanceof Char) {
			aegis.count--;
			if (aegis.count <= 0) aegis.detach();
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(UltimateSkills.class, "blocked"));
			return 0;
		}
		if (active(hero, Ultimate.IRON_STOMACH) != null) dmg = Math.round(dmg * 0.65f);
		if (active(hero, Ultimate.BLOOD_RAGE) != null) dmg = Math.round(dmg * 1.15f);
		if (active(hero, Ultimate.REFLECTION) != null && src instanceof Char && src != hero && ((Char) src).isAlive()) {
			int back = Math.round(dmg * 0.5f);
			if (back > 0) ((Char) src).damage(back, Ultimate.class);
		}
		if (active(hero, Ultimate.GOLD_ARMOR) != null) {
			GreedVault v = hero.buff(GreedVault.class);
			if (v != null) {
				int paid = Math.min(dmg, v.chargeInt());
				if (paid > 0 && v.spend(paid)) dmg -= paid;
			}
		}
		if (active(hero, Ultimate.BERSERK) != null) {
			dmg = Math.max(0, Math.min(dmg, hero.HP + hero.shielding() - 1));
		}
		return dmg;
	}

	/** from Hero.attackProc */
	public static int onAttack(Hero hero, Char enemy, int damage) {
		if (damage <= 0) return damage;
		float steal = 0;
		if (active(hero, Ultimate.GLUTTON_AVATAR) != null) steal += 0.25f;
		if (active(hero, Ultimate.CRIMSON_NIGHT) != null) steal += 0.30f;
		if (steal > 0) heal(hero, Math.round(damage * steal));
		return damage;
	}

	/** from Hero.attackSkill */
	public static boolean sureHit(Hero hero) {
		return active(hero, Ultimate.DIVINE_RIGHT) != null;
	}

	/** from Hero.defenseSkill */
	public static float evasionMult(Hero hero) {
		return active(hero, Ultimate.SHADOW_DANCE) != null ? 2f : 1f;
	}

	/** from PrideGlory.shatter */
	public static boolean keepsDignity(Hero hero) {
		return active(hero, Ultimate.ABSOLUTE) != null;
	}

	/** from GoldenCard.discounted */
	public static boolean freePayments(Hero hero) {
		return active(hero, Ultimate.GOLDEN_AGE) != null;
	}

	/** from Mob.die */
	public static void onMobDeath(Mob mob) {
		Hero hero = Dungeon.hero;
		if (hero == null || !hero.isAlive()) return;
		if (marked(mob, Ultimate.BOUNTY)) {
			Dungeon.level.drop(new Gold(30 * Math.max(1, Dungeon.depth)), mob.pos).sprite.drop();
			GreedVault v = hero.buff(GreedVault.class);
			if (v != null) v.addCharge(v.maxCharge() * 0.25f);
		}
		if (marked(mob, Ultimate.VENDETTA)) {
			WrathGrudge g = hero.buff(WrathGrudge.class);
			if (g != null) g.rage = g.maxRage();
			BuffIndicator.refreshHero();
		}
	}
}
