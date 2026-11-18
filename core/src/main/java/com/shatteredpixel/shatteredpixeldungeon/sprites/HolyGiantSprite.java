package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.GiantFist;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.heaven.HolyGiant;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

/**
 * Seven Sins (Nov 18 2026, owner's second design): the holy giant towering over the void that fills the upper half of
 * floor 31. The armless body (sprites/heaven_giant_body.png, x GiantArt.BODY_ZOOM) stands on the ledge cell and rises
 * through the void; the two arms (sprites/heaven_giant_arms.png: the lightning arm on the left, the gold bracelet arm
 * on the right) are separate pictures drawn behind it, turned and scaled from the shoulder to wherever the hand is:
 * resting on the ledge, raised over the tile it is about to slam (open hand), or lying on the floor as a fist while
 * the {@link GiantFist} is down. Everything is read from the {@link HolyGiant} each frame.
 */
public class HolyGiantSprite extends MobSprite {

	private static final float ZOOM_MIN = 2.2f, ZOOM_MAX = 3.8f;

	private final Image[] arms = new Image[2];
	private final PointF[] hand = new PointF[2];
	private final int[] lastHP = {-1, -1};
	private final float[] flash = {0, 0};
	private TextureFilm armFilm;
	private float time = 0;

	public HolyGiantSprite() {
		super();
		texture("sprites/heaven_giant_body.png");
		TextureFilm frames = new TextureFilm(texture, GiantArt.BODY_W, GiantArt.BODY_H);
		idle = new Animation(1, true);
		idle.frames(frames, 0);
		run = idle.clone();
		attack = idle.clone();
		zap = idle.clone();
		die = new Animation(1, false);
		die.frames(frames, 0);
		scale.set(GiantArt.BODY_ZOOM);
		origin.set(0, 0);
		play(idle);
	}

	@Override
	public void link(Char ch) {
		super.link(ch);
		renderShadow = false;
		origin.set(0, 0);
		if (parent != null && arms[0] == null) {
			for (int s = 0; s < 2; s++) {
				Image a = new Image("sprites/heaven_giant_arms.png");
				if (armFilm == null) armFilm = new TextureFilm(a.texture, GiantArt.ARM_W, GiantArt.ARM_H);
				a.frame(armFilm.get(s * 2));
				arms[s] = a;
				//under every mob sprite (the body hides the arm's cut end, the hero walks over it), over the floor
				com.watabou.noosa.Group layer = Dungeon.level instanceof com.shatteredpixel.shatteredpixeldungeon.levels.HolyGiantLevel
						? ((com.shatteredpixel.shatteredpixeldungeon.levels.HolyGiantLevel) Dungeon.level).visualLayer() : null;
				if (layer != null) layer.add(a);
				else parent.addToBack(a);
				hand[s] = idlePoint(s, ch.pos);
			}
		}
	}

	// ------------------------------------------------------------------ geometry (world pixels)

	/** top-left of the body picture for a giant standing on this cell */
	public static PointF bodyTopLeft(int cell) {
		int w = Dungeon.level.width();
		float bw = GiantArt.BODY_W * GiantArt.BODY_ZOOM, bh = GiantArt.BODY_H * GiantArt.BODY_ZOOM;
		return new PointF(((cell % w) + 0.5f) * DungeonTilemap.SIZE - bw / 2f,
				(cell / w) * DungeonTilemap.SIZE + 4 - bh);
	}

	/** where the arm of this side leaves the body */
	public static PointF shoulder(int side, int cell) {
		PointF tl = bodyTopLeft(cell);
		return new PointF(tl.x + GiantArt.SHOULDERS[side][0] * GiantArt.BODY_ZOOM,
				tl.y + GiantArt.SHOULDERS[side][1] * GiantArt.BODY_ZOOM);
	}

	/** the hand at rest, gripping the edge of the ledge */
	public static PointF idlePoint(int side, int cell) {
		int w = Dungeon.level.width();
		float gx = (cell % w) + 0.5f;
		return new PointF((gx + (side == 0 ? -7f : 7f)) * DungeonTilemap.SIZE,
				(cell / w) * DungeonTilemap.SIZE + 4);
	}

	private static PointF center(int cell) {
		int w = Dungeon.level.width();
		return new PointF(((cell % w) + 0.5f) * DungeonTilemap.SIZE, ((cell / w) + 0.5f) * DungeonTilemap.SIZE);
	}

	@Override
	public PointF worldToCamera(int cell) {
		return bodyTopLeft(cell);
	}

	@Override
	public void turnTo(int from, int to) {
		//it faces the arena whatever it does
	}

	@Override
	public void bloodBurstA(PointF from, int damage) {
	}

	// ------------------------------------------------------------------ every frame

	@Override
	public void update() {
		super.update();
		time += Game.elapsed;
		if (!(ch instanceof HolyGiant) || arms[0] == null) return;
		HolyGiant g = (HolyGiant) ch;

		//breathing, and a glow while it gathers its judgment
		if (g.charging()) brightness(1.15f + 0.25f * (float) Math.sin(time * 9));
		else brightness(1f);

		for (int s = 0; s < 2; s++) {
			Image a = arms[s];
			if (a == null) continue;
			a.visible = visible && !g.fallen() && g.armHP[s] > 0;
			a.alpha(alpha());
			if (!a.visible) continue;

			GiantFist f = g.fist(s);
			PointF want;
			int frame;
			float pulse = 1f;
			if (f != null) {
				frame = s * 2 + 1;
				want = center(f.pos);
			} else if (g.telegraphSide() == s && g.telegraphCell() >= 0) {
				frame = s * 2;
				PointF sh = shoulder(s, g.pos);
				PointF t = center(g.telegraphCell());
				want = new PointF(sh.x + (t.x - sh.x) * 0.6f, sh.y + (t.y - sh.y) * 0.6f);
				pulse = 1.2f + 0.2f * (float) Math.sin(time * 12);
			} else {
				frame = s * 2;
				want = idlePoint(s, g.pos);
				want.y += (float) Math.sin(time * 1.6f + s) * 1.5f;
			}
			//the fist comes down fast, everything else drifts
			float k = Math.min(1f, Game.elapsed * (f != null ? 20f : 8f));
			hand[s].x += (want.x - hand[s].x) * k;
			hand[s].y += (want.y - hand[s].y) * k;
			a.frame(armFilm.get(frame));

			//a hit on the fist flashes the arm
			int hp = g.armHP[s];
			if (lastHP[s] >= 0 && hp < lastHP[s]) flash[s] = 0.2f;
			lastHP[s] = hp;
			if (flash[s] > 0) {
				flash[s] -= Game.elapsed;
				a.tint(1f, 1f, 1f, 0.6f);
			} else {
				a.resetColor();
				a.brightness(pulse);
			}
			a.alpha(alpha());

			layArm(a, frame, shoulder(s, g.pos), hand[s]);
		}
	}

	/** turns and scales an arm picture so that its hand sits on the given point and it points back to the shoulder */
	private static void layArm(Image a, int frame, PointF sh, PointF to) {
		float[] p = GiantArt.ARMS[frame];
		float ax = p[0], ay = p[1], hx = p[2], hy = p[3];
		float dx = to.x - sh.x, dy = to.y - sh.y;
		float dist = (float) Math.sqrt(dx * dx + dy * dy);
		float ilen = (float) Math.sqrt((hx - ax) * (hx - ax) + (hy - ay) * (hy - ay));
		float s = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, dist / Math.max(1f, ilen)));
		a.scale.set(s);
		a.origin.set(hx, hy);
		a.angle = (float) Math.toDegrees(Math.atan2(dy, dx) - Math.atan2(hy - ay, hx - ax));
		a.x = to.x - hx;
		a.y = to.y - hy;
	}

	@Override
	public void kill() {
		super.kill();
		for (Image a : arms) if (a != null) a.killAndErase();
	}

	@Override
	public int blood() {
		return 0xFF80C8FF;
	}
}
