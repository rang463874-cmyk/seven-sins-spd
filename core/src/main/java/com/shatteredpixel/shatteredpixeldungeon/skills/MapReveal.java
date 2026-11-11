package com.shatteredpixel.shatteredpixeldungeon.skills;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.watabou.noosa.audio.Sample;

/** Seven Sins: magic-mapping style reveals for greed's treasure map and sloth's dream patrol. */
public class MapReveal {

	/** maps every discoverable tile of the floor; with secrets, hidden doors and traps are found too */
	public static void reveal(boolean secrets) {
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean noticed = false;
		for (int i = 0; i < length; i++) {
			if (!Dungeon.level.discoverable[i]) continue;
			Dungeon.level.mapped[i] = true;
			int terr = map[i];
			if (secrets && (Terrain.flags[terr] & Terrain.SECRET) != 0) {
				Dungeon.level.discover(i);
				if (Dungeon.level.heroFOV[i]) {
					GameScene.discoverTile(i, terr);
					ScrollOfMagicMapping.discover(i);
					noticed = true;
				}
			}
		}
		GameScene.updateFog();
		if (noticed) Sample.INSTANCE.play(Assets.Sounds.SECRET);
	}
}
