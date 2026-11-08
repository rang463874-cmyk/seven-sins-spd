package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonHooks;
import com.shatteredpixel.shatteredpixeldungeon.demons.DemonLord;
import com.shatteredpixel.shatteredpixeldungeon.items.DemonSeal;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * A demon lord's boon, earned by enduring its trial up to Tengu. The passive itself lives in DemonHooks;
 * this buff shows it and keeps the demon seal's charge (+1 per turn, +2 once the lord is defeated).
 */
public class DemonPerk extends Buff {

	public static final float MAX_CHARGE = 100f;

	{
		type = buffType.POSITIVE;
		announced = false;
		revivePersists = true;
	}

	public float charge = MAX_CHARGE;

	public float rate() {
		return DemonHooks.stage == DemonHooks.DEFEATED ? 2f : 1f;
	}

	public boolean ready() {
		return charge >= MAX_CHARGE;
	}

	@Override
	public boolean act() {
		if (DemonHooks.stage < DemonHooks.GRANTED || DemonHooks.lord == null) {
			detach();
			return true;
		}
		if (charge < MAX_CHARGE) {
			boolean was = ready();
			charge = Math.min(MAX_CHARGE, charge + rate());
			if (!was && ready()) QuickSlotButton.refresh();
			if (Dungeon.hero != null && Dungeon.hero.belongings.getItem(DemonSeal.class) != null) {
				QuickSlotButton.refresh();
			}
		}
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.ASCEND;
	}

	@Override
	public void tintIcon(Image icon) {
		DemonLord l = DemonHooks.lord;
		int c = l == null ? 0xFFFFFF : l.color;
		icon.hardlight(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (MAX_CHARGE - charge) / MAX_CHARGE);
	}

	@Override
	public String iconTextDisplay() {
		return ready() ? "" : Integer.toString((int) charge);
	}

	@Override
	public String name() {
		DemonLord l = DemonHooks.lord;
		return l == null ? "" : Messages.get(this, "name", l.title());
	}

	@Override
	public String desc() {
		DemonLord l = DemonHooks.lord;
		if (l == null) return "";
		String d = Messages.get(this, "desc", l.title(), l.get("perk"), (int) charge);
		if (DemonHooks.stage == DemonHooks.DEFEATED) d += "\n\n" + Messages.get(this, "desc_defeated");
		return d;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("charge", charge);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		charge = bundle.getFloat("charge");
	}
}
