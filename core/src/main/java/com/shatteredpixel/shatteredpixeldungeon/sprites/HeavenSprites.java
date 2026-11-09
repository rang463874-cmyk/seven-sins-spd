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
 * Sprites of the heavens' monsters (floors 27-30), all from the owner's art via
 * tools/sinsart/make_virtue_sprites.py (virtue_art/src/heaven_*.png).
 */
public class HeavenSprites {

	/** a 24px one-row sheet from make_virtue_sprites.py: idle 2, run 4, attack 3, die 3 */
	public static abstract class Sheet24 extends MobSprite {
		public Sheet24(String tex) {
			super();
			texture(tex);
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
	}

	/** the owner's believer: a grey-hooded girl with a staff and a book */
	public static class Believer extends Sheet24 {
		public Believer() { super("sprites/heaven_believer.png"); }
	}

	/** the owner's priest: an old bearded priest with a golden staff */
	public static class Priest extends Sheet24 {
		public Priest() { super("sprites/heaven_priest.png"); }
	}

	/** the owner's lesser angel: a white figure with a red bow (24px, shoots from where it stands) */
	public static class LesserAngel extends Sheet24 {

		private int cellToAttack;

		public LesserAngel() {
			super("sprites/heaven_lesser_angel.png");
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
			} else {
				super.onComplete(anim);
			}
		}

		@Override
		public int blood() {
			return 0xFFFFF0C0;
		}

		//it holds a bow in the owner's art, so it shoots a glowing arrow
		public static class Spear extends Item {
			{
				image = ItemSpriteSheet.SPIRIT_ARROW;
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

	/** the owner's holy giant (112x62), its cell is the ledge; the body towers over the void behind it */
	public static class HolyGiant extends MobSprite {
		public HolyGiant() {
			super();
			texture("sprites/heaven_giant.png");
			TextureFilm frames = new TextureFilm(texture, 112, 63);
			idle = new Animation(2, true);
			idle.frames(frames, 0, 1);
			run = idle.clone();
			attack = new Animation(10, false);
			attack.frames(frames, 2, 3, 4);
			zap = attack.clone();
			die = new Animation(5, false);
			die.frames(frames, 5, 6, 7);
			play(idle);
		}
		@Override
		public void link(Char ch) {
			super.link(ch);
			renderShadow = false;
		}
		@Override
		public PointF worldToCamera(int cell) {
			final int csize = DungeonTilemap.SIZE;
			int w = Dungeon.level.width();
			return new PointF(
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(Camera.main, ((cell % w) + 0.5f) * csize - width() * 0.5f),
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(Camera.main, ((cell / w) + 1f) * csize - height())
			);
		}
		@Override
		public int blood() {
			return 0xFF80C8FF;
		}
	}

	/** the owner's giant fists (32px over the tile they lie on): row 0 left, row 1 right */
	public static class GiantFist extends MobSprite {
		private int row = -1;
		private TextureFilm film;
		public GiantFist() {
			super();
			texture("sprites/heaven_fists.png");
			film = new TextureFilm(texture, 32, 33);
			perspectiveRaise = -8 / 16f;
			setRow(0);
		}
		private void setRow(int r) {
			if (r == row) return;
			row = r;
			int o = r * 5;
			idle = new Animation(2, true);
			idle.frames(film, o, o + 1);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(6, false);
			die.frames(film, o + 2, o + 3, o + 4);
			play(idle);
		}
		@Override
		public void link(Char ch) {
			super.link(ch);
			if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.GiantFist) {
				setRow(((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.GiantFist) ch).side == 0 ? 0 : 1);
			}
		}
		@Override
		public int blood() {
			return 0xFF80C8FF;
		}
	}

	/** the whole arm slammed down (48x128 over the 3x8 strip), drawn up from the fist's tile: row 0 left, row 1 right */
	public static class GiantArm extends MobSprite {
		private int row = -1;
		private TextureFilm film;
		public GiantArm() {
			super();
			texture("sprites/heaven_arms.png");
			film = new TextureFilm(texture, 48, 129);
			setRow(0);
		}
		private void setRow(int r) {
			if (r == row) return;
			row = r;
			int o = r * 5;
			idle = new Animation(2, true);
			idle.frames(film, o, o + 1);
			run = idle.clone();
			attack = idle.clone();
			zap = idle.clone();
			die = new Animation(6, false);
			die.frames(film, o + 2, o + 3, o + 4);
			play(idle);
		}
		@Override
		public void link(Char ch) {
			super.link(ch);
			renderShadow = false;
			if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.GiantFist) {
				setRow(((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.GiantFist) ch).side == 0 ? 0 : 1);
			}
		}
		@Override
		public PointF worldToCamera(int cell) {
			final int csize = DungeonTilemap.SIZE;
			int w = Dungeon.level.width();
			return new PointF(
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(Camera.main, ((cell % w) + 0.5f) * csize - width() * 0.5f),
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(Camera.main, ((cell / w) + 1f) * csize - height())
			);
		}
		@Override
		public int blood() {
			return 0xFF80C8FF;
		}
	}

	/** the giant's branching lightning (21x17 tiles) over the arena; fades out by itself */
	public static void lightning(int startX, int startY, boolean mirror, float alpha, float fade) {
		if (com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon.scene()
				instanceof com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene == false) return;
		final com.watabou.noosa.Image img = new com.watabou.noosa.Image("sprites/heaven_lightning.png");
		img.x = startX * DungeonTilemap.SIZE;
		img.y = startY * DungeonTilemap.SIZE;
		if (mirror) {
			img.origin.set(img.width / 2f, 0);
			img.scale.x = -1;
		}
		img.alpha(alpha);
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.effectOverFog(img);
		img.parent.add(new com.watabou.noosa.tweeners.AlphaTweener(img, 0f, fade) {
			@Override
			protected void onComplete() {
				img.killAndErase();
			}
		});
	}
}
