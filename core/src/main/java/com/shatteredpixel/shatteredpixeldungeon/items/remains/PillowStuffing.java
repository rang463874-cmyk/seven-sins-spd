package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;

/** Sloth's remains: a puff of old stuffing makes nearby enemies drowsy and lets you rest a moment. */
public class PillowStuffing extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_SLOTH;
	}

	@Override
	protected void doEffect(Hero hero) {
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= 3
					&& !ch.properties().contains(Char.Property.BOSS)) {
				Buff.affect(ch, Drowsy.class);
				ch.sprite.emitter().burst(Speck.factory(Speck.NOTE), 2);
			}
		}
		int heal = Math.min(hero.HT - hero.HP, Math.round(hero.HT / 20f));
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		Sample.INSTANCE.play(Assets.Sounds.LULLABY);
	}
}
