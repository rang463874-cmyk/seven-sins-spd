package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/** Seven Sins: while this lasts the Lesser Key's "safety net" (60 demons) can't save the hero again. */
public class SafetyNetCooldown extends FlavourBuff {

	public static final float DURATION = 300f;

	@Override
	public int icon() {
		return BuffIndicator.TIME;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.7f, 0.4f, 1f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, visualcooldown() / DURATION);
	}
}
