package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** Lust's remains: the last drops of perfume charm every nearby enemy. */
public class BrokenPerfume extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_LUST;
	}

	@Override
	protected void doEffect(Hero hero) {
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= 3) {
				Charm c = Buff.prolong(ch, Charm.class, 5f);
				if (c != null) c.object = hero.id();
				ch.sprite.emitter().burst(Speck.factory(Speck.HEART), 3);
			}
		}
		Sample.INSTANCE.play(Assets.Sounds.CHARMS);
	}
}
