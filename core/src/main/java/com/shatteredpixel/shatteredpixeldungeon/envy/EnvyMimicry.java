package com.shatteredpixel.shatteredpixeldungeon.envy;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet.Trait;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DreamPuppet;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.EnvyMirror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sloth.SlothHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Envy's mimicry: copies of the other sins' skills (unlocked one by one with hero level) and of
 * special monsters' abilities (unlocked by registering them in the bestiary). Every copy is paid
 * for from Envy's slowly filling jealousy gauge; the mirror holds a few of them at a time.
 * Envy can also imitate one other sin's way of growing, at half efficiency (see EnvyHooks).
 */
public final class EnvyMimicry {

	private EnvyMimicry() {}

	public enum Kind { SIN, MONSTER }

	public enum Mimic {
		// ---- the other sins' skills, by unlock level
		BITE(HeroClass.GLUTTONY, 2, 1, 20),
		COIN(HeroClass.GREED, 4, 8, 15),
		PROVOKE(HeroClass.WRATH, 6, 16, 20),
		COMMAND(HeroClass.PRIDE, 8, 24, 25),
		KISS(HeroClass.LUST, 10, 32, 25),
		PUPPET(HeroClass.SLOTH, 12, 48, 35),
		ACID(HeroClass.GLUTTONY, 14, 4, 30),
		GUARD(HeroClass.GREED, 16, 9, 25),
		CONTAGION(HeroClass.WRATH, 18, 18, 35),
		PROCESSION(HeroClass.PRIDE, 20, 26, 20),
		STEP(HeroClass.LUST, 22, 33, 20),
		YAWN(HeroClass.SLOTH, 24, 51, 30),
		// ---- monster abilities, by the bestiary entries that teach them
		ROCK(56, 25, Trait.GNOLL_GEOMANCER, Trait.CRYSTAL_GUARDIAN, Trait.ARMORED_BRUTE, Trait.GNOLL_EXILE),
		GAS(57, 30, Trait.DM300, Trait.CAUSTIC_SLIME, Trait.ACIDIC, Trait.FETID_RAT),
		LEAP(58, 20, Trait.TENGU, Trait.BANDIT, Trait.GNOLL_TRICKSTER, Trait.PHANTOM_PIRANHA),
		DRAIN(59, 25, Trait.SPECTRAL_NECRO, Trait.TORMENTED_SPIRIT, Trait.ALBINO),
		FIRE(60, 25, Trait.NEWBORN_ELEMENTAL, Trait.CHAOS_ELEMENTAL),
		OOZE(61, 25, Trait.GOO, Trait.ROT_HEART, Trait.FUNGAL_CORE),
		SHELL(62, 30, Trait.HERMIT_CRAB, Trait.GREAT_CRAB, Trait.DWARF_KING, Trait.DM201, Trait.GOLDEN_MIMIC),
		GAZE(63, 40, Trait.YOG, Trait.SENIOR);

		public final Kind kind;
		public final HeroClass sin;
		public final int level;
		public final int icon;
		public final int baseCost;
		public final Trait[] teachers;

		Mimic(HeroClass sin, int level, int icon, int cost) {
			this.kind = Kind.SIN;
			this.sin = sin;
			this.level = level;
			this.icon = icon;
			this.baseCost = cost;
			this.teachers = new Trait[0];
		}

		Mimic(int icon, int cost, Trait... teachers) {
			this.kind = Kind.MONSTER;
			this.sin = null;
			this.level = 0;
			this.icon = icon;
			this.baseCost = cost;
			this.teachers = teachers;
		}

		public String id() {
			return "m_" + name().toLowerCase();
		}

		public String title() {
			return Messages.get(EnvyMirror.class, "skill_" + id());
		}

		public boolean unlocked(Hero hero, EnvyCovet c) {
			if (kind == Kind.SIN) return hero.lvl >= level;
			for (Trait t : teachers) if (c.has(t)) return true;
			return false;
		}

		/** where it comes from, for the selection window */
		public String source() {
			if (kind == Kind.SIN) return Messages.get(EnvyMimicry.class, "src_sin", sin.title(), level);
			StringBuilder sb = new StringBuilder();
			for (Trait t : teachers) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(t.title());
			}
			return Messages.get(EnvyMimicry.class, "src_monster", sb.toString());
		}
	}

	/** the other sins whose growth Envy can imitate */
	public enum Growth {
		NONE, GLUTTONY, GREED, WRATH, PRIDE, LUST, SLOTH;

		public String title() {
			return Messages.get(EnvyMimicry.class, "growth_" + name().toLowerCase());
		}

		public String desc() {
			return Messages.get(EnvyMimicry.class, "growth_" + name().toLowerCase() + "_desc");
		}
	}

	/** share of the original sin's growth that Envy gets */
	public static float growthEfficiency(Hero hero) {
		return 0.5f + 0.15f * hero.pointsInTalent(Talent.ENVY_SWITCH);
	}

	public static int cost(Hero hero, Mimic m) {
		float mult = 1f - 0.10f * hero.pointsInTalent(Talent.ENVY_LEVEL);
		if (m.kind == Kind.SIN && hero.subClass == HeroSubClass.USURPER) {
			mult *= 0.75f - 0.05f * hero.pointsInTalent(Talent.USURP_DRAIN);
		}
		return Math.max(1, Math.round(m.baseCost * mult));
	}

	/** Copycat's Mask: monster abilities hit harder */
	public static float power(Hero hero, Mimic m) {
		if (m.kind == Kind.MONSTER && hero.subClass == HeroSubClass.COPYCAT) {
			return 1f + 0.2f * hero.pointsInTalent(Talent.COPY_MASK);
		}
		return 1f;
	}

	/** description arguments for the skill window */
	public static Object[] descArgs(Hero hero, Mimic m) {
		int lvl = hero.lvl;
		float p = power(hero, m);
		switch (m) {
			case COIN: return new Object[]{1 + lvl / 2, 4 + lvl};
			case GUARD: return new Object[]{guardAmount(hero)};
			case ROCK: return new Object[]{Math.round((2 + lvl / 2) * p), Math.round((6 + lvl) * p)};
			case DRAIN: return new Object[]{Math.round((2 + lvl / 3) * p), Math.round((5 + lvl * 2 / 3) * p)};
			case SHELL: return new Object[]{Math.round(hero.HT * 0.20f * p)};
			case GAZE: return new Object[]{Math.round((4 + lvl) * p), Math.round((8 + lvl * 1.5f) * p)};
			default: return new Object[0];
		}
	}

	private static int guardAmount(Hero hero) {
		return 5 + hero.lvl + Math.round(hero.HT * 0.10f);
	}

	// ------------------------------------------------------------------ using

	private static Mimic pending;

	public static void use(Hero hero, Mimic m) {
		EnvyCovet c = hero.buff(EnvyCovet.class);
		if (c == null) return;
		if (c.jealousy < cost(hero, m)) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_jealousy"));
			return;
		}
		switch (m) {
			//instant ones
			case ACID: acid(hero, m); return;
			case GUARD:
				if (!pay(hero, m)) return;
				Buff.affect(hero, Barrier.class).incShield(guardAmount(hero));
				hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(guardAmount(hero)));
				finish(hero, hero.pos);
				return;
			case PUPPET:
				DreamPuppet p = DreamPuppet.create(true, 30f);
				if (!SlothHooks.spawnNear(hero.pos, p)) {
					GLog.w(Messages.get(EnvyMimicry.class, "no_room"));
					return;
				}
				pay(hero, m);
				finish(hero, p.pos);
				return;
			case STEP:
				if (!pay(hero, m)) return;
				Buff.prolong(hero, EnvyCovet.Sway.class, 6f);
				hero.sprite.emitter().burst(Speck.factory(Speck.HEART), 3);
				finish(hero, hero.pos);
				return;
			case YAWN: yawn(hero, m); return;
			case OOZE: ooze(hero, m); return;
			case SHELL:
				if (!pay(hero, m)) return;
				int sh = Math.round(hero.HT * 0.20f * power(hero, m));
				Buff.affect(hero, Barrier.class).incShield(sh);
				hero.sprite.showStatus(CharSprite.POSITIVE, Integer.toString(sh));
				hero.sprite.emitter().burst(Speck.factory(Speck.FORGE), 6);
				finish(hero, hero.pos);
				return;
			default:
				//targeted
				pending = m;
				GameScene.selectCell(targeter);
		}
	}

	private static boolean pay(Hero hero, Mimic m) {
		EnvyCovet c = hero.buff(EnvyCovet.class);
		if (c == null || !c.useJealousy(cost(hero, m))) return false;
		if (hero.pointsInTalent(Talent.ENVY_PETTY) > 0) Buff.prolong(hero, EnvyCovet.Spite.class, 10f);
		//Doppelganger: wearing another sin's skill teaches a little of its strength
		int crown = hero.pointsInTalent(Talent.USURP_CROWN);
		if (m.kind == Kind.SIN && crown > 0 && hero.subClass == HeroSubClass.USURPER) c.addEnvy(crown);
		return true;
	}

	private static void finish(Hero hero, int pos) {
		if (pos != hero.pos) hero.sprite.zap(pos);
		else hero.sprite.operate(hero.pos);
		CellEmitter.center(pos).burst(Speck.factory(Speck.LIGHT), 3);
		Sample.INSTANCE.play(Assets.Sounds.TELEPORT, 0.5f, 1.5f);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	private static Mob enemyAt(Integer cell, int range) {
		Hero hero = Dungeon.hero;
		Char ch = cell == null ? null : Actor.findChar(cell);
		if (!(ch instanceof Mob) || ch.alignment == Char.Alignment.ALLY || !Dungeon.level.heroFOV[cell]
				|| (range > 0 && Dungeon.level.distance(hero.pos, cell) > range)) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_target"));
			return null;
		}
		return (Mob) ch;
	}

	private static boolean boss(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS);
	}

	/** an empty visible tile within range, or -1 */
	private static int emptyTile(Integer cell, int range) {
		Hero hero = Dungeon.hero;
		if (cell == null || cell == hero.pos || !Dungeon.level.heroFOV[cell] || !Dungeon.level.passable[cell]
				|| Dungeon.level.avoid[cell] || Actor.findChar(cell) != null
				|| Dungeon.level.distance(hero.pos, cell) > range) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_tile"));
			return -1;
		}
		return cell;
	}

	private static void blink(Hero hero, int to) {
		ScrollOfTeleportation.appear(hero, to);
		Dungeon.level.occupyCell(hero);
		Dungeon.observe();
		GameScene.updateFog();
	}

	/** free tile next to the target closest to the hero (or the hero's own tile if adjacent) */
	private static int landNextTo(Hero hero, Char target) {
		if (Dungeon.level.adjacent(hero.pos, target.pos)) return hero.pos;
		int land = -1;
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = target.pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null
					&& Dungeon.level.heroFOV[c]
					&& (land == -1 || Dungeon.level.distance(hero.pos, c) < Dungeon.level.distance(hero.pos, land))) {
				land = c;
			}
		}
		return land;
	}

	private static void heal(Hero hero, int amount) {
		amount = Math.min(amount, hero.HT - hero.HP);
		if (amount <= 0) return;
		hero.HP += amount;
		hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	private static final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Mimic m = pending;
			if (cell == null || m == null) return;
			Hero hero = Dungeon.hero;
			switch (m) {
				case BITE: bite(hero, m, cell); break;
				case COIN: coin(hero, m, cell); break;
				case PROVOKE: provoke(hero, m, cell); break;
				case COMMAND: command(hero, m, cell); break;
				case KISS: kiss(hero, m, cell); break;
				case CONTAGION: contagion(hero, m, cell); break;
				case PROCESSION: {
					int to = emptyTile(cell, 5);
					if (to < 0 || !pay(hero, m)) return;
					blink(hero, to);
					finish(hero, hero.pos);
					break;
				}
				case ROCK: rock(hero, m, cell); break;
				case GAS: {
					if (!Dungeon.level.heroFOV[cell] || Dungeon.level.solid[cell] || Dungeon.level.distance(hero.pos, cell) > 6) {
						GLog.w(Messages.get(EnvyMimicry.class, "no_tile"));
						return;
					}
					if (!pay(hero, m)) return;
					GameScene.add(Blob.seed(cell, Math.round(250 * power(hero, m)), ToxicGas.class));
					Sample.INSTANCE.play(Assets.Sounds.GAS);
					finish(hero, cell);
					break;
				}
				case LEAP: {
					int to = emptyTile(cell, 6);
					if (to < 0 || !pay(hero, m)) return;
					blink(hero, to);
					Buff.prolong(hero, Invisibility.class, 3f);
					hero.sprite.operate(hero.pos);
					Sample.INSTANCE.play(Assets.Sounds.PUFF);
					hero.spendAndNext(1f);
					break;
				}
				case DRAIN: drain(hero, m, cell); break;
				case FIRE: {
					Mob mob = enemyAt(cell, 0);
					if (mob == null || !pay(hero, m)) return;
					Buff.affect(mob, Burning.class).reignite(mob, 6f * power(hero, m));
					finish(hero, mob.pos);
					break;
				}
				case GAZE: gaze(hero, m, cell); break;
				default: break;
			}
		}

		@Override
		public String prompt() {
			return Messages.get(EnvyMimicry.class, "prompt", pending == null ? "" : pending.title());
		}
	};

	// ------------------------------------------------------------------ copied sin skills

	private static void bite(Hero hero, Mimic m, int cell) {
		Char enemy = Actor.findChar(cell);
		if (enemy == null || enemy == hero || enemy.alignment == Char.Alignment.ALLY || !Dungeon.level.adjacent(hero.pos, cell)) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_adjacent"));
			return;
		}
		if (!pay(hero, m)) return;
		int before = enemy.HP + enemy.shielding();
		hero.sprite.turnTo(hero.pos, cell);
		hero.attack(enemy, 1.5f, 0f, 1000f);
		int dealt = Math.max(0, before - (enemy.isAlive() ? enemy.HP + enemy.shielding() : 0));
		heal(hero, Math.round(dealt * 0.30f));
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		finish(hero, hero.pos);
	}

	private static void coin(Hero hero, Mimic m, int cell) {
		final Ballistica shot = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
		final int target = shot.collisionPos;
		final Char ch = Actor.findChar(target);
		if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_target"));
			return;
		}
		if (!pay(hero, m)) return;
		hero.sprite.zap(target);
		hero.busy();
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.BEACON, hero.sprite, target, () -> {
			ch.damage(Random.NormalIntRange(1 + hero.lvl / 2, 4 + hero.lvl), EnvyMimicry.class);
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
			Invisibility.dispel();
			hero.spendAndNext(1f);
		});
	}

	private static void provoke(Hero hero, Mimic m, int cell) {
		Mob mob = enemyAt(cell, 0);
		if (mob == null) return;
		if (boss(mob)) {
			GLog.w(Messages.get(EnvyMimicry.class, "immune"));
			return;
		}
		if (!pay(hero, m)) return;
		Buff.prolong(mob, Amok.class, 8f);
		mob.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 3);
		finish(hero, mob.pos);
	}

	private static void command(Hero hero, Mimic m, int cell) {
		Mob mob = enemyAt(cell, 0);
		if (mob == null) return;
		if (boss(mob)) {
			GLog.w(Messages.get(EnvyMimicry.class, "immune"));
			return;
		}
		if (!pay(hero, m)) return;
		Buff.prolong(mob, Paralysis.class, 3f);
		mob.sprite.emitter().burst(Speck.factory(Speck.STAR), 4);
		finish(hero, mob.pos);
	}

	private static void kiss(Hero hero, Mimic m, int cell) {
		Mob mob = enemyAt(cell, 3);
		if (mob == null) return;
		int land = landNextTo(hero, mob);
		if (land < 0) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_tile"));
			return;
		}
		if (!pay(hero, m)) return;
		if (land != hero.pos) blink(hero, land);
		hero.sprite.turnTo(hero.pos, mob.pos);
		hero.attack(mob, 1f, 0f, 1000f);
		if (mob.isAlive() && !LustDesire.charmImmune(mob)) LustDesire.charm(hero, mob, 4f);
		if (mob.sprite != null) mob.sprite.emitter().burst(Speck.factory(Speck.HEART), 4);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		hero.spendAndNext(1f);
	}

	private static void acid(Hero hero, Mimic m) {
		ArrayList<Char> targets = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch != hero && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.heroFOV[ch.pos] && Dungeon.level.distance(hero.pos, ch.pos) <= 2) {
				targets.add(ch);
			}
		}
		if (targets.isEmpty()) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_target"));
			return;
		}
		if (!pay(hero, m)) return;
		Splash.at(hero.pos, 0x99BB22, 16);
		for (Char ch : targets) {
			ch.damage(Random.NormalIntRange(1 + hero.lvl / 4, 3 + hero.lvl / 2), EnvyMimicry.class);
			if (ch.isAlive()) {
				Buff.prolong(ch, Weakness.class, 6f);
				Buff.prolong(ch, Cripple.class, 4f);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.GAS);
		finish(hero, hero.pos);
	}

	private static void contagion(Hero hero, Mimic m, int cell) {
		Mob mob = enemyAt(cell, 0);
		if (mob == null) return;
		if (!pay(hero, m)) return;
		for (Mob other : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (other.alignment == Char.Alignment.ENEMY && !boss(other)
					&& Dungeon.level.distance(mob.pos, other.pos) <= 2) {
				Buff.prolong(other, Amok.class, 6f);
				if (other.sprite != null) other.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 2);
			}
		}
		finish(hero, mob.pos);
	}

	private static void yawn(Hero hero, Mimic m) {
		if (!pay(hero, m)) return;
		for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (mob.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[mob.pos]
					&& Dungeon.level.distance(hero.pos, mob.pos) <= 2 && !boss(mob)) {
				Buff.affect(mob, MagicalSleep.class);
				if (mob.sprite != null) mob.sprite.emitter().burst(Speck.factory(Speck.NOTE), 2);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		finish(hero, hero.pos);
	}

	// ------------------------------------------------------------------ copied monster abilities

	private static void rock(Hero hero, Mimic m, int cell) {
		final Ballistica shot = new Ballistica(hero.pos, cell, Ballistica.PROJECTILE);
		final int target = shot.collisionPos;
		final Char ch = Actor.findChar(target);
		if (ch == null || ch == hero || ch.alignment == Char.Alignment.ALLY) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_target"));
			return;
		}
		if (!pay(hero, m)) return;
		final float p = power(hero, m);
		hero.sprite.zap(target);
		hero.busy();
		MagicMissile.boltFromChar(hero.sprite.parent, MagicMissile.EARTH, hero.sprite, target, () -> {
			ch.damage(Math.round(Random.NormalIntRange(2 + hero.lvl / 2, 6 + hero.lvl) * p), EnvyMimicry.class);
			if (ch.isAlive()) Buff.prolong(ch, Paralysis.class, 1f);
			Sample.INSTANCE.play(Assets.Sounds.HIT_CRUSH);
			Invisibility.dispel();
			hero.spendAndNext(1f);
		});
	}

	private static void drain(Hero hero, Mimic m, int cell) {
		Mob mob = enemyAt(cell, 0);
		if (mob == null || !pay(hero, m)) return;
		float p = power(hero, m);
		int dmg = Math.round(Random.NormalIntRange(2 + hero.lvl / 3, 5 + hero.lvl * 2 / 3) * p);
		dmg = Math.min(dmg, mob.HP);
		mob.damage(dmg, EnvyMimicry.class);
		heal(hero, dmg);
		if (mob.sprite != null) mob.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
		finish(hero, mob.pos);
	}

	private static void ooze(Hero hero, Mimic m) {
		ArrayList<Char> targets = new ArrayList<>();
		for (int n : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar(hero.pos + n);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) targets.add(ch);
		}
		if (targets.isEmpty()) {
			GLog.w(Messages.get(EnvyMimicry.class, "no_target"));
			return;
		}
		if (!pay(hero, m)) return;
		float p = power(hero, m);
		Splash.at(hero.pos, 0x000000, 14);
		for (Char ch : targets) {
			ch.damage(Math.round(Random.NormalIntRange(1 + hero.lvl / 3, 3 + hero.lvl / 2) * p), EnvyMimicry.class);
			if (ch.isAlive()) Buff.affect(ch, Ooze.class).set(Ooze.DURATION);
		}
		finish(hero, hero.pos);
	}

	private static void gaze(Hero hero, Mimic m, int cell) {
		if (cell == hero.pos) return;
		Ballistica beam = new Ballistica(hero.pos, cell, Ballistica.STOP_SOLID);
		int end = beam.collisionPos;
		ArrayList<Char> hit = new ArrayList<>();
		for (int c : beam.subPath(1, Math.min(beam.dist, 8))) {
			Char ch = Actor.findChar(c);
			if (ch != null && ch.alignment == Char.Alignment.ENEMY) hit.add(ch);
			end = c;
		}
		if (!pay(hero, m)) return;
		float p = power(hero, m);
		hero.sprite.zap(end);
		hero.sprite.parent.add(new Beam.DeathRay(hero.sprite.center(), DungeonTilemap.raisedTileCenterToWorld(end)));
		for (Char ch : hit) {
			ch.damage(Math.round(Random.NormalIntRange(4 + hero.lvl, Math.round(8 + hero.lvl * 1.5f)) * p), EnvyMimicry.class);
		}
		Sample.INSTANCE.play(Assets.Sounds.RAY);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}
}
