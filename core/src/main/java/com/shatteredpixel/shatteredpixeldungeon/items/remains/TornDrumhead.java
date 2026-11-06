package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** Wrath's remains: one last drumbeat drives nearby enemies to fight each other. */
public class TornDrumhead extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_WRATH;
	}

	@Override
	protected void doEffect(Hero hero) {
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= 3
					&& !ch.properties().contains(Char.Property.BOSS)) {
				Buff.prolong(ch, Amok.class, 3f);
				ch.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 2);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
	}
}
