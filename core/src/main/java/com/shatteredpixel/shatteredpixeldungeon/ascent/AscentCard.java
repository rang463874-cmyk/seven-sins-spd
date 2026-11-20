package com.shatteredpixel.shatteredpixeldungeon.ascent;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * Seven Sins: the crossroads cards of the ascent (Nov 20 2026, owner). After the first choice (the faction, for the whole
 * ascent) the hero picks one of three random cards on entering each region on the way up (halls, city, caves, prison);
 * the card holds for that region's five floors. Sin cards and virtue cards count towards the ending, neutral ones don't.
 * Strings: ascent.ascentcard.[id], [id]_risk, [id]_reward (tools/strings/ascent_items_*).
 */
public enum AscentCard {

	// ---- sins
	GLUTTON_MARCH(Side.SIN),
	GLUTTON_FEAST(Side.SIN),
	GREED_CURSE(Side.SIN),
	GREED_VAULT(Side.SIN),
	WRATH_FIRE(Side.SIN),
	WRATH_LEGION(Side.SIN),
	PRIDE_ROAD(Side.SIN),
	LUST_BLOOD(Side.SIN),
	ENVY_MIRROR(Side.SIN),
	SLOTH_FOG(Side.SIN),
	SIN_PARADE(Side.SIN),
	// ---- virtues
	HUMILITY_PILGRIM(Side.VIRTUE),
	CHARITY_HAND(Side.VIRTUE),
	CHASTITY_ABSTAIN(Side.VIRTUE),
	PATIENCE_ENDURE(Side.VIRTUE),
	TEMPERANCE_SILENCE(Side.VIRTUE),
	KINDNESS_TRIAL(Side.VIRTUE),
	DILIGENCE_TIME(Side.VIRTUE),
	MARTYR(Side.VIRTUE),
	// ---- neutral
	QUIET(Side.NEUTRAL),
	DARK(Side.NEUTRAL),
	FLOOD(Side.NEUTRAL),
	ALCHEMIST(Side.NEUTRAL),
	GAMBLER(Side.NEUTRAL);

	public enum Side { SIN, VIRTUE, NEUTRAL }

	public final Side side;

	AscentCard(Side side) {
		this.side = side;
	}

	public String id() {
		return name().toLowerCase();
	}

	public String title() {
		return Messages.get(AscentCard.class, id());
	}

	public String sideName() {
		return Messages.get(AscentCard.class, "side_" + side.name().toLowerCase());
	}

	/** risk and reward, for the choice window and the scale buff */
	public String desc() {
		return Messages.get(AscentCard.class, "card_desc", sideName(),
				Messages.get(AscentCard.class, id() + "_risk"), Messages.get(AscentCard.class, id() + "_reward"));
	}

	public static AscentCard byName(String n) {
		if (n == null) return null;
		try {
			return valueOf(n);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
