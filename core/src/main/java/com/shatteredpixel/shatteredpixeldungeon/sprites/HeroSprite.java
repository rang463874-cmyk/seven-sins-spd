/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;

public class HeroSprite extends CharSprite {
	
	private static final int FRAME_WIDTH	= 12;
	private static final int FRAME_HEIGHT	= 15;
	
	private static final int RUN_FRAMERATE	= 20;
	
	private static TextureFilm tiers;
	
	private Animation fly;
	private Animation read;

	public HeroSprite() {
		super();
		
		texture( Dungeon.hero.heroClass.spritesheet() );
		updateArmor();
		
		link( Dungeon.hero );

		if (ch.isAlive())
			idle();
		else
			die();
	}

	public void disguise(HeroClass cls){
		texture( cls.spritesheet() );
		updateArmor();
	}
	
	public void updateArmor() {

		//Seven Sins: hi-res sheets are shown at their own frame size, scaled down to the vanilla height
		TextureFilm film = film( texture, Dungeon.hero.tier() );
		applyArtScale( this, texture );
		
		idle = new Animation( 1, true );
		idle.frames( film, 0, 0, 0, 1, 0, 0, 1, 1 );
		
		run = new Animation( RUN_FRAMERATE, true );
		run.frames( film, 2, 3, 4, 5, 6, 7 );
		
		die = new Animation( 20, false );
		die.frames( film, 8, 9, 10, 11, 12, 11 );
		
		attack = new Animation( 15, false );
		attack.frames( film, 13, 14, 15, 0 );
		
		zap = attack.clone();
		
		operate = new Animation( 8, false );
		operate.frames( film, 16, 17, 16, 17 );
		
		fly = new Animation( 1, true );
		fly.frames( film, 18 );

		read = new Animation( 20, false );
		read.frames( film, 19, 20, 20, 20, 20, 20, 20, 20, 20, 19 );
		
		if (Dungeon.hero.isAlive())
			idle();
		else
			die();
	}
	
	@Override
	public void place( int p ) {
		super.place( p );
		if (Game.scene() instanceof GameScene) Camera.main.panFollow(this, 5f);
	}

	@Override
	public void move( int from, int to ) {
		super.move( from, to );
		if (ch != null && ch.flying) {
			play( fly );
		}
		Camera.main.panFollow(this, 20f);
	}

	@Override
	public void idle() {
		super.idle();
		if (ch != null && ch.flying) {
			play( fly );
		}
	}

	@Override
	public void jump( int from, int to, float height, float duration,  Callback callback ) {
		super.jump( from, to, height, duration, callback );
		play( fly );
		Camera.main.panFollow(this, 20f);
	}

	public synchronized void read() {
		animCallback = new Callback() {
			@Override
			public void call() {
				idle();
				ch.onOperateComplete();
			}
		};
		play( read );
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
		//Does nothing.

		/*
		 * This is both for visual clarity, and also for content ratings regarding violence
		 * towards human characters. The heroes are the only human or human-like characters which
		 * participate in combat, so removing all blood associated with them is a simple way to
		 * reduce the violence rating of the game.
		 */
	}

	@Override
	public void update() {
		sleeping = ch.isAlive() && ((Hero)ch).resting;
		
		super.update();
	}
	
	public void sprint( float speed ) {
		run.delay = 1f / speed / RUN_FRAMERATE;
	}
	
	//Seven Sins: hi-res hero sheets (the owner's redesigns). A sheet whose width is exactly FRAMES frames and whose
	//height is exactly TIERS rows (the vanilla sheets are 256x128) holds frames of width/FRAMES x height/TIERS pixels;
	//they are drawn at FRAME_HEIGHT / frame height so the hero keeps its usual size on screen, with more detail.
	public static final int FRAMES = 21;
	public static final int TIERS = 7;

	//owner's call: hi-res heroes are drawn a little bigger than vanilla heroes in the dungeon (not in UI avatars)
	public static final float HI_RES_ZOOM = 1.2f;

	public static boolean hiRes( SmartTexture tx ) {
		return tx != null && tx.width != 256 && tx.width % FRAMES == 0 && tx.height % TIERS == 0;
	}

	public static int frameW( SmartTexture tx ) {
		return hiRes( tx ) ? tx.width / FRAMES : FRAME_WIDTH;
	}

	public static int frameH( SmartTexture tx ) {
		return hiRes( tx ) ? tx.height / TIERS : FRAME_HEIGHT;
	}

	public static float artScale( SmartTexture tx ) {
		return hiRes( tx ) ? FRAME_HEIGHT / (float)frameH( tx ) : 1f;
	}

	public static TextureFilm film( SmartTexture tx, int tier ) {
		if (!hiRes( tx )) {
			return new TextureFilm( tiers(), tier, FRAME_WIDTH, FRAME_HEIGHT );
		}
		TextureFilm rows = new TextureFilm( tx, tx.width, frameH( tx ) );
		return new TextureFilm( rows, Math.min( tier, TIERS - 1 ), frameW( tx ), frameH( tx ) );
	}

	//scale (x HI_RES_ZOOM) + a shadow as wide as the figure's (hi-res frames are wider than the figure, for lying/attack poses)
	public static void applyArtScale( CharSprite sprite, SmartTexture tx ) {
		if (!hiRes( tx )) {
			sprite.scale.set( 1f );
			sprite.shadowWidth = 1.2f;
			return;
		}
		float s = artScale( tx ) * HI_RES_ZOOM;
		sprite.scale.set( s );
		sprite.shadowWidth = 1.2f * FRAME_WIDTH * HI_RES_ZOOM / (frameW( tx ) * s);
	}

	public static TextureFilm tiers() {
		if (tiers == null) {
			SmartTexture texture = TextureCache.get( Assets.Sprites.ROGUE );
			tiers = new TextureFilm( texture, texture.width, FRAME_HEIGHT );
		}
		
		return tiers;
	}

	public static Image avatar( Hero hero ){
		if (hero.buff(HeroDisguise.class) != null){
			return avatar(hero.buff(HeroDisguise.class).getDisguise(), hero.tier());
		} else {
			return avatar(hero.heroClass, hero.tier());
		}
	}
	
	public static Image avatar( HeroClass cl, int armorTier ) {

		SmartTexture tx = TextureCache.get( cl.spritesheet() );
		if (hiRes( tx )) {
			//a square from the middle of the first frame (the figures are wider than vanilla's), scaled to 15 high
			int fw = frameW( tx ), fh = frameH( tx );
			float cw = Math.min( fw, fh );
			float x0 = Math.round( (fw - cw) / 2f );
			float y0 = armorTier * fh;
			Image avatar = new Image( tx );
			avatar.frame( tx.uvRect( x0, y0, x0 + Math.round(cw), y0 + fh ) );
			avatar.scale.set( artScale( tx ) );
			return avatar;
		}
		
		RectF patch = tiers().get( armorTier );
		Image avatar = new Image( cl.spritesheet() );
		RectF frame = avatar.texture.uvRect( 1, 0, FRAME_WIDTH, FRAME_HEIGHT );
		frame.shift( patch.left, patch.top );
		avatar.frame( frame );
		
		return avatar;
	}
}
