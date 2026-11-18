package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * Seven Sins (Nov 18 2026, owner): a pulsing "!" on a tile that an attack is about to hit (the holy giant's
 * judgment). The marks stay until {@link #clear()}.
 */
public class AlertCell extends Image {

	private static final ArrayList<AlertCell> shown = new ArrayList<>();
	private float time;

	private AlertCell(int cell) {
		copy(Icons.get(Icons.ALERT));
		origin.set(width / 2f, height / 2f);
		camera = null;
		point(DungeonTilemap.tileCenterToWorld(cell));
		x -= width / 2f;
		y -= height / 2f;
		hardlight(1f, 0.85f, 0.2f);
		time = (cell % 7) * 0.13f;
	}

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		float k = 0.75f + 0.25f * (float) Math.sin(time * 8);
		scale.set(0.9f * k);
		alpha(0.55f + 0.45f * k);
	}

	/** may be called from the actor thread: the marks are made on the render thread */
	public static void show(final int[] cells) {
		Game.runOnRenderThread(new com.watabou.utils.Callback() {
			@Override
			public void call() {
				clearNow();
				synchronized (shown) {
					for (int c : cells) {
						AlertCell a = new AlertCell(c);
						shown.add(a);
						GameScene.effect(a);
					}
				}
			}
		});
	}

	public static boolean showing() {
		synchronized (shown) {
			return !shown.isEmpty();
		}
	}

	public static void clear() {
		Game.runOnRenderThread(new com.watabou.utils.Callback() {
			@Override
			public void call() {
				clearNow();
			}
		});
	}

	private static void clearNow() {
		synchronized (shown) {
			for (AlertCell a : shown) a.killAndErase();
			shown.clear();
		}
	}
}
