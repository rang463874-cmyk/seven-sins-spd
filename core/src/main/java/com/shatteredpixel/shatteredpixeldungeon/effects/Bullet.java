package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Group;
import com.watabou.noosa.tweeners.PosTweener;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;

/**
 * A real bullet for the gun of conquest: a tiny black slug that crosses the screen almost instantly.
 */
public class Bullet extends ColorBlock {

	public static final float SIZE = 2f;
	/** world pixels per second (a tile is 16px, so ~150 tiles/s) */
	public static final float SPEED = 2400f;

	private Bullet() {
		super(SIZE, SIZE, 0xFF000000);
		origin.set(SIZE / 2f, SIZE / 2f);
	}

	public static void fire(Group parent, PointF from, PointF to, final Callback callback) {
		final Bullet b = new Bullet();
		b.x = from.x - SIZE / 2f;
		b.y = from.y - SIZE / 2f;
		parent.add(b);

		float time = Math.max(0.01f, PointF.distance(from, to) / SPEED);
		PosTweener tween = new PosTweener(b, new PointF(to.x - SIZE / 2f, to.y - SIZE / 2f), time) {
			@Override
			protected void onComplete() {
				b.killAndErase();
				if (callback != null) callback.call();
			}
		};
		parent.add(tween);
	}
}
