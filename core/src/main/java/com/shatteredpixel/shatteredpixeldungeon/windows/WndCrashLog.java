package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.Gdx;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.CrashLog;
import com.watabou.noosa.ui.Component;

/**
 * Seven Sins: shows the last crash report (CrashLog) on the title screen. Copy puts the full report on the clipboard;
 * closing the window deletes it.
 */
public class WndCrashLog extends Window {

	private static final int WIDTH_P = 130;
	private static final int WIDTH_L = 220;
	private static final int BTN_H = 18;
	private static final int GAP = 2;

	public WndCrashLog(final String log) {
		super();
		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle title = new IconTitle(Icons.get(Icons.WARNING), Messages.get(this, "title"));
		title.setRect(0, 0, width, 0);
		add(title);

		RenderedTextBlock intro = PixelScene.renderTextBlock(Messages.get(this, "intro"), 6);
		intro.maxWidth(width);
		intro.setPos(0, title.bottom() + 2 * GAP);
		add(intro);

		RenderedTextBlock text = PixelScene.renderTextBlock(5);
		text.text(CrashLog.forDisplay(log), width);
		text.hardlight(0xCCCCCC);
		Component content = new Component();
		content.add(text);
		text.setPos(0, 0);
		content.setSize(width, text.height());
		ScrollPane pane = new ScrollPane(content);
		add(pane);

		final RedButton copy = new RedButton(Messages.get(this, "copy")) {
			@Override
			protected void onClick() {
				try {
					Gdx.app.getClipboard().setContents(log);
					text(Messages.get(WndCrashLog.class, "copied"));
				} catch (Throwable ignored) {
				}
			}
		};
		add(copy);

		RedButton close = new RedButton(Messages.get(this, "close")) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		add(close);

		float top = intro.bottom() + 2 * GAP;
		int maxHeight = (int) (PixelScene.uiCamera.height * 0.8f);
		float paneH = Math.max(30, Math.min(text.height() + 2, maxHeight - top - BTN_H - 2 * GAP));
		pane.setRect(0, top, width, paneH);
		copy.setRect(0, pane.bottom() + GAP, (width - GAP) / 2f, BTN_H);
		close.setRect(copy.right() + GAP, copy.top(), (width - GAP) / 2f, BTN_H);
		resize(width, (int) Math.ceil(close.bottom()));
	}

	@Override
	public void hide() {
		super.hide();
		CrashLog.clear();
	}
}
