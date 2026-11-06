package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/** Envy's remains: steals life from the nearest visible enemy. */
public class MirrorShard extends RemainsItem {

	{
		image = ItemSpriteSheet.REMAINS_ENVY;
	}

	@Override
	protected void doEffect(Hero hero) {
		Char target = null;
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && ch.isAlive() && Dungeon.level.heroFOV[ch.pos]
					&& Dungeon.level.distance(hero.pos, ch.pos) <= 4
					&& (target == null || Dungeon.level.distance(hero.pos, ch.pos) < Dungeon.level.distance(hero.pos, target.pos))) {
				target = ch;
			}
		}
		if (target == null) {
			GLog.i(Messages.get(this, "nothing"));
			return;
		}
		int steal = Math.max(1, Math.min(target.HP, Math.round(hero.HT * 0.15f)));
		target.damage(steal, this);
		target.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2);
		int heal = Math.min(hero.HT - hero.HP, steal);
		if (heal > 0) {
			hero.HP += heal;
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
		}
		Sample.INSTANCE.play(Assets.Sounds.DRINK);
	}
}
