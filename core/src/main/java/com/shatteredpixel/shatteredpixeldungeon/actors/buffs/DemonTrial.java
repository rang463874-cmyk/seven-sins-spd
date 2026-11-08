package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * A demon lord's trial, taken at the altar after Goo. Only a marker for the player: the penalty itself is
 * applied by DemonHooks while the pact is in the TRIAL stage, and lasts until Tengu falls.
 */
public class DemonTrial extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = false;
		revivePersists = true;
	}

	@Override
	public boolean act() {
		if (DemonHooks.stage != DemonHooks.TRIAL || DemonHooks.lord == null) {
			detach();
			return true;
		}
		spend(TICK * 10);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public void tintIcon(Image icon) {
		DemonLord l = DemonHooks.lord;
		int c = l == null ? 0xFF0000 : l.color;
		icon.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
	}

	@Override
	public String name() {
		DemonLord l = DemonHooks.lord;
		return l == null ? Messages.get(this, "name_none") : Messages.get(this, "name", l.title());
	}

	@Override
	public String desc() {
		DemonLord l = DemonHooks.lord;
		if (l == null) return "";
		return Messages.get(this, "desc", l.title(), l.get("trial"));
	}
}
