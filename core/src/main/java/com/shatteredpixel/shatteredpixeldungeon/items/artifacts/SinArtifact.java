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
		return 0;
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
