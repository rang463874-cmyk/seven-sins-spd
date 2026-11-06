package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * Scythe of Death, the pale horseman. Charges as things die around the hero; its power reaps
 * every visible enemy near death. Once per floor it refuses to let the hero die.
 */
public class DeathScythe extends HorsemanArtifact {

	{
		image = ItemSpriteSheet.ARTIFACT_DEATH;
	}

	/** deepest floor where the scythe already spared the hero */
	public int sparedDepth = -1;

	@Override
	protected float rechargeTurns() {
		return 250f - 10f * level();
	}

	public float threshold() {
		return 0.15f + 0.02f * level();
	}

	public float bossCut() {
		return 0.05f + 0.01f * level();
	}

	public void onNearbyDeath() {
		gainCharge(5f + 0.5f * level());
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
		ArrayList<Char> reap = new ArrayList<>();
		ArrayList<Char> cut = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch.alignment != Char.Alignment.ENEMY || !ch.isAlive() || !Dungeon.level.heroFOV[ch.pos]) continue;
			if (ch.properties().contains(Char.Property.BOSS) || ch.properties().contains(Char.Property.MINIBOSS)) {
				cut.add(ch);
			} else if (ch.HP <= ch.HT * threshold()) {
				reap.add(ch);
			}
		}
		if (reap.isEmpty() && cut.isEmpty()) {
			GLog.w(Messages.get(this, "no_targets"));
			return false;
		}
		for (Char ch : reap) {
			ch.sprite.emitter().burst(Speck.factory(Speck.BONE), 4);
			ch.die(this);
		}
		for (Char ch : cut) {
			ch.damage(Math.max(1, Math.round(ch.HT * bossCut())), this);
		}
		Sample.INSTANCE.play(Assets.Sounds.CURSED);
		hero.sprite.operate(hero.pos);
		spendCharge(Math.max(1, reap.size()));
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	protected String statsDesc() {
		String s = Messages.get(this, "stats", Math.round(threshold() * 100), Math.round(bossCut() * 100));
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
}
