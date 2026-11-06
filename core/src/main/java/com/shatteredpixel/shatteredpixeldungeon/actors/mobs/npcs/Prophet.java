package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.SealWarden;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.SealFragment;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ProphetSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndProphet;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The Prophet of Seals, a sewer quest NPC. The Seal Warden, a miniboss on the prophet's floor,
 * guards the apocalypse's seal; bring the seal back to choose one of the four horsemen artifacts.
 */
public class Prophet extends NPC {

	/** seals needed for the reward (old saves may still hold fragments from four seal bearers) */
	public static final int FRAGMENTS = 1;

	{
		spriteClass = ProphetSprite.class;
		properties.add(Property.IMMOVABLE);
	}

	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null) {
			die(null);
			return true;
		}
		return super.act();
	}

	@Override
	public int defenseSkill(Char enemy) {
		return INFINITE_EVASION;
	}

	@Override
	public void damage(int dmg, Object src) {
		//do nothing
	}

	@Override
	public boolean add(Buff buff) {
		return false;
	}

	@Override
	public boolean reset() {
		return true;
	}

	public static int fragmentsHeld() {
		SealFragment f = Dungeon.hero == null ? null : Dungeon.hero.belongings.getItem(SealFragment.class);
		return f == null ? 0 : f.quantity();
	}

	@Override
	public boolean interact(Char c) {
		sprite.turnTo(pos, Dungeon.hero.pos);
		if (c != Dungeon.hero) return true;

		final int held = fragmentsHeld();
		if (held >= FRAGMENTS) {
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndProphet(Prophet.this));
				}
			});
		} else {
			final String msg = Quest.given
					? Messages.get(this, "reminder", held, FRAGMENTS)
					: Messages.get(this, "intro", Messages.titleCase(Dungeon.hero.name()));
			Quest.given = true;
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(Prophet.this, msg));
				}
			});
		}
		return true;
	}

	/** marked one of the four champions that carried a seal fragment (older quest version, kept for old saves) */
	public static class SealBearer extends Buff {

		{
			type = buffType.NEUTRAL;
			announced = true;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.6f, 0.3f, 0.9f);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc");
		}
	}

	public static class Quest {

		private static boolean spawned;
		private static boolean given;
		private static boolean completed;
		private static int depth;

		public static void reset() {
			spawned = false;
			given = false;
			completed = false;
			depth = 0;
		}

		public static boolean completed() {
			return spawned && completed;
		}

		public static void complete() {
			completed = true;
		}

		private static final String NODE = "sealProphet";

		public static void storeInBundle(Bundle bundle) {
			Bundle node = new Bundle();
			node.put("spawned", spawned);
			if (spawned) {
				node.put("given", given);
				node.put("completed", completed);
				node.put("depth", depth);
			}
			bundle.put(NODE, node);
		}

		public static void restoreFromBundle(Bundle bundle) {
			Bundle node = bundle.getBundle(NODE);
			if (!node.isNull() && (spawned = node.getBoolean("spawned"))) {
				given = node.getBoolean("given");
				completed = node.getBoolean("completed");
				depth = node.getInt("depth");
			} else {
				reset();
			}
		}

		/** called while a sewer level is generated; same odds as the sad ghost (forced by floor 4) */
		public static void spawn(Level level, ArrayList<Room> rooms, Room entrance, Room exit) {
			if (spawned || Dungeon.depth < 2 || Dungeon.depth > 4) return;
			if (Random.Int(5 - Dungeon.depth) != 0) return;

			ArrayList<Room> candidates = new ArrayList<>();
			for (Room r : rooms) {
				if (r != entrance && r != exit && r.width() > 3 && r.height() > 3) candidates.add(r);
			}
			if (candidates.size() < 2) return;

			Prophet prophet = new Prophet();
			prophet.pos = freeCell(level, Random.element(candidates));
			if (prophet.pos == -1) return;
			level.mobs.add(prophet);

			//the seal's guardian, kept away from the prophet's own room when possible
			SealWarden warden = new SealWarden();
			int cell = -1;
			for (int tries = 0; tries < 10 && cell == -1; tries++) {
				int c = freeCell(level, Random.element(candidates));
				if (c != -1 && level.distance(c, prophet.pos) > 4) cell = c;
			}
			if (cell == -1) cell = freeCell(level, Random.element(candidates));
			if (cell == -1) {
				level.mobs.remove(prophet);
				return;
			}
			warden.pos = cell;
			level.mobs.add(warden);

			spawned = true;
			given = false;
			completed = false;
			depth = Dungeon.depth;
		}

		private static int freeCell(Level level, Room room) {
			for (int tries = 0; tries < 30; tries++) {
				int cell = level.pointToCell(room.random());
				if (cell != -1 && level.passable[cell] && !level.solid[cell] && level.openSpace[cell]
						&& level.findMob(cell) == null && cell != level.exit() && cell != level.entrance()) {
					return cell;
				}
			}
			return -1;
		}

		/** a seal bearer drops its fragment wherever it dies */
		public static void onMobDeath(Mob mob) {
			if (mob.buff(SealBearer.class) == null || Dungeon.level == null) return;
			Dungeon.level.drop(new SealFragment(), mob.pos).sprite.drop();
		}
	}
}
