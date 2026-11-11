package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaFiend;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.goetia.GoetiaLord;

/** Seven Sins: placeholder art for Solomon's demons, the ripper demon tinted with the demon's colour (lords are larger). */
public class GoetiaSprite extends RipperSprite {

	@Override
	public void link(Char ch) {
		super.link(ch);
		int color = 0xFFFFFF;
		if (ch instanceof GoetiaFiend) color = ((GoetiaFiend) ch).demon.color;
		if (ch instanceof GoetiaLord) {
			color = ((GoetiaLord) ch).demon.color;
			scale.set(1.6f);
		}
		tint(color, 0.45f);
	}

	@Override
	public void resetColor() {
		super.resetColor();
		if (ch instanceof GoetiaFiend) tint(((GoetiaFiend) ch).demon.color, 0.45f);
		else if (ch instanceof GoetiaLord) tint(((GoetiaLord) ch).demon.color, 0.45f);
	}

	@Override
	public int blood() {
		return 0xFF600010;
	}
}
