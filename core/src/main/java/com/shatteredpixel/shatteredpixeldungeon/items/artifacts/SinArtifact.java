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

	/** the level the mirror form (ascension) comes at */
	public static final int ASCEND_LEVEL = 10;
	/** the cap after the trial of sin (Nov 18 2026, owner) */
	public static final int AWAKENED_CAP = 20;

	/**
	 * Seven Sins: the trial of sin's second awakening (sins/TrialHooks). Raises the level cap to 20 and adds the
	 * class's trial skill (SkillHolder.visibleSkills). Carried over by ascend through the bundle.
	 */
	public boolean awakened = false;

	public void awaken() {
		awakened = true;
		levelCap = AWAKENED_CAP;
		updateQuickslot();
	}

	@Override
	public String name() {
		String n = super.name();
		return awakened ? Messages.get(SinArtifact.class, "awakened_name", n) : n;
	}

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
		if (level() >= ASCEND_LEVEL) ascend(com.shatteredpixel.shatteredpixeldungeon.Dungeon.hero);
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
		com.shatteredpixel.shatteredpixeldungeon.sins.MetaTalents.onSkillSpent(hero);
		SinArtifact a = of(hero);
		if (a != null && a.isEquipped(hero)) {
			a.gainExp(exp);
			//artifacts that reached +10 before this rule came in ascend on their next use
			if (a.level() >= ASCEND_LEVEL) a.ascend(hero);
		}
	}

	/** true for the vault mirror's reflections (the +10 form) */
	public boolean isMirrorForm() {
		return getClass().getEnclosingClass() == com.shatteredpixel.shatteredpixeldungeon.items.MirrorRelics.class;
	}

	/**
	 * Seven Sins (Nov 2026, owner's call): at +10 the signature item turns into its old vault-mirror
	 * reflection (golden maw, black card, ...) and switches the buff's mirrored bonuses on. Level, exp
	 * and charge carry over through the bundle; the new item takes the old one's slot and quickslot.
	 */
	public void ascend(Hero hero) {
		if (hero == null || isMirrorForm()) return;
		Item r = com.shatteredpixel.shatteredpixeldungeon.items.MirrorRelics.rewardFor(hero.heroClass);
		if (!(r instanceof SinArtifact) || !getClass().isInstance(r)) return;
		SinArtifact relic = (SinArtifact) r;
		com.watabou.utils.Bundle b = new com.watabou.utils.Bundle();
		storeInBundle(b);
		relic.restoreFromBundle(b);
		relic.cursed = false;

		com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings bel = hero.belongings;
		boolean worn = false;
		if (bel.artifact == this) { bel.artifact = relic; worn = true; }
		else if (bel.misc == this) { bel.misc = relic; worn = true; }
		else if (bel.misc2 == this) { bel.misc2 = relic; worn = true; }
		else if (bel.misc3 == this) { bel.misc3 = relic; worn = true; }
		else {
			int idx = bel.backpack.items.indexOf(this);
			if (idx < 0) return;
			bel.backpack.items.set(idx, relic);
		}
		if (worn) {
			if (passiveBuff != null) {
				if (passiveBuff.target != null) passiveBuff.detach();
				passiveBuff = null;
			}
			relic.activate(hero);
		}
		int slot = com.shatteredpixel.shatteredpixeldungeon.Dungeon.quickslot.getSlot(this);
		if (slot != -1) com.shatteredpixel.shatteredpixeldungeon.Dungeon.quickslot.setSlot(slot, relic);
		com.shatteredpixel.shatteredpixeldungeon.items.MirrorRelics.markMirrored(hero);
		relic.identify();

		GLog.p(Messages.get(SinArtifact.class, "ascended", name(), relic.name()));
		if (hero.sprite != null) {
			hero.sprite.emitter().burst(com.shatteredpixel.shatteredpixeldungeon.effects.Speck.factory(
					com.shatteredpixel.shatteredpixeldungeon.effects.Speck.LIGHT), 12);
		}
		com.watabou.noosa.audio.Sample.INSTANCE.play(com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.LEVELUP);
		hero.updateHT(false);
		Item.updateQuickslot();
		com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene.syncSkillBarPos();
	}

	/** +1 max charge at +3, +6 and +9 */
	public static int bonusCharges(Hero hero) {
		return level(hero) / 3;
	}

	/** the level the scaling effects use: levels past +10 (after the trial) count half */
	public static float effLevel(Hero hero) {
		int l = level(hero);
		return l <= ASCEND_LEVEL ? l : ASCEND_LEVEL + (l - ASCEND_LEVEL) * 0.5f;
	}

	/** multiply turns-per-charge by this: up to 60% faster recharge at +10, 90% at +20 */
	public static float rechargeScale(Hero hero) {
		return 1f / (1f + 0.06f * effLevel(hero));
	}

	/** Lust: HP costs, up to 30% cheaper at +10, 45% at +20 */
	public static float costScale(Hero hero) {
		return 1f - 0.03f * effLevel(hero);
	}

	/** Greed: payment limit size and how much it fills from gold, up to +60% at +10, +90% at +20 */
	public static float gaugeScale(Hero hero) {
		return 1f + 0.06f * effLevel(hero);
	}

	/** Gluttony: satiety costs, down to -33% at +10, -50% at +20 */
	public static float satietyScale(Hero hero) {
		return 1f - effLevel(hero) / 30f;
	}

	/** Gluttony: consumable points, up to +50% at +10, +75% at +20 */
	public static float devourScale(Hero hero) {
		return 1f + 0.05f * effLevel(hero);
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
		if (awakened) s += "\n\n" + Messages.get(SinArtifact.class, "awakened_info");
		else if (isMirrorForm()) s += "\n\n" + Messages.get(SinArtifact.class, "trial_hint");
		return s;
	}

	@Override
	public void storeInBundle(com.watabou.utils.Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("partial_exp", partialExp);
		bundle.put("trial_awakened", awakened);
	}

	@Override
	public void restoreFromBundle(com.watabou.utils.Bundle bundle) {
		super.restoreFromBundle(bundle);
		partialExp = bundle.getFloat("partial_exp");
		awakened = bundle.getBoolean("trial_awakened");
		if (awakened) levelCap = AWAKENED_CAP;
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
