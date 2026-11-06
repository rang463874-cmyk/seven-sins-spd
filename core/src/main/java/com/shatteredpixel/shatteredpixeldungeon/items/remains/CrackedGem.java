package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;

/** Pride's remains: the next attack that would hit you is blocked completely. */
public class CrackedGem extends RemainsItem {

	public static final float DURATION = 30f;

	{
		image = ItemSpriteSheet.REMAINS_PRIDE;
	}

	@Override
	protected void doEffect(Hero hero) {
		Buff.prolong(hero, Ward.class, DURATION);
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
	}

	public static class Ward extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		/** called from Hero.defenseProc: blocks the whole hit and fades */
		public static int absorb(Char ch, int damage) {
			Ward w = ch.buff(Ward.class);
			if (w == null || damage <= 0) return damage;
			w.detach();
			ch.sprite.showStatus(CharSprite.POSITIVE, Messages.get(Ward.class, "blocked"));
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY);
			return 0;
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.8f, 0.6f, 1f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (DURATION - visualcooldown()) / DURATION);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dispTurns());
		}
	}
}
