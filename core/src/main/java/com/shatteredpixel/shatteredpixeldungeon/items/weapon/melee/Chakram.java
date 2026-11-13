package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MissileSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;

import java.util.ArrayList;

/**
 * Seven Sins: a tier 3 returning weapon. A normal melee weapon that, while wielded, can be thrown (1 attack's time):
 * it flies up to RANGE tiles, cuts every enemy on the way for THROW_DMG of a hit, and comes back to the hand.
 */
public class Chakram extends MeleeWeapon {

	public static final String AC_HURL = "HURL";
	public static final int RANGE = 6;
	public static final float THROW_DMG = 0.8f;

	{
		image = ItemSpriteSheet.CHAKRAM;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.2f;
		tier = 3;
		usesTargeting = true;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && hero.belongings.weapon == this) actions.add(AC_HURL);
		return actions;
	}

	@Override
	public String defaultAction() {
		if (Dungeon.hero != null && Dungeon.hero.belongings.weapon == this) return AC_HURL;
		return super.defaultAction();
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_HURL)) return Messages.get(this, "ac_hurl");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_HURL)) {
			if (hero.belongings.weapon != this) {
				GLog.w(Messages.get(this, "need_wield"));
				return;
			}
			curUser = hero;
			curItem = this;
			GameScene.selectCell(hurler);
		}
	}

	private static final CellSelector.Listener hurler = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell != null && curItem instanceof Chakram && curUser != null && cell != curUser.pos) {
				((Chakram) curItem).hurl(curUser, cell);
			}
		}

		@Override
		public String prompt() {
			return Messages.get(Chakram.class, "prompt");
		}
	};

	public void hurl(final Hero hero, int target) {
		Ballistica path = new Ballistica(hero.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID);
		int last = Math.min(path.dist, RANGE);
		final int end = path.path.get(last);
		final ArrayList<Char> victims = new ArrayList<>();
		for (int i = 1; i <= last; i++) {
			Char ch = Actor.findChar(path.path.get(i));
			if (ch != null && ch != hero && ch.alignment != hero.alignment) victims.add(ch);
		}

		hero.sprite.zap(end);
		Invisibility.dispel();
		Sample.INSTANCE.play(Assets.Sounds.MISS, 1f, 1.3f);
		final Chakram self = this;
		((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(hero.pos, end, this, new Callback() {
			@Override
			public void call() {
				for (Char ch : victims) {
					if (ch.isAlive()) hero.attack(ch, THROW_DMG, 0, 1f);
				}
				((MissileSprite) hero.sprite.parent.recycle(MissileSprite.class)).reset(end, hero.pos, self, new Callback() {
					@Override
					public void call() {
						hero.spendAndNext(hero.attackDelay());
					}
				});
			}
		});
	}

	@Override
	public String statsInfo() {
		return Messages.get(this, "stats_desc", RANGE, Math.round(THROW_DMG * 100));
	}
}
