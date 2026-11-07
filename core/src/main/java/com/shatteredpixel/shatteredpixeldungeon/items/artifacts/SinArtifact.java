package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.skills.SkillHolder;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * Base for the seven sins' signature items, which are worn in an artifact/ring slot.
 * The sin's growth buff and stats work regardless; the item's skills (and the skill bar)
 * only work while it is equipped. Never cursed, can't be upgraded, transmuted or dropped.
 */
public abstract class SinArtifact extends Artifact implements SkillHolder {

	{
		unique = true;
		bones = false;
		keptThoughLostInvent = true;
		levelKnown = true;
		cursedKnown = true;
		levelCap = 10;
	}

	// ------------------------------------------------------------------ leveling (like the cloak of shadows)

	/** fractional exp carried between uses */
	private float partialExp = 0;

	/** resource spent until the next level: grows by one each level (75 in total to reach +10) */
	public int expToLevel() {
		return 3 + level();
	}

	public void gainExp(float amount) {
		if (amount <= 0 || level() >= levelCap) return;
		partialExp += amount;
		while (partialExp >= 1f && level() < levelCap) {
			partialExp -= 1f;
			exp++;
			if (exp >= expToLevel()) {
				exp = 0;
				upgrade();
				GLog.p(Messages.get(SinArtifact.class, "levelup", name(), level()));
			}
		}
		if (level() >= levelCap) partialExp = 0;
		updateQuickslot();
	}

	/** the hero's signature artifact (worn or carried), or null */
	public static SinArtifact of(Hero hero) {
		if (hero == null || hero.belongings == null) return null;
		for (Item i : hero.belongings) {
			if (i instanceof SinArtifact) return (SinArtifact) i;
		}
		return null;
	}

	public static int level(Hero hero) {
		SinArtifact a = of(hero);
		return a == null ? 0 : a.level();
	}

	/** call whenever a skill actually spends its resource; only a worn artifact learns from it */
	public static void onSpent(Hero hero, float exp) {
		SinArtifact a = of(hero);
		if (a != null && a.isEquipped(hero)) a.gainExp(exp);
	}

	/** +1 max charge at +3, +6 and +9 */
	public static int bonusCharges(Hero hero) {
		return level(hero) / 3;
	}

	/** multiply turns-per-charge by this: up to 60% faster recharge at +10 */
	public static float rechargeScale(Hero hero) {
		return 1f / (1f + 0.06f * level(hero));
	}

	/** Lust: HP costs, up to 30% cheaper at +10 */
	public static float costScale(Hero hero) {
		return 1f - 0.03f * level(hero);
	}

	/** Greed: payment limit size and how much it fills from gold, up to +60% at +10 */
	public static float gaugeScale(Hero hero) {
		return 1f + 0.06f * level(hero);
	}

	/** Gluttony: satiety costs, down to -33% at +10 */
	public static float satietyScale(Hero hero) {
		return 1f - level(hero) / 30f;
	}

	/** Gluttony: consumable points, up to +50% at +10 */
	public static float devourScale(Hero hero) {
		return 1f + 0.05f * level(hero);
	}

	/** what the current level does for this item; charge-based sins by default */
	protected String levelEffects(Hero hero) {
		return Messages.get(SinArtifact.class, "effects_charges", bonusCharges(hero),
				Math.round((1f / rechargeScale(hero) - 1f) * 100));
	}

	@Override
	public String info() {
		String s = super.info();
		Hero hero = com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero;
		if (hero == null) return s;
		s += "\n\n" + (level() >= levelCap
				? Messages.get(SinArtifact.class, "level_max", level(), levelCap)
				: Messages.get(SinArtifact.class, "level_line", level(), levelCap, Math.max(0, expToLevel() - exp)));
		if (of(hero) == this) s += "\n" + levelEffects(hero);
		return s;
	}

	@Override
	public void storeInBundle(com.watabou.utils.Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("partial_exp", partialExp);
	}

	@Override
	public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		super.restoreFromBundle(bundle);
		partialExp = bundle.getFloat("partial_exp");
	}

	/** call at the start of execute(): false (and a message) if a skill is used while not worn */
	protected boolean canUseSkills(Hero hero, String action) {
		if (action.equals(AC_EQUIP) || action.equals(AC_UNEQUIP)
				|| action.equals(AC_DROP) || action.equals(AC_THROW)) {
			return false;
		}
		if (!isEquipped(hero)) {
			GLog.w(Messages.get(SinArtifact.class, "need_equip", name()));
			return false;
		}
		return true;
	}

	@Override
	public Item random() {
		cursed = false;
		return this;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int visiblyUpgraded() {
		return level();
	}

	@Override
	public int buffedVisiblyUpgraded() {
		return level();
	}

	@Override
	public int value() {
		return 0;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		//nothing passive: the sin's own buff carries the effects
		return new Bond();
	}

	public class Bond extends ArtifactBuff {
	}
}
