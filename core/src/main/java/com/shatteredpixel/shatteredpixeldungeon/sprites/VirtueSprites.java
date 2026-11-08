package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;

/**
 * Placeholder art for the virtues' champions until real sprites exist: paladins are armored statues,
 * saints are robed casters, hardlit in the virtue's colour (kept after flashes).
 */
public class VirtueSprites {

	static void lordTint(CharSprite s, Virtue v) {
		int c = v.color;
		s.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
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

	public static class Humility extends WarlockSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.HUMILITY); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
	public static class Charity extends WarlockSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.CHARITY); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
	public static class Chastity extends WarlockSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.CHASTITY); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
	public static class Kindness extends WarlockSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.KINDNESS); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
	public static class Patience extends StatueSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.PATIENCE); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
	public static class Temperance extends StatueSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.TEMPERANCE); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
	public static class Diligence extends StatueSprite {
		@Override public void resetColor() { super.resetColor(); lordTint(this, Virtue.DILIGENCE); }
		@Override public void link(Char ch) { super.link(ch); resetColor(); }
	}
}
