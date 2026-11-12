package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.virtues;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.*;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.virtues.Virtue;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/** Saint of kindness (nemesis of envy): mends itself and blesses, and holds the hero in a gentle embrace. */
public class KindnessChampion extends VirtueChampion {
	private static final int MERCY = 0, EMBRACE = 1;
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Kindness.class; }
	@Override public Virtue virtue() { return Virtue.KINDNESS; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == MERCY ? 9 : 5; }
	@Override protected String skillKey(int s) { return s == MERCY ? "mercy" : "embrace"; }

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == MERCY) return HP < HT * 0.7f ? new int[0] : null;
		return dist(target) <= 6 ? checker(target.pos, 2) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == MERCY) {
			int heal = Math.min(HT - HP, Math.round(HT * 0.2f));
			HP += heal;
			if (sprite != null) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			Buff.prolong(this, Bless.class, 10f);
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
			return;
		}
		for (int c : cells) {
			CellEmitter.center(c).burst(Speck.factory(Speck.HEART), 2);
			Char ch = strike(c, sMin() / 2, sMax() / 2);
			if (ch != null && ch.isAlive()) Buff.prolong(ch, Roots.class, 3f);
		}
		Sample.INSTANCE.play(Assets.Sounds.PLANT);
	}
}
