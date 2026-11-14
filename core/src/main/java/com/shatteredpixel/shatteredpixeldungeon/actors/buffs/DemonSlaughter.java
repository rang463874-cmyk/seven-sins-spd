package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Seven Sins: the Lesser Key's "slaughter combo" (50 demons). Every enemy that dies while it lasts adds a
 * stack (max 3) and renews it; each stack is +20% movement speed and +15% evasion (GoetiaHooks).
 */
public class DemonSlaughter extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	public static final float DURATION = 6f;
	public static final int MAX = 3;
	public static final float SPEED = 0.20f;
	public static final float EVASION = 0.15f;

	public int stacks = 0;

	public void kill() {
		stacks = Math.min(MAX, stacks + 1);
	}

	@Override
	public int icon() {
		return BuffIndicator.HASTE;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(1f, 0.3f, 0.3f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", stacks, Math.round(stacks * SPEED * 100), Math.round(stacks * EVASION * 100), dispTurns());
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("stacks", stacks);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt("stacks");
	}
}
