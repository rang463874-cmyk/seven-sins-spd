package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.HolyGiant;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.RankingsScene;
import com.shatteredpixel.shatteredpixeldungeon.sins.SinAwakening;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * The blue gem the holy giant leaves behind (floor 31). Reaching it ends the fight (the giant falls and
 * every monster on the floor fades). Carried, it lets a sin hero take the second awakening
 * ({@link SinAwakening}), or ends the journey: the run is won, like ascending with the amulet.
 */
public class HolyGem extends Item {

	public static final String AC_AWAKEN = "AWAKEN";
	public static final String AC_END = "END";
	public static final String AC_TALENTS = "TALENTS";

	{
		image = ItemSpriteSheet.HOLY_GEM;
		unique = true;
		bones = false;
		defaultAction = AC_AWAKEN;
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {
		HolyGiant giant = null;
		for (Mob m : Dungeon.level.mobs) {
			if (m instanceof HolyGiant) giant = (HolyGiant) m;
		}
		boolean picked = super.doPickUp(hero, pos);
		if (picked && giant != null && giant.isAlive()) {
			GLog.p(Messages.get(this, "claimed"));
			giant.vanquish();
		}
		return picked;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (SinAwakening.canAwaken(hero)) actions.add(AC_AWAKEN);
		if (hero.awakening != SinAwakening.NONE) {
			actions.add(AC_TALENTS);
			//the gem holds the awakening: it can't be dropped or thrown away
			actions.remove(AC_DROP);
			actions.remove(AC_THROW);
		}
		actions.add(AC_END);
		return actions;
	}

	@Override
	public String defaultAction() {
		if (SinAwakening.canAwaken(Dungeon.hero)) return AC_AWAKEN;
		if (Dungeon.hero != null && Dungeon.hero.awakening != SinAwakening.NONE) return AC_TALENTS;
		return AC_END;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_AWAKEN)) return Messages.get(this, "ac_awaken");
		if (action.equals(AC_END)) return Messages.get(this, "ac_end");
		if (action.equals(AC_TALENTS)) return Messages.get(this, "ac_talents");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_AWAKEN)) {
			if (!SinAwakening.canAwaken(hero)) {
				GLog.w(Messages.get(this, "no_awaken"));
				return;
			}
			final Hero h = hero;
			String control = SinAwakening.pathName(hero.heroClass, SinAwakening.CONTROL);
			String monster = SinAwakening.pathName(hero.heroClass, SinAwakening.MONSTER);
			GameScene.show(new WndOptions(new ItemSprite(this),
					Messages.titleCase(name()),
					Messages.get(this, "awaken_prompt",
							control, SinAwakening.pathDesc(hero.heroClass, SinAwakening.CONTROL),
							monster, SinAwakening.pathDesc(hero.heroClass, SinAwakening.MONSTER)),
					control, monster) {
				@Override
				protected void onSelect(int index) {
					int path = index == 0 ? SinAwakening.CONTROL : SinAwakening.MONSTER;
					confirmAwaken(h, path);
				}
			});
		} else if (action.equals(AC_TALENTS)) {
			if (hero.awakening != SinAwakening.NONE) GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndAwakening(hero));
		} else if (action.equals(AC_END)) {
			GameScene.show(new WndOptions(new ItemSprite(this),
					Messages.titleCase(name()),
					Messages.get(this, "end_prompt"),
					Messages.get(this, "end_yes"), Messages.get(this, "end_no")) {
				@Override
				protected void onSelect(int index) {
					if (index == 0) endJourney();
				}
			});
		}
	}

	private void confirmAwaken(final Hero hero, final int path) {
		final String name = SinAwakening.pathName(hero.heroClass, path);
		GameScene.show(new WndOptions(new ItemSprite(this),
				Messages.titleCase(name),
				Messages.get(this, "awaken_confirm", name),
				Messages.get(this, "end_yes"), Messages.get(this, "end_no")) {
			@Override
			protected void onSelect(int index) {
				if (index != 0) return;
				SinAwakening.awaken(hero, path);
				hero.sprite.emitter().burst(Speck.factory(Speck.LIGHT), 16);
				hero.sprite.operate(hero.pos);
				GameScene.flash(path == SinAwakening.MONSTER ? 0x802020 : 0x88BBFF);
				Sample.INSTANCE.play(Assets.Sounds.LEVELUP);
				GLog.p(Messages.get(HolyGem.class, "awakened", name, SinAwakening.POINTS));
				hero.updateHT(false);
				hero.spendAndNext(1f);
				GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndAwakening(hero));
			}
		});
	}

	private void endJourney() {
		Dungeon.win(Amulet.class);
		Dungeon.deleteGame(GamesInProgress.curSlot, true);
		Badges.saveGlobal();
		Game.switchScene(RankingsScene.class);
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
	public ItemSprite.Glowing glowing() {
		return new ItemSprite.Glowing(0x60B0FF, 1.2f);
	}

	@Override
	public int value() {
		return 0;
	}
}
