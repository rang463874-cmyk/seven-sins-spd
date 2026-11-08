package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;

/**
 * Placeholder art for the demon lords: an existing monster's sprite, hardlit in the lord's colour
 * (re-applied after every flash). One subclass per base sprite.
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

	public static class Lucifer extends StatueSprite {
		public Lucifer() { super(); setArmor(5); }
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.LUCIFER); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	public static class Mammon extends GolemSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.MAMMON); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	public static class Asmodeus extends SuccubusSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.ASMODEUS); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	public static class Leviathan extends ElementalSprite.Frost {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.LEVIATHAN); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	public static class Beelzebub extends ScorpioSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.BEELZEBUB); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	public static class Satan extends WarlockSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.SATAN); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	public static class Belphegor extends BruteSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.BELPHEGOR); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	/** the altar: a demon spawner, dark red */
	public static class Altar extends SpawnerSprite {
		@Override public void resetColor() { super.resetColor(); hardlight(0.75f, 0.2f, 0.35f); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}

	/** the lords' flies (Beelzebub's seal and boss) */
	public static class Fly extends SwarmSprite {
		@Override public void resetColor() { super.resetColor(); DemonSprites.lordTint(this, DemonLord.BEELZEBUB); }
		@Override public void link(com.shatteredpixel.shatteredpixeldungeon.actors.Char ch) { super.link(ch); resetColor(); }
	}
}
