package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

/** Seven Sins: the trial of sin's shadow, drawn with the hero's own sheet in black and violet */
public class ShadowSprite extends MobSprite {

	public ShadowSprite(HeroClass cls) {
		super();
		texture((cls == null ? HeroClass.WARRIOR : cls).spritesheet());
		TextureFilm film = HeroSprite.film(texture, 0);
		HeroSprite.applyArtScale(this, texture);

		idle = new Animation(1, true);
		idle.frames(film, 0, 0, 0, 1, 0, 0, 1, 1);
		run = new Animation(20, true);
		run.frames(film, 2, 3, 4, 5, 6, 7);
		die = new Animation(20, false);
		die.frames(film, 8, 9, 10, 11, 12, 11);
		attack = new Animation(15, false);
		attack.frames(film, 13, 14, 15, 0);
		zap = attack.clone();
		idle();
		shade();
	}

	private void shade() {
		tint(0.12f, 0.02f, 0.2f, 0.75f);
	}

	@Override
	public void resetColor() {
		super.resetColor();
		shade();
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//shadows don't bleed
	}
}
