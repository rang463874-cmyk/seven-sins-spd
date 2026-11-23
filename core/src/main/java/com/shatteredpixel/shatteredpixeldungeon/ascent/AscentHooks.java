package com.shatteredpixel.shatteredpixeldungeon.ascent;

import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Alchemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.WaterOfHealth;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SinShadow;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaFiend;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.Believer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.Priest;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.NPC;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Shopkeeper;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues.VirtueChampion;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.elites.SinElite;
import com.shatteredpixel.shatteredpixeldungeon.goetia.GoetiaDemon;
import com.shatteredpixel.shatteredpixeldungeon.items.Amulet;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.SinEssence;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.food.MysteryMeat;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.AlarmTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.BlazingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.BurningTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ChillingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.CorrosionTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ExplosiveTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.FlashingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrippingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.PoisonDartTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ShockingTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.ToxicTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sins.ChallengeHooks;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Seven Sins: the ascent's crossroads (Nov 20 2026, owner). Climbing back up with the Amulet of Yendor:
 * <ul>
 * <li>the first choice (right after taking the amulet) sets the faction that haunts the whole way up: "become a new
 * divinity" (demons) or "remain a sin" (angels);</li>
 * <li>entering each region on the way up (halls, city, caves, prison) offers three random {@link AscentCard}s, one of
 * which holds for that region's five floors;</li>
 * <li>what the hero does on the way scores sin or virtue points (capped per floor and per trigger), and so does each
 * card; at the surface the faction and the balance pick one of five endings, each with a badge.</li>
 * </ul>
 * State lives with the quests in Dungeon (node "sevenSinsAscent"). Hooks are marked "Seven Sins: ascent".
 */
public final class AscentHooks {

	private AscentHooks() {}

	public static final int NONE = 0, DIVINITY = 1, SINNER = 2;
	public static final int CARD_POINTS = 25;
	public static final int FLOOR_CAP = 3;
	public static final int FACTION_DEPTH = 26;
	public static final float FAST_TURNS = 150f;
	public static final float DILIGENCE_LIMIT = 300f;
	public static final float WAVE_EVERY = 50f;
	public static final float FIRE_EVERY = 12f;
	public static final float SPAWN_SHARE = 0.3f;

	/** behaviour triggers: seven sins, seven virtues */
	public enum Trigger {
		GLUTTONY(true), GREED(true), WRATH(true), PRIDE(true), LUST(true), ENVY(true), SLOTH(true),
		TEMPERANCE(false), CHARITY(false), PATIENCE(false), HUMILITY(false), CHASTITY(false), KINDNESS(false), DILIGENCE(false);

		public final boolean sin;

		Trigger(boolean sin) {
			this.sin = sin;
		}
	}

	public enum Ending {
		GOD_OF_SIN(Badges.Badge.ASCENT_GOD_OF_SIN),
		GOD_OF_DAWN(Badges.Badge.ASCENT_GOD_OF_DAWN),
		HEIR(Badges.Badge.ASCENT_HEIR),
		PENITENT(Badges.Badge.ASCENT_PENITENT),
		WANDERER(Badges.Badge.ASCENT_WANDERER);

		public final Badges.Badge badge;

		Ending(Badges.Badge badge) {
			this.badge = badge;
		}
	}

	// ------------------------------------------------------------------ state

	private static int faction = NONE;
	private static final AscentCard[] cards = new AscentCard[4];
	private static AscentCard[] offer = null;
	private static int offerRegion = -1;
	private static float sinScore = 0, virtueScore = 0;
	private static final HashSet<Integer> seeded = new HashSet<>();
	private static final HashSet<Integer> cardSeeded = new HashSet<>();
	private static final HashSet<Integer> judged = new HashSet<>();
	private static boolean endingShown = false;

	//the floor being climbed
	private static int floorDepth = 0, floorBranch = 0;
	private static float floorStart = 0;
	private static boolean consumed, goldTaken, killed, tookDamage, lustTouched, allyPresent;
	private static final int[] floorCounts = new int[Trigger.values().length];
	private static float goldAcc = 0;
	private static int restTurns = 0;
	private static boolean lowHp = false;
	private static float lastKill = -1;
	private static float nextWave = 0, nextFire = 0;
	private static boolean waveTold = false;
	//the region being climbed
	private static int lastRegion = -2;
	private static int regionGold = 0;
	private static int prideStacks = 0;
	private static boolean hurtSinceKill = false;
	private static boolean martyrUsed = false;
	//the bosses
	private static final HashSet<Integer> bossSpawned = new HashSet<>();
	private static float chaserDue = 0;
	//the sewers' cells broken by the chaser, by depth
	private static final java.util.HashMap<Integer, HashSet<Integer>> ruins = new java.util.HashMap<>();

	//not saved: a choice window is open
	private static boolean promptOpen = false;

	public static void reset() {
		faction = NONE;
		Arrays.fill(cards, null);
		offer = null;
		offerRegion = -1;
		sinScore = virtueScore = 0;
		seeded.clear();
		cardSeeded.clear();
		judged.clear();
		endingShown = false;
		floorDepth = floorBranch = 0;
		floorStart = 0;
		resetFloor();
		lastRegion = -2;
		regionGold = 0;
		prideStacks = 0;
		hurtSinceKill = false;
		martyrUsed = false;
		bossSpawned.clear();
		chaserDue = 0;
		ruins.clear();
		promptOpen = false;
	}

	private static void resetFloor() {
		consumed = goldTaken = killed = tookDamage = lustTouched = allyPresent = false;
		Arrays.fill(floorCounts, 0);
		goldAcc = 0;
		restTurns = 0;
		lastKill = -1;
		waveTold = false;
		prideStacks = 0;
	}

	private static final String NODE = "sevenSinsAscent";

	private static int[] ints(HashSet<Integer> set) {
		int[] out = new int[set.size()];
		int i = 0;
		for (int v : set) out[i++] = v;
		return out;
	}

	private static String[] names(AscentCard[] list) {
		String[] out = new String[list.length];
		for (int i = 0; i < list.length; i++) out[i] = list[i] == null ? "" : list[i].name();
		return out;
	}

	public static void storeInBundle(Bundle bundle) {
		Bundle b = new Bundle();
		b.put("faction", faction);
		b.put("cards", names(cards));
		if (offer != null) b.put("offer", names(offer));
		b.put("offer_region", offerRegion);
		b.put("sin", sinScore);
		b.put("virtue", virtueScore);
		b.put("seeded", ints(seeded));
		b.put("card_seeded", ints(cardSeeded));
		b.put("judged", ints(judged));
		b.put("ending_shown", endingShown);
		b.put("floor_depth", floorDepth);
		b.put("floor_branch", floorBranch);
		b.put("floor_start", floorStart);
		b.put("consumed", consumed);
		b.put("gold_taken", goldTaken);
		b.put("killed", killed);
		b.put("took_damage", tookDamage);
		b.put("lust", lustTouched);
		b.put("ally", allyPresent);
		b.put("floor_counts", floorCounts);
		b.put("gold_acc", goldAcc);
		b.put("rest_turns", restTurns);
		b.put("low_hp", lowHp);
		b.put("next_wave", nextWave);
		b.put("next_fire", nextFire);
		b.put("wave_told", waveTold);
		b.put("last_region", lastRegion);
		b.put("region_gold", regionGold);
		b.put("pride_stacks", prideStacks);
		b.put("hurt_since_kill", hurtSinceKill);
		b.put("martyr_used", martyrUsed);
		b.put("boss_spawned", ints(bossSpawned));
		b.put("chaser_due", chaserDue);
		for (java.util.Map.Entry<Integer, HashSet<Integer>> e : ruins.entrySet()) b.put("ruins_" + e.getKey(), ints(e.getValue()));
		bundle.put(NODE, b);
	}

	public static void restoreFromBundle(Bundle bundle) {
		reset();
		Bundle b = bundle.getBundle(NODE);
		if (b == null || b.isNull()) return;
		faction = b.getInt("faction");
		String[] c = b.getStringArray("cards");
		if (c != null) for (int i = 0; i < Math.min(c.length, cards.length); i++) cards[i] = AscentCard.byName(c[i]);
		if (b.contains("offer")) {
			String[] o = b.getStringArray("offer");
			offer = new AscentCard[o.length];
			for (int i = 0; i < o.length; i++) offer[i] = AscentCard.byName(o[i]);
		}
		offerRegion = b.getInt("offer_region");
		sinScore = b.getFloat("sin");
		virtueScore = b.getFloat("virtue");
		for (int v : b.getIntArray("seeded")) seeded.add(v);
		for (int v : b.getIntArray("card_seeded")) cardSeeded.add(v);
		for (int v : b.getIntArray("judged")) judged.add(v);
		endingShown = b.getBoolean("ending_shown");
		floorDepth = b.getInt("floor_depth");
		floorBranch = b.getInt("floor_branch");
		floorStart = b.getFloat("floor_start");
		consumed = b.getBoolean("consumed");
		goldTaken = b.getBoolean("gold_taken");
		killed = b.getBoolean("killed");
		tookDamage = b.getBoolean("took_damage");
		lustTouched = b.getBoolean("lust");
		allyPresent = b.getBoolean("ally");
		int[] fc = b.getIntArray("floor_counts");
		if (fc != null) System.arraycopy(fc, 0, floorCounts, 0, Math.min(fc.length, floorCounts.length));
		goldAcc = b.getFloat("gold_acc");
		restTurns = b.getInt("rest_turns");
		lowHp = b.getBoolean("low_hp");
		nextWave = b.getFloat("next_wave");
		nextFire = b.getFloat("next_fire");
		waveTold = b.getBoolean("wave_told");
		lastRegion = b.contains("last_region") ? b.getInt("last_region") : -2;
		regionGold = b.getInt("region_gold");
		prideStacks = b.getInt("pride_stacks");
		hurtSinceKill = b.getBoolean("hurt_since_kill");
		martyrUsed = b.getBoolean("martyr_used");
		if (b.contains("boss_spawned")) for (int v : b.getIntArray("boss_spawned")) bossSpawned.add(v);
		chaserDue = b.getFloat("chaser_due");
		for (int d = 1; d <= 5; d++) {
			if (!b.contains("ruins_" + d)) continue;
			HashSet<Integer> set = new HashSet<>();
			for (int v : b.getIntArray("ruins_" + d)) set.add(v);
			ruins.put(d, set);
		}
	}

	// ------------------------------------------------------------------ queries

	public static int faction() {
		return faction;
	}

	public static float sinScore() {
		return sinScore;
	}

	public static float virtueScore() {
		return virtueScore;
	}

	/** the game clock: Statistics.duration only catches up when the actors are re-timed (floor changes, saves), so within a
	 *  floor it stands still; the vanilla quickslot hint adds Actor.now() the same way */
	public static float gameTime() {
		return Statistics.duration + Actor.now();
	}

	/** climbing with the amulet after the first choice */
	public static boolean active() {
		Hero h = Dungeon.hero;
		return faction != NONE && h != null && Statistics.amuletObtained && h.belongings.getItem(Amulet.class) != null;
	}

	/** the region index of a depth on the way up: halls 0, city 1, caves 2, prison 3, else -1 */
	public static int region(int depth) {
		if (Dungeon.branch != 0) return -1;
		if (depth >= 21 && depth <= 25) return 0;
		if (depth >= 16 && depth <= 20) return 1;
		if (depth >= 11 && depth <= 15) return 2;
		if (depth >= 6 && depth <= 10) return 3;
		return -1;
	}

	/** the card holding on the current floor, or null */
	public static AscentCard card() {
		if (!active()) return null;
		int r = region(Dungeon.depth);
		return r < 0 ? null : cards[r];
	}

	private static boolean has(AscentCard c) {
		return card() == c;
	}

	public static AscentCard cardOf(int region) {
		return region < 0 || region >= cards.length ? null : cards[region];
	}

	// ------------------------------------------------------------------ the windows

	private static Image amuletIcon() {
		return new ItemSprite(ItemSpriteSheet.AMULET);
	}

	private static void showFaction() {
		promptOpen = true;
		Game.runOnRenderThread(() -> GameScene.show(new WndOptions(amuletIcon(),
				Messages.get(AscentHooks.class, "faction_title"),
				Messages.get(AscentHooks.class, "faction_msg"),
				Messages.get(AscentHooks.class, "faction_divinity"),
				Messages.get(AscentHooks.class, "faction_sinner")) {
			@Override
			protected void onSelect(int index) {
				promptOpen = false;
				chooseFaction(index == 0 ? DIVINITY : SINNER);
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				String k = index == 0 ? "faction_divinity" : "faction_sinner";
				GameScene.show(new WndTitledMessage(amuletIcon(), Messages.get(AscentHooks.class, k),
						Messages.get(AscentHooks.class, k + "_desc")));
			}

			@Override
			public void onBackPressed() {
				//the choice can't be skipped
			}
		}));
	}

	private static void chooseFaction(int f) {
		Hero hero = Dungeon.hero;
		if (faction != NONE || hero == null) return;
		faction = f;
		floorDepth = Dungeon.depth;
		floorBranch = Dungeon.branch;
		floorStart = gameTime();
		resetFloor();
		judged.add(Dungeon.depth);
		seeded.add(Dungeon.depth);
		Buff.affect(hero, Scale.class);
		GLog.h(Messages.get(AscentHooks.class, f == DIVINITY ? "chose_divinity" : "chose_sinner"));
		BuffIndicator.refreshHero();
	}

	private static AscentCard[] rollOffer() {
		HashSet<AscentCard> used = new HashSet<>(Arrays.asList(cards));
		ArrayList<AscentCard> sins = new ArrayList<>(), virtues = new ArrayList<>(), all = new ArrayList<>();
		for (AscentCard c : AscentCard.values()) {
			if (used.contains(c)) continue;
			all.add(c);
			if (c.side == AscentCard.Side.SIN) sins.add(c);
			if (c.side == AscentCard.Side.VIRTUE) virtues.add(c);
		}
		AscentCard a = Random.element(sins);
		AscentCard b = Random.element(virtues);
		all.remove(a);
		all.remove(b);
		AscentCard c = Random.element(all);
		ArrayList<AscentCard> out = new ArrayList<>(Arrays.asList(a, b, c));
		java.util.Collections.shuffle(out, new java.util.Random(Random.Long()));
		return out.toArray(new AscentCard[0]);
	}

	private static void showCards(final int region) {
		if (offer == null || offerRegion != region) {
			offer = rollOffer();
			offerRegion = region;
		}
		final AscentCard[] options = offer.clone();
		String[] labels = new String[options.length];
		for (int i = 0; i < labels.length; i++) labels[i] = options[i].title() + " (" + options[i].sideName() + ")";
		promptOpen = true;
		Game.runOnRenderThread(() -> GameScene.show(new WndOptions(amuletIcon(),
				Messages.get(AscentHooks.class, "cards_title"),
				Messages.get(AscentHooks.class, "cards_msg", Messages.get(AscentHooks.class, "region_" + region)), labels) {
			@Override
			protected void onSelect(int index) {
				promptOpen = false;
				chooseCard(region, options[index]);
			}

			@Override
			protected boolean hasInfo(int index) {
				return true;
			}

			@Override
			protected void onInfo(int index) {
				GameScene.show(new WndTitledMessage(amuletIcon(), options[index].title(), options[index].desc()));
			}

			@Override
			public void onBackPressed() {
				//the choice can't be skipped
			}
		}));
	}

	private static void chooseCard(int region, AscentCard c) {
		Hero hero = Dungeon.hero;
		if (cards[region] != null || hero == null) return;
		cards[region] = c;
		offer = null;
		offerRegion = -1;
		if (c.side == AscentCard.Side.SIN) sinScore += CARD_POINTS;
		else if (c.side == AscentCard.Side.VIRTUE) virtueScore += CARD_POINTS;
		GLog.h(Messages.get(AscentHooks.class, "chose_card", c.title()));
		hero.updateHT(false);
		if (region(Dungeon.depth) == region && !cardSeeded.contains(Dungeon.depth)) {
			cardSeeded.add(Dungeon.depth);
			applyCard(Dungeon.level, c);
		}
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ every hero turn

	/** from Hero.act */
	public static void tick(Hero hero) {
		if (hero == null || !hero.isAlive()) return;
		if (faction == NONE) {
			if (!promptOpen && Statistics.amuletObtained && hero.belongings.getItem(Amulet.class) != null) showFaction();
			return;
		}
		if (!active()) return;
		if (hero.buff(Scale.class) == null) Buff.affect(hero, Scale.class);

		if (Dungeon.depth != floorDepth || Dungeon.branch != floorBranch) onFloorChange(hero);
		//also frees a save that was already stuck in the dark prison start map
		if (Dungeon.branch == 0 && Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)
			((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel) Dungeon.level).ascentOpen();

		int r = region(Dungeon.depth);
		if (r != lastRegion) {
			lastRegion = r;
			regionGold = 0;
			martyrUsed = false;
			hero.updateHT(false);
		}
		if (r >= 0 && cards[r] == null && !promptOpen) showCards(r);

		//humility: back from the brink
		if (hero.HP < hero.HT / 4) lowHp = true;
		else if (lowHp && hero.HP >= hero.HT / 2) {
			lowHp = false;
			award(Trigger.HUMILITY, 2);
		}
		//sloth: resting
		if (hero.resting && ++restTurns >= 20) {
			restTurns = 0;
			award(Trigger.SLOTH, 1);
		}
		//kindness: allies kept alive
		boolean ally = false;
		for (Mob m : Dungeon.level.mobs) if (m.alignment == Char.Alignment.ALLY && m.isAlive() && !(m instanceof NPC)) { ally = true; break; }
		allyPresent = ally;

		float now = gameTime();
		if (Dungeon.branch == 0 && Dungeon.depth >= 1 && Dungeon.depth <= 5 && now >= chaserDue) raiseChaser(Dungeon.level);
		HashSet<Integer> here = Dungeon.branch == 0 ? ruins.get(Dungeon.depth) : null;
		if (here != null && !here.isEmpty() && !com.shatteredpixel.shatteredpixeldungeon.effects.RuinTile.showingFor(Dungeon.level)) {
			com.shatteredpixel.shatteredpixeldungeon.effects.RuinTile.show(Dungeon.level, ints(here), ruinColor(), ruinCrack());
		}
		AscentCard c = card();
		if (c == AscentCard.WRATH_FIRE && now >= nextFire) {
			nextFire = now + FIRE_EVERY;
			for (int i = 0; i < 3; i++) {
				int cell = randomCell(Dungeon.level, 3, false);
				if (cell != -1) GameScene.add(Blob.seed(cell, 4, Fire.class));
			}
		}
		if (c == AscentCard.DILIGENCE_TIME && now - floorStart > DILIGENCE_LIMIT && now >= nextWave) {
			nextWave = now + WAVE_EVERY;
			if (!waveTold) {
				waveTold = true;
				GLog.n(Messages.get(AscentHooks.class, "time_up"));
			}
			spawnFaction(Dungeon.level, 2, 2);
		}
	}

	private static void onFloorChange(Hero hero) {
		int prev = floorDepth;
		if (prev > 0 && prev <= 30 && floorBranch == 0 && Dungeon.depth < prev && !judged.contains(prev)) {
			judge(hero, prev);
			judged.add(prev);
		}
		floorDepth = Dungeon.depth;
		floorBranch = Dungeon.branch;
		floorStart = gameTime();
		resetFloor();
		hurtSinceKill = false;
		nextWave = floorStart + DILIGENCE_LIMIT;
		nextFire = floorStart + 5;
		chaserDue = floorStart + 6;
		if (Dungeon.branch != 0) return;

		Level level = Dungeon.level;
		int d = Dungeon.depth;
		if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)
			((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel) level).ascentOpen();
		if (!seeded.contains(d) && d <= 30) {
			seeded.add(d);
			int n = factionCount(d);
			spawnFaction(level, n, n / 3 + (d <= 5 ? 2 : 0));
			GLog.w(Messages.get(AscentHooks.class, faction == DIVINITY ? "demons_near" : "angels_near"));
		}
		AscentCard c = card();
		if (c != null && !cardSeeded.contains(d)) {
			cardSeeded.add(d);
			applyCard(level, c);
		}
		if (d == 25 || d == 20 || d == 15 || d == 10 || d == 5) clearOldBosses(level);
		if ((d == 25 || d == 20 || d == 15 || d == 10) && !bossSpawned.contains(d)) {
			bossSpawned.add(d);
			spawnBoss(level, d);
		}
	}

	// ------------------------------------------------------------------ the bosses

	public static final int BOSS_POINTS = 15;

	private static com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentBoss bossFor(int depth) {
		boolean demons = faction == DIVINITY;
		switch (depth) {
			case 25: return demons ? new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Moloch() : new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Cherubim();
			case 20: return demons ? new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Lilith() : new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Ophanim();
			case 15: return demons ? new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Azazel() : new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Azrael();
			default: return demons ? new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Abaddon() : new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Sandalphon();
		}
	}

	/** an open tile 4..9 steps from the hero (any open tile if none) */
	private static int bossCell(Level level) {
		Hero hero = Dungeon.hero;
		ArrayList<Integer> near = new ArrayList<>(), any = new ArrayList<>();
		for (int c = 0; c < level.length(); c++) {
			if (!level.passable[c] || level.solid[c] || level.avoid[c] || level.pit[c] || Actor.findChar(c) != null) continue;
			if (level.getTransition(c) != null) continue;
			int d = hero == null ? 6 : level.distance(c, hero.pos);
			if (d >= 4 && d <= 9) near.add(c);
			else if (d >= 3) any.add(c);
		}
		if (!near.isEmpty()) return Random.element(near);
		return any.isEmpty() ? -1 : Random.element(any);
	}

	private static void spawnBoss(Level level, int depth) {
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentBoss b = bossFor(depth);
		b.setTier((25 - depth) / 5);
		int cell = arenaCell(level);
		if (cell == -1) cell = bossCell(level);
		if (cell == -1) return;
		b.pos = cell;
		GameScene.add(b);
		if (Dungeon.hero != null) b.beckon(Dungeon.hero.pos);
		lock(level);
		GLog.n(Messages.get(AscentHooks.class, "boss_arrives", b.name()));
	}

	/** the original boss's arena on this floor, -1 if none (or it is taken) */
	private static int arenaCell(Level level) {
		int c = -1;
		if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel)
			c = ((com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel) level).ascentBossCell();
		else if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel)
			c = ((com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel) level).ascentBossCell();
		else if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel)
			c = ((com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel) level).ascentBossCell();
		else if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel)
			c = ((com.shatteredpixel.shatteredpixeldungeon.levels.PrisonBossLevel) level).ascentBossCell();
		if (c < 0 || c >= level.length()) return -1;
		if (level.passable[c] && !level.solid[c] && Actor.findChar(c) == null) return c;
		for (int n : PathFinder.NEIGHBOURS8) {
			int x = c + n;
			if (x >= 0 && x < level.length() && level.passable[x] && !level.solid[x] && Actor.findChar(x) == null) return x;
		}
		return -1;
	}

	/** the original boss floors seal themselves by summoning their own boss (CavesBossLevel.seal and the like), so the
	 *  ascent locks the stairs the plain way */
	private static void lock(Level level) {
		level.locked = true;
		if (Dungeon.hero != null) Buff.affect(Dungeon.hero, com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor.class);
	}

	private static void unlock(Level level) {
		level.locked = false;
		if (Dungeon.hero != null) {
			com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor lf =
					Dungeon.hero.buff(com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor.class);
			if (lf != null) lf.detach();
		}
		//an arena whose own boss was never fought (floors skipped with the dev tools): its own unseal opens gates and doors
		boolean closed = false;
		if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel)
			closed = ((com.shatteredpixel.shatteredpixeldungeon.levels.CavesBossLevel) level).ascentArenaClosed();
		else if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel)
			closed = ((com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel) level).ascentArenaClosed();
		else if (level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel)
			closed = ((com.shatteredpixel.shatteredpixeldungeon.levels.HallsBossLevel) level).ascentArenaClosed();
		if (closed) level.unseal();
	}

	/** the original bosses never come back on the way up */
	private static void clearOldBosses(Level level) {
		for (Mob m : level.mobs.toArray(new Mob[0])) {
			if (m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Goo
					|| m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu
					|| m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DM300
					|| m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing
					|| m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa
					|| m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogFist) {
				m.destroy();
				if (m.sprite != null) m.sprite.killAndErase();
			}
		}
	}

	// ------------------------------------------------------------------ the sewers' ruins

	public static int ruinColor() {
		return faction == DIVINITY ? 0x5E1E9E : 0xCFCBBE;
	}

	public static int ruinCrack() {
		return faction == DIVINITY ? 0xC27BFF : 0xFFF4CF;
	}

	public static boolean isRuin(int cell) {
		HashSet<Integer> here = ruins.get(Dungeon.depth);
		return Dungeon.branch == 0 && here != null && here.contains(cell);
	}

	private static boolean protectedCell(Level level, int cell) {
		int w = level.width();
		int x = cell % w, y = cell / w;
		if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) return true;
		for (LevelTransition t : level.transitions) {
			if (level.distance(cell, t.cell()) <= 1) return true;
		}
		return false;
	}

	/** from AscentChaser: the floor it passed over breaks for good (a statue tile under a glowing ruin) */
	public static void ruin(int cell) {
		if (!active() || Dungeon.branch != 0 || Dungeon.depth < 1 || Dungeon.depth > 5) return;
		Level level = Dungeon.level;
		if (cell < 0 || cell >= level.length() || protectedCell(level, cell) || isRuin(cell)) return;
		if (Actor.findChar(cell) != null) return;
		//never cut the hero off from the stairs up
		Hero hero = Dungeon.hero;
		int up = level.entrance();
		if (hero != null && up >= 0 && up < level.length()) {
			boolean[] pass = level.passable.clone();
			pass[cell] = false;
			PathFinder.buildDistanceMap(up, pass);
			if (PathFinder.distance[hero.pos] == Integer.MAX_VALUE) return;
		}
		Heap h = level.heaps.get(cell);
		if (h != null) {
			//what lay there tumbles aside if it can
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = cell + n;
				if (level.passable[c] && !isRuin(c)) {
					for (Item it : h.items.toArray(new Item[0])) level.drop(it, c).sprite.drop(cell);
					break;
				}
			}
			h.destroy();
		}
		Level.set(cell, Terrain.STATUE, level);
		GameScene.updateMap(cell);
		ruins.computeIfAbsent(Dungeon.depth, k -> new HashSet<>()).add(cell);
		com.shatteredpixel.shatteredpixeldungeon.effects.RuinTile.show(level, new int[]{cell}, ruinColor(), ruinCrack());
	}

	/** from AscentChaser: a wall in its way is smashed */
	public static void smash(int cell) {
		Level level = Dungeon.level;
		if (cell < 0 || cell >= level.length() || protectedCell(level, cell) || isRuin(cell) || !level.solid[cell]) return;
		Level.set(cell, Terrain.EMPTY, level);
		GameScene.updateMap(cell);
		com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter.get(cell).burst(
				com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.ROCK), 6);
	}

	/** from AscentBoss.die: the stairs open, rewards, and the scale tips (slaying demons is virtuous, angels sinful) */
	public static void onBossSlain(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentBoss b) {
		if (Dungeon.level != null) unlock(Dungeon.level);
		Dungeon.level.drop(new ScrollOfUpgrade(), b.pos).sprite.drop();
		if (ChallengeHooks.essences()) Dungeon.level.drop(new SinEssence().quantity(2), b.pos).sprite.drop();
		if (!active()) return;
		if (faction == DIVINITY) virtueScore += BOSS_POINTS;
		else sinScore += BOSS_POINTS;
		GLog.h(Messages.get(AscentHooks.class, faction == DIVINITY ? "boss_slain_virtue" : "boss_slain_sin", b.name()));
		BuffIndicator.refreshHero();
	}

	/** the sewers' giant rises from the stairs below */
	private static void raiseChaser(Level level) {
		for (Mob m : level.mobs) if (m instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentChaser) return;
		com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentChaser ch = faction == DIVINITY
				? new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Behemoth()
				: new com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.Metatron();
		int exit = level.exit();
		int cell = -1;
		if (exit > 0 && level.passable[exit] && Actor.findChar(exit) == null) cell = exit;
		else if (exit > 0) {
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = exit + n;
				if (level.passable[c] && !level.solid[c] && Actor.findChar(c) == null) { cell = c; break; }
			}
		}
		if (cell == -1) cell = level.randomRespawnCell(ch);
		if (cell == -1) return;
		ch.pos = cell;
		GameScene.add(ch);
		if (Dungeon.hero != null) ch.beckon(Dungeon.hero.pos);
		chaserDue = Float.MAX_VALUE;
		GLog.n(Messages.get(AscentHooks.class, "chaser_rises", ch.name()));
		com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.shake(3, 0.8f);
	}

	/** the virtues of a floor left behind on the way up */
	private static void judge(Hero hero, int depth) {
		float elapsed = gameTime() - floorStart;
		if (!consumed) award(Trigger.TEMPERANCE, 3);
		if (!goldTaken) award(Trigger.CHARITY, 3);
		if (!killed) award(Trigger.PATIENCE, 3);
		if (!tookDamage) award(Trigger.PRIDE, 3);
		if (!lustTouched) award(Trigger.CHASTITY, 2);
		if (allyPresent) award(Trigger.KINDNESS, 1);
		if (elapsed <= FAST_TURNS) award(Trigger.DILIGENCE, 2);
		if (cardOf(region(depth)) == AscentCard.DILIGENCE_TIME && elapsed <= DILIGENCE_LIMIT) {
			Item prize = Generator.random(Random.Int(2) == 0 ? Generator.Category.POTION : Generator.Category.SCROLL);
			if (prize != null) {
				if (!prize.collect()) Dungeon.level.drop(prize, hero.pos).sprite.drop();
				GLog.p(Messages.get(AscentHooks.class, "diligence_prize", prize.name()));
			}
		}
	}

	/** scores a behaviour, at most FLOOR_CAP points per trigger and floor */
	public static void award(Trigger t, int pts) {
		if (!active()) return;
		int i = t.ordinal();
		int room = FLOOR_CAP - floorCounts[i];
		if (room <= 0 || pts <= 0) return;
		boolean first = floorCounts[i] == 0;
		int n = Math.min(room, pts);
		floorCounts[i] += n;
		if (t.sin) sinScore += n;
		else virtueScore += n;
		if (first) {
			String msg = Messages.get(AscentHooks.class, "t_" + t.name().toLowerCase());
			if (t.sin) GLog.w(msg);
			else GLog.p(msg);
		}
		BuffIndicator.refreshHero();
	}

	// ------------------------------------------------------------------ the faction

	private static int factionCount(int depth) {
		return 2 + (30 - depth) / 6 + (depth <= 5 ? 2 : 0);
	}

	private static Mob factionMob() {
		Mob m;
		if (faction == DIVINITY) {
			GoetiaFiend f = new GoetiaFiend();
			ArrayList<GoetiaDemon> pool = GoetiaDemon.ofRegion(Random.Int(5));
			f.setup(pool.isEmpty() ? GoetiaDemon.MARBAS : Random.element(pool), FACTION_DEPTH);
			m = f;
		} else {
			m = Random.Int(5) < 3 ? new Believer() : new Priest();
		}
		Buff.affect(m, Herald.class);
		return m;
	}

	private static void spawnFaction(Level level, int n, int hunting) {
		Hero hero = Dungeon.hero;
		for (int i = 0; i < n; i++) {
			Mob m = factionMob();
			int cell = level.randomRespawnCell(m);
			if (cell == -1) continue;
			m.pos = cell;
			m.state = m.WANDERING;
			GameScene.add(m);
			if (i < hunting && hero != null) m.beckon(hero.pos);
		}
	}

	public static boolean isHerald(Char ch) {
		return ch != null && ch.buff(Herald.class) != null;
	}

	/** from Level.createMob: respawns during the ascent */
	public static Mob onCreateMob(Mob m) {
		if (!active() || Dungeon.branch != 0) return m;
		if (Random.Float() < SPAWN_SHARE) return factionMob();
		AscentCard c = card();
		if (c == AscentCard.PATIENCE_ENDURE) harden(m);
		if (c == AscentCard.SIN_PARADE) makeElite(m);
		return m;
	}

	// ------------------------------------------------------------------ the cards on a floor

	private static boolean plainEnemy(Mob m) {
		return m.alignment == Char.Alignment.ENEMY && m.isAlive() && !(m instanceof NPC) && !isHerald(m)
				&& !Char.hasProp(m, Char.Property.BOSS) && !Char.hasProp(m, Char.Property.MINIBOSS);
	}

	private static ArrayList<Mob> enemies(Level level) {
		ArrayList<Mob> out = new ArrayList<>();
		for (Mob m : level.mobs.toArray(new Mob[0])) if (plainEnemy(m)) out.add(m);
		return out;
	}

	private static void harden(Mob m) {
		if (m.buff(Hardened.class) != null) return;
		m.HT = Math.round(m.HT * 1.5f);
		m.HP = Math.round(m.HP * 1.5f);
		Buff.affect(m, Hardened.class);
	}

	@SuppressWarnings("unchecked")
	private static void makeElite(Mob m) {
		if (!plainEnemy(m) || Char.hasProp(m, Char.Property.IMMOVABLE) || !m.buffs(ChampionEnemy.class).isEmpty()) return;
		Buff.affect(m, (Class<? extends Buff>) Random.element(SinElite.ALL));
	}

	/** an empty floor tile away from the hero, the stairs, items and characters */
	private static int randomCell(Level level, int minDist, boolean emptyOnly) {
		Hero hero = Dungeon.hero;
		for (int tries = 0; tries < 400; tries++) {
			int c = Random.Int(level.length());
			int t = level.map[c];
			if (emptyOnly ? t != Terrain.EMPTY : !(t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS)) continue;
			if (!level.passable[c] || Actor.findChar(c) != null || level.heaps.get(c) != null) continue;
			if (hero != null && level.distance(c, hero.pos) < minDist) continue;
			if (level.getTransition(c) != null || level.traps.get(c) != null) continue;
			return c;
		}
		return -1;
	}

	private static final Class<?>[] MILD_TRAPS = {AlarmTrap.class, ChillingTrap.class, BurningTrap.class,
			ShockingTrap.class, PoisonDartTrap.class, GrippingTrap.class};
	private static final Class<?>[] HARSH_TRAPS = {ExplosiveTrap.class, CorrosionTrap.class, ToxicTrap.class,
			FlashingTrap.class, BlazingTrap.class};

	private static void traps(Level level, int n, Class<?>[] kinds) {
		for (int i = 0; i < n; i++) {
			int c = randomCell(level, 3, true);
			if (c == -1) return;
			Trap t = (Trap) Reflection.newInstance(Random.element(kinds));
			if (t == null) continue;
			level.setTrap(t.hide(), c);
			Level.set(c, Terrain.SECRET_TRAP, level);
			GameScene.updateMap(c);
		}
	}

	private static void applyCard(Level level, AscentCard c) {
		if (level == null) return;
		boolean boss = Dungeon.bossLevel();
		switch (c) {
			case GLUTTON_FEAST: {
				ArrayList<Mob> foes = enemies(level);
				if (foes.isEmpty()) break;
				for (Heap h : level.heaps.valueList()) {
					if (h.type != Heap.Type.HEAP || h.isEmpty() || Random.Int(3) != 0) continue;
					if (Dungeon.hero != null && h.pos == Dungeon.hero.pos) continue;
					Item it = h.pickUp();
					if (it == null) continue;
					Buff.affect(Random.element(foes), Swallowed.class).items.add(it);
				}
				break;
			}
			case GREED_VAULT:
				if (boss) break;
				traps(level, 6, MILD_TRAPS);
				for (int i = 0; i < 2; i++) {
					int cell = randomCell(level, 3, false);
					if (cell == -1) break;
					Item it = Generator.random();
					if (it == null) continue;
					Heap h = level.drop(it, cell);
					h.type = Heap.Type.CHEST;
					if (h.sprite != null) h.sprite.view(h);
				}
				break;
			case WRATH_LEGION: {
				int n = Math.min(6, enemies(level).size());
				for (int i = 0; i < n; i++) {
					Mob m = level.createMob();
					int cell = level.randomRespawnCell(m);
					if (cell == -1) continue;
					m.pos = cell;
					m.state = m.WANDERING;
					GameScene.add(m);
				}
				break;
			}
			case ENVY_MIRROR: {
				SinShadow s = new SinShadow();
				s.HT = s.HP = Math.max(1, s.HT / 2);
				int cell = level.randomRespawnCell(s);
				if (cell == -1) break;
				s.pos = cell;
				Buff.affect(s, Shade.class);
				GameScene.add(s);
				break;
			}
			case SLOTH_FOG:
				for (Mob m : enemies(level)) m.state = m.SLEEPING;
				break;
			case SIN_PARADE:
				for (Mob m : enemies(level)) makeElite(m);
				break;
			case HUMILITY_PILGRIM: {
				if (boss) break;
				int cell = randomCell(level, 3, true);
				if (cell == -1) break;
				Level.set(cell, Terrain.WELL, level);
				GameScene.add(Blob.seed(cell, 1, WaterOfHealth.class, level));
				GameScene.updateMap(cell);
				break;
			}
			case CHARITY_HAND: {
				if (boss) break;
				int cell = randomCell(level, 4, true);
				if (cell == -1) break;
				Shopkeeper sk = new Shopkeeper();
				sk.pos = cell;
				GameScene.add(sk);
				int wares = 0;
				for (int n : PathFinder.NEIGHBOURS8) {
					int w = cell + n;
					if (wares >= 3 || !level.passable[w] || level.map[w] != Terrain.EMPTY || level.heaps.get(w) != null
							|| Actor.findChar(w) != null) continue;
					Generator.Category cat = Random.oneOf(Generator.Category.POTION, Generator.Category.SCROLL, Generator.Category.STONE);
					Item it = Generator.random(cat);
					if (it == null) continue;
					Heap h = level.drop(it, w);
					h.type = Heap.Type.FOR_SALE;
					if (h.sprite != null) h.sprite.view(h);
					wares++;
				}
				break;
			}
			case PATIENCE_ENDURE:
				for (Mob m : enemies(level)) harden(m);
				break;
			case KINDNESS_TRIAL: {
				Virtue v = Random.element(Virtue.values());
				VirtueChampion ch = Reflection.newInstance(v.champion);
				if (ch == null) break;
				int cell = level.randomRespawnCell(ch);
				if (cell == -1) break;
				ch.pos = cell;
				GameScene.add(ch);
				break;
			}
			case QUIET: {
				ArrayList<Mob> foes = enemies(level);
				java.util.Collections.shuffle(foes, new java.util.Random(Random.Long()));
				for (int i = 0; i < foes.size() / 2; i++) {
					Mob m = foes.get(i);
					m.destroy();
					if (m.sprite != null) m.sprite.killAndErase();
				}
				break;
			}
			case FLOOD:
				if (boss) break;
				for (int cell = 0; cell < level.length(); cell++) {
					if (level.map[cell] != Terrain.EMPTY || level.getTransition(cell) != null || Random.Float() >= 0.35f) continue;
					Level.set(cell, Terrain.WATER, level);
					GameScene.updateMap(cell);
				}
				break;
			case ALCHEMIST: {
				if (!boss) {
					int cell = randomCell(level, 3, true);
					if (cell != -1) {
						Level.set(cell, Terrain.ALCHEMY, level);
						GameScene.add(Blob.seed(cell, 1, Alchemy.class, level));
						GameScene.updateMap(cell);
						if (ChallengeHooks.essences()) {
							for (int n : PathFinder.NEIGHBOURS8) {
								int w = cell + n;
								if (level.passable[w] && level.heaps.get(w) == null) {
									level.drop(new SinEssence().quantity(2), w).sprite.drop();
									break;
								}
							}
						}
					}
					traps(level, 4, HARSH_TRAPS);
				}
				break;
			}
			case GAMBLER:
				gamble(Dungeon.hero);
				break;
			default:
		}
	}

	@SuppressWarnings("unchecked")
	private static final Class<? extends FlavourBuff>[] BOONS = new Class[]{Bless.class, Haste.class, Adrenaline.class,
			MindVision.class, Levitation.class};
	@SuppressWarnings("unchecked")
	private static final Class<? extends FlavourBuff>[] BANES = new Class[]{Weakness.class, Cripple.class, Hex.class,
			Vulnerable.class, Slow.class};

	private static void gamble(Hero hero) {
		if (hero == null) return;
		Class<? extends FlavourBuff> bane = Random.element(BANES);
		Class<? extends FlavourBuff> boon = Random.element(BOONS);
		Buff.prolong(hero, bane, 40f);
		Buff.prolong(hero, boon, 40f);
		GLog.w(Messages.get(AscentHooks.class, "gamble"));
	}

	// ------------------------------------------------------------------ hooks

	/** from Mob.die */
	public static void onMobDeath(Mob mob, Object cause) {
		if (!active()) return;
		Hero hero = Dungeon.hero;
		Swallowed sw = mob.buff(Swallowed.class);
		if (sw != null) {
			for (Item it : sw.items) Dungeon.level.drop(it, mob.pos).sprite.drop();
			sw.items.clear();
		}
		boolean herald = isHerald(mob) || mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentBoss
				|| mob instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.ascent.AscentChaser;
		AscentCard c = card();
		if (c == AscentCard.GLUTTON_MARCH && !herald && Random.Float() < 0.15f) {
			Dungeon.level.drop(new MysteryMeat(), mob.pos).sprite.drop();
		}
		float expBonus = c == AscentCard.WRATH_FIRE ? 0.5f : c == AscentCard.WRATH_LEGION ? 1f : 0f;
		if (expBonus > 0 && mob.EXP > 0 && hero != null && hero.isAlive()) {
			int exp = Math.round(mob.EXP * expBonus);
			if (exp > 0) {
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(exp), FloatingText.EXPERIENCE);
				hero.earnExp(exp, mob.getClass());
			}
		}
		if (mob.buff(Shade.class) != null && hero != null && hero.isAlive()) {
			Class<? extends FlavourBuff> boon = Random.element(BOONS);
			Buff.prolong(hero, boon, 40f);
			Buff.affect(hero, Barrier.class).setShield(hero.HT / 5);
			GLog.p(Messages.get(AscentHooks.class, "shade_reward"));
		}
		if (c == AscentCard.KINDNESS_TRIAL && mob instanceof VirtueChampion && !(mob instanceof SinShadow)) {
			Dungeon.level.drop(new ScrollOfUpgrade(), mob.pos).sprite.drop();
		}
		if (c == AscentCard.PRIDE_ROAD && mob.alignment == Char.Alignment.ENEMY) {
			if (!hurtSinceKill) Dungeon.level.drop(new Gold(10 + 5 * Dungeon.depth), mob.pos).sprite.drop();
			hurtSinceKill = false;
		}
		if (herald || mob.alignment != Char.Alignment.ENEMY) return;
		killed = true;
		if (cause instanceof Mob && ((Mob) cause).alignment == Char.Alignment.ALLY) {
			award(Trigger.SLOTH, 1);
		} else {
			float now = gameTime();
			award(Trigger.WRATH, now == lastKill ? 2 : 1);
			lastKill = now;
		}
	}

	/** from Food.execute */
	public static void onFoodEaten(Hero hero) {
		if (!active()) return;
		consumed = true;
		award(Trigger.GLUTTONY, 1);
		if (has(AscentCard.GLUTTON_MARCH)) {
			int heal = Math.min(hero.HT - hero.HP, hero.HT / 5);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
	}

	/** from PredatorHooks.onConsumed: potions count as indulgence */
	public static void onConsumed(Item item) {
		if (!active() || !(item instanceof Potion)) return;
		consumed = true;
		award(Trigger.GLUTTONY, 1);
	}

	/** from Item.doPickUp */
	public static void onPickedUp(Item item) {
		if (!active()) return;
		if ((item instanceof EquipableItem && !(item instanceof Amulet)) || item instanceof Wand) award(Trigger.ENVY, 1);
	}

	/** from Gold.doPickUp: true = the gold stays where it is (Charity) */
	public static boolean refusesGold() {
		if (!has(AscentCard.CHARITY_HAND)) return false;
		GLog.w(Messages.get(AscentHooks.class, "charity_gold"));
		return true;
	}

	/** from Gold.doPickUp */
	public static int goldPickup(int quantity) {
		if (!active()) return quantity;
		AscentCard c = card();
		if (c == AscentCard.GREED_CURSE) {
			quantity *= 2;
			regionGold += quantity;
		}
		if (c == AscentCard.QUIET) quantity = Math.max(1, quantity / 2);
		goldTaken = true;
		goldAcc += quantity;
		while (goldAcc >= 100) {
			goldAcc -= 100;
			award(Trigger.GREED, 1);
		}
		return quantity;
	}

	/** from Shopkeeper.sellPrice */
	public static boolean freeShop() {
		return has(AscentCard.CHARITY_HAND);
	}

	/** from Char.damage */
	public static int modifyDamage(Char target, Object src, int dmg) {
		if (dmg <= 0 || !active()) return dmg;
		Hero hero = Dungeon.hero;
		AscentCard c = card();
		if (target instanceof Mob && target.alignment == Char.Alignment.ENEMY) {
			if (src == hero) {
				if (c == AscentCard.GLUTTON_FEAST) dmg = Math.round(dmg * 1.2f);
				if (c == AscentCard.DARK && ((Mob) target).surprisedBy(hero, false)) dmg = Math.round(dmg * 1.5f);
			}
			if (c == AscentCard.WRATH_FIRE && (src instanceof Burning || src instanceof Fire)) dmg = Math.round(dmg * 1.5f);
			return dmg;
		}
		if (target != hero) return dmg;
		if (src instanceof Char || src instanceof Buff || src instanceof Blob || src instanceof Trap) {
			if (!(src instanceof com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger)) {
				tookDamage = true;
				hurtSinceKill = true;
			}
		}
		if (c == AscentCard.GREED_CURSE && src instanceof Mob) {
			dmg = Math.round(dmg * (1f + Math.min(0.5f, regionGold / 3000f)));
		}
		if (c == AscentCard.PRIDE_ROAD && src instanceof Char) {
			dmg = Math.round(dmg * (1f + 0.1f * prideStacks));
			prideStacks = Math.min(10, prideStacks + 1);
		}
		if (c == AscentCard.PATIENCE_ENDURE) dmg = Math.round(dmg * 0.75f);
		if (c == AscentCard.MARTYR && !martyrUsed && dmg >= hero.HP + hero.shielding()) {
			martyrUsed = true;
			dmg = Math.max(0, hero.HP + hero.shielding() - 1);
			hero.sprite.showStatus(CharSprite.POSITIVE, Messages.get(AscentHooks.class, "martyr"));
			GLog.p(Messages.get(AscentHooks.class, "martyr_saved"));
		}
		return dmg;
	}

	/** from Hero.attackProc */
	public static int onAttack(Hero hero, Char enemy, int damage) {
		if (!active() || damage <= 0) return damage;
		if (enemy.buff(Charm.class) != null) {
			lustTouched = true;
			award(Trigger.LUST, 1);
		}
		if (has(AscentCard.LUST_BLOOD)) {
			lustTouched = true;
			int heal = Math.min(hero.HT - hero.HP, Math.round(damage * 0.15f));
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
		}
		return damage;
	}

	/** from Regeneration */
	public static float regenMult(Char target) {
		if (!(target instanceof Hero) || !active()) return 1f;
		AscentCard c = card();
		if (c == AscentCard.LUST_BLOOD) return 0f;
		if (c == AscentCard.SLOTH_FOG && ((Hero) target).resting) return 3f;
		return 1f;
	}

	/** from Hunger.act: hunger comes this many times as fast */
	public static float hungerRate() {
		return has(AscentCard.GLUTTON_MARCH) ? 2f : 1f;
	}

	/** from Hero.speed */
	public static float moveSpeed() {
		return has(AscentCard.SLOTH_FOG) ? 0.75f : 1f;
	}

	/** from Hero.defenseSkill */
	public static float evasion(Hero hero) {
		return has(AscentCard.FLOOD) && Dungeon.level.map[hero.pos] == Terrain.WATER ? 1.2f : 1f;
	}

	/** from Level.updateFieldOfView, for the hero */
	public static float viewMult() {
		return has(AscentCard.DARK) ? 0.5f : 1f;
	}

	/** from Hero.updateHT */
	public static float htMult() {
		AscentCard c = card();
		if (c == AscentCard.CHASTITY_ABSTAIN) return 1.3f;
		if (c == AscentCard.MARTYR) return 0.8f;
		return 1f;
	}

	/** from Potion.execute: true = can't drink (Chastity) */
	public static boolean blocksPotion() {
		if (!has(AscentCard.CHASTITY_ABSTAIN)) return false;
		GLog.w(Messages.get(AscentHooks.class, "no_potions"));
		return true;
	}

	/** from Scroll.execute: true = can't read (Temperance) */
	public static boolean blocksScroll() {
		if (!has(AscentCard.TEMPERANCE_SILENCE)) return false;
		GLog.w(Messages.get(AscentHooks.class, "no_scrolls"));
		return true;
	}

	/** from MetaTalents.costMult and SolomonHooks.adjustCost */
	public static float costMult() {
		return has(AscentCard.TEMPERANCE_SILENCE) ? 0.5f : 1f;
	}

	/** from MetaTalents.pointGain and SolomonArcana.addWisdom */
	public static float pointMult() {
		return has(AscentCard.SIN_PARADE) ? 2f : 1f;
	}

	/** from Mob.lootChance */
	public static float lootMult() {
		return has(AscentCard.QUIET) ? 0.5f : 1f;
	}

	/** from Item.buffedLvl: the pilgrim's equipment counts one level lower */
	public static int levelPenalty(Item item) {
		if (!has(AscentCard.HUMILITY_PILGRIM) || !(item instanceof Weapon || item instanceof Armor)) return 0;
		return item.isEquipped(Dungeon.hero) ? 1 : 0;
	}

	// ------------------------------------------------------------------ the ending

	public static Ending ending() {
		float diff = sinScore - virtueScore;
		float band = Math.max(10f, 0.1f * (sinScore + virtueScore));
		if (Math.abs(diff) <= band) return Ending.WANDERER;
		boolean sin = diff > 0;
		if (faction == DIVINITY) return sin ? Ending.GOD_OF_SIN : Ending.GOD_OF_DAWN;
		return sin ? Ending.HEIR : Ending.PENITENT;
	}

	/** from SewerLevel.activateTransition (the surface): true = the ending is shown first */
	public static boolean interceptSurface(final Level level, final Hero hero, final LevelTransition transition) {
		if (!active() || endingShown) return false;
		final Ending e = ending();
		final String id = e.name().toLowerCase();
		Game.runOnRenderThread(() -> GameScene.show(new WndOptions(amuletIcon(),
				Messages.get(AscentHooks.class, "end_" + id),
				Messages.get(AscentHooks.class, "end_" + id + "_text") + "\n\n"
						+ Messages.get(AscentHooks.class, "end_scores", Math.round(sinScore), Math.round(virtueScore)),
				Messages.get(AscentHooks.class, "end_go")) {
			@Override
			protected void onSelect(int index) {
				endingShown = true;
				Badges.validateAscentEnding(e.badge);
				level.activateTransition(hero, transition);
			}

			@Override
			public void onBackPressed() {
				//the ending has to be read
			}
		}));
		return true;
	}

	// ------------------------------------------------------------------ buffs

	/** the scale on the hero: the faction, the cards and the balance */
	public static class Scale extends Buff {

		{
			type = buffType.NEUTRAL;
			revivePersists = true;
		}

		@Override
		public int icon() {
			return active() ? BuffIndicator.AMULET : BuffIndicator.NONE;
		}

		@Override
		public void tintIcon(Image icon) {
			if (sinScore > virtueScore) icon.hardlight(0.9f, 0.25f, 0.25f);
			else if (virtueScore > sinScore) icon.hardlight(1f, 0.9f, 0.4f);
		}

		@Override
		public String name() {
			return Messages.get(AscentHooks.class, "scale_name");
		}

		@Override
		public String desc() {
			StringBuilder sb = new StringBuilder(Messages.get(AscentHooks.class, "scale_desc",
					Messages.get(AscentHooks.class, faction == DIVINITY ? "faction_divinity" : "faction_sinner"),
					Math.round(sinScore), Math.round(virtueScore)));
			for (int r = 0; r < cards.length; r++) {
				if (cards[r] == null) continue;
				sb.append("\n\n").append(Messages.get(AscentHooks.class, "region_" + r)).append(": _")
						.append(cards[r].title()).append("_");
			}
			AscentCard c = card();
			if (c != null) sb.append("\n\n").append(c.desc());
			return sb.toString();
		}
	}

	/** the faction's monsters: their kills count for nothing */
	public static class Herald extends Buff {
		{
			revivePersists = true;
		}
	}

	/** Patience: its health was raised */
	public static class Hardened extends Buff {
	}

	/** Envy's mirror maze: a shadow of the hero, not the trial's */
	public static class Shade extends Buff {
	}

	/** the predator's feast: what the monster swallowed */
	public static class Swallowed extends Buff {

		public final ArrayList<Item> items = new ArrayList<>();

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("items", items);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			items.clear();
			if (bundle.contains("items")) {
				for (Bundlable b : bundle.getCollection("items")) items.add((Item) b);
			}
		}
	}
}
