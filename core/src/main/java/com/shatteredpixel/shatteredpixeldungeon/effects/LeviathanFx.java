package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;

/**
 * Pictures for Leviathan's attacks (the owner's art, made by tools/sinsart/make_leviathan_art.py):
 * the laser, the tidal wave sliding over the floor and the falling drops. They are only pictures: the
 * damage is dealt by LeviathanBoss.
 */
public final class LeviathanFx {

	private LeviathanFx() {}

	private static final int T = DungeonTilemap.SIZE;

	private static boolean inGame() {
		return ShatteredPixelDungeon.scene() instanceof GameScene;
	}

	/** an image that lives for a while, moving and fading out over its last part */
	private static class Fading extends Image {
		private final float life, fadeFrom, startAlpha;
		private float time = 0;
		private float vx = 0, vy = 0;

		Fading(String tx, float life, float fadeFrom, float alpha) {
			super(tx);
			this.life = life;
			this.fadeFrom = fadeFrom;
			this.startAlpha = alpha;
			alpha(alpha);
		}

		Fading speed(float vx, float vy) {
			this.vx = vx;
			this.vy = vy;
			return this;
		}

		@Override
		public void update() {
			super.update();
			time += Game.elapsed;
			x += vx * Game.elapsed;
			y += vy * Game.elapsed;
			if (time >= life) {
				killAndErase();
			} else if (time > fadeFrom) {
				alpha(startAlpha * (1f - (time - fadeFrom) / (life - fadeFrom)));
			}
		}
	}

	/** the laser from the head's mouth to a far tile */
	public static void beam(int from, int to, boolean wide) {
		if (!inGame()) return;
		PointF a = DungeonTilemap.tileCenterToWorld(from);
		a.y -= T;   //the mouth is above the head's own tile
		PointF b = DungeonTilemap.tileCenterToWorld(to);
		float dx = b.x - a.x, dy = b.y - a.y;
		float dist = (float) Math.hypot(dx, dy);
		if (dist < 1f) return;
		float thick = wide ? 2.6f : 1f;

		Fading shaft = new Fading("effects/leviathan_beam.png", 0.6f, 0.25f, 1f);
		shaft.origin.set(0, shaft.height / 2f);
		shaft.x = a.x;
		shaft.y = a.y - shaft.height / 2f;
		shaft.angle = (float) Math.toDegrees(Math.atan2(dy, dx));
		shaft.scale.set(dist / shaft.width, thick);
		GameScene.effectOverFog(shaft);

		Fading ball = new Fading("effects/leviathan_ball.png", 0.6f, 0.25f, 1f);
		ball.origin.set(ball.width / 2f, ball.height / 2f);
		ball.scale.set(wide ? 1.8f : 1f);
		ball.x = b.x - ball.width / 2f;
		ball.y = b.y - ball.height / 2f;
		GameScene.effectOverFog(ball);
	}

	/** the tidal wave crossing the columns left..right; top..bottom are the floor's rows */
	public static void wave(int left, int right, int top, int bottom) {
		if (!inGame() || bottom < top) return;
		float life = 0.5f;
		Fading img = new Fading("effects/leviathan_wave.png", life, 0.3f, 0.92f);
		float scale = (bottom - top + 1) * T / img.height;
		img.scale.set(scale);
		float width = img.width * scale;
		//its crest starts at the band's left edge and ends at its right edge
		float travel = (right - left + 1) * T;
		img.x = left * T - width;
		img.y = top * T;
		img.speed(travel / (life * 0.7f), 0);
		GameScene.effectOverFog(img);
	}

	/** a drop falling onto a tile */
	public static void drop(int cell) {
		if (!inGame() || Dungeon.level == null || !Dungeon.level.heroFOV[cell]) return;
		float life = 0.28f, height = 44f;
		Fading img = new Fading("effects/leviathan_drop.png", life, life * 0.85f, 1f);
		PointF p = DungeonTilemap.tileCenterToWorld(cell);
		img.x = p.x - img.width / 2f;
		img.y = p.y - img.height - height;
		img.speed(0, height / life);
		GameScene.effectOverFog(img);
	}
}
