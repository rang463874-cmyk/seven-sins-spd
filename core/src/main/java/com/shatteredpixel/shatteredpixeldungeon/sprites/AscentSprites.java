package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.watabou.noosa.TextureFilm;

/**
 * Seven Sins: the ascent's bosses, drawn from the owner's pictures (Nov 20 2026; sheet by tools/sinsart/make_ascent_sprites.py).
 */
public class AscentSprites {

	/** a boss drawn from the owner's picture: one row of sprites/ascent_bosses.png (tools/sinsart/make_ascent_sprites.py,
	 *  96x80 frames, rows in its ORDER: moloch 0, lilith 1, azazel 2, abaddon 3, cherubim 4, ophanim 5, azrael 6,
	 *  sandalphon 7, behemoth 8, metatron 9) */
	public static abstract class ArtSprite extends MobSprite {

		public static final String TEXTURE = "sprites/ascent_bosses.png";

		protected abstract int row();

		protected int blood = 0xFF800000;

		public ArtSprite() {
			super();
			texture(TEXTURE);
			TextureFilm frames = new TextureFilm(texture, 96, 80);
			int o = row() * 12;

			perspectiveRaise = -8 / 16f;
			shadowWidth = 0.9f;
			shadowHeight = 0.14f;
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
			return blood;
		}
	}

	//the owner's pictures (Nov 20 2026)
	public static class Moloch extends ArtSprite {
		protected int row() { return 0; }
		{ blood = 0xFFFF7020; }
	}

	public static class Lilith extends ArtSprite {
		protected int row() { return 1; }
		{ blood = 0xFF8030C0; }
	}

	public static class Azazel extends ArtSprite {
		protected int row() { return 2; }
		{ blood = 0xFF404048; }
	}

	public static class Abaddon extends ArtSprite {
		protected int row() { return 3; }
		{ blood = 0xFF507030; }
	}

	/** drawn side-on and wide, so it is scaled up to tower over the sewers */
	public static class Behemoth extends ArtSprite {
		protected int row() { return 8; }
		{ blood = 0xFF806040; }
		public Behemoth() { super(); scale.set(1.25f); }
	}

	public static class Cherubim extends ArtSprite {
		protected int row() { return 4; }
		{ blood = 0xFFFFA040; }
	}

	public static class Ophanim extends ArtSprite {
		protected int row() { return 5; }
		{ blood = 0xFFFFE070; }
	}

	public static class Azrael extends ArtSprite {
		protected int row() { return 6; }
		{ blood = 0xFF505058; }
	}

	public static class Sandalphon extends ArtSprite {
		protected int row() { return 7; }
		{ blood = 0xFF80C8FF; }
	}

	/** the voice of God, as tall as heaven: scaled up like Behemoth */
	public static class Metatron extends ArtSprite {
		protected int row() { return 9; }
		{ blood = 0xFFFFF4D0; }
		public Metatron() { super(); scale.set(1.25f); }
	}
}
