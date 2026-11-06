package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.lust;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/** Dances from one visible enemy to the next, striking each in turn. */
public class DanseMacabre extends ArmorAbility {

	public static final int RANGE = 6;

	{
		baseChargeUse = 50f;
	}

	public static int targets(Hero hero) {
		return 3 + hero.pointsInTalent(Talent.MACABRE_COUNT);
	}

	@Override
	public void activate(ClassArmor armor, Hero hero, Integer target) {
		final int start = hero.pos;
		ArrayList<Char> foes = new ArrayList<>();
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(start, ch.pos) <= RANGE && ch.invisible <= 0) {
				foes.add(ch);
			}
		}
		if (foes.isEmpty()) {
			com.shatteredpixel.shatteredpixeldungeon.utils.GLog.w(
					com.shatteredpixel.shatteredpixeldungeon.messages.Messages.get(this, "no_targets"));
			return;
		}
		Collections.sort(foes, new Comparator<Char>() {
			@Override
			public int compare(Char a, Char b) {
				return Dungeon.level.distance(start, a.pos) - Dungeon.level.distance(start, b.pos);
			}
		});

		armor.charge -= chargeUse(hero);
		Item.updateQuickslot();

		float mult = 1f + 0.15f * hero.pointsInTalent(Talent.MACABRE_POWER);
		int charm = hero.pointsInTalent(Talent.MACABRE_CHARM);
		int count = Math.min(targets(hero), foes.size());
		for (int i = 0; i < count; i++) {
			Char foe = foes.get(i);
			if (!foe.isAlive()) continue;
			int land = landingCell(hero, foe);
			if (land == -1) continue;
			if (land != hero.pos) {
				ScrollOfTeleportation.appear(hero, land);
				Dungeon.level.occupyCell(hero);
			}
			hero.sprite.turnTo(hero.pos, foe.pos);
			hero.attack(foe, mult, 0f, 1000f);
			if (foe.isAlive() && charm > 0 && foe instanceof Mob && !LustDesire.charmImmune(foe)) {
				LustDesire.charm(hero, (Mob) foe, charm);
			}
			if (foe.sprite != null) foe.sprite.emitter().burst(Speck.factory(Speck.HEART), 2);
		}

		Dungeon.observe();
		GameScene.updateFog();
		Invisibility.dispel();
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
		hero.spendAndNext(1f);
	}

	private static int landingCell(Hero hero, Char foe) {
		if (Dungeon.level.adjacent(hero.pos, foe.pos)) return hero.pos;
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8) {
			int c = foe.pos + n;
			if (Dungeon.level.passable[c] && !Dungeon.level.avoid[c] && Actor.findChar(c) == null
					&& (best == -1 || Dungeon.level.distance(hero.pos, c) < Dungeon.level.distance(hero.pos, best))) {
				best = c;
			}
		}
		return best;
	}

	@Override
	public int icon() {
		return HeroIcon.DANSE_MACABRE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.MACABRE_COUNT, Talent.MACABRE_POWER, Talent.MACABRE_CHARM, Talent.HEROIC_ENERGY};
	}
}
