package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.noosa.Image;
import com.watabou.noosa.ui.Component;

/**
 * A titled message that scrolls when it is too long for the screen (used by the sins' codexes).
 */
public class WndScrollMessage extends Window {

	private static final int WIDTH_P = 130;
	private static final int WIDTH_L = 200;

	public WndScrollMessage(Image icon, String title, String message) {
		super();
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle titlebar = new IconTitle(icon, title);
		titlebar.setRect(0, 0, width, 0);
		add(titlebar);

		RenderedTextBlock text = PixelScene.renderTextBlock(6);
		text.text(message, width);

		Component content = new Component();
		content.add(text);
		text.setPos(0, 0);
		content.setSize(width, text.height());

		ScrollPane pane = new ScrollPane(content);
		add(pane);

		float top = titlebar.bottom() + 4;
		int maxHeight = (int) (PixelScene.uiCamera.height * 0.8f);
		int height = (int) Math.min(maxHeight, top + text.height() + 2);
		resize(width, height);
		pane.setRect(0, top, width, height - top);
	}
}
