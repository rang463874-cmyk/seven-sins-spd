package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.ArtifactShard;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollingListPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.utils.Reflection;

/**
 * Seven Sins: the artifact shard's power list. A row opens the power's text with Use / Cancel.
 */
public class WndArtifactShard extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 160;

	public WndArtifactShard(final ArtifactShard shard) {
		super();
		ScrollingListPane list = new ScrollingListPane();
		add(list);
		list.addTitle(Messages.get(this, "title"));

		for (final Class<? extends Artifact> cls : ArtifactShard.SOURCES.keySet()) {
			final Artifact sample = Reflection.newInstance(cls);
			if (sample == null) continue;
			ScrollingListPane.ListItem item = new ScrollingListPane.ListItem(
					new ItemSprite(sample), null, Messages.titleCase(sample.name())) {
				@Override
				public boolean onClick(float x, float y) {
					if (!inside(x, y)) return false;
					ShatteredPixelDungeon.scene().addToFront(new WndOptions(new ItemSprite(sample),
							Messages.titleCase(sample.name()), ArtifactShard.effectText(cls),
							Messages.get(WndArtifactShard.class, "use"), Messages.get(WndArtifactShard.class, "cancel")) {
						@Override
						protected void onSelect(int index) {
							if (index != 0) return;
							WndArtifactShard.this.hide();
							shard.usePower(Dungeon.hero, cls);
						}
					});
					return true;
				}
			};
			list.addItem(item);
		}

		int w = PixelScene.landscape() ? WIDTH_L : WIDTH_P;
		int maxH = (int) (PixelScene.uiCamera.height * 0.8f);
		resize(w, Math.min(maxH, (int) list.content().height()));
		list.setRect(0, 0, width, height);
	}
}
