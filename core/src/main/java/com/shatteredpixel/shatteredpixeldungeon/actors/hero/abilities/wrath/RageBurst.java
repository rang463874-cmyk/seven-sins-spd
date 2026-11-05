package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.wrath;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.BlastParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.HashMap;

/** Every visible enraged enemy explodes with fury, hurting itself and everything around it. */
public class RageBurst extends ArmorAbility {

	{
		baseChargeUse = 50f;
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		ArrayList<Char> sources = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch != hero && ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos] && WrathGrudge.enraged(ch)) {
				sources.add(ch);
			}
		}
		if (sources.isEmpty()) {
			GLog.w(Messages.get(this, "no_targets"));
			return;
		}

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		float power = 1f + 0.25f * hero.pointsInTalent(Talent.BURST_POWER);
		int radius = 1 + hero.pointsInTalent(Talent.BURST_RADIUS) / 2;
		int chain = hero.pointsInTalent(Talent.BURST_CHAIN);

		//total damage per char, so overlapping blasts stack but each char is damaged once
		HashMap<Char, Integer> hits = new HashMap<>();
		for (Char src : sources) {
			int base = Math.round(Random.NormalIntRange(hero.lvl, 2 * hero.lvl + 4) * power);
			hits.merge(src, base, Integer::sum);
			PathFinder.buildDistanceMap(src.pos, com.watabou.utils.BArray.not(Dungeon.level.solid, null), radius);
			for (int i = 0; i < PathFinder.distance.length; i++) {
				if (PathFinder.distance[i] == Integer.MAX_VALUE || i == src.pos) continue;
				Char ch = Actor.findChar(i);
				if (ch != null && ch != hero && ch.alignment != Char.Alignment.ALLY) {
					hits.merge(ch, base / 2, Integer::sum);
				}
				if (Dungeon.level.heroFOV[i]) CellEmitter.center(i).burst(BlastParticle.FACTORY, 4);
			}
			CellEmitter.center(src.pos).burst(BlastParticle.FACTORY, 10);
		}
		for (HashMap.Entry<Char, Integer> e : hits.entrySet()) {
			Char ch = e.getKey();
			ch.damage(e.getValue(), this);
			if (ch.isAlive() && chain > 0 && !WrathGrudge.immune(ch)) {
				Buff.prolong(ch, Amok.class, 2f * chain);
			}
		}

		hero.sprite.operate(hero.pos);
		Sample.INSTANCE.play(Assets.Sounds.BLAST);
		Invisibility.dispel();
		hero.spendAndNext(1f);
	}

	@Override
	public int icon() {
		return HeroIcon.RAGE_BURST;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BURST_POWER, Talent.BURST_RADIUS, Talent.BURST_CHAIN, Talent.HEROIC_ENERGY};
	}
}
