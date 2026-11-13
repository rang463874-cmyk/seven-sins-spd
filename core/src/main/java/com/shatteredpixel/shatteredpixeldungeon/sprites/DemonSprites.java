package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.watabou.noosa.TextureFilm;

/**
 * Sprites for the demon lords (own sheet, one row per lord), their altar, and Beelzebub's flies
 * (a tinted swarm).
 */
public class DemonSprites {

	static void lordTint(CharSprite s, DemonLord lord) {
		if (lord == null) return;
		int c = lord.color;
		s.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
	}

	public static CharSprite forLord(DemonLord lord) {
		if (lord == null) return new Lucifer();
		switch (lord) {
			case MAMMON:    return new Mammon();
			case ASMODEUS:  return new Asmodeus();
			case LEVIATHAN: return new Leviathan();
			case BEELZEBUB: return new Beelzebub();
			case SATAN:     return new Satan();
			case BELPHEGOR: return new Belphegor();
			default:        return new Lucifer();
		}
	}

	/** one lord's row of sprites/demon_lords.png (tools/sinsart/make_demon_sprites.py): 48x49 frames, 12 per row.
	 *  Drawn over the 3x3 tiles around the lord's cell, like a big boss. */
	public static abstract class LordSprite extends MobSprite {

		public static final String TEXTURE = "sprites/demon_lords.png";

		protected abstract DemonLord lord();

		public LordSprite() {
			super();
			texture(TEXTURE);
			TextureFilm frames = new TextureFilm(texture, 48, 49);
			int o = lord().ordinal() * 12;

			//feet half a tile below the cell, so the 48px body covers the cells around it
			perspectiveRaise = -8 / 16f;
			shadowWidth = 0.7f;
			shadowHeight = 0.12f;
			shadowOffset = 0.2f;

			idle = new Animation(3, true);
			idle.frames(frames, o, o + 1);

			run = new Animation(8, true);
			run.frames(frames, o + 2, o + 3, o + 4, o + 5);

			attack = new Animation(12, false);
			attack.frames(frames, o + 6, o + 7, o + 8);

			zap = attack.clone();

			die = new Animation(6, false);
			die.frames(frames, o + 9, o + 10, o + 11);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF000000 | lord().color;
		}
	}

	/** a lord's sprite at half size, for window icons */
	public static CharSprite icon(DemonLord lord) {
		CharSprite s = forLord(lord);
		s.scale.set(0.5f);
		return s;
	}

	/** one piece of Leviathan's body along the wall (sprites/demon_leviathan.png, 48x49 frames, the owner's art).
	 *  Only every third cell shows a piece; the cells in between are hidden behind them. */
	public static class Coil extends MobSprite {

		public static final int BODY = 0, TAIL = 1, HIDDEN = 2;

		private TextureFilm film;

		public Coil() {
			super();
			texture("sprites/demon_leviathan.png");
			film = new TextureFilm(texture, 48, 49);
			perspectiveRaise = -8 / 16f;
			shadowWidth = 0f;
			setVariant(BODY);
		}

		public void setVariant(int v) {
			if (v == HIDDEN) {
				idle = new Animation(1, true);
				idle.frames(film, 6);
				die = idle.clone();
			} else {
				int o = Math.max(0, Math.min(1, v)) * 3;
				idle = new Animation(2, true);
				idle.frames(film, o, o + 1);
				die = new Animation(4, false);
				die.frames(film, o, o + 2);
			}
			run = idle.clone();
			attack = idle.clone();
			play(idle);
		}

		@Override
		public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) {
			super.link(ch);
			if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LeviathanCoil) {
				com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LeviathanCoil c =
						(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LeviathanCoil) ch;
				setVariant(c.variant);
				flipHorizontal = c.flip;
			}
		}

		@Override
		public void turnTo(int from, int to) {
			//the body never turns
		}

		@Override
		public int blood() {
			return 0xFF4080E0;
		}
	}

	/** Leviathan's head rising out of the abyss (sprites/demon_leviathan_head.png, 56x71 frames, the owner's art,
	 *  tools/sinsart/make_leviathan_art.py). Its cell is the ledge tile under its jaw; the rest towers over the void. */
	public static class LeviathanHead extends MobSprite {

		private Animation charge;
		private boolean open = false;

		public LeviathanHead() {
			super();
			texture("sprites/demon_leviathan_head.png");
			TextureFilm frames = new TextureFilm(texture, 56, 71);
			idle = new Animation(2, true);
			idle.frames(frames, 0, 1);
			run = idle.clone();
			charge = new Animation(4, true);
			charge.frames(frames, 2, 3);
			attack = new Animation(8, false);
			attack.frames(frames, 2, 2, 0);
			zap = attack.clone();
			die = new Animation(5, false);
			die.frames(frames, 4, 5, 6, 7);
			play(idle);
		}

		/** the mouth stays open while the laser charges */
		public void open(boolean value) {
			if (open == value) return;
			open = value;
			if (curAnim != die) play(open ? charge : idle);
		}

		@Override
		public void idle() {
			if (open && charge != null) play(charge);
			else super.idle();
		}

		@Override
		public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) {
			super.link(ch);
			renderShadow = false;
			if (ch instanceof com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LeviathanBoss) {
				open(((com.shatteredpixel.shatteredpixeldungeon.actors.mobs.LeviathanBoss) ch).charging());
			}
		}

		@Override
		public void turnTo(int from, int to) {
			//it always faces the arena
		}

		@Override
		public com.watabou.utils.PointF worldToCamera(int cell) {
			final int csize = com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap.SIZE;
			int w = com.shatteredpixel.shatteredpixeldungeon.Dungeon.level.width();
			return new com.watabou.utils.PointF(
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(com.watabou.noosa.Camera.main, ((cell % w) + 0.5f) * csize - width() * 0.5f),
					com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.align(com.watabou.noosa.Camera.main, ((cell / w) + 1f) * csize - height())
			);
		}

		@Override
		public int blood() {
			return 0xFF40C0FF;
		}
	}

	public static class Lucifer extends LordSprite { protected DemonLord lord() { return DemonLord.LUCIFER; } }
	public static class Mammon extends LordSprite { protected DemonLord lord() { return DemonLord.MAMMON; } }
	public static class Asmodeus extends LordSprite { protected DemonLord lord() { return DemonLord.ASMODEUS; } }
	public static class Leviathan extends LordSprite { protected DemonLord lord() { return DemonLord.LEVIATHAN; } }
	public static class Beelzebub extends LordSprite { protected DemonLord lord() { return DemonLord.BEELZEBUB; } }
	public static class Satan extends LordSprite { protected DemonLord lord() { return DemonLord.SATAN; } }
	public static class Belphegor extends LordSprite { protected DemonLord lord() { return DemonLord.BELPHEGOR; } }

	/** the altar (sprites/demon_altar.png, 48x49 frames, the owner's art): drawn over 3x3 tiles like the lords */
	public static class Altar extends MobSprite {
		public Altar() {
			super();
			texture("sprites/demon_altar.png");
			TextureFilm frames = new TextureFilm(texture, 48, 49);
			perspectiveRaise = -8 / 16f;
			shadowWidth = 0f;
			idle = new Animation(3, true);
			idle.frames(frames, 0, 1);
			run = idle.clone();
			attack = idle.clone();
			die = new Animation(1, false);
			die.frames(frames, 0);
			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF400010;
		}
	}

	public static CharSprite altarIcon() {
		CharSprite s = new Altar();
		s.scale.set(0.5f);
		return s;
	}

	/** the lords' flies (Beelzebub's seal and boss) */
	public static class Fly extends SwarmSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.BEELZEBUB); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}
}
