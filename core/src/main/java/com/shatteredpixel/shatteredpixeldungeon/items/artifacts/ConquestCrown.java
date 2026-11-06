package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * Crown of Conquest, the white horseman. Its power forces a badly wounded enemy to surrender:
 * it fights for the hero for a while (granting exp and loot right away), then collapses.
 */
public class ConquestCrown extends HorsemanArtifact {

	{
		image = ItemSpriteSheet.ARTIFACT_CONQUEST;
	}

	@Override
	protected float rechargeTurns() {
		return 150f - 7f * level();
	}

	/** fraction of max HP an enemy must be at or below to surrender */
	public float threshold() {
		return 0.30f + 0.03f * level();
	}

	public float serviceTurns() {
		return 10f + 2f * level();
	}

	@Override
	protected boolean usePower(Hero hero) {
		GameScene.selectCell(selector);
		return true;
	}

	@Override
	protected String statsDesc() {
		return Messages.get(this, "stats", Math.round(threshold() * 100), Math.round(serviceTurns()));
	}

	private final CellSelector.Listener selector = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			Hero hero = Dungeon.hero;
			Char ch = Actor.findChar(cell);
			if (!(ch instanceof Mob) || ch.alignment != Char.Alignment.ENEMY || !Dungeon.level.heroFOV[cell]) {
				GLog.w(Messages.get(ConquestCrown.class, "no_target"));
				return;
			}
			Mob mob = (Mob) ch;
			if (mob.properties().contains(Char.Property.BOSS) || mob.properties().contains(Char.Property.MINIBOSS)
					|| mob.isImmune(Subjugated.class)) {
				GLog.w(Messages.get(ConquestCrown.class, "refuses"));
				return;
			}
			if (mob.HP > mob.HT * threshold()) {
				GLog.w(Messages.get(ConquestCrown.class, "too_strong"));
				return;
			}

			AllyBuff.affectAndLoot(mob, hero, Subjugated.class);
			Subjugated s = mob.buff(Subjugated.class);
			if (s != null) {
				s.left = serviceTurns();
				mob.aggro(null);
				mob.state = mob.WANDERING;
			}
			mob.sprite.emitter().burst(Speck.factory(Speck.CROWN), 6);
			mob.sprite.showStatus(CharSprite.POSITIVE, Messages.get(ConquestCrown.class, "kneel"));
			Sample.INSTANCE.play(Assets.Sounds.CHARMS);
			spendCharge(1);
			hero.sprite.zap(cell);
			hero.spendAndNext(1f);
		}

		@Override
		public String prompt() {
			return Messages.get(ConquestCrown.class, "prompt");
		}
	};

	/** a surrendered enemy fighting for the hero, until it collapses */
	public static class Subjugated extends AllyBuff {

		public float left = 10f;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			left -= TICK;
			if (left <= 0) {
				target.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(ConquestCrown.class, "collapse"));
				target.die(null);
				return true;
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.CORRUPT;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 1f, 0.8f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(Math.max(0, Math.round(left)));
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", Math.max(0, Math.round(left)));
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("left", left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getFloat("left");
		}
	}
}
