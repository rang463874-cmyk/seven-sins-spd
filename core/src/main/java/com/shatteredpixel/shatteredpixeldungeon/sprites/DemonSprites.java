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

	/** one lord's row of sprites/demon_lords.png (tools/sinsart/make_demon_sprites.py), 24x25 frames, 12 per row */
	public static abstract class LordSprite extends MobSprite {

		public static final String TEXTURE = "sprites/demon_lords.png";

		protected abstract DemonLord lord();

		public LordSprite() {
			super();
			texture(TEXTURE);
			TextureFilm frames = new TextureFilm(texture, 24, 25);
			int o = lord().ordinal() * 12;

			idle = new Animation(3, true);
			idle.frames(frames, o, o + 1);

			run = new Animation(10, true);
			run.frames(frames, o + 2, o + 3, o + 4, o + 5);

			attack = new Animation(12, false);
			attack.frames(frames, o + 6, o + 7, o + 8);

			zap = attack.clone();

			die = new Animation(8, false);
			die.frames(frames, o + 9, o + 10, o + 11);

			play(idle);
		}

		@Override
		public int blood() {
			return 0xFF000000 | lord().color;
		}
	}

	public static class Lucifer extends LordSprite { protected DemonLord lord() { return DemonLord.LUCIFER; } }
	public static class Mammon extends LordSprite { protected DemonLord lord() { return DemonLord.MAMMON; } }
	public static class Asmodeus extends LordSprite { protected DemonLord lord() { return DemonLord.ASMODEUS; } }
	public static class Leviathan extends LordSprite { protected DemonLord lord() { return DemonLord.LEVIATHAN; } }
	public static class Beelzebub extends LordSprite { protected DemonLord lord() { return DemonLord.BEELZEBUB; } }
	public static class Satan extends LordSprite { protected DemonLord lord() { return DemonLord.SATAN; } }
	public static class Belphegor extends LordSprite { protected DemonLord lord() { return DemonLord.BELPHEGOR; } }

	/** the altar: sprites/demon_altar.png, 16x16, flickering braziers */
	public static class Altar extends MobSprite {
		public Altar() {
			super();
			texture("sprites/demon_altar.png");
			TextureFilm frames = new TextureFilm(texture, 16, 16);
			idle = new Animation(4, true);
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

	/** the lords' flies (Beelzebub's seal and boss) */
	public static class Fly extends SwarmSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.BEELZEBUB); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}
}
