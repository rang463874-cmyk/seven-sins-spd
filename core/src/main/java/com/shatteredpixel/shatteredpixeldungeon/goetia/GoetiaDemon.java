package com.shatteredpixel.shatteredpixeldungeon.goetia;

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import java.util.ArrayList;

/**
 * Seven Sins: the 72 demons of the Ars Goetia. Asmoday (32) was left out at first because the demon lord
 * Asmodeus is the same figure; the owner wanted the full 72 (Nov 14 2026), so he roams the halls as a king
 * under his Goetia name, apart from the demon lord.
 * Regular demons (region 0..4 = sewers..halls) roam floors 2-24 as rare strong monsters; the five
 * kings with a bossFloor wait behind a hidden sigil on that boss floor (5, 10, 15, 20, 25).
 * Order = the Goetia's numbering. Names/ranks: strings "goetia.goetiademon.NAME" / "rank_RANK".
 */
public enum GoetiaDemon {

	BAEL(Rank.KING, -1, 25, Kind.FIRE, 0xE04020),
	AGARES(Rank.DUKE, 2, 0, Kind.CRIPPLE, 0xA07040),
	VASSAGO(Rank.PRINCE, 3, 0, Kind.BLINK, 0x8060C0),
	SAMIGINA(Rank.MARQUIS, 1, 0, Kind.SUMMON, 0x8090A0),
	MARBAS(Rank.PRESIDENT, 0, 0, Kind.WEAKEN, 0xB09060),
	VALEFOR(Rank.DUKE, 1, 0, Kind.BLINK, 0xC08040),
	AMON(Rank.MARQUIS, 1, 0, Kind.FIRE, 0xFF6020),
	BARBATOS(Rank.DUKE, 2, 0, Kind.SUMMON, 0x60A040),
	PAIMON(Rank.KING, -1, 20, Kind.CHARM, 0xF0D060),
	BUER(Rank.PRESIDENT, 1, 0, Kind.HEAL, 0x60C080),
	GUSION(Rank.DUKE, 1, 0, Kind.EVASIVE, 0x80A0E0),
	SITRI(Rank.PRINCE, 3, 0, Kind.CHARM, 0xFF70B0),
	BELETH(Rank.KING, -1, 15, Kind.FEAR, 0xC04080),
	LERAJE(Rank.MARQUIS, 0, 0, Kind.ARROW, 0x40A060),
	ELIGOS(Rank.DUKE, 1, 0, Kind.LANCE, 0xA0A0C0),
	ZEPAR(Rank.DUKE, 1, 0, Kind.CHARM, 0xE060A0),
	BOTIS(Rank.PRESIDENT, 0, 0, Kind.POISON, 0x60C040),
	BATHIN(Rank.DUKE, 1, 0, Kind.BLINK, 0xA08060),
	SALLOS(Rank.DUKE, 2, 0, Kind.ARMOR, 0xC0A060),
	PURSON(Rank.KING, -1, 5, Kind.LANCE, 0xD0A040),
	MARAX(Rank.PRESIDENT, 2, 0, Kind.ARMOR, 0x907050),
	IPOS(Rank.PRINCE, 3, 0, Kind.FEAR, 0xC08040),
	AIM(Rank.DUKE, 2, 0, Kind.BURN, 0xFF5010),
	NABERIUS(Rank.MARQUIS, 1, 0, Kind.BLIND, 0x404040),
	GLASYA_LABOLAS(Rank.PRESIDENT, 2, 0, Kind.BLEED, 0xC02020),
	BUNE(Rank.DUKE, 2, 0, Kind.SUMMON, 0x5070A0),
	RONOVE(Rank.MARQUIS, 1, 0, Kind.WEAKEN, 0x8080A0),
	BERITH(Rank.DUKE, 2, 0, Kind.BURN, 0xE03030),
	ASTAROTH(Rank.DUKE, 4, 0, Kind.CORRODE, 0x70A030),
	FORNEUS(Rank.MARQUIS, 2, 0, Kind.FROST, 0x4080E0),
	FORAS(Rank.PRESIDENT, 1, 0, Kind.HEAL, 0x50B070),
	ASMODAY(Rank.KING, 4, 0, Kind.FIRE, 0xD03070),
	GAAP(Rank.PRESIDENT, 3, 0, Kind.BLINK, 0x9070D0),
	FURFUR(Rank.EARL, 3, 0, Kind.LIGHTNING, 0xE0E060),
	MARCHOSIAS(Rank.MARQUIS, 2, 0, Kind.FIRE, 0xFF7040),
	STOLAS(Rank.PRINCE, 3, 0, Kind.BLIND, 0x6040A0),
	PHENEX(Rank.MARQUIS, 3, 0, Kind.HEAL, 0xFF9030),
	HALPHAS(Rank.EARL, 2, 0, Kind.SUMMON, 0xA06060),
	MALPHAS(Rank.PRESIDENT, 2, 0, Kind.ARMOR, 0x303030),
	RAUM(Rank.EARL, 2, 0, Kind.BLINK, 0x505060),
	FOCALOR(Rank.DUKE, 3, 0, Kind.FROST, 0x3070C0),
	VEPAR(Rank.DUKE, 2, 0, Kind.BLEED, 0x2080A0),
	SABNOCK(Rank.MARQUIS, 3, 0, Kind.CRIPPLE, 0xB05030),
	SHAX(Rank.MARQUIS, 1, 0, Kind.BLIND, 0x707070),
	VINE(Rank.KING, -1, 10, Kind.LIGHTNING, 0x8040E0),
	BIFRONS(Rank.EARL, 4, 0, Kind.SUMMON, 0x6060A0),
	VUAL(Rank.DUKE, 3, 0, Kind.CHARM, 0xC070C0),
	HAAGENTI(Rank.PRESIDENT, 4, 0, Kind.CORRODE, 0xA0C040),
	CROCELL(Rank.DUKE, 4, 0, Kind.FROST, 0x60A0F0),
	FURCAS(Rank.KNIGHT, 3, 0, Kind.LANCE, 0x909090),
	BALAM(Rank.KING, 4, 0, Kind.FEAR, 0xA03060),
	ALLOCES(Rank.DUKE, 3, 0, Kind.LANCE, 0xD06030),
	CAIM(Rank.PRESIDENT, 2, 0, Kind.EVASIVE, 0x505070),
	MURMUR(Rank.DUKE, 4, 0, Kind.FEAR, 0x7050A0),
	OROBAS(Rank.PRINCE, 4, 0, Kind.ARMOR, 0xB09070),
	GREMORY(Rank.DUKE, 3, 0, Kind.CHARM, 0xE080C0),
	OSE(Rank.PRESIDENT, 2, 0, Kind.CONFUSE, 0xC0A030),
	AMY(Rank.PRESIDENT, 4, 0, Kind.FIRE, 0xFF4000),
	ORIAS(Rank.MARQUIS, 4, 0, Kind.BLINK, 0xD08040),
	VAPULA(Rank.DUKE, 1, 0, Kind.BLEED, 0xC06040),
	ZAGAN(Rank.KING, 4, 0, Kind.CORRODE, 0x90B030),
	VOLAC(Rank.PRESIDENT, 3, 0, Kind.EVASIVE, 0x60A0A0),
	ANDRAS(Rank.MARQUIS, 4, 0, Kind.BLEED, 0x902020),
	HAURES(Rank.DUKE, 4, 0, Kind.BURN, 0xFF3000),
	ANDREALPHUS(Rank.MARQUIS, 3, 0, Kind.CONFUSE, 0x60C0C0),
	CIMEIES(Rank.MARQUIS, 1, 0, Kind.LANCE, 0x404080),
	AMDUSIAS(Rank.DUKE, 3, 0, Kind.LIGHTNING, 0xC0C0F0),
	BELIAL(Rank.KING, 4, 0, Kind.LIGHTNING, 0xC02040),
	DECARABIA(Rank.MARQUIS, 4, 0, Kind.BLINK, 0xE0C0E0),
	SEERE(Rank.PRINCE, 4, 0, Kind.EVASIVE, 0xA0E0A0),
	DANTALION(Rank.DUKE, 4, 0, Kind.CONFUSE, 0x8080C0),
	ANDROMALIUS(Rank.EARL, 1, 0, Kind.CRIPPLE, 0x806040);

	public enum Rank { KNIGHT, PRESIDENT, MARQUIS, EARL, DUKE, PRINCE, KING }

	/** what the demon does in a fight (see actors/mobs/goetia/GoetiaFiend) */
	public enum Kind {
		POISON, WEAKEN, CRIPPLE, BLEED, BURN, CHARM, BLIND, CONFUSE, FEAR, CORRODE,
		FIRE, FROST, LIGHTNING, ARROW, SUMMON, HEAL, BLINK, EVASIVE, ARMOR, LANCE;

		public boolean ranged() {
			return this == FIRE || this == FROST || this == LIGHTNING || this == ARROW;
		}
	}

	public final Rank rank;
	public final int region;
	public final int bossFloor;
	public final Kind kind;
	public final int color;

	GoetiaDemon(Rank rank, int region, int bossFloor, Kind kind, int color) {
		this.rank = rank;
		this.region = region;
		this.bossFloor = bossFloor;
		this.kind = kind;
		this.color = color;
	}

	public boolean isLord() {
		return bossFloor > 0;
	}

	/** the Goetia's number (1..72) */
	public int number() {
		return ordinal() + 1;
	}

	public String title() {
		return Messages.get(GoetiaDemon.class, name().toLowerCase());
	}

	public String rankName() {
		return Messages.get(GoetiaDemon.class, "rank_" + rank.name().toLowerCase());
	}

	public String kindDesc() {
		return Messages.get(GoetiaDemon.class, "kind_" + kind.name().toLowerCase());
	}

	public static ArrayList<GoetiaDemon> ofRegion(int region) {
		ArrayList<GoetiaDemon> list = new ArrayList<>();
		for (GoetiaDemon d : values()) if (d.region == region) list.add(d);
		return list;
	}

	public static GoetiaDemon lordOf(int depth) {
		for (GoetiaDemon d : values()) if (d.bossFloor == depth) return d;
		return null;
	}
}
