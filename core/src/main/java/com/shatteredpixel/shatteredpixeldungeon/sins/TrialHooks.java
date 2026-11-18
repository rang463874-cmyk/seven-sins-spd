package com.shatteredpixel.shatteredpixeldungeon.sins;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SolomonArcana;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SinShadow;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MirrorRelics;
import com.shatteredpixel.shatteredpixeldungeon.items.PredatorMaw;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SinArtifact;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.quest.TrialSanctumRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.skills.BasicSkill;
import com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Seven Sins: the trial of sin (Nov 18 2026, owner's pick). On one city floor (16-19; 1/floors left, forced on 19) a
 * dark sanctum holds the hero's own shadow (actors/mobs/SinShadow), for every class with a signature item. Beating it
 * awakens the item a second time: straight to its +10 mirror form if it isn't there yet, then the level cap goes to
 * 20 (SinArtifact.awaken) and the class gets one more skill, its trial skill (added by SkillHolder.visibleSkills).
 * Once per run; state saved with the quests in Dungeon.
 */
public final class TrialHooks {

	private TrialHooks() {}

	/** 0 = not yet, 1 = the shadow waits on its floor, 2 = beaten */
	public static int stage = 0;
	private static boolean pending = false;

	public static final int FIRST_FLOOR = 16, LAST_FLOOR = 19;

	public static void reset() {
		stage = 0;
		pending = false;
	}

	private static final String NODE = "sevenSinsTrial";

	public static void storeInBundle(Bundle bundle) {
		Bundle node = new Bundle();
		node.put("stage", stage);
		bundle.put(NODE, node);
	}

	public static void restoreFromBundle(Bundle bundle) {
		reset();
		Bundle node = bundle.getBundle(NODE);
		if (node.isNull()) return;
		stage = node.getInt("stage");
	}

	/** classes with a signature item (the sins and Solomon) */
	public static boolean eligible(Hero hero) {
		return hero != null && MirrorRelics.rewardFor(hero.heroClass) != null;
	}

	/** RegularLevel.build: maybe this city floor gets the dark sanctum */
	public static void addSanctum(ArrayList<Room> rooms) {
		pending = false;
		if (stage != 0 || Dungeon.branch != 0 || Dungeon.bossLevel()) return;
		if (Dungeon.depth < FIRST_FLOOR || Dungeon.depth > LAST_FLOOR || !eligible(Dungeon.hero)) return;
		int floorsLeft = LAST_FLOOR - Dungeon.depth + 1;
		if (Random.Int(floorsLeft) != 0) return;
		rooms.add(new TrialSanctumRoom());
		pending = true;
	}

	/** EliteHooks.onLevelMobs: the shadow takes the pedestal */
	public static void onLevelMobs(Level level, ArrayList<Room> rooms) {
		if (!pending) return;
		pending = false;
		TrialSanctumRoom sanctum = null;
		for (Room r : rooms) if (r instanceof TrialSanctumRoom) sanctum = (TrialSanctumRoom) r;
		if (sanctum == null) return;
		SinShadow s = new SinShadow();
		s.cls = Dungeon.hero.heroClass;
		s.pos = level.pointToCell(sanctum.altar());
		s.home = s.pos;
		level.mobs.add(s);
		stage = 1;
	}

	/** SinShadow.onSlain */
	public static void onShadowSlain(Hero hero) {
		stage = 2;
		awaken(hero);
	}

	/** the reward: the signature item's second awakening (also used by the dev tools) */
	public static void awaken(Hero hero) {
		SinArtifact a = SinArtifact.of(hero);
		if (a == null) {
			GLog.w(Messages.get(TrialHooks.class, "no_item"));
			return;
		}
		if (a.awakened) return;
		if (!a.isMirrorForm()) {
			if (a.level() < SinArtifact.ASCEND_LEVEL) a.level(SinArtifact.ASCEND_LEVEL);
			a.ascend(hero);
			a = SinArtifact.of(hero);
			if (a == null) return;
		}
		a.awaken();
		GLog.p(Messages.get(TrialHooks.class, "awakened", a.name()));
		if (hero.sprite != null) hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 16);
		Sample.INSTANCE.play(Assets.Sounds.LEVELUP);
		hero.updateHT(false);
		Item.updateQuickslot();
		GameScene.syncSkillBarPos();
	}

	// ------------------------------------------------------------------ the trial skills (one per class)

	public static final float COOLDOWN = 40f;

	public static class Cooldown extends FlavourBuff {
		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon(com.watabou.noosa.Image icon) {
			icon.hardlight(0.7f, 0.3f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (COOLDOWN - visualcooldown()) / COOLDOWN);
		}
	}

	private static boolean ready(Hero h) {
		return h.buff(Cooldown.class) == null;
	}

	private static String id(Hero h) {
		return "trial_" + h.heroClass.name().toLowerCase();
	}

	// costs
	public static final int GLUTTONY_SATIETY = 150;
	public static final int WRATH_MIN_RAGE = 50;
	public static final int PRIDE_DIGNITY = 3;
	public static final float LUST_HP = 0.15f;
	public static final int ENVY_JEALOUSY = 60;
	public static final int SLOTH_TURNS = 6;
	public static final int SOLOMON_MANA = 20;

	private static int greedCost(Hero h) {
		GreedVault v = h.buff(GreedVault.class);
		float c = (40 + 8 * Dungeon.depth) * MetaTalents.costMult(h);
		if (v != null && v.mirrored) c *= 0.75f;
		if (UltimateSkills.freePayments(h)) return 0;
		return Math.max(1, Math.round(c));
	}

	/** the class's trial skill, or null */
	public static SinSkill skill(Hero hero) {
		if (hero == null) return null;
		switch (hero.heroClass) {
			case GLUTTONY:
				return new BasicSkill(TrialHooks.class, id(hero), 72, SinSkill.CostType.SATIETY)
						.cost(h -> PredatorMaw.hungerCost(h, GLUTTONY_SATIETY))
						.usableIf(h -> ready(h) && PredatorMaw.canAfford(h, GLUTTONY_SATIETY))
						.onUse(TrialHooks::feast);
			case GREED:
				return new BasicSkill(TrialHooks.class, id(hero), 73, SinSkill.CostType.GAUGE).targetsEnemy()
						.cost(TrialHooks::greedCost)
						.usableIf(h -> ready(h) && h.buff(GreedVault.class) != null && h.buff(GreedVault.class).charge >= greedCost(h))
						.onUse(h -> GameScene.selectCell(midas));
			case WRATH:
				return new BasicSkill(TrialHooks.class, id(hero), 74, SinSkill.CostType.RAGE)
						.cost(h -> h.buff(WrathGrudge.class) == null ? WRATH_MIN_RAGE : Math.max(WRATH_MIN_RAGE, (int) h.buff(WrathGrudge.class).rage))
						.usableIf(h -> ready(h) && h.buff(WrathGrudge.class) != null && h.buff(WrathGrudge.class).rage >= WRATH_MIN_RAGE)
						.onUse(TrialHooks::cataclysm);
			case PRIDE:
				return new BasicSkill(TrialHooks.class, id(hero), 75, SinSkill.CostType.DIGNITY)
						.cost(h -> PRIDE_DIGNITY)
						.usableIf(h -> ready(h) && h.buff(PrideGlory.class) != null && h.buff(PrideGlory.class).dignity >= PRIDE_DIGNITY)
						.onUse(TrialHooks::decree);
			case LUST:
				return new BasicSkill(TrialHooks.class, id(hero), 76, SinSkill.CostType.HP)
						.cost(h -> h.buff(LustDesire.class) == null ? 0 : h.buff(LustDesire.class).hpCost(LUST_HP))
						.usableIf(h -> ready(h) && h.buff(LustDesire.class) != null
								&& h.buff(LustDesire.class).canPay(h.buff(LustDesire.class).hpCost(LUST_HP)))
						.onUse(TrialHooks::attraction);
			case ENVY:
				return new BasicSkill(TrialHooks.class, id(hero), 77, SinSkill.CostType.JEALOUSY)
						.cost(h -> ENVY_JEALOUSY)
						.usableIf(h -> ready(h) && h.buff(EnvyCovet.class) != null && h.buff(EnvyCovet.class).jealousy >= ENVY_JEALOUSY)
						.onUse(TrialHooks::doppelganger);
			case SLOTH:
				return new BasicSkill(TrialHooks.class, id(hero), 78, SinSkill.CostType.TURNS)
						.cost(h -> SlothRepose.turnCost(h, SLOTH_TURNS))
						.usableIf(TrialHooks::ready)
						.onUse(TrialHooks::slumber);
			case SOLOMON:
				return new BasicSkill(TrialHooks.class, id(hero), 79, SinSkill.CostType.VARIES) {
							@Override
							public String costShort(Hero h) {
								return Integer.toString(SOLOMON_MANA);
							}

							@Override
							public String costLabel(Hero h) {
								return Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.SolomonRing.class, "cost_mana", SOLOMON_MANA);
							}
						}.targetsEnemy()
						.cost(h -> SOLOMON_MANA)
						.usableIf(h -> ready(h) && h.buff(SolomonArcana.class) != null && h.buff(SolomonArcana.class).mana >= SOLOMON_MANA)
						.onUse(h -> GameScene.selectCell(bind));
			default:
				return null;
		}
	}

	// ------------------------------------------------------------------ helpers

	private static boolean boss(Char ch) {
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	private static ArrayList<Mob> foes(Hero h, int range) {
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (m.alignment != Char.Alignment.ENEMY || !m.isAlive()) continue;
			if (!Dungeon.level.heroFOV[m.pos]) continue;
			if (range > 0 && Dungeon.level.distance(h.pos, m.pos) > range) continue;
			out.add(m);
		}
		return out;
	}

	private static void heal(Hero h, int amount) {
		amount = Math.min(amount, h.HT - h.HP);
		if (amount <= 0) return;
		h.HP += amount;
		h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	private static void finish(Hero h, boolean spendTurn) {
		Buff.affect(h, Cooldown.class, COOLDOWN);
		Invisibility.dispel();
		h.sprite.operate(h.pos);
		h.sprite.showStatus(0xCC88FF, Messages.get(TrialHooks.class, "skill_" + id(h)));
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
		if (spendTurn) h.spendAndNext(1f);
	}

	// ------------------------------------------------------------------ effects

	/** Gluttony, Feast: bites everything within 2, heals half of it; the weak are swallowed whole */
	private static void feast(Hero h) {
		if (!PredatorMaw.canAfford(h, GLUTTONY_SATIETY)) return;
		h.buff(Hunger.class).affectHunger(-PredatorMaw.hungerCost(h, GLUTTONY_SATIETY));
		SinArtifact.onSpent(h, 3);
		int total = 0;
		for (Mob m : foes(h, 2)) {
			int dmg = Random.NormalIntRange(h.lvl, 2 * h.lvl + 10);
			int before = m.HP;
			if (!boss(m) && m.HP - dmg <= m.HT / 4) dmg = m.HP;
			m.damage(dmg, h);
			total += Math.max(0, before - Math.max(0, m.HP));
			CellEmitter.get(m.pos).burst(Speck.factory(Speck.BONE), 4);
			if (!m.isAlive()) h.buff(Hunger.class).affectHunger(30);
		}
		heal(h, total / 2);
		Sample.INSTANCE.play(Assets.Sounds.EAT);
		finish(h, true);
	}

	/** Greed, Midas Touch: the target turns to gold for a while and sheds coins */
	private static final CellSelector.Listener midas = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero h = Dungeon.hero;
			if (cell == null) return;
			Char ch = Actor.findChar(cell);
			if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]) {
				GLog.w(Messages.get(TrialHooks.class, "no_target"));
				return;
			}
			GreedVault v = h.buff(GreedVault.class);
			if (v == null || !v.spend(greedCost(h))) return;
			if (boss(ch)) {
				Buff.prolong(ch, Slow.class, 4f);
			} else {
				Buff.prolong(ch, Paralysis.class, 8f);
			}
			Buff.prolong(ch, Vulnerable.class, 8f);
			Dungeon.level.drop(new Gold(5 * Dungeon.depth), ch.pos).sprite.drop();
			CellEmitter.get(ch.pos).burst(Speck.factory(Speck.COIN), 12);
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
			finish(h, true);
		}

		@Override
		public String prompt() {
			return Messages.get(TrialHooks.class, "prompt");
		}
	};

	/** Wrath, Cataclysm: all the rage at once into everything within 3 */
	private static void cataclysm(Hero h) {
		WrathGrudge g = h.buff(WrathGrudge.class);
		if (g == null || g.rage < WRATH_MIN_RAGE) return;
		int rage = (int) g.rage;
		g.useRage(rage);
		for (Mob m : foes(h, 3)) {
			m.damage(Math.round(rage * 0.5f) + h.lvl, h);
			if (m.isAlive()) Buff.prolong(m, Cripple.class, 5f);
			CellEmitter.get(m.pos).burst(Speck.factory(Speck.ROCK), 4);
		}
		PixelScene.shake(3, 0.5f);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		finish(h, true);
	}

	/** Pride, Royal Decree: every enemy in sight kneels */
	private static void decree(Hero h) {
		PrideGlory g = h.buff(PrideGlory.class);
		if (g == null || !g.useDignity(PRIDE_DIGNITY)) return;
		for (Mob m : foes(h, 0)) {
			if (boss(m)) {
				Buff.prolong(m, Weakness.class, 6f);
				Buff.prolong(m, Vulnerable.class, 6f);
			} else {
				Buff.prolong(m, Paralysis.class, 4f);
			}
			m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 4);
		}
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		finish(h, true);
	}

	/** Lust, Fatal Attraction: everything within 4 is charmed and drained */
	private static void attraction(Hero h) {
		LustDesire d = h.buff(LustDesire.class);
		if (d == null || !d.pay(d.hpCost(LUST_HP))) return;
		int total = 0;
		for (Mob m : foes(h, 4)) {
			if (!LustDesire.charmImmune(m)) Buff.affect(m, Charm.class, 8f).object = h.id();
			int before = m.HP;
			m.damage(h.lvl / 2 + 3, h);
			total += Math.max(0, before - Math.max(0, m.HP));
			m.sprite.centerEmitter().burst(Speck.factory(Speck.HEART), 4);
		}
		heal(h, total);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		finish(h, true);
	}

	/** Envy, Doppelganger: three copies of yourself */
	private static void doppelganger(Hero h) {
		EnvyCovet c = h.buff(EnvyCovet.class);
		if (c == null || !c.useJealousy(ENVY_JEALOUSY)) return;
		ScrollOfMirrorImage.spawnImages(h, 3);
		Sample.INSTANCE.play(Assets.Sounds.READ);
		finish(h, true);
	}

	/** Sloth, Eternal Slumber: every enemy in sight falls asleep; the sloth dozes too */
	private static void slumber(Hero h) {
		for (Mob m : foes(h, 0)) {
			if (boss(m)) Buff.prolong(m, Slow.class, 5f);
			else Buff.affect(m, MagicalSleep.class);
		}
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
		finish(h, false);
		SlothRepose.doze(h, SlothRepose.turnCost(h, SLOTH_TURNS));
	}

	/** Solomon, Binding Seal: a lesser foe swears itself to Solomon for good; the great ones are scorched */
	private static final CellSelector.Listener bind = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero h = Dungeon.hero;
			if (cell == null) return;
			Char ch = Actor.findChar(cell);
			if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]) {
				GLog.w(Messages.get(TrialHooks.class, "no_target"));
				return;
			}
			SolomonArcana a = h.buff(SolomonArcana.class);
			if (a == null || !a.useMana(SOLOMON_MANA)) return;
			Mob m = (Mob) ch;
			if (!boss(m) && !m.isImmune(Corruption.class)) {
				AllyBuff.affectAndLoot(m, h, Corruption.class);
				m.sprite.emitter().burst(ShadowParticle.UP, 10);
				GLog.p(Messages.get(TrialHooks.class, "bound", m.name()));
			} else {
				int lvl = Math.max(1, h.lvl);
				m.damage(Math.round(Random.NormalIntRange(2 * lvl, 4 * lvl + 10) * a.spellPower()), h);
				if (m.isAlive()) Buff.prolong(m, Hex.class, 8f);
				m.sprite.emitter().burst(ShadowParticle.CURSE, 10);
			}
			Sample.INSTANCE.play(Assets.Sounds.CURSED);
			finish(h, true);
		}

		@Override
		public String prompt() {
			return Messages.get(TrialHooks.class, "prompt");
		}
	};
}
