package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.watabou.noosa.TextureFilm;

/** Placeholder art for the sewer prophet: the shopkeeper's hooded figure in deep violet. */
public class ProphetSprite extends MobSprite {

	public ProphetSprite() {
		super();

		texture( Assets.Sprites.KEEPER );
		TextureFilm film = new TextureFilm( texture, 14, 14 );

		idle = new Animation( 10, true );
		idle.frames( film, 1, 1, 1, 1, 1, 0, 0, 0, 0 );

		die = new Animation( 20, false );
		die.frames( film, 0 );

		run = idle.clone();
		attack = idle.clone();

		idle();
	}

	@Override
	public void link( Char ch ) {
		super.link( ch );
		hardlight( 0.55f, 0.35f, 0.8f );
	}

	@Override
	public void resetColor() {
		super.resetColor();
		hardlight( 0.55f, 0.35f, 0.8f );
	}

	@Override
	public void die() {
		super.die();
		emitter().burst( Speck.factory( Speck.LIGHT ), 8 );
	}
}
