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

/** Saint of charity (nemesis of greed): heals every enemy of the hero in sight, and tithes the hero's gold. */
public class CharityChampion extends VirtueChampion {
	private static final int ALMS = 0, TITHE = 1;
	{ spriteClass = com.shatteredpixel.shatteredpixeldungeon.sprites.VirtueSprites.Charity.class; }
	@Override public Virtue virtue() { return Virtue.CHARITY; }
	@Override protected int skillCount() { return 2; }
	@Override protected int cooldown(int s) { return s == ALMS ? 8 : 4; }
	@Override protected String skillKey(int s) { return s == ALMS ? "alms" : "tithe"; }

	private ArrayList<Char> flock() {
		ArrayList<Char> out = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs) {
			if (m.alignment == alignment && m.isAlive() && Dungeon.level.distance(pos, m.pos) <= 6
					&& m.HP < m.HT * 0.8f) out.add(m);
		}
		return out;
	}

	@Override
	protected int[] prepare(int s, Char target) {
		if (s == ALMS) return flock().isEmpty() ? null : new int[0];
		return dist(target) <= 6 ? square(target.pos) : null;
	}

	@Override
	protected void release(int s, int[] cells) {
		if (s == ALMS) {
			for (Char m : flock()) {
				int heal = Math.min(m.HT - m.HP, Math.round(m.HT * 0.15f));
				if (heal <= 0) continue;
				m.HP += heal;
				if (m.sprite != null) m.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);
			}
			Sample.INSTANCE.play(Assets.Sounds.DRINK);
		} else {
			for (int c : cells) {
				CellEmitter.center(c).burst(Speck.factory(Speck.COIN), 2);
				Char ch = strike(c, sMin(), sMax());
				if (ch == Dungeon.hero && Dungeon.gold > 0) {
					int take = Math.min(Dungeon.gold, Math.max(10, Dungeon.gold / 10));
					Dungeon.gold -= take;
					GLog.w(Messages.get(VirtueChampion.class, "tithe_taken", take));
				}
			}
			Sample.INSTANCE.play(Assets.Sounds.GOLD);
		}
	}
}
