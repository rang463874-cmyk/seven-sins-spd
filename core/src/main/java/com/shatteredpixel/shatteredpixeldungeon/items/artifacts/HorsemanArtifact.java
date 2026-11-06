package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * Shared logic for the four horsemen artifacts, rewards of the sewer prophet's quest.
 * Each one charges from 0 to 100%, spends a full charge on its active power,
 * and levels up (0..10) as that power is used. They are never cursed.
 */
public abstract class HorsemanArtifact extends Artifact {

	public static final String AC_USE = "USE";

	{
		levelCap = 10;
		charge = 0;
		chargeCap = 100;
		defaultAction = AC_USE;
		unique = true;
		bones = false;
	}

	/** turns for a full charge from nothing, at the current level */
	protected abstract float rechargeTurns();

	/** the active power; return true if it was used (a charge is then spent) */
	protected abstract boolean usePower(Hero hero);

	/** extra lines for the description while equipped */
	protected String statsDesc() {
		return null;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero) && !cursed && hero.buff(MagicImmune.class) == null) {
			actions.add(AC_USE);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_USE)) return Messages.get(this, "ac_use");
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (!action.equals(AC_USE)) return;
		if (hero.buff(MagicImmune.class) != null) return;
		if (!isEquipped(hero)) {
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
		} else if (cursed) {
			GLog.i(Messages.get(HorsemanArtifact.class, "cursed"));
		} else if (charge < chargeCap) {
			GLog.i(Messages.get(HorsemanArtifact.class, "no_charge"));
		} else {
			usePower(hero);
		}
	}

	/** call from usePower once the power actually went off */
	protected void spendCharge(int exp) {
		charge = 0;
		partialCharge = 0;
		gainExp(exp);
		Item.updateQuickslot();
	}

	public void gainCharge(float percent) {
		if (cursed || charge >= chargeCap || percent <= 0) return;
		partialCharge += percent;
		while (partialCharge >= 1f && charge < chargeCap) {
			partialCharge -= 1f;
			charge++;
		}
		if (charge >= chargeCap) {
			charge = chargeCap;
			partialCharge = 0;
			GLog.p(Messages.get(HorsemanArtifact.class, "full"));
		}
		Item.updateQuickslot();
	}

	public int expToLevel() {
		return 1 + level() / 3;
	}

	public void gainExp(int amount) {
		if (amount <= 0) return;
		exp += amount;
		while (level() < levelCap && exp >= expToLevel()) {
			exp -= expToLevel();
			upgrade();
			GLog.p(Messages.get(HorsemanArtifact.class, "levelup", name()));
		}
	}

	@Override
	public void charge(Hero target, float amount) {
		//scrolls of recharging and similar: 'amount' is in turns of recharge
		gainCharge(amount * 100f / rechargeTurns());
	}

	@Override
	public String desc() {
		String desc = Messages.get(this, "desc");
		Hero h = Dungeon.hero;
		if (h != null && isEquipped(h) && !cursed) {
			String stats = statsDesc();
			if (stats != null) desc += "\n\n" + stats;
		}
		return desc;
	}

	@Override
	public Item random() {
		//only ever given as a quest reward, never cursed
		cursed = false;
		return this;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	/** passive buff shared by all four: handles time-based charging */
	public class HorsemanBuff extends ArtifactBuff {

		@Override
		public boolean act() {
			if (!isCursed() && charge < chargeCap && target instanceof Hero
					&& target.buff(MagicImmune.class) == null) {
				gainCharge(100f / rechargeTurns() * RingOfEnergy.artifactChargeMultiplier(target));
			}
			tick();
			spend(TICK);
			return true;
		}

		/** per-turn hook for subclasses */
		protected void tick() {}

		public HorsemanArtifact artifact() {
			return HorsemanArtifact.this;
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new HorsemanBuff();
	}
}
