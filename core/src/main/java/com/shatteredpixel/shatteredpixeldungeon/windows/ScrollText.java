package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.ui.Component;

/**
 * Seven Sins (Nov 18 2026, owner: long descriptions ran off the screen): puts a window's text into a scroll pane when it
 * would make the window taller than the screen allows. Used by WndTitledMessage, WndInfoItem (and WndUseItem), WndInfoBuff
 * and WndInfoTalent; WndSkills scrolls its whole list.
 */
public final class ScrollText {

	private ScrollText() {}

	/** the tallest a window may get, leaving room for buttons added under the text */
	public static int maxHeight(int reserve) {
		return Math.max(60, (int) (PixelScene.uiCamera.height * 0.85f) - reserve);
	}

	/**
	 * Adds the text at (0, top), or a scroll pane holding it when it is too tall; returns the bottom of what was added.
	 * Call before the window's final resize: the pane follows the window when it moves.
	 */
	public static float place(Window w, RenderedTextBlock text, float top, int width, int reserve) {
		int maxH = maxHeight(reserve);
		if (top + text.height() + 2 <= maxH) {
			text.setPos(0, top);
			w.add(text);
			return text.bottom();
		}
		Component content = new Component();
		content.add(text);
		text.setPos(0, 0);
		content.setSize(width, text.height() + 2);
		Pane pane = new Pane(content);
		w.add(pane);
		pane.setRect(0, top, width, maxH - top);
		return pane.bottom();
	}

	/** a scroll pane that re-lays itself out every frame, so it follows its window when the window is resized */
	public static class Pane extends ScrollPane {
		public Pane(Component content) {
			super(content);
		}

		@Override
		public synchronized void update() {
			super.update();
			layout();
		}
	}
}
