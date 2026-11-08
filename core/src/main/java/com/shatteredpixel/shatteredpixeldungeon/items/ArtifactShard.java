package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Healing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Wraith;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.ChaliceOfBlood;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SandalsOfNature;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Seven Sins: a shard of a broken artifact. Made in the alchemy pot from one of the game's own
 * artifacts (not the mod's: sin artifacts, horsemen relics...), 1 + level/3 shards per artifact.
 * Using one gives a one-time version of that artifact's power. Shards of the same artifact stack.
 */
public class ArtifactShard extends Item {

	public static final String AC_USE = "USE";

	/** the artifacts that can be broken into shards, with the shard's glow colour */
	public static final LinkedHashMap<Class<? extends Artifact>, Integer> SOURCES = new LinkedHashMap<>();
	static {
		SOURCES.put(AlchemistsToolkit.class, 0x40C0FF);
		SOURCES.put(CapeOfThorns.class, 0xC08040);
		SOURCES.put(ChaliceOfBlood.class, 0xD01020);
		SOURCES.put(CloakOfShadows.class, 0x404060);
		SOURCES.put(DriedRose.class, 0xE06080);
		SOURCES.put(EtherealChains.class, 0x80F0E0);
		SOURCES.put(HolyTome.class, 0xFFF0A0);
		SOURCES.put(HornOfPlenty.class, 0xE0C040);
		SOURCES.put(MasterThievesArmband.class, 0xA060E0);
		SOURCES.put(SandalsOfNature.class, 0x40C040);
		SOURCES.put(SkeletonKey.class, 0xC0C0C0);
		SOURCES.put(TalismanOfForesight.class, 0x6080FF);
		SOURCES.put(TimekeepersHourglass.class, 0xF0D080);
		SOURCES.put(UnstableSpellbook.class, 0xFF60FF);
	}

	public static boolean canShatter(Item item) {
		return item != null && SOURCES.containsKey(item.getClass());
	}

	private Class<? extends Artifact> artifact = CloakOfShadows.class;

	{
		image = ItemSpriteSheet.ARTIFACT_SHARD;
		stackable = true;
		defaultAction = AC_USE;
	}

	public ArtifactShard() {
	}

	public ArtifactShard(Class<? extends Artifact> artifact) {
		this.artifact = artifact;
	}

	private String key() {
		return artifact.getSimpleName().toLowerCase();
	}

	@Override
	public boolean isSimilar(Item item) {
		return item instanceof ArtifactShard && ((ArtifactShard) item).artifact == artifact;
	}

	@Override
	public String name() {
		return Messages.get(this, "name", Messages.get(artifact, "name"));
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc") + "\n\n" + Messages.get(this, "effect_" + key());
	}

	@Override
	public ItemSprite.Glowing glowing() {
		Integer c = SOURCES.get(artifact);
		return new ItemSprite.Glowing(c == null ? 0xFFFFFF : c, 1.5f);
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_USE)) return Messages.get(this, "ac_use");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_USE)) return;

		if (artifact == EtherealChains.class || artifact == SkeletonKey.class) {
			curUser = hero;
			curItem = this;
			GameScene.selectCell(targeter);
			return;
		}
		if (use(hero)) consume(hero);
	}

	private void consume(Hero hero) {
		detach(hero.belongings.backpack);
		hero.sprite.operate(hero.pos);
		hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 6);
		Sample.INSTANCE.play(Assets.Sounds.SHATTER, 0.6f, 1.4f);
		updateQuickslot();
		hero.spendAndNext(1f);
	}

	/** the untargeted powers; returns false when nothing happened (the shard is kept) */
	private boolean use(Hero hero) {
		int depth = Dungeon.scalingDepth();
		if (artifact == AlchemistsToolkit.class) {
			Dungeon.energy += 12;
			hero.sprite.showStatus(0x44CCFF, "+12");
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == CapeOfThorns.class) {
			Buff.affect(hero, Barrier.class).setShield(hero.HT / 4 + depth);
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == ChaliceOfBlood.class) {
			Buff.affect(hero, Healing.class).setHeal(hero.HT / 2, 0.25f, 0);
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == CloakOfShadows.class) {
			Buff.affect(hero, Invisibility.class, 12f);
			Sample.INSTANCE.play(Assets.Sounds.MELD);
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == DriedRose.class) {
			int made = 0;
			ArrayList<Integer> cells = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8) {
				int c = hero.pos + n;
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) cells.add(c);
			}
			Random.shuffle(cells);
			for (int c : cells) {
				if (made >= 2) break;
				Spirit s = new Spirit();
				s.adjustStats(depth);
				s.pos = c;
				GameScene.add(s);
				CellEmitter.get(c).burst(Speck.factory(Speck.LIGHT), 4);
				made++;
			}
			if (made == 0) {
				GLog.w(Messages.get(this, "no_space"));
				return false;
			}
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == HolyTome.class) {
			Buff.affect(hero, Bless.class, Bless.DURATION);
			int heal = Math.min(hero.HT - hero.HP, hero.HT / 4);
			if (heal > 0) {
				hero.HP += heal;
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText.HEALING);
			}
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == HornOfPlenty.class) {
			Hunger hunger = hero.buff(Hunger.class);
			if (hunger != null) hunger.satisfy(450);
			hero.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);
			Sample.INSTANCE.play(Assets.Sounds.EAT);
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == MasterThievesArmband.class) {
			Dungeon.level.drop(new Gold(40 + 12 * depth), hero.pos).sprite.drop();
			Item loot = Generator.random(Random.oneOf(Generator.Category.POTION, Generator.Category.SCROLL, Generator.Category.STONE));
			if (loot != null) Dungeon.level.drop(loot, hero.pos).sprite.drop();
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == SandalsOfNature.class) {
			for (int c = 0; c < Dungeon.level.length(); c++) {
				if (Dungeon.level.distance(hero.pos, c) > 2) continue;
				int t = Dungeon.level.map[c];
				if (t == Terrain.EMPTY || t == Terrain.GRASS || t == Terrain.EMBERS || t == Terrain.EMPTY_DECO) {
					com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(c, Terrain.HIGH_GRASS);
					GameScene.updateMap(c);
				}
			}
			for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
				if (m.alignment == Char.Alignment.ENEMY && Dungeon.level.heroFOV[m.pos]
						&& Dungeon.level.distance(hero.pos, m.pos) <= 3) {
					Buff.prolong(m, Roots.class, 5f);
					CellEmitter.bottom(m.pos).start(com.shatteredpixel.shatteredpixeldungeon.effects.particles.EarthParticle.FACTORY, 0.05f, 8);
				}
			}
			Barkskin.conditionallyAppend(hero, 4 + depth / 2, 3);
			Dungeon.observe();
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == TalismanOfForesight.class) {
			boolean[] mapped = Dungeon.level.mapped;
			boolean[] discoverable = Dungeon.level.discoverable;
			for (int i = 0; i < Dungeon.level.length(); i++) {
				if (!discoverable[i]) continue;
				mapped[i] = true;
				int terr = Dungeon.level.map[i];
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {
					Dungeon.level.discover(i);
					if (Dungeon.level.heroFOV[i]) {
						GameScene.discoverTile(i, terr);
						ScrollOfMagicMapping.discover(i);
					}
				}
			}
			GameScene.updateFog();
			Buff.affect(hero, MindVision.class, 5f);
			Dungeon.observe();
			SpellSprite.show(hero, SpellSprite.MAP);
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == TimekeepersHourglass.class) {
			Buff.affect(hero, Swiftthistle.TimeBubble.class).reset(5);
			Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
			GLog.p(Messages.get(this, "used_" + key()));
		} else if (artifact == UnstableSpellbook.class) {
			//detach first: the scroll's own read takes the turn
			detach(hero.belongings.backpack);
			updateQuickslot();
			Scroll scroll;
			do {
				scroll = (Scroll) Generator.randomUsingDefaults(Generator.Category.SCROLL);
			} while (scroll == null || scroll instanceof ScrollOfTransmutation);
			scroll.anonymize();
			scroll.talentChance = 0;
			curUser = hero;
			curItem = scroll;
			GLog.p(Messages.get(this, "used_" + key(), scroll.trueName()));
			scroll.doRead();
			return false;
		} else {
			return false;
		}
		return true;
	}

	// ------------------------------------------------------------------ targeted powers

	private static final CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null || !(curItem instanceof ArtifactShard)) return;
			ArtifactShard shard = (ArtifactShard) curItem;
			Hero hero = curUser;
			boolean done = shard.artifact == EtherealChains.class ? chain(hero, cell) : unlock(hero, cell);
			if (done) shard.consume(hero);
		}

		@Override
		public String prompt() {
			return Messages.get(ArtifactShard.class, "prompt");
		}
	};

	/** ethereal chains: pull an enemy to you, or pull yourself to a spot */
	private static boolean chain(Hero hero, int cell) {
		if (cell == hero.pos || Dungeon.level.distance(hero.pos, cell) > 8) {
			GLog.w(Messages.get(ArtifactShard.class, "invalid"));
			return false;
		}
		Ballistica line = new Ballistica(hero.pos, cell, Ballistica.STOP_TARGET | Ballistica.STOP_CHARS | Ballistica.STOP_SOLID);
		Char ch = Actor.findChar(line.collisionPos);
		if (ch != null && ch != hero) {
			if (ch.properties().contains(Char.Property.IMMOVABLE) || ch.properties().contains(Char.Property.BOSS)) {
				GLog.w(Messages.get(ArtifactShard.class, "too_heavy"));
				return false;
			}
			int dest = -1;
			for (int i = 1; i < line.path.size() && line.path.get(i) != ch.pos; i++) {
				int c = line.path.get(i);
				if (Dungeon.level.passable[c] && Actor.findChar(c) == null) { dest = c; break; }
			}
			if (dest == -1) {
				GLog.w(Messages.get(ArtifactShard.class, "invalid"));
				return false;
			}
			Actor.add(new Pushing(ch, ch.pos, dest));
			ch.pos = dest;
			Dungeon.level.occupyCell(ch);
			Sample.INSTANCE.play(Assets.Sounds.CHAINS);
			Dungeon.observe();
			return true;
		}
		//no one there: pull yourself to the farthest free cell along the chain
		int dest = -1;
		for (int i = 1; i < line.path.size(); i++) {
			int c = line.path.get(i);
			if (Dungeon.level.solid[c] || !Dungeon.level.passable[c] || Actor.findChar(c) != null) break;
			dest = c;
			if (c == line.collisionPos) break;
		}
		if (dest == -1) {
			GLog.w(Messages.get(ArtifactShard.class, "invalid"));
			return false;
		}
		Actor.add(new Pushing(hero, hero.pos, dest));
		hero.pos = dest;
		Dungeon.level.occupyCell(hero);
		Sample.INSTANCE.play(Assets.Sounds.CHAINS);
		Dungeon.observe();
		GameScene.updateFog();
		return true;
	}

	/** skeleton key: open a locked door or chest (no key needed) */
	private static boolean unlock(Hero hero, int cell) {
		int t = Dungeon.level.map[cell];
		if (t == Terrain.LOCKED_DOOR || t == Terrain.CRYSTAL_DOOR) {
			com.shatteredpixel.shatteredpixeldungeon.levels.Level.set(cell, Terrain.DOOR);
			GameScene.updateMap(cell);
			Dungeon.observe();
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
			GLog.p(Messages.get(ArtifactShard.class, "used_skeletonkey"));
			return true;
		}
		Heap heap = Dungeon.level.heaps.get(cell);
		if (heap != null && (heap.type == Heap.Type.LOCKED_CHEST || heap.type == Heap.Type.CRYSTAL_CHEST)) {
			heap.type = Heap.Type.CHEST;
			if (heap.sprite != null) heap.sprite.link();
			Sample.INSTANCE.play(Assets.Sounds.UNLOCK);
			GLog.p(Messages.get(ArtifactShard.class, "used_skeletonkey"));
			return true;
		}
		GLog.w(Messages.get(ArtifactShard.class, "invalid"));
		return false;
	}

	// ------------------------------------------------------------------ dried rose: short-lived allied spirits

	public static class Spirit extends Wraith {

		private int left = 30;

		{
			alignment = Alignment.ALLY;
			state = HUNTING;
		}

		@Override
		protected boolean act() {
			if (--left <= 0) {
				die(null);
				return true;
			}
			return super.act();
		}

		@Override
		public String name() {
			return Messages.get(ArtifactShard.class, "spirit");
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt("left");
		}
	}

	// ------------------------------------------------------------------

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("artifact", artifact);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		Class<?> c = bundle.getClass("artifact");
		if (c != null && SOURCES.containsKey(c)) artifact = (Class<? extends Artifact>) c;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 30 * quantity;
	}

	/** alchemy: one of the game's artifacts (not cursed) -> 1 + level/3 shards of it, 6 energy */
	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			return ingredients.size() == 1 && canShatter(ingredients.get(0)) && !ingredients.get(0).cursed;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 6;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Item out = sampleOutput(ingredients);
			ingredients.get(0).quantity(ingredients.get(0).quantity() - 1);
			return out;
		}

		@Override
		@SuppressWarnings("unchecked")
		public Item sampleOutput(ArrayList<Item> ingredients) {
			if (ingredients.isEmpty() || !canShatter(ingredients.get(0))) return new ArtifactShard();
			Artifact a = (Artifact) ingredients.get(0);
			return new ArtifactShard((Class<? extends Artifact>) a.getClass()).quantity(1 + a.visiblyUpgraded() / 3);
		}
	}
}
