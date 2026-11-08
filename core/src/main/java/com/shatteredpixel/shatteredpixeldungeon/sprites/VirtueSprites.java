package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.watabou.noosa.TextureFilm;

/**
 * The virtues' champions use the owner's art (sprites/virtue_champions.png from
 * tools/sinsart/make_virtue_sprites.py): row 0 = paladin (patience, temperance, diligence),
 * row 1 = saint (humility, charity, chastity, kindness). 48x49 frames drawn over the 3x3 tiles
 * around the cell, like the demon lords.
 * The archangels (final boss) have their own art too: sprites/virtue_angels.png, 64x65 frames, one row
 * each in Virtue order, via {@link #forAngel}.
 */
public class VirtueSprites {

	public static boolean isPaladin(Virtue v) {
		return v == Virtue.PATIENCE || v == Virtue.TEMPERANCE || v == Virtue.DILIGENCE;
	}

	public static CharSprite forVirtue(Virtue v) {
		switch (v) {
			case HUMILITY:   return new Humility();
			case CHARITY:    return new Charity();
			case CHASTITY:   return new Chastity();
			case PATIENCE:   return new Patience();
			case TEMPERANCE: return new Temperance();
			case KINDNESS:   return new Kindness();
			default:         return new Diligence();
		}
	}

	/** one champion's row of the sheet: idle (2), run (4), attack (3), die (3) */
	public static abstract class ChampionSprite extends MobSprite {

		public static final String TEXTURE = "sprites/virtue_champions.png";

		protected abstract Virtue virtue();

		public ChampionSprite() {
			super();
			texture(TEXTURE);
			TextureFilm frames = new TextureFilm(texture, 48, 49);
			int o = (isPaladin(virtue()) ? 0 : 1) * 12;

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
			return 0xFF000000 | virtue().color;
		}
	}

	public static class Humility extends ChampionSprite { protected Virtue virtue() { return Virtue.HUMILITY; } }
	public static class Charity extends ChampionSprite { protected Virtue virtue() { return Virtue.CHARITY; } }
	public static class Chastity extends ChampionSprite { protected Virtue virtue() { return Virtue.CHASTITY; } }
	public static class Kindness extends ChampionSprite { protected Virtue virtue() { return Virtue.KINDNESS; } }
	public static class Patience extends ChampionSprite { protected Virtue virtue() { return Virtue.PATIENCE; } }
	public static class Temperance extends ChampionSprite { protected Virtue virtue() { return Virtue.TEMPERANCE; } }
	public static class Diligence extends ChampionSprite { protected Virtue virtue() { return Virtue.DILIGENCE; } }

	// ------------------------------------------------ the archangels (final boss)

	public static CharSprite forAngel(Virtue v) {
		switch (v) {
			case HUMILITY:   return new Michael();
			case CHARITY:    return new Raguel();
			case CHASTITY:   return new Gabriel();
			case PATIENCE:   return new Uriel();
			case TEMPERANCE: return new Raphael();
			case KINDNESS:   return new Jeremiel();
			default:         return new Sariel();
		}
	}

	/** one archangel's row of sprites/virtue_angels.png (the owner's art): 64x65 frames, rows in Virtue order */
	public static abstract class AngelSprite extends MobSprite {

		public static final String TEXTURE = "sprites/virtue_angels.png";

		protected abstract Virtue virtue();

		public AngelSprite() {
			super();
			texture(TEXTURE);
			TextureFilm frames = new TextureFilm(texture, 64, 65);
			int o = virtue().ordinal() * 12;

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
			return 0xFFFFF0C0;
		}
	}

	public static class Michael extends AngelSprite { protected Virtue virtue() { return Virtue.HUMILITY; } }
	public static class Raguel extends AngelSprite { protected Virtue virtue() { return Virtue.CHARITY; } }
	public static class Gabriel extends AngelSprite { protected Virtue virtue() { return Virtue.CHASTITY; } }
	public static class Uriel extends AngelSprite { protected Virtue virtue() { return Virtue.PATIENCE; } }
	public static class Raphael extends AngelSprite { protected Virtue virtue() { return Virtue.TEMPERANCE; } }
	public static class Jeremiel extends AngelSprite { protected Virtue virtue() { return Virtue.KINDNESS; } }
	public static class Sariel extends AngelSprite { protected Virtue virtue() { return Virtue.DILIGENCE; } }
}
