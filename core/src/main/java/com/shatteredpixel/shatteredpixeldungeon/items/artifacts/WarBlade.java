package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * Blade of War, the red horseman. Hits harder the more wounded the hero is, charges from
 * damage taken, and its power drives every nearby enemy into a frenzy against each other.
 */
public class WarBlade extends HorsemanArtifact {

	public static final int RANGE = 6;

	{
		image = ItemSpriteSheet.ARTIFACT_WAR;
	}

	@Override
	protected float rechargeTurns() {
		return 300f - 10f * level();
	}

	/** bonus damage at 0 HP; scales linearly with missing HP */
	public float maxBonus() {
		return 0.20f + 0.03f * level();
	}

	public float frenzyTurns() {
		return 4f + level() / 2f;
	}

	/** charge from a hit taken, as a percent */
	public void onHurt(Hero hero, int dmg) {
		if (dmg <= 0 || hero.HT <= 0) return;
		gainCharge(dmg * 100f / hero.HT * (1f + 0.05f * level()));
	}

	public int modifyAttack(Hero hero, int dmg) {
		float missing = 1f - hero.HP / (float) Math.max(1, hero.HT);
		return Math.round(dmg * (1f + maxBonus() * Math.max(0, missing)));
	}

	@Override
	protected boolean usePower(Hero hero) {
		int n = 0;
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive()
					&& Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= RANGE
					&& !ch.properties().contains(Char.Property.BOSS)) {
				Buff.prolong(ch, Amok.class, frenzyTurns());
				ch.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 3);
				n++;
			}
		}
		if (n == 0) {
			GLog.w(Messages.get(this, "no_targets"));
			return false;
		}
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
		hero.sprite.operate(hero.pos);
		spendCharge(1);
		hero.spendAndNext(1f);
		return true;
	}

	@Override
	protected String statsDesc() {
		return Messages.get(this, "stats", Math.round(maxBonus() * 100), Math.round(frenzyTurns()));
	}
}
