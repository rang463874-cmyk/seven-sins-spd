package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sins.Ultimate;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * Seven Sins: which ultimates a sin hero has learned and their cooldowns (invisible, kept through death).
 * Effect = a timed ultimate on the hero, Mark = one on an enemy; both are appended, so several can coexist.
 */
public class UltimateBook extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	public Ultimate first;
	public Ultimate second;
	public float cd1 = 0;
	public float cd2 = 0;
	/** the "you can learn an ultimate" notices were shown */
	public boolean told1 = false;
	public boolean told2 = false;

	public Ultimate learned(int tier) {
		return tier == 1 ? first : second;
	}

	public float cooldown(Ultimate u) {
		return u.tier == 1 ? cd1 : cd2;
	}

	public void startCooldown(Ultimate u) {
		if (u.tier == 1) cd1 = u.cooldown;
		else cd2 = u.cooldown;
	}

	@Override
	public boolean act() {
		if (cd1 > 0) cd1 = Math.max(0, cd1 - TICK);
		if (cd2 > 0) cd2 = Math.max(0, cd2 - TICK);
		spend(TICK);
		return true;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (first != null) bundle.put("first", first.name());
		if (second != null) bundle.put("second", second.name());
		bundle.put("cd1", cd1);
		bundle.put("cd2", cd2);
		bundle.put("told1", told1);
		bundle.put("told2", told2);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		first = bundle.contains("first") ? Ultimate.byName(bundle.getString("first")) : null;
		second = bundle.contains("second") ? Ultimate.byName(bundle.getString("second")) : null;
		cd1 = bundle.getFloat("cd1");
		cd2 = bundle.getFloat("cd2");
		told1 = bundle.getBoolean("told1");
		told2 = bundle.getBoolean("told2");
	}

	/** a timed ultimate on the hero */
	public static class Effect extends FlavourBuff {

		{
			type = buffType.POSITIVE;
		}

		public Ultimate kind;
		/** charges, for ultimates that count (Inviolable's blocked hits) */
		public int count = 0;
		public float total = 1;

		public Effect set(Ultimate kind, float duration, int count) {
			this.kind = kind;
			this.total = duration;
			this.count = count;
			return this;
		}

		@Override
		public int icon() {
			return BuffIndicator.UPGRADE;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(1f, 0.8f, 0.2f);
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (total - visualcooldown()) / total);
		}

		@Override
		public String name() {
			return kind == null ? "" : kind.title();
		}

		@Override
		public String desc() {
			String s = Messages.get(UltimateBook.class, "effect_desc", kind == null ? "" : Messages.get(Ultimate.class, kind.id() + "_buff"), dispTurns());
			if (kind == Ultimate.AEGIS) s += "\n\n" + Messages.get(UltimateBook.class, "effect_count", count);
			return s;
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			if (kind != null) bundle.put("kind", kind.name());
			bundle.put("count", count);
			bundle.put("total", total);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			kind = Ultimate.byName(bundle.getString("kind"));
			count = bundle.getInt("count");
			total = Math.max(1, bundle.getFloat("total"));
		}
	}

	/** an ultimate's mark on an enemy (Bounty, Vendetta) */
	public static class Mark extends FlavourBuff {

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		public Ultimate kind;

		public Mark set(Ultimate kind) {
			this.kind = kind;
			return this;
		}

		@Override
		public int icon() {
			return BuffIndicator.MARK;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(kind == Ultimate.BOUNTY ? 1f : 0.9f, kind == Ultimate.BOUNTY ? 0.8f : 0.1f, 0.1f);
		}

		@Override
		public String name() {
			return kind == null ? "" : kind.title();
		}

		@Override
		public String desc() {
			return Messages.get(UltimateBook.class, "effect_desc", kind == null ? "" : Messages.get(Ultimate.class, kind.id() + "_buff"), dispTurns());
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			if (kind != null) bundle.put("kind", kind.name());
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			kind = Ultimate.byName(bundle.getString("kind"));
		}
	}
}
