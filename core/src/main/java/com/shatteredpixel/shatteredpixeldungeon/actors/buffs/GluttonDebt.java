package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Gluttony's hunger debt: what heavy rumination ate beyond an empty stomach. Every point of satiety the
 * hero gains goes to the debt first (see Hunger.affectHunger). Until it is fully repaid the glutton is
 * weakened, vulnerable and hexed.
 */
public class GluttonDebt extends Buff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public float debt = 0;

	public void add(float amount) {
		debt += amount;
		BuffIndicator.refreshHero();
	}

	/** takes satiety the hero would gain; returns what is left over */
	public float repay(float energy) {
		float pay = Math.min(energy, debt);
		debt -= pay;
		if (debt <= 0) {
			debt = 0;
			GLog.p(Messages.get(this, "repaid"));
			if (target.sprite != null) target.sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "repaid_status"));
			detach();
		} else {
			BuffIndicator.refreshHero();
		}
		return energy - pay;
	}

	@Override
	public boolean act() {
		if (debt <= 0) {
			detach();
			return true;
		}
		//the empty stomach gnaws at everything
		Buff.prolong(target, Weakness.class, 2f);
		Buff.prolong(target, Vulnerable.class, 2f);
		Buff.prolong(target, Hex.class, 2f);
		spend(TICK);
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.HUNGER;
	}

	@Override
	public void tintIcon(Image icon) {
		icon.hardlight(0.8f, 0.1f, 0.1f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(Math.round(debt));
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round(debt));
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("debt", debt);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		debt = bundle.getFloat("debt");
	}
}
