package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

/**
 * Sprites of the heavens' monsters (floors 27-30). Believer and priest are tinted vanilla sprites,
 * the lesser angel and seraph come from tools/sinsart/make_virtue_sprites.py (placeholders made from
 * the archangel art until the owner draws them).
 */
public class HeavenSprites {

	/** a white-robed monk */
	public static class Believer extends MonkSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(0xFFF4D8, 0.55f);
		}
		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}

	/** a senior monk in white and gold */
	public static class Priest extends SeniorSprite {
		@Override
		public void resetColor() {
			super.resetColor();
			tint(0xFFE8A0, 0.6f);
		}
		@Override
		public void link(Char ch) {
			super.link(ch);
			resetColor();
		}
	}

	/** 24px, throws white spears */
	public static class LesserAngel extends MobSprite {

		private int cellToAttack;

		public LesserAngel() {
			super();
			texture("sprites/heaven_lesser_angel.png");
			TextureFilm frames = new TextureFilm(texture, 24, 25);
			perspectiveRaise = -4 / 16f;

			idle = new Animation(3, true);
			idle.frames(frames, 0, 1);
			run = new Animation(8, true);
			run.frames(frames, 2, 3, 4, 5);
			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 8);
			zap = attack.clone();
			die = new Animation(6, false);
			die.frames(frames, 9, 10, 11);
			play(idle);
		}

		@Override
		public void attack(int cell) {
			if (!Dungeon.level.adjacent(cell, ch.pos)) {
				cellToAttack = cell;
				zap(cell);
			} else {
				super.attack(cell);
			}
		}

		@Override
		public void onComplete(Animation anim) {
			if (anim == zap) {
				idle();
				MissileSprite spear = (MissileSprite) parent.recycle(MissileSprite.class);
				spear.reset(this, cellToAttack, new Spear(), new Callback() {
					@Override
					public void call() {
						ch.onAttackComplete();
					}
				});
				spear.lightness(0.9f);
			} else {
				super.onComplete(anim);
			}
		}

		@Override
		public int blood() {
			return 0xFFFFF0C0;
		}

		public static class Spear extends Item {
			{
				image = ItemSpriteSheet.JAVELIN;
			}
		}
	}

	/** 32px drawn over the seraph's 2x2 tiles (its cell is the top-left one) */
	public static class Seraph extends MobSprite {

		public Seraph() {
			super();
			texture("sprites/heaven_seraph.png");
			TextureFilm frames = new TextureFilm(texture, 32, 33);
			shadowWidth = 1.4f;

			idle = new Animation(3, true);
			idle.frames(frames, 0, 1);
			run = new Animation(8, true);
			run.frames(frames, 2, 3, 4, 5);
			attack = new Animation(12, false);
			attack.frames(frames, 6, 7, 8);
			zap = attack.clone();
			die = new Animation(6, false);
			die.frames(frames, 9, 10, 11);
			play(idle);
		}

		@Override
		public PointF worldToCamera(int cell) {
			final int csize = DungeonTilemap.SIZE;
			int w = Dungeon.level.width();
			//centred on the 2x2 block, feet on its bottom edge
			return new PointF(
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(Camera.main, ((cell % w) + 1f) * csize - width() * 0.5f),
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(Camera.main, ((cell / w) + 2f) * csize - height())
			);
		}

		@Override
		public int blood() {
			return 0xFFFFF0C0;
		}
	}

	/** the seraph's other three tiles: an empty frame */
	public static class Blank extends MobSprite {
		public Blank() {
			super();
			texture("sprites/heaven_seraph.png");
			TextureFilm frames = new TextureFilm(texture, 32, 33);
			idle = new Animation(1, true);
			idle.frames(frames, 12);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = idle.clone();
			play(idle);
		}

		@Override
		public void link(Char ch) {
			super.link(ch);
			renderShadow = false;
		}
	}
}
