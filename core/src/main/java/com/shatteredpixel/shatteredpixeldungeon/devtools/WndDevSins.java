package com.shatteredpixel.shatteredpixeldungeon.devtools;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnvyCovet;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreedVault;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LustDesire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PredatorStomach;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PrideGlory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SlothRepose;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WrathGrudge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Dev tools: raise the current sin's growth stacks (and refill its skill resource).
 * Values are written straight into the sin's buff, skipping talent multipliers.
 */
public class WndDevSins extends Window {

	static final int WIDTH = WndDevTools.WIDTH;
	static final int[] STEPS = {10, 100, 1000};

	/** one adjustable value: a label, how to read it and how to add to it */
	static class Row {
		final String label;
		final Supplier<Float> get;
		final Consumer<Float> add;
		/** button steps are multiplied by this (gold moves in bigger amounts) */
		final int scale;

		Row(String label, Supplier<Float> get, Consumer<Float> add) {
			this(label, get, add, 1);
		}

		Row(String label, Supplier<Float> get, Consumer<Float> add, int scale) {
			this.label = label;
			this.get = get;
			this.add = add;
			this.scale = scale;
		}
	}

	private final ArrayList<RenderedTextBlock> valueTexts = new ArrayList<>();
	private final ArrayList<Row> rows = new ArrayList<>();
	private Runnable refill = null;
	private String refillLabel = null;

	public WndDevSins() {
		super();
		final Hero h = Dungeon.hero;
		float y = WndDevTools.title(this, "칠죄종 스택");
		collect(h);

		if (rows.isEmpty()) {
			RenderedTextBlock none = PixelScene.renderTextBlock("지금 영웅은 칠죄종이 아닙니다.", 7);
			none.maxWidth(WIDTH);
			none.setPos(0, y);
			add(none);
			resize(WIDTH, (int) none.bottom());
			return;
		}

		for (final Row r : rows) {
			RenderedTextBlock t = PixelScene.renderTextBlock("", 7);
			t.setPos(0, y);
			add(t);
			valueTexts.add(t);
			y = t.bottom() + 2;

			float bw = (WIDTH - (STEPS.length - 1) * 2) / (float) STEPS.length;
			for (int i = 0; i < STEPS.length; i++) {
				final int step = STEPS[i] * r.scale;
				RedButton b = new RedButton("+" + step, 7) {
					@Override
					protected void onClick() {
						r.add.accept((float) step);
						changed();
					}
				};
				add(b);
				b.setRect(i * (bw + 2), y, bw, 16);
			}
			y += 16 + 4;
		}

		if (refill != null) {
			RedButton b = new RedButton(refillLabel, 8) {
				@Override
				protected void onClick() {
					refill.run();
					changed();
					GLog.p("가득 채웠습니다.");
				}
			};
			b.textColor(0xFFFF44);
			add(b);
			b.setRect(0, y, WIDTH, 18);
			y += 18;
		}
		refreshTexts();
		resize(WIDTH, (int) y);
	}

	private void collect(final Hero h) {
		if (h == null) return;
		final PredatorStomach st = h.buff(PredatorStomach.class);
		if (st != null) {
			rows.add(new Row("식탐 · 물약 점수", () -> st.weaponPts, v -> st.weaponPts += v));
			rows.add(new Row("식탐 · 음식 점수", () -> st.armorPts, v -> st.armorPts += v));
			rows.add(new Row("식탐 · 주문서 점수", () -> st.trinketPts, v -> st.trinketPts += v));
			rows.add(new Row("식탐 · 미식 도감", () -> (float) st.codex.size(), v -> { for (int i = 0; i < Math.min(v, 100); i++) st.codex.add("dev." + st.codex.size()); }));
		}
		final GreedVault gv = h.buff(GreedVault.class);
		if (gv != null) {
			rows.add(new Row("탐욕 · 소지 골드", () -> (float) Dungeon.gold, v -> Dungeon.gold += Math.round(v), 10));
			refillLabel = "결제 한도 가득";
			refill = () -> gv.addCharge(gv.maxCharge());
		}
		final WrathGrudge wg = h.buff(WrathGrudge.class);
		if (wg != null) {
			rows.add(new Row("분노 · 원한", () -> wg.grudge, v -> wg.grudge += v));
			refillLabel = "분노 가득";
			refill = () -> wg.rage = wg.maxRage();
		}
		final PrideGlory pg = h.buff(PrideGlory.class);
		if (pg != null) {
			rows.add(new Row("교만 · 영광", () -> pg.glory, v -> pg.glory += v));
			refillLabel = "위엄 가득";
			refill = () -> pg.dignity = pg.maxDignity();
		}
		final LustDesire ld = h.buff(LustDesire.class);
		if (ld != null) {
			rows.add(new Row("색욕 · 갈망", () -> ld.desire, v -> ld.desire += v));
			refillLabel = "체력 가득";
			refill = () -> h.HP = h.HT;
		}
		final EnvyCovet ec = h.buff(EnvyCovet.class);
		if (ec != null) {
			rows.add(new Row("시기 · 시기", () -> ec.envy, v -> ec.envy += v));
			refillLabel = "질투 가득";
			refill = () -> ec.jealousy = EnvyCovet.MAX_JEALOUSY;
		}
		final SlothRepose sr = h.buff(SlothRepose.class);
		if (sr != null) {
			rows.add(new Row("나태 · 나태 점수", () -> sr.sloth, v -> sr.sloth += v));
			refillLabel = "체력 가득";
			refill = () -> h.HP = h.HT;
		}
	}

	private void changed() {
		Hero h = Dungeon.hero;
		if (h != null) h.updateHT(true);
		BuffIndicator.refreshHero();
		Item.updateQuickslot();
		refreshTexts();
	}

	private void refreshTexts() {
		for (int i = 0; i < rows.size(); i++) {
			Row r = rows.get(i);
			valueTexts.get(i).text(r.label + ": " + Math.round(r.get.get()));
		}
	}

	/** opened from the main dev tools window */
	public static void open() {
		GameScene.show(new WndDevSins());
	}
}
