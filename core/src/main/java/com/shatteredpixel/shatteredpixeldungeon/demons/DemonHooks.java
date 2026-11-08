package com.shatteredpixel.shatteredpixeldungeon.demons;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonPerk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DemonTrial;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DemonAltar;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DemonLordNPC;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.SinEssence;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinRewards;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

/**
 * The demon lords' pact, run by static state saved with the quests:
 *   NONE -> (altar on Goo's floor, pick a lord) TRIAL -> (Tengu falls) GRANTED -> (beat the lord) DEFEATED.
 * Trial penalties and perk passives are applied through the hooks below.
 */
public class DemonHooks {

	public static final int NONE = 0, TRIAL = 1, GRANTED = 2, DEFEATED = 3;

	/** the lord's domain: a side branch of Tengu's floor */
	public static final int ARENA_BRANCH = 7;
	public static final int ARENA_DEPTH = 10;

	/** takes the hero to the lord's domain for the boss fight */
	public static void travelToArena() {
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.mode =
				com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.Mode.RETURN;
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.returnDepth = ARENA_DEPTH;
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.returnBranch = ARENA_BRANCH;
		com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.returnPos =
				com.shatteredpixel.shatteredpixeldungeon.levels.DemonArenaLevel.arrivalCell();
		com.watabou.noosa.Game.switchScene(com.shatteredpixel.shatteredpixeldungeon.scenes.InterlevelScene.class);
	}

	public static DemonLord lord = null;
	public static int stage = NONE;
	/** set once the altar has been placed this run */
	public static boolean altarSpawned = false;

	public static void reset() {
		lord = null;
		stage = NONE;
		altarSpawned = false;
	}

	private static final String NODE = "sevenSinsDemons";

	public static void storeInBundle(Bundle bundle) {
		Bundle node = new Bundle();
		node.put("lord", lord == null ? "" : lord.name());
		node.put("stage", stage);
		node.put("altar", altarSpawned);
		bundle.put(NODE, node);
	}

	public static void restoreFromBundle(Bundle bundle) {
		reset();
		Bundle node = bundle.getBundle(NODE);
		if (node.isNull()) return;
		String l = node.getString("lord");
		lord = l == null || l.isEmpty() ? null : DemonLord.valueOf(l);
		stage = node.getInt("stage");
		altarSpawned = node.getBoolean("altar");
	}

	/** whether the hero's own sin is this lord's (special dialogue) */
	public static boolean ownLord(DemonLord d) {
		return d != null && Dungeon.hero != null && SinRewards.sinOf(Dungeon.hero) == d.sin;
	}

	/** picks the right line: "<part>_own" for the hero's own lord, else "<part>" */
	public static String line(DemonLord d, String part, Object... args) {
		return d.get(ownLord(d) ? part + "_own" : part, args);
	}

	public static boolean trial(DemonLord d) {
		return stage == TRIAL && lord == d;
	}

	public static boolean perk(DemonLord d) {
		return (stage == GRANTED || stage == DEFEATED) && lord == d;
	}

	/** the perk is stronger once the lord itself has been defeated */
	public static float perkPower() {
		return stage == DEFEATED ? 1.5f : 1f;
	}

	// ------------------------------------------------------------------ story beats

	private static int freeCellNear(int pos) {
		if (Dungeon.level.passable[pos] && Actor.findChar(pos) == null && Dungeon.level.heaps.get(pos) == null) return pos;
		for (int r = 1; r <= 3; r++) {
			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (Dungeon.level.distance(pos, i) != r) continue;
				if (Dungeon.level.passable[i] && !Dungeon.level.solid[i] && Actor.findChar(i) == null
						&& Dungeon.level.heaps.get(i) == null && !Dungeon.level.pit[i]) return i;
			}
		}
		return -1;
	}

	/** Goo.die: the altar rises where the goo fell */
	public static void onGooDefeated(int pos) {
		if (altarSpawned || stage != NONE || Dungeon.level == null) return;
		//on the goo's own starting tile if possible
		int cell = freeCellNear(pos);
		if (cell == -1) return;
		DemonAltar altar = new DemonAltar();
		altar.pos = cell;
		GameScene.add(altar);
		CellEmitter.get(cell).burst(ShadowParticle.UP, 12);
		altarSpawned = true;
		GLog.n(Messages.get(DemonAltar.class, "appears"));
	}

	/** chosen at the altar */
	public static void beginTrial(Hero hero, DemonLord d) {
		lord = d;
		stage = TRIAL;
		Buff.affect(hero, DemonTrial.class);
		hero.updateHT(false);
	}

	/** PrisonBossLevel, once Tengu is beaten: the lord comes to judge the trial */
	public static void onTenguDefeated(int nearCell) {
		if (stage != TRIAL || lord == null || Dungeon.level == null) return;
		int cell = freeCellNear(nearCell);
		if (cell == -1) return;
		DemonLordNPC npc = new DemonLordNPC();
		npc.lord = lord;
		npc.pos = cell;
		GameScene.add(npc);
		CellEmitter.get(cell).burst(ShadowParticle.UP, 16);
		GLog.n(Messages.get(DemonLordNPC.class, "appears", lord.title()));
	}

	/** the lord's skill for the skill bar's extra row, or null before it is granted */
	public static com.shatteredpixel.shatteredpixeldungeon.skills.SinSkill skill(Hero hero) {
		if (stage < GRANTED || lord == null || hero == null || hero.buff(DemonPerk.class) == null) return null;
		return new DemonSkill(lord);
	}

	/** the lord accepts the trial: trial ends, its skill and passive are granted */
	public static void grantPerk(Hero hero) {
		if (stage != TRIAL || lord == null) return;
		stage = GRANTED;
		Buff.detach(hero, DemonTrial.class);
		Buff.affect(hero, DemonPerk.class);
		//the skill appears in its own row above the skill bar
		com.shatteredpixel.shatteredpixeldungeon.utils.GLog.p(Messages.get(DemonLordNPC.class, "skill_granted", lord.get("skill_name")));
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.syncSkillBarPos();
		hero.updateHT(false);
	}

	/** the lord's boss form was defeated */
	public static void onLordDefeated(int pos) {
		if (lord == null) return;
		stage = DEFEATED;
		SinEssence ess = new SinEssence();
		ess.quantity(3);
		Dungeon.level.drop(ess, pos).sprite.drop();
		GLog.p(Messages.get(DemonLordNPC.class, "empowered"));
		if (Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.DemonArenaLevel) {
			((com.shatteredpixel.shatteredpixeldungeon.levels.DemonArenaLevel) Dungeon.level).unlockPortal();
			GLog.i(Messages.get(DemonLordNPC.class, "portal_open"));
		}
	}

	/** dev tools: NONE -> altar next to the hero, TRIAL -> the lord appears, GRANTED -> its boss form */
	public static void devAdvance(Hero hero) {
		if (hero == null || Dungeon.level == null) return;
		switch (stage) {
			case NONE:
				altarSpawned = false;
				onGooDefeated(hero.pos);
				break;
			case TRIAL:
				onTenguDefeated(hero.pos);
				break;
			case GRANTED:
				boolean found = false;
				for (com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob m : Dungeon.level.mobs) {
					if (m instanceof DemonLordNPC) found = true;
				}
				if (!found) {
					int cell = freeCellNear(hero.pos);
					if (cell == -1) return;
					DemonLordNPC npc = new DemonLordNPC();
					npc.lord = lord;
					npc.pos = cell;
					GameScene.add(npc);
				}
				GLog.i("마왕에게 말을 걸어 보스전을 시작하세요.");
				break;
			default:
				reset();
				Buff.detach(hero, DemonTrial.class);
				Buff.detach(hero, DemonPerk.class);
				hero.updateHT(false);
				GLog.i("마왕 진행도를 초기화했습니다.");
				break;
		}
	}

	// ------------------------------------------------------------------ trial penalties and perk passives

	/** Hero.updateHT: Lucifer's trial takes a quarter of max HP */
	public static int bonusHT(Hero hero, int ht) {
		if (trial(DemonLord.LUCIFER)) return Math.max(1, Math.round(ht * 0.75f));
		return ht;
	}

	/** Gold.doPickUp: Mammon's trial halves gold, his perk adds 30% (45% once he is beaten) */
	public static int goldDelta(Hero hero, int quantity) {
		if (trial(DemonLord.MAMMON)) return -quantity / 2;
		if (perk(DemonLord.MAMMON)) return Math.round(quantity * 0.30f * perkPower());
		return 0;
	}

	/** Regeneration: Asmodeus's trial stops natural regeneration */
	public static boolean regenBlocked() {
		return trial(DemonLord.ASMODEUS);
	}

	/** Hero.attackSkill / defenseSkill: Leviathan's trial makes you clumsier */
	public static float accuracy(Hero hero, float acc) {
		return trial(DemonLord.LEVIATHAN) ? acc * 0.8f : acc;
	}

	public static float evasion(Hero hero, float ev) {
		return trial(DemonLord.LEVIATHAN) ? ev * 0.8f : ev;
	}

	/** Hunger.act: Beelzebub's trial makes you hungry twice as fast */
	public static float hungerRate() {
		return trial(DemonLord.BEELZEBUB) ? 2f : 1f;
	}

	/** Hero.speed: Belphegor's trial slows you down */
	public static float moveSpeed() {
		return trial(DemonLord.BELPHEGOR) ? 0.75f : 1f;
	}

	/** Char.damage, before it lands */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (!(target instanceof Hero) || dmg <= 0) return dmg;
		//Satan's trial: enemies hit 25% harder
		if (trial(DemonLord.SATAN) && src instanceof Char && ((Char) src).alignment == Char.Alignment.ENEMY) {
			dmg = Math.round(dmg * 1.25f);
		}
		//Leviathan's perk: the deep throws a fifth of every blow back
		if (perk(DemonLord.LEVIATHAN) && src instanceof Char && src != target && ((Char) src).isAlive()) {
			int back = Math.round(dmg * 0.20f * perkPower());
			if (back > 0) ((Char) src).damage(back, DemonSeal.class);
		}
		return dmg;
	}

	/** Hero.attackProc */
	public static int onAttack(Hero hero, Char enemy, int dmg) {
		if (stage < GRANTED || lord == null) return dmg;
		float p = perkPower();
		switch (lord) {
			case LUCIFER:
				//the light-bringer shines while unhurt
				if (hero.HP >= hero.HT * 0.75f) dmg = Math.round(dmg * (1f + 0.20f * p));
				break;
			case SATAN:
				//+3% per 10% of health missing
				float missing = 1f - hero.HP / (float) Math.max(1, hero.HT);
				dmg = Math.round(dmg * (1f + 0.30f * p * missing));
				break;
			case ASMODEUS:
				int heal = Math.min(hero.HT - hero.HP, Math.round(dmg * 0.10f * p));
				if (heal > 0) {
					hero.HP += heal;
					hero.sprite.showStatusWithIcon(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
							Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
				}
				break;
			default:
				break;
		}
		return dmg;
	}

	private static float restPartial = 0;

	/** Hero.rest: Belphegor's perk heals 1% max HP per turn of rest */
	public static void onRest(Hero hero) {
		if (!perk(DemonLord.BELPHEGOR) || hero.HP >= hero.HT) return;
		restPartial += hero.HT * 0.01f * perkPower();
		int heal = (int) restPartial;
		if (heal <= 0) return;
		restPartial -= heal;
		heal = Math.min(heal, hero.HT - hero.HP);
		hero.HP += heal;
		hero.sprite.showStatusWithIcon(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
				Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
	}

	/** Food.execute: Beelzebub's perk turns every meal into healing */
	public static void onFoodEaten(Hero hero) {
		if (!perk(DemonLord.BEELZEBUB)) return;
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT * 0.25f * perkPower()));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite.POSITIVE,
					Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
		}
	}
}
