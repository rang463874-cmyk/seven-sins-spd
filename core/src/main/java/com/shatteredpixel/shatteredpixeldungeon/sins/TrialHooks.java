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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
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

	/**
	 * The class's trial skill, or null. Nov 18 2026 (owner): every one is a single-target attack / control skill meant
	 * for boss fights; bosses get shorter controls, the rest the full effect.
	 */
	public static SinSkill skill(Hero hero) {
		if (hero == null) return null;
		BasicSkill s;
		switch (hero.heroClass) {
			case GLUTTONY:
				s = new BasicSkill(TrialHooks.class, id(hero), 72, SinSkill.CostType.SATIETY)
						.cost(h -> PredatorMaw.hungerCost(h, GLUTTONY_SATIETY))
						.usableIf(h -> ready(h) && PredatorMaw.canAfford(h, GLUTTONY_SATIETY));
				break;
			case GREED:
				s = new BasicSkill(TrialHooks.class, id(hero), 73, SinSkill.CostType.GAUGE)
						.cost(TrialHooks::greedCost)
						.usableIf(h -> ready(h) && h.buff(GreedVault.class) != null && h.buff(GreedVault.class).charge >= greedCost(h));
				break;
			case WRATH:
				s = new BasicSkill(TrialHooks.class, id(hero), 74, SinSkill.CostType.RAGE)
						.cost(h -> h.buff(WrathGrudge.class) == null ? WRATH_MIN_RAGE : Math.max(WRATH_MIN_RAGE, (int) h.buff(WrathGrudge.class).rage))
						.usableIf(h -> ready(h) && h.buff(WrathGrudge.class) != null && h.buff(WrathGrudge.class).rage >= WRATH_MIN_RAGE);
				break;
			case PRIDE:
				s = new BasicSkill(TrialHooks.class, id(hero), 75, SinSkill.CostType.DIGNITY)
						.cost(h -> PRIDE_DIGNITY)
						.usableIf(h -> ready(h) && h.buff(PrideGlory.class) != null && h.buff(PrideGlory.class).dignity >= PRIDE_DIGNITY);
				break;
			case LUST:
				s = new BasicSkill(TrialHooks.class, id(hero), 76, SinSkill.CostType.HP)
						.cost(h -> h.buff(LustDesire.class) == null ? 0 : h.buff(LustDesire.class).hpCost(LUST_HP))
						.usableIf(h -> ready(h) && h.buff(LustDesire.class) != null
								&& h.buff(LustDesire.class).canPay(h.buff(LustDesire.class).hpCost(LUST_HP)));
				break;
			case ENVY:
				s = new BasicSkill(TrialHooks.class, id(hero), 77, SinSkill.CostType.JEALOUSY)
						.cost(h -> ENVY_JEALOUSY)
						.usableIf(h -> ready(h) && h.buff(EnvyCovet.class) != null && h.buff(EnvyCovet.class).jealousy >= ENVY_JEALOUSY);
				break;
			case SLOTH:
				s = new BasicSkill(TrialHooks.class, id(hero), 78, SinSkill.CostType.TURNS)
						.cost(h -> SlothRepose.turnCost(h, SLOTH_TURNS))
						.usableIf(TrialHooks::ready);
				break;
			case SOLOMON:
				s = new BasicSkill(TrialHooks.class, id(hero), 79, SinSkill.CostType.VARIES) {
					@Override
					public String costShort(Hero h) {
						return Integer.toString(SOLOMON_MANA);
					}

					@Override
					public String costLabel(Hero h) {
						return Messages.get(com.shatteredpixel.shatteredpixeldungeon.items.SolomonRing.class, "cost_mana", SOLOMON_MANA);
					}
				}.usableIf(h -> ready(h) && h.buff(SolomonArcana.class) != null && h.buff(SolomonArcana.class).mana >= SOLOMON_MANA);
				break;
			default:
				return null;
		}
		return s.targetsEnemy().upperRow(true).onUse(h -> GameScene.selectCell(TARGET));
	}

	// ------------------------------------------------------------------ helpers

	private static boolean boss(Char ch) {
		return Char.hasProp(ch, Char.Property.BOSS) || Char.hasProp(ch, Char.Property.MINIBOSS);
	}

	/** a control's turns: full on regular enemies, this share on bosses/minibosses */
	private static float ctrl(Char ch, float turns, float bossShare) {
		return boss(ch) ? Math.max(1f, turns * bossShare) : turns;
	}

	/** the base damage roll of the trial skills */
	private static int roll(Hero h, float mult) {
		int lvl = Math.max(1, h.lvl);
		return Math.max(1, Math.round(Random.NormalIntRange(2 * lvl, 4 * lvl + 15) * mult));
	}

	/** deals the damage, returns what was actually taken off */
	private static int hit(Hero h, Char ch, int dmg) {
		int before = ch.HP + ch.shielding();
		ch.damage(dmg, h);
		if (ch.sprite != null) ch.sprite.flash();
		return Math.max(0, before - Math.max(0, ch.HP) - ch.shielding());
	}

	private static void heal(Hero h, int amount) {
		amount = Math.min(amount, h.HT - h.HP);
		if (amount <= 0) return;
		h.HP += amount;
		h.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(amount), FloatingText.HEALING);
	}

	private static void finish(Hero h, int target, boolean spendTurn) {
		Buff.affect(h, Cooldown.class, COOLDOWN);
		Invisibility.dispel();
		h.sprite.zap(target);
		h.sprite.showStatus(0xCC88FF, Messages.get(TrialHooks.class, "skill_" + id(h)));
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
		if (spendTurn) h.spendAndNext(1f);
	}

	/** pays the class's cost; false (nothing spent) if it can't */
	private static boolean pay(Hero h) {
		switch (h.heroClass) {
			case GLUTTONY:
				if (!PredatorMaw.canAfford(h, GLUTTONY_SATIETY)) return false;
				h.buff(Hunger.class).affectHunger(-PredatorMaw.hungerCost(h, GLUTTONY_SATIETY));
				SinArtifact.onSpent(h, 3);
				return true;
			case GREED: {
				GreedVault v = h.buff(GreedVault.class);
				return v != null && v.spend(greedCost(h));
			}
			case WRATH: {
				WrathGrudge g = h.buff(WrathGrudge.class);
				if (g == null || g.rage < WRATH_MIN_RAGE) return false;
				spentRage = (int) g.rage;
				return g.useRage(spentRage);
			}
			case PRIDE: {
				PrideGlory g = h.buff(PrideGlory.class);
				return g != null && g.useDignity(PRIDE_DIGNITY);
			}
			case LUST: {
				LustDesire d = h.buff(LustDesire.class);
				return d != null && d.pay(d.hpCost(LUST_HP));
			}
			case ENVY: {
				EnvyCovet c = h.buff(EnvyCovet.class);
				return c != null && c.useJealousy(ENVY_JEALOUSY);
			}
			case SLOTH:
				return true; //pays by dozing afterwards
			case SOLOMON: {
				SolomonArcana a = h.buff(SolomonArcana.class);
				return a != null && a.useMana(SOLOMON_MANA);
			}
			default:
				return false;
		}
	}

	private static int spentRage = 0;

	// ------------------------------------------------------------------ effects

	private static final CellSelector.Listener TARGET = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			Hero h = Dungeon.hero;
			if (cell == null || h == null) return;
			Char ch = Actor.findChar(cell);
			if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]) {
				GLog.w(Messages.get(TrialHooks.class, "no_target"));
				return;
			}
			if (!pay(h)) return;
			use(h, (Mob) ch);
		}

		@Override
		public String prompt() {
			return Messages.get(TrialHooks.class, "prompt");
		}
	};

	private static void use(Hero h, Mob m) {
		boolean doze = false;
		switch (h.heroClass) {
			case GLUTTONY: {
				//Devour: a huge bite that heals half of it and saps the target's strength
				int dealt = hit(h, m, roll(h, 1.6f));
				heal(h, dealt / 2);
				if (m.isAlive()) Buff.prolong(m, Weakness.class, 8f);
				CellEmitter.get(m.pos).burst(Speck.factory(Speck.BONE), 6);
				Sample.INSTANCE.play(Assets.Sounds.EAT);
				break;
			}
			case GREED: {
				//Midas Touch: turned to gold for a moment, and it stays brittle
				hit(h, m, roll(h, 1.0f));
				if (m.isAlive()) {
					Buff.prolong(m, Paralysis.class, ctrl(m, 6f, 0.34f));
					Buff.prolong(m, Vulnerable.class, 10f);
				}
				Dungeon.level.drop(new Gold(5 * Dungeon.depth), m.pos).sprite.drop();
				CellEmitter.get(m.pos).burst(Speck.factory(Speck.COIN), 12);
				Sample.INSTANCE.play(Assets.Sounds.GOLD);
				break;
			}
			case WRATH: {
				//Cataclysm: all the rage in one blow
				hit(h, m, spentRage + 2 * h.lvl);
				if (m.isAlive()) {
					Buff.prolong(m, Cripple.class, 6f);
					Buff.prolong(m, Paralysis.class, ctrl(m, 2f, 0.5f));
				}
				CellEmitter.get(m.pos).burst(Speck.factory(Speck.ROCK), 8);
				PixelScene.shake(3, 0.5f);
				Sample.INSTANCE.play(Assets.Sounds.BLAST);
				break;
			}
			case PRIDE: {
				//Royal Decree: an execution order, the deadlier the lower it has fallen
				float missing = 1f - m.HP / (float) Math.max(1, m.HT);
				hit(h, m, roll(h, 1.2f * (1f + missing)));
				if (m.isAlive()) {
					Buff.prolong(m, Paralysis.class, ctrl(m, 4f, 0.25f));
					Buff.prolong(m, Weakness.class, 8f);
				}
				m.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 8);
				Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
				break;
			}
			case LUST: {
				//Fatal Kiss: drains its life whole and leaves it smitten
				int dealt = hit(h, m, roll(h, 1.2f));
				heal(h, dealt);
				if (m.isAlive() && !LustDesire.charmImmune(m)) Buff.affect(m, Charm.class, ctrl(m, 6f, 0.34f)).object = h.id();
				m.sprite.centerEmitter().burst(Speck.factory(Speck.HEART), 8);
				Sample.INSTANCE.play(Assets.Sounds.CHARMS);
				break;
			}
			case ENVY: {
				//Covetous Curse: takes a share of what it has, and leaves it hexed, weak and exposed
				int share = Math.min(Math.round(m.HP * 0.12f), 4 * h.lvl + 20);
				hit(h, m, roll(h, 0.8f) + share);
				if (m.isAlive()) {
					Buff.prolong(m, Hex.class, 10f);
					Buff.prolong(m, Weakness.class, 10f);
					Buff.prolong(m, Vulnerable.class, 10f);
				}
				m.sprite.emitter().burst(ShadowParticle.CURSE, 10);
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				break;
			}
			case SLOTH: {
				//Nightmare: a dream that hurts; lesser foes don't wake from it, bosses crawl
				hit(h, m, roll(h, 1.0f));
				if (m.isAlive()) {
					if (boss(m)) {
						Buff.prolong(m, Slow.class, 8f);
						Buff.prolong(m, Paralysis.class, 1f);
					} else {
						Buff.affect(m, MagicalSleep.class);
					}
				}
				m.sprite.emitter().burst(Speck.factory(Speck.NOTE), 6);
				Sample.INSTANCE.play(Assets.Sounds.LULLABY);
				doze = true;
				break;
			}
			case SOLOMON: {
				//Binding Seal: a lesser foe swears itself to Solomon for good; the great ones are bound and scorched
				if (!boss(m) && !m.isImmune(Corruption.class)) {
					AllyBuff.affectAndLoot(m, h, Corruption.class);
					GLog.p(Messages.get(TrialHooks.class, "bound", m.name()));
				} else {
					SolomonArcana a = h.buff(SolomonArcana.class);
					hit(h, m, roll(h, 1.4f * (a == null ? 1f : a.spellPower())));
					if (m.isAlive()) {
						Buff.prolong(m, Roots.class, 4f);
						Buff.prolong(m, Hex.class, 8f);
					}
				}
				m.sprite.emitter().burst(ShadowParticle.UP, 10);
				Sample.INSTANCE.play(Assets.Sounds.CURSED);
				break;
			}
			default:
				break;
		}
		finish(h, m.pos, !doze);
		if (doze) SlothRepose.doze(h, SlothRepose.turnCost(h, SLOTH_TURNS));
	}
}
