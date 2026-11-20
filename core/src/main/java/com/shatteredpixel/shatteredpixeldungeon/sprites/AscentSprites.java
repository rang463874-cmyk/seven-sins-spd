package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;

/**
 * Seven Sins: placeholder sprites for the ascent's bosses (Nov 20 2026) until the owner's pictures arrive: the demon lords'
 * and the archangels' pictures, tinted (the tint is put back after every flash) and, for the two chasers, drawn bigger.
 */
public class AscentSprites {

	private static void paint(CharSprite s, int rgb) {
		s.hardlight(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
	}

	public static class Moloch extends DemonSprites.LordSprite {
		protected DemonLord lord() { return DemonLord.SATAN; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xFF9030); }
	}

	public static class Lilith extends DemonSprites.LordSprite {
		protected DemonLord lord() { return DemonLord.ASMODEUS; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xB050FF); }
	}

	public static class Azazel extends DemonSprites.LordSprite {
		protected DemonLord lord() { return DemonLord.MAMMON; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xA0A8B8); }
	}

	public static class Abaddon extends DemonSprites.LordSprite {
		protected DemonLord lord() { return DemonLord.BEELZEBUB; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0x607040); }
	}

	public static class Behemoth extends DemonSprites.LordSprite {
		protected DemonLord lord() { return DemonLord.BELPHEGOR; }
		public Behemoth() { super(); scale.set(1.6f); }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xA07040); }
	}

	public static class Cherubim extends VirtueSprites.AngelSprite {
		protected Virtue virtue() { return Virtue.HUMILITY; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xFFA040); }
	}

	public static class Ophanim extends VirtueSprites.AngelSprite {
		protected Virtue virtue() { return Virtue.TEMPERANCE; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xFFE070); }
	}

	public static class Azrael extends VirtueSprites.AngelSprite {
		protected Virtue virtue() { return Virtue.DILIGENCE; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0x707080); }
	}

	public static class Sandalphon extends VirtueSprites.AngelSprite {
		protected Virtue virtue() { return Virtue.CHASTITY; }
		@Override public void resetColor() { super.resetColor(); paint(this, 0x80C8FF); }
	}

	public static class Metatron extends VirtueSprites.AngelSprite {
		protected Virtue virtue() { return Virtue.PATIENCE; }
		public Metatron() { super(); scale.set(1.6f); }
		@Override public void resetColor() { super.resetColor(); paint(this, 0xFFF4D0); }
	}
}
