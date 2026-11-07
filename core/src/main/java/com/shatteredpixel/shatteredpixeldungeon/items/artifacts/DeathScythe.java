package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Scythe of Death, the pale horseman. Every hit the hero lands leaves death marks on the enemy.
 * Once an enemy's marks reach its remaining HP it is reaped on the spot; bosses and minibosses
 * can't be reaped, but when their marks reach a share of their max HP the marks burst for that
 * much damage. Its power makes every marked enemy in sight burst at once. Charges as things die
 * nearby. Once per floor it refuses to let the hero die.
 */
public class DeathScythe extends HorsemanArtifact {

	{
		image = ItemSpriteSheet.ARTIFACT_DEATH;
	}

	/** deepest floor where the scythe already spared the hero */
	public int sparedDepth = -1;

	@Override
	protected float rechargeTurns() {
		return 200f - 8f * level();
	}

	/** marks left per point of damage dealt */
	public float markRate() {
		return 0.25f + 0.025f * level();
	}

	/** share of a boss's max HP its marks need to burst */
	public static final float BOSS_BURST = 0.15f;

	/** burst damage multiplier for the scythe's power */
	public float harvestMult() {
		return 1.5f + 0.1f * level();
	}

	public void onNearbyDeath() {
		gainCharge(5f + 0.5f * level());
	}

	public static boolean isBoss(Char ch) {
		return ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS);
	}

	/** called from the hero's attackProc */
	public void mark(Char enemy, int dmg) {
		if (enemy == null || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY || dmg <= 0) return;
		Mark m = Buff.affect(enemy, Mark.class);
		m.marks += Math.max(1, Math.round(dmg * markRate()));
	}

	/**
	 * Called from Char.damage right after HP is lowered. Reaps a marked enemy or bursts a
	 * marked boss. Returns the extra damage dealt (HP is already lowered by it).
	 */
	public static int reap(Char ch) {
		if (ch.HP <= 0) return 0;
		Mark m = ch.buff(Mark.class);
		if (m == null || m.marks <= 0) return 0;
		if (isBoss(ch)) {
			if (m.marks < ch.HT * BOSS_BURST) return 0;
			int extra = Math.min(ch.HP, m.marks);
			m.marks = 0;
			burstFx(ch);
			ch.HP -= extra;
			return extra;
		}
		if (m.marks < ch.HP) return 0;
		int extra = ch.HP;
		burstFx(ch);
		if (ch.sprite != null) ch.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(DeathScythe.class, "reaped"));
		ch.HP = 0;
		return extra;
	}

	private static void burstFx(Char ch) {
		if (ch.sprite != null) {
			ch.sprite.emitter().burst(ShadowParticle.UP, 8);
			ch.sprite.emitter().burst(Speck.factory(Speck.BONE), 3);
		}
		Sample.INSTANCE.play(Assets.Sounds.CURSED, 0.7f);
	}

	public boolean canSpare() {
		return !cursed && sparedDepth != Dungeon.depth;
	}

	/** called when the hero would die; returns true if the hero was spared */
	public boolean spare(Hero hero) {
		if (!canSpare()) return false;
		sparedDepth = Dungeon.depth;
		hero.HP = 1;
		Buff.prolong(hero, Invulnerability.class, 1f);
		hero.sprite.emitter().burst(Speck.factory(Speck.BONE), 6);
		GameScene.flash(0x80A0C0A0);
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		GLog.p(Messages.get(this, "spared"));
		return true;
	}

	@Override
	protected boolean usePower(Hero hero) {
		ArrayList<Char> marked = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch.alignment != Char.Alignment.ENEMY || !ch.isAlive() || !Dungeon.level.heroFOV[ch.pos]) continue;
			Mark m = ch.buff(Mark.class);
			if (m != null && m.marks > 0) marked.add(ch);
		}
		if (marked.isEmpty()) {
			GLog.w(Messages.get(this, "no_targets"));
			return false;
		}
		int reaped = 0;
		for (Char ch : marked) {
			Mark m = ch.buff(Mark.class);
			int dmg = Math.round(m.marks * harvestMult());
			m.marks = 0;
			burstFx(ch);
			ch.damage(dmg, this);
			if (!ch.isAlive()) reaped++;
		}
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		hero.sprite.operate(hero.pos);
		spendCharge(Math.max(1, reaped));
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	protected String statsDesc() {
		String s = Messages.get(this, "stats", Math.round(markRate() * 100), Math.round(BOSS_BURST * 100),
				Math.round(harvestMult() * 100));
		s += "\n\n" + Messages.get(this, canSpare() ? "spare_ready" : "spare_used");
		return s;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("spared_depth", sparedDepth);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		sparedDepth = bundle.contains("spared_depth") ? bundle.getInt("spared_depth") : -1;
	}

	/** death marks on an enemy */
	public static class Mark extends Buff {

		public int marks = 0;

		{
			type = buffType.NEGATIVE;
			announced = false;
		}

		@Override
		public int icon() {
			return BuffIndicator.CORRUPT;
		}

		@Override
		public void tintIcon(Image icon) {
			icon.hardlight(0.6f, 0.8f, 0.6f);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(marks);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", marks);
		}

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put("marks", marks);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			marks = bundle.getInt("marks");
		}
	}
}
