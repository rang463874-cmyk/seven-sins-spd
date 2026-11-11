# Seven Sins mod for Shattered Pixel Dungeon

A mod of Shattered Pixel Dungeon v4.0.2 (GPLv3) that adds seven "Seven Deadly Sins" hero classes.
The owner plays and tests on an Android phone and talks in Korean, so reply in Korean.

## Current state

Done and pushed (see `git log`):
- Developer tools (in-game menu, debug builds only): item spawner, sin stacks (`devtools/WndDevSins`:
  +10/+100/+1000 to the current sin's points, refill charges/gauge/HP), monster summon
  (`devtools/WndDevMobs`: bestiary categories, tap a tile; bosses except Goo left out), level up, heal,
  identify all.
- Cleric is hidden from hero select (kept for old saves). Hero select fits 12 classes.
- Seven sin classes, each with: signature artifact (`items/artifacts/SinArtifact`, starts worn in the
  artifact slot; growth/stats always work, skills and the skill bar only while worn; levels +0..+10 like
  the cloak of shadows from resource spent via `SinArtifact.onSpent`, 3+level per level: charge sins +1 max
  charge per 3 levels and up to +60% recharge, Lust -30% HP costs, Greed +60% gauge, Gluttony -33% satiety
  costs and +50% consumable points), a permanent-growth buff,
  2 subclasses, 26 talents, 3 armor abilities, class armor, en/ko strings, placeholder art.
- Tier 1 sin weapons (starting weapons) and tier 5 sin weapons (via the blacksmith's
  Sigil of Sin, 2000 favor, keeps upgrades/enchantment). Effects only work for the owning sin.
- Imp quest vault mirror gives `items/MirrorCase` (every class; originals get it alongside their usual
  reward): one use per run, sets `Belongings.extraSlot`, opening `Belongings.misc2`, a second shared
  artifact/ring slot (equip logic in `KindofMisc`, shown in `WndBag`; `InventoryPane` lists it first in
  the bag). The archangel drops `items/HeavenlyHalo` (one use per run, sets `Belongings.extraSlot2`, opening a third
  shared slot `misc3`; works with or without the case). `KindofMisc` walks the shared slots misc/misc2/misc3 through
  `sharedCount/shared/setShared`; every other misc2 call site has a misc3 twin (Ring/Artifact/Mob/Talent/Toolbar/
  InventorySlot/SkillHolder). The backpack holds 25 items (`Belongings.Backpack.SIZE`, was 20): `WndBag` sizes its rows
  from 5 + extra slots + 25 (and shrinks slot height on short portrait screens), `InventoryPane` has 3 rows of 10
  (`HEIGHT` 107, slots past the bag's room hidden, 6 narrower bag buttons; the 6th bag tab has no hotkey). The four
  vanilla bags hold 24 (was 19). New `items/bags/EssenceBag` (24, holds `items/EssenceItem`: SinEssence now, Solomon's
  72 demons' essences later; tab icon = tinted seed pouch; items.png bags row +7). Start kit (`HeroClass.initHero`):
  every class gets the velvet pouch and the essence bag; gluttony/lust/sloth + potion bandolier, greed/wrath/envy +
  scroll holder, pride + magical holster (each marks its LimitedDrops so shops skip it). The old mirror relics (`MirrorRelics`, buffs' `mirrored` flag) are kept only for old saves.
- Artifact shards (`items/ArtifactShard`, stackable per source artifact, glow = source colour, art row 37 +15): alchemy
  one-ingredient recipe (`ArtifactShard.Recipe`, 6 energy, 1 + level/3 shards) from the 14 vanilla artifacts in
  `ArtifactShard.SOURCES` only (`Recipe.usableInRecipe` lets those in); each use = a one-time version of the artifact's
  power (`use`; chains and skeleton key are targeted via `targeter`; the rose summons two 30-turn `ArtifactShard.Spirit`
  wraith allies). On the alchemy guide's Seven_Sins page.
- GitHub Actions builds a debug APK on every push and publishes it to the `test-build`
  pre-release: https://github.com/rang463874-cmyk/seven-sins-spd/releases/tag/test-build
- Four horsemen relics (all classes) from the sewer "prophet of seals" quest, see below.
- Elite monsters (Nov 2026, `elites/EliteHooks`, called from the end of `RegularLevel.createMobs`, `Mob.die`,
  `Mob.name`; region record saved with the quests in `Dungeon`). Sin elites (`elites/SinElite`, built on
  `ChampionEnemy`: aura, factors and attack procs are already wired; +50% HT once via `boosted`, x2 exp, name
  prefix; 7 kinds gluttony..sloth) appear on 25% of floors 2-24. `Char.buff(Class)` matches exact classes, so use
  `EliteHooks.sinElite(ch)`. Region elites (`actors/mobs/elites/`, base `RegionElite` = Seal Warden style telegraphed
  skills via `prepare/release`): RatChieftain (sewers), ChainWarden (prison), GnollForeman (caves), FallenPriest
  (city), Hellhound twins (halls, `twin` id, survivor enrages); one per region (chance 1/floors left, forced on the
  region's last regular floor), listed in the bestiary's QUEST group. Drops: sin elite 1 `items/SinEssence` (+ own
  sin: `sins/SinRewards.grant` 15 pts; greedy: stolen gold x2), region elite 2 essences + uncursed +1
  weapon/armor/ring (`EliteHooks.regionReward`).
- Alchemy (Nov 2026): `items/elite/ElixirOfSin` (2 essences + any potion: +20 sin points and resource refill,
  else full heal + bless), `items/bombs/SinBomb` (bomb + essence, random sin debuff), `items/bombs/StickyBomb`
  (bomb + earthroot seed, roots + webs, no blast; both in `Bomb.EnhanceBomb`), `items/elite/Bandage` (2 sungrass
  seeds -> 2), `items/elite/Stimulant` (swiftthistle + blindweed seeds, instant haste/adrenaline),
  `items/elite/SinInfusion` (curse infusion + 2 essences: curses a melee weapon/armor/wand, +1 level per stack via
  `sinInfusions` on Weapon/Armor/Wand, reset wherever curseInfusionBonus is cleared, copied by transmutation/sigil/
  class armor), `items/elite/CastingMold` (12 liquid metal: +1 weapon to a thrown set via
  `MissileWeapon.bonusSetSize`/`setSize()`, which upgrade/merge/liquid metal/WndUpgrade use instead of
  defaultQuantity), `items/bombs/CaltropBomb` (bomb + blindweed: damage, bleed, cripple; the original SmokeBomb is
  untouched), `items/elite/DecoyDoll` (stormvine + fadeleaf -> 2, thrown: `DecoyDoll.Decoy` = PrideEffigy subclass).
  Terrain items (thrown, base `items/elite/TerrainCharm`: throw direction from `from`, `protectedCell` = locked/crystal
  vault or quest sanctum rooms): `WallAcid` (sorrowmoss + firebloom, 3 energy -> 1: walls/bookshelves in 3x3 around the
  landing cell + up to 3 deep along the throw become floor, secret doors open; not on boss floors/non-RegularLevel, never
  the outer edge), `StoneDust` (earthroot + icecap -> 2: 3-wide wall across the throw for 20 turns on open empty tiles,
  `StoneDust.RisenWall` blob counts down and restores the saved terrain), `DryingPowder` (firebloom + swiftthistle, 1
  energy -> 2: water within 2 becomes floor for good). They feed the scroll pool (`PredatorStomach.category`) but are
  not in `MOD_ALCHEMY` (the gourmet perk stays at 9 items, owner's call).
  Art items.png row 38 +1..+3.
  The alchemy pot puts one item per slot (only liquid metal/thrown weapons go in as a stack) and `Recipe.findRecipes`
  only checks the list matching the slot count, so recipes needing several of one item (ElixirOfSin, SinInfusion,
  Bandage) live in `Recipe.variableRecipes` (strict: `Recipe.StrictRecipe` rejects unlisted ingredients).
  All mod recipes are on the alchemy guide's 10th page "Seven_Sins" (`QuickRecipe.sevenSinsRecipes`, always READ;
  strings in messages/journal, not synced). Art: items.png row 37 (+8 sin infusion .. +11 decoy doll).
- Demon lords (Nov 2026, `demons/DemonHooks` + `demons/DemonLord` enum, state saved with the quests in `Dungeon`:
  stage NONE -> TRIAL -> GRANTED -> DEFEATED). `Goo.die` -> `onGooDefeated` raises `actors/mobs/npcs/DemonAltar`
  (`windows/WndDemonAltar`: 7 lords, then trial/boon/skill + accept; once per run). The trial (`actors/buffs/DemonTrial`,
  marker only) lasts until Tengu: `PrisonBossLevel.progress` (after `state = State.WON`) -> `onTenguDefeated` spawns
  `actors/mobs/npcs/DemonLordNPC`; first talk = `grantPerk` (`actors/buffs/DemonPerk`, also holds the skill charge 0..100,
  +1/turn, +2 after the boss) and the lord's skill `demons/DemonSkill` (a SinSkill shown in its own row above the skill
  bar via `SkillBar.extraSkills` -> `DemonHooks.skill`, for every class; icons skill_icons.png row 8 = 64 + lord ordinal;
  effects in `DemonSeal.invokeSkill`, the old `items/DemonSeal` item is only kept for saves that got it), then "fight / later": the fight takes the hero (InterlevelScene RETURN, `DemonHooks.travelToArena`) to
  `levels/DemonArenaLevel`, a side floor of depth 10 on branch `DemonHooks.ARENA_BRANCH` = 7 (built in `Dungeon.newLevel`;
  15x13 halls hall, long top wall; boss made in `createMobs` (no notice()/beckon during build: no sprite yet); the way back
  is a LOCKED_EXIT stair (BRANCH_ENTRANCE to depth 10's REGULAR_EXIT) opened by `unlockPortal` on the lord's death; the
  NPC avatar stays on Tengu's floor until DEFEATED). The altar rises on Goo's starting tile (`Goo.home`). Boss: `actors/mobs/DemonLordBoss` (RegionElite with BOSS, 300 HP, hellfire ring + wraiths + the
  lord's signature skill, enraged under 50%; death -> `onLordDefeated`: boon x1.5, 3 essences). Trial penalties/boons are
  hooked in Hero (updateHT, accuracy, evasion, speed, attackProc, rest), Char.damage, Gold, Hunger, Regeneration, Food.
  Every line has an `_own` variant used when the lord's sin is the hero's (`DemonHooks.line`). Strings in
  `tools/strings/demons*` (generated once by a script; edit them directly now). Art: the owner's own 48x48 PNGs in `tools/sinsart/demon_art/png/`
  (7 lords, `leviathan_coil`, `leviathan_tail`, `altar`; Leviathan's head faces left and is mirrored) are turned into
  `sprites/demon_lords.png` (48x49 frames, one row of 12 per lord in enum order; `DemonSprites.LordSprite` draws them
  over the 3x3 tiles around the cell, `perspectiveRaise` -0.5; windows use `DemonSprites.icon`/`altarIcon` at half
  size), `sprites/demon_leviathan.png` (48x49: body x3, tail x3, blank) and `sprites/demon_altar.png` (48x49 x2) by
  `tools/sinsart/make_demon_sprites.py` (art cropped and bottom-aligned, frames derived; the text art in
  `demon_art/<lord>.py` is only a fallback).
  Leviathan's boss is `actors/mobs/LeviathanBoss` (wall form, `emerge(level, cell, live)` (the arena calls it while building): picks the longest wall run
  of 3..11 cells, never covering a door, prefers horizontal walls; head in the middle, `LeviathanCoil`s on the rest,
  which forward damage to the head; immovable; Great Wave = every lane but 1-2 safe ones, Bite, Undertow pull, wraiths;
  falls back to the normal boss if no wall fits). Flies are a tinted swarm; seal at items.png row 37 +12. Dev tools: "마왕 단계 진행" advances the stage.
- Seven virtues, phase 1 (Nov 2026): the opposing faction. `virtues/Virtue` enum (humility/pride, charity/greed,
  chastity/lust, patience/wrath, temperance/gluttony, kindness/envy, diligence/sloth; `sin`, `color`, champion class)
  and `virtues/VirtueHooks` (state in the Dungeon quests bundle: regions done, virtues used, nemesis region).
  One champion per region 1-4 (prison..halls; rolled in `RegularLevel.build` -> `VirtueHooks.addSanctum`, chance 1/floors
  left, forced on the region's last regular floor, which adds `levels/rooms/quest/VirtueSanctumRoom` (13x13 dead end,
  4 pillars, pedestal, never merged); `EliteHooks.onLevelMobs` -> `VirtueHooks.onLevelMobs` puts the champion on the
  pedestal with `home` set), each virtue once per run; a sin hero's nemesis (opposing virtue) is guaranteed in
  one random region. Champions: `actors/mobs/virtues/*Champion` on `VirtueChampion` (RegionElite; stats from the
  floor: HT 30+12d etc.; with a `home` they announce ("awaits") and wait on the pedestal, aggro when the hero steps
  inside, wall up the door like the Seal Warden (`sealedDoors`, reopened on death or when the hero is out), never leave
  the room, and their skill cells are clipped to it (`canReach`, also in Diligence's charge); champions from old saves
  or dev summons (`home` -1) still spawn HUNTING, announce + beckon on their first act; 2 telegraphed skills each; drop the
  region reward; nemesis: +25% damage both ways via `VirtueHooks.modifyDamage` in `Char.damage`, own lines (`_own`),
  +50 sin points + scroll of upgrade). Listed in the bestiary's QUEST group (so dev-summonable). Art: the owner's paladin/saint
  pictures in `tools/sinsart/virtue_art/src/` -> `sprites/virtue_champions.png` (48x49, row 0 paladin = patience/temperance/
  diligence, row 1 saint = the rest; background cut out, shrunk, outlined) by `tools/sinsart/make_virtue_sprites.py`;
  `VirtueSprites.ChampionSprite` draws them over 3x3 tiles like the demon lords. The archangels have the owner's
  art too (`virtue_art/src/angel_<name>.png` -> `sprites/virtue_angels.png`, 64x65 frames, one row per angel in Virtue
  order, `VirtueSprites.forAngel` / `AngelSprite`). Strings `tools/strings/virtues_*`. Phase 2: the final boss
  `actors/mobs/virtues/ArchangelBoss` (BOSS, 650 HP; `virtue` field, `Virtue.angel()` names: Michael/humility,
  Raguel/charity, Gabriel/chastity, Uriel/patience, Raphael/temperance, Jeremiel/kindness, Sariel/diligence) is put
  before the amulet by `LastLevel.createMobs` -> `VirtueHooks.onAmuletLevel` (`angelFor`: the hero's nemesis, else the
  virtue opposing its demon pact, else Michael; `angelStage` 0/1/2 saved); `Amulet.doPickUp` refuses while it lives
  (`amuletGuarded`). Wakes when the hero comes within 6; Judgment (3x3, blind), Holy Lance (line 9), a virtue
  signature; ascends at 2/3 and 1/3 HP (barrier, then faster skills); nemesis kill +100 sin points. Both champions and
  the archangel implement `virtues/VirtueFoe` (nemesis damage). Dev tools: "대천사 소환".
- The heavens, floors 27-30 (Nov 2026): the archangel's death calls `LastLevel.openHeaven` (EXIT at `HEAVEN_STAIRS`, row 8
  above the amulet, + REGULAR_EXIT transition; re-applied in `LastLevel.restoreFromBundle` when `angelStage` >= 2).
  `Dungeon.newLevel` builds `levels/HeavenLevel` for 27-30 (RegularLevel, city tiles/music, `HeavenPainter` = CityPainter
  with a fixed wall-deco rate, 10-13 standard + 3-4 special rooms, mob limit 5-7 before groups; floor 30 fills chasms
  and removes its exit). The four room chance tables (Standard/Connection/Entrance/ExitRoom) are 31 long with rows 27-30
  = the sum of the sewer/prison/caves/city rows; `SecretRoom.secretsForFloor` returns 2-3 past 25, TrapsRoom/WeakFloorRoom
  clamp their region index. Mobs (`actors/mobs/heaven/`, stats from `HeavenMobs.depth()` = max(26, scalingDepth), saved
  as `depth`): `Believer` (groups of 2-3: HeavenLevel adds 1-2 companions to each believer/priest at build and on
  respawn; death cry aggroes enemies within 3), `Priest` (heals 1/6 HT + Barrier to allies within 4 every 5 turns; when
  hunting with the hero in view and no ally within 6 it dies and a `LesserAngel` takes its place), `LesserAngel`
  (IMMOVABLE, ranged spear via `HeavenSprites.LesserAngel` zap, `Radiance` shield losing 1/8 per turn, dies after 20
  turns), `Seraph` (MINIBOSS, 35% per floor in a 7x7+ standard room via `HeavenLevel.placeSeraph`; 2x2: own cell = top-left
  + 3 invisible `SeraphPart`s forwarding damage like LeviathanCoil; moves as a block inside its room bounds
  (`fits`/`getCloser`), burns non-enemies adjacent to the block each turn, reach 2 from the nearest body cell, one
  light-chain pull without cripple; `ensureBody` gives dev-summoned ones parts and the whole floor). All four are in the
  bestiary's QUEST group. Rotation in `MobSpawner` case 27-30. Art: the owner's pictures `virtue_art/src/heaven_*.png` -> 24px
  sheets for believer/priest/lesser angel (`HeavenSprites.Sheet24`; the angel has a bow, so it shoots a spirit arrow)
  and the 32px seraph (over 2x2 via `worldToCamera`, + blank frame 12 for parts), by `make_virtue_sprites.py`.
  Strings in `tools/strings/virtues_*`. Tileset (27-31, also HolyGiantLevel): `environment/tiles_heaven.png` +
  `water_heaven.png` (`Assets.Environment.TILES_HEAVEN/WATER_HEAVEN`) by `tools/sinsart/make_heaven_tiles.py`: the city
  sheet's layout recoloured (stone -> cream marble, wood -> gold, floor -> pale marble, blacks kept) with floor/deco/special
  floor/chasm tiles cut from the owner's mock-up `heaven_art/heaven_map.png` (~31px tiles; boxes in the script).
  Headless level test trick: build `HeavenLevel` subclasses without special/secret rooms and a texture-free
  `TextureFilm` stub on the classpath (Level.create needs textures for items).
- Floor 31, the holy giant (Nov 2026): floor 30's stairs (kept; its chasms are filled) lead to `levels/HolyGiantLevel`
  (29x25, city tiles, `bossLevel(31)` = true; rows 1-5 are a void of CHASM that is neither passable nor a pit; 2x2 wall
  blocks as cover; giant at `giantCell()` on the ledge row 6, gem at `centerCell()`). `actors/mobs/heaven/HolyGiant`
  (RegionElite, BOSS, IMMOVABLE, body immune: `damage` is a no-op; HP = sum of `armHP` (2 x `ARM_HT` 300); starts when the
  hero is within 10 and sees it; `GiantHunting` waits while the hero hides). Skills: SLAM (whole arm: a 3x`ARM_LEN` 8
  strip from the ledge down through the hero's column (`pendingCol`), only if the hero is within those rows; then a
  `GiantFist` stays FIST_TURNS 3 at the strip's far end, its column up to the ledge filled with invisible `GiantArmPart`s
  that forward hits (`GiantFist.parts`, cleared on retract/death); the fist's sprite `HeavenSprites.GiantArm` draws the
  whole 48x128 arm up from its tile; passive mob whose damage updates the giant via `onFistDamaged`, retract keeps the HP,
  0 HP = arm destroyed), BOLT (the owner's branching lightning `giant_art/lightning.png` -> `GiantLightningPattern`
  (21x17 tiles, generated) from the ledge down, shifted +-2 and randomly mirrored (`boltX/boltMirror`), paralysis 2;
  `HeavenSprites.lightning` shows the picture faintly while marked and fully on release). Judgment at 1/2 and 1/4 HP:
  3-turn charge, then hits every non-enemy char in its line of sight (`Ballistica` STOP_SOLID). Both arms gone ->
  `collapse`: `items/HolyGem` dropped at the centre, `SWARM` 14 believers/priests from the edges hunt the hero; picking
  the gem up calls `vanquish` (all enemies vanish, bossSlain, giant dies). The gem: AWAKEN (sin classes, once:
  `sins/SinAwakening` CONTROL/MONSTER stored as `Hero.awakening` + `awakeningPoints` = 8; path names
  `sins.sinawakening.*`) and END (`Dungeon.win(Amulet.class)` + delete save + RankingsScene). Art: the owner's giant and fist
  pictures (`tools/sinsart/giant_art/holy_giant.png`, cropped at the waterline; `fists.png`, two panels) ->
  `sprites/heaven_giant.png` (112x63 frames, drawn up from its ledge cell) and `sprites/heaven_fists.png` (32x33, row 0
  left / row 1 right by `GiantFist.side`, now unused), `sprites/heaven_arms.png` (48x129, fist turned down + mirrored
  forearm bands) and `sprites/heaven_lightning.png` by `tools/sinsart/make_giant_sprites.py` (reuses make_virtue_sprites' cutout); gem icon =
  items.png row 38 +0 (`ELITE2`, TX_HEIGHT now 608). Dev tools: travel to 27 / 31. Awakening talents (agreed with the owner):
  `sins/AwakeningTalent` (42: 3 per path, max 3 points, levels in `Hero.awakeningTalents`, per-run counters in
  `Hero.awakeningCounters`, both saved in the hero bundle), spent in `windows/WndAwakening` (the gem's TALENTS action;
  the gem can't be dropped after awakening). Effects live in `sins/AwakeningHooks` (call sites marked "Seven Sins:
  awakening"): path drawbacks (greed monster -25% HT + shops refuse; wrath monster armor x0.5 and regen/Healing x0.5;
  pride monster +20% damage taken below full HP; lust monster regen/Healing x0.5; envy monster -20% HT and hurt each
  turn with an empty gauge and enemies in view; sloth monster melee x0.5) and talents (hooked in PredatorStomach.score,
  PredatorMaw.hungerCost/bite, GreedVault interest, Shopkeeper.sellPrice, GreedHooks, WarDrum costs, WrathGrudge cooling,
  WrathHooks.onAttack, Char.damage (`onHeroDamaged`, `wrathUndying`), PrideGlory, PrideHooks.onAttack, Hero.attackSkill
  (`divineEye`), LustDesire, LustHooks, EnvyCovet/EnvyMimicry/EnvyHooks, SlothRepose, DreamPuppet, SlothHooks, Mob.die
  (`onMobDeath`)). `AwakeningHooks.AwakeningTicker` (buff added at the awakening) runs the lust aura and envy's drain.
  Gluttony's monster (`swallowBags` at the awakening, `onPickedUp` from Item.doPickUp): side bags except the magical
  holster (owner's call) become growth
  (150 each + contents); items of a swallowed bag's kind go off on pickup (`Potion.forceDrink`, `Scroll.forceRead`,
  seeds eaten, runestones/thrown weapons/wands digested); +50% move/attack speed; bite reach 2.
- Boss damage softcap (Nov 2026): `RegionElite.soften(dmg, softThreshold(HT))` (threshold HT/12, min 12; past it only
  ~sqrt(2 x excess), like the vanilla Pylon cap) in `VirtueChampion.damage`, `ArchangelBoss.damage`, `DemonLordBoss.damage`
  (Leviathan too) and `GiantFist.damage` (threshold from `ARM_HT`).
- Stairs past depth 26: `Hero.handle` only allowed entrances at depth >= 26; REGULAR_EXIT is allowed too now (26's heaven
  stairs and the exits of 27-30).
- Nov 11 2026 batch (owner's list):
  - Growth copy (`skills/GrowthCopy`, every sin, skill id `replicate`, icons skill_icons.png row 9 = 72 + sin index):
    pick an identified scroll of upgrade / potion of strength / scroll of enchantment, pay a full gauge, get another.
    No use limit (owner: stronger monsters will balance it). Costs: gluttony `PredatorMaw.COPY_COST` 300 satiety, greed
    the whole `maxCharge`, wrath 100 rage, pride 10 dignity, lust 50% max HP (`Perfume.COPY_SHARE`), envy 100 jealousy,
    sloth `turnCost(150)` doze. Strings `skills.growthcopy.*` + `skill_replicate(_desc)` per item.
  - Wrath: War Cry (`WarDrum.rally`, skill `rally`, icon 20): free, 30-turn `WarDrum.RallyCooldown`; enemies within 8 are
    beckoned and `SlothRepose.Taunted` to the hero for 10 turns, +5 rage each (max 30). Wrath's stat growth is back to the
    original rates (melee 0.10, HP 0.5, armor 0.06 per grudge point; the halvings no longer apply to Wrath).
  - Signature items ascend at +10 (`SinArtifact.ascend`, from `gainExp` and `onSpent` for older +10 items): replaced by the
    `MirrorRelics` reflection of the class (state copied through a bundle, same slot/quickslot, passive buff re-activated)
    and `MirrorRelics.markMirrored` switches the buff's `mirrored` bonuses on. Relic bonus strings were rewritten to match
    the current effects.
  - Sloth: dream sight sightings don't interrupt rest (`SlothHooks.spotted` from `Hero.checkVisibleMobs`: an enemy only in
    `mindVisionEnemies` and seen by a puppet isn't "new"; seeing it with the hero's own eyes later still interrupts).
    SLOTH_RESTFUL ("restful sleep") is now: puppet kills drop no loot (`SlothHooks.dropsLoot` in `Mob.die`) but each feeds
    15/30 satiety and cures one/all harmful states (`restfulSleep`, `RESTFUL_CURES`); its old doze reduction is gone.
    Dream Forge has no once-per-floor limit (`forgedHere` always false, `forgedDepths` kept for saves).
  - Greed: gilding cost capped at `GoldenCard.GILD_MAX` 600.
- Remains items for every sin (`items/remains/`: MawFang, MeltedCard, TornDrumhead, CrackedGem,
  BrokenPerfume, MirrorShard, PillowStuffing; dispatched in `RemainsItem.get`; art row 36 +5..11).
  CrackedGem's `Ward` buff is checked in `Hero.defenseProc`.

Not yet done / known gaps:
- Never play-tested on a device until the first APK; expect UI/layout fixes.
- Balance is untuned (Hibernator armor, Pride's Duel, Banker interest are on the strong side).
- Sigil costs 2000 favor (lowered from 3000, the quest maximum, in Nov 2026).
- Talent/hero icons for the sins are recolored cleric icons (placeholders).

Design rule for every sin: progress is **permanent and unbounded** (stat bonuses scale with
a points value that never decreases; accuracy/evasion are capped at 40%/30%).
Melee/max HP/armor growth from sin points was halved again for all seven sins (Wrath restored to its original rates in Nov 2026) (Nov 2026, in each buff's
`meleeBonus/hpBonus/drMax`; accuracy/evasion and unique stats unchanged). Later in Nov 2026 the six non-Pride
`meleeBonus()` values were cut to 75% and now apply to thrown weapons too ("weapon damage"; Gluttony's thrown path is
`PredatorHooks.onThrownAttack`; Sloth's x0.7 penalty stays melee-only). Max HP growth from sin points was halved (Nov 2026), and starvation damage scales with `Hero.starvationHT`
(max HP before the sins' bonus, set in `updateHT`), not the full HT.
Exception (owner's call): Greed's stats follow the gold currently carried, so shopping lowers them.
Greed's card has no deposit/withdraw; payment skills and Remote Sale (shop price, anywhere)
spend a gauge filled 1:1 by gold pickups (`GreedHooks.selling` keeps sale proceeds from filling it).
Greed also has Mail Order (`GreedVault.catalog`, 8 consumables restocked per floor, gold at shop price +
gauge fee) and Gilding (+1 upgrade for 300 + 150 per previous use of gauge, `GreedVault.gilded`).
Sloth (reworked Nov 2026, "auto-battle"): `actors/mobs/npcs/DreamPuppet` allies (DirectableAlly that roam the
level on their own via a `Roaming` wandering state, `intelligentAlly = false`; they vanish when the hero changes floors (`Mob.holdAllies` drops them); max puppets +1 at pillow +3/+5/+10 (`SlothRepose.pillowPuppets`); `nightmare` = temporary 70% copy). Taunt = `SlothRepose.Taunted` on an enemy, read by
`SlothHooks.tauntTarget` in `Mob.chooseEnemy`. Sleepwalker/Auto Hunt free attacks run in `SlothRepose.act`.
Pillow skills also include Order (`SlothRepose.DreamMark` on one enemy: every puppet hunts it via
`DreamPuppet.chooseEnemy`/`act`, +25% damage, 30 turns) and Dream Forge (`SlothRepose.DreamForge`: paralysed deep sleep
of 200 + 25/upgrade turns (max 300), then +1 to the equipped weapon or armor; any damage wakes and cancels it via
`SlothHooks.modifyDamage`; no floor limit any more; can't start with enemies in view).
Sloth's own melee deals x0.7 (`SlothHooks.MELEE_PENALTY`, not for Sleepwalker auto-attacks). Puppets have no armor
(`DreamPuppet.drRoll` = 0) and each non-nightmare puppet borrows 15% of Sloth's max HP (`SlothRepose.puppetDrain`,
max 75%, applied in `SlothHooks.bonusHT`; `refreshDrain` re-runs `updateHT(false)` on summon and each turn when the count
changes). Talents SLOTH_COMFY is now "dream sight" (+1 tiles within 2 of puppets, +2 puppets' full FOV, via
`SlothHooks.revealAroundPuppets/sharesSight` in `Level.updateFieldOfView`) and SLOTH_SHOVE is "light dreams" (-25%/pt drain).
Old sloth talent names are mapped in `Talent.renamedTalents`; old subclass saves fall back to none.
Gluttony (reworked Nov 2026, "eats anything"): no more equipment devouring (that moved to the conquest gun).
`PredatorStomach.consume` runs from `PredatorHooks.onConsumed` for potions (`Potion.drink`), scrolls
(`Scroll.readAnimation` -> `onScrollRead`), food (`Food.execute` -> `onFoodEaten`), runestones
(`Runestone.onThrow`, `InventoryStone.useAnimation`) and eaten seeds (`Plant.Seed`, AC_EAT_SEED). Pools keep
their old field names: weaponPts = potions, armorPts = food/seeds, trinketPts = scrolls/stones. New kinds go in
the `codex` (class names) with milestones 5/10/20/30/40/50/60/70/80 (`PredatorStomach.CODEX_*`; 70 = Bite lifesteal x2,
80 = 5% sin essence per consumable) plus the "sin gourmet" perk for tasting all 9 `MOD_ALCHEMY` items (+20% points).
The codex (105 entries in `PredatorStomach.sections()`; seeds skip rotberry, food is the 11 kinds every run can find: no berry/supply ration; `Item.info` appends `PredatorStomach.codexNote` = tasted / not yet / identify first) is read from the maw's CODEX action in `windows/WndScrollMessage`
(scrolling; Envy's codex uses it too). Mod alchemy items report to `PredatorHooks.onConsumed` when used/thrown and
are mapped in `PredatorStomach.category`. Harmful potions and
scroll of rage become `GluttonImbue` buffs (`PredatorHooks.drinkOverride/readRage`), psionic blast has no
backlash. Maw skills: Bite, Hunger Dash (haste), Acid Spray, Swallow Whole (free, 3x points, identifies),
Ruminate (repeat `lastUsed`; growth items that fail `PredatorHooks.canRepeat` can be ruminated too as "heavy
rumination": flat 600 satiety, `PredatorMaw.payHeavy` empties the stomach and puts the rest into
`actors/buffs/GluttonDebt`, which Hunger.affectHunger repays first and which keeps Weakness/Vulnerable/Hex up; brews never); satiety costs via
`PredatorMaw.hungerCost`, except Bite, which costs 6% max HP (`biteHpCost`, never lethal) and fills 80 satiety
on a kill (`BITE_FEED`). PREDATOR_FANG ("hungry fang") now gives satiety per consumable (`FANG_CONSUME` x pts, in
`PredatorStomach.consume`) and per kill (`FANG_KILL` x pts, `PredatorHooks.onMobDeath` from `Mob.die`); the old
`PredatorStomach.Fang` damage buff is no longer applied. Talent enum names are unchanged (meanings and strings changed); INHERITOR is now
shown as the "alchemic glutton". Old Inheritor enchant/glyph lists are kept in saves but unused.
Pride is a ranged class (Nov 2026): starts with a set of throwing knives in quickslot 0, crown in slot 1; older
Pride saves get a knife set once via `PrideGlory.bowGiven` (unless they already got the bow). `items/weapon/RoyalBow`
(SpiritBow subclass) was the first starter and is kept only for saves that received it. `PrideHooks.onAttack` gives glory's flat bonus
(`meleeBonus`, name kept) and the dignity multiplier to MissileWeapon attacks only, and melee deals x0.7
(`MELEE_PENALTY`). KingsAegis: +30% ranged damage (`RANGED_BONUS`) and +5 max dignity (no more glory bonus).
Paragon-only crown skill Effigy (`actors/mobs/npcs/PrideEffigy`, immovable ALLY statue, taunts enemies within 4 via
`SlothRepose.Taunted`, 20 turns, excluded from `PrideHooks.alone`).
Skill resources (Nov 2026): Wrath spends **rage** (`WrathGrudge.rage`, 0..maxRage 100+, filled by hits dealt
(+4) and damage taken (100 per max HP), cools 1/turn with no enemies in view; old charge sources convert at
1 charge = 25 rage). Pride spends **dignity** stacks (`PrideGlory.useDignity`; dignity also gives +4% melee per
stack and shatters when hit); Disdain = 1-dignity ranged bolt, x dignityMult, kills count as flawless via
`PrideHooks.heroCaused` accepting `PrideCrown.class`; Disdain gets only half the dignity bonus via
`PrideCrown.disdainMult`). Pride's crown also has Kneel Back (`repel`, 2 dignity: enemies within 2 thrown 3 tiles + Vertigo).
PRIDE_SWIFT_CROWN is now "royal retrieval": 25%/pt that a thrown weapon returns to the pack (`PrideHooks.swiftReturn`,
called from `MissileWeapon.rangedHit/rangedMiss`). Pride starts with a magical holster (and the velvet pouch). Sloth pays in **time**: each pillow skill ends with `SlothRepose.doze(hero, turns)`
(Doze buff + spendAndNext; `turnCost` scales with level and SLOTH_RESTFUL; a meal with SLOTH_PILLOW skips
dozing). Cost types RAGE/DIGNITY/TURNS/JEALOUSY in `SinSkill.CostType`.
Envy (reworked Nov 2026, "copycat"): no skills of its own. `envy/EnvyMimicry` lists `Mimic`s: 12 copies of other
sins' skills (unlocked by hero level 2,4..24) and 8 monster abilities (unlocked by bestiary traits); their own
implementations live there (not the other sins' code). The mirror holds `EnvyCovet.slots` (3 + Copycat +
mirrored), chosen in `EnvyMirror.chooseMimic`; newly learned copies fill free slots (`refreshSlots`).
Paid from `EnvyCovet.jealousy` (0..maxJealousy, ~3 turns per point, x2 with a rival in view). `EnvyCovet.growth`
imitates another sin's growth at 50% (+15%/pt ENVY_SWITCH) in `EnvyHooks` (gluttony via
`PredatorHooks.onConsumed`, greed = carried gold in `pts()`, wrath/pride/sloth kills in `mimicKill`, lust
lifesteal in `onAttack`). Robbed stacks are gone (`EnvyCovet.Robbed` kept only for old saves, detaches
itself); Envy's own envy comes from new bestiary entries (`EnvyHooks.registerEnvy`). USURPER is now shown as the
**Doppelganger**: imitates two growths (`growth2`, `secondEfficiency`), sin copies -25% cost. Talent enums
unchanged, meanings changed. `WndHeroInfo` tolerates more description paragraphs than icons.
Monster ability icons are row 7 of skill_icons.png.
Lust (reworked Nov 2026, dancer + vampire): perfume skills cost HP (`LustDesire.hpCost/pay`), lifesteal
on charmed enemies via `LustDesire.drain` from `LustHooks.onAttack`; dodging charms the attacker
(`LustHooks.onDodge`, called from the miss branch of `Char.attack`; Night Dancer counters there).
Siren infighting is gone. Old lust talent names are mapped in `Talent.renamedTalents` too.

Meal talents (Nov 2026): each sin's first tier-2 talent (PREDATOR_GLUTTONY, GREED_HOARD, WRATH_DRUMMER,
PRIDE_REGALIA, LUST_BOTTLE, ENVY_MIRROR, SLOTH_PILLOW) makes eating take 1 turn and refills the sin's resource
(`sins/SinMeals`, from `Talent.onFoodEaten` / `Food.eatingTime`); their old effects are gone. Sloth's meal gives
`SlothRepose.Refreshed` (next skills skip dozing). Envy's gauge max = 100 + 10/mirror level + 25/pt ENVY_POLISH
(`EnvyCovet.maxJealousy`).

Unique stats (one per sin, on top of the shared five; all in `sins/UniqueStats`, scaled by the sin's
points, capped): Wrath attack speed (Hero.attackDelay), Gluttony HP regen (ticked from Hunger.act),
Greed loot chance (Mob.lootChance), Pride debuff resistance (Char.resist, negative buffs only),
Lust move speed (Hero.speed), Envy rare variant spawns (x up to 4, via `RatSkull.exoticChanceMultiplier`;
at the cap, kills have a 5% chance of 3 turns of mind vision), Sloth slower hunger (Hunger.act).
Each sin buff's `statsText()` appends `UniqueStats.line(target)`.

## Skills and the skill bar

Every sin's signature item implements `skills/SkillHolder` and lists its active skills as
`skills/SinSkill`s (usually `BasicSkill` built from lambdas: cost type/amount, usable, onUse, desc args).
The same list drives the item's skill window (`windows/WndSkills`, name + cost + description) and the
skill bar (`ui/SkillBar`, icon buttons right above the quickslot toolbar, right-aligned; tap = use, long-press = info).
`GameScene.syncSkillBar` keeps it above the toolbar; tags/log are laid out from `toolbarAreaTop()`.
Adding a skill: add a `BasicSkill` to the item's `skills()`, strings `skill_<id>` and `skill_<id>_desc`
in the item's namespace, and an icon in `tools/sinsart/make_skill_icons.py`
(`interfaces/skill_icons.png`, 16x16, row = sin index, 8 per row; re-run the script).
Auto-aim: `BasicSkill.targetsEnemy()` / `.projectile()` mark targeted skills; after the first tap the bar picks
`SkillBar.autoTarget()` (last quickslot target, else nearest visible enemy), shows a crosshair, and a second tap
fires (`GameScene.handleCell`, projectiles via `QuickSlotButton.autoAim`); `startAiming` also calls `QuickSlotButton.target`
(health bar) and makes fresh crosshair images each time (a killed Gizmo never draws again). The conquest gun uses the quickslot's
`usesTargeting`. Cost labels live under `skills.sinskill.cost_*`. Two-step cell selections must open the second
selector on the next frame (`Game.runOnRenderThread`), because the scene resets the selector after
each pick (that was the Discord bug).

## Four horsemen artifacts and the sewer prophet

Reworked for combat (Nov 2026). The prophet now gives `items/weapon/missiles/ConquestSpear` instead of the gun
(tier 4 thrown set of 2, `conquest` points from CONQUERed gear = +1 min/10 and +1 max/5, refills durability and
reforges a lost spear, takes good enchantments; casting molds count 1/3 via `pourMold`; reuses `ConquestCrown.bulletsFor`;
art items.png row 37 +13). `items/artifacts/HorsemanArtifact` (shared: 0-100% charge, full charge per use,
levels 0-10 by uses, never cursed; `timeCharges()` off for the gun) with `ConquestCrown` (class name kept for
saves, now the **gun of conquest**: loads bullets by devouring unequipped weapons/armor (tier+2*lvl each), whole thrown-weapon stacks (half that per weapon) and rings (4+3*lvl),
keeps the last good enchantment for its bullets; fire = one bullet, ranged hit, drawn as `effects/Bullet` (2px black, 2400px/s); levels by shots (damage only),
no magazine limit (`maxAmmo` = Integer.MAX_VALUE); `Subjugated` kept for old saves), `FamineScales` (hunger factor 0..1 at HUNGRY, 1.25 starving,
1.5 in `Frenzy`: attack speed/move speed/damage bonuses; power burns satiety down to HUNGRY, heals, Frenzy)
and `DeathScythe` (hits leave `DeathScythe.Mark`s = dmg*rate; reaped when marks >= HP via
`HorsemenHooks.afterDamage` in `Char.damage`; bosses burst at 15% HT; power bursts all marks in sight;
survive a lethal hit once per floor). War is now `items/WarEdge` ("sword of war", Nov 2026): fused onto any melee weapon like the
broken seal (`Weapon.warEdge`, saved in the weapon bundle, cleared by `reset` for bones, copied by transmutation and
the Sigil of Sin; `MeleeWeapon` AC_DETACH_WAR takes it back off with its stacks). Adds Random(stacks/36..stacks/12)
to melee hits with the equipped weapon (`HorsemenHooks.onAttack`); kills with it add stacks (each sword level +20% per
kill; bosses x10, minibosses x3). The prophet gives it instead of the old `items/weapon/melee/WarSword` weapon, which
old saves keep and can REFORGE into a WarEdge (stacks/kills/level carried over; the sword's own enchant is lost).
The old `WarBlade` artifact is kept only for old saves.
Hooks: `horsemen/HorsemenHooks` (Hero.attackProc/defenseProc/die/speed/attackDelay, Char.damage, Mob.die).
Quest: `actors/mobs/npcs/Prophet` (+ `Prophet.Quest`, saved with the other quests in `Dungeon`) spawns on
sewer floor 2-4: the roll happens in `SewerLevel.initRooms` (`Prophet.Quest.spawnRoom` adds
`levels/rooms/quest/SealSanctumRoom`, 11x11 dead end, 4 statues, pedestal) and `Quest.spawn` (from `createMobs`)
puts the miniboss `actors/mobs/SealWarden` on the pedestal (`home`). The warden (60 HP) never leaves the arena,
walls up the door (`sealedDoors`) once it hunts a hero inside, reopens it on death or when the hero is gone.
It shifts aspect every 25% HP lost (Conquest armor, War 1.5x attack speed, Famine weakness + satiety drain,
Death +50% vs a hero below half HP) and each aspect adds a telegraphed skill (`TargetedCell` marks, released on
its next turn; skills accumulate, newest preferred): Volley (plus shape on the hero), Charge (straight dash
through the hero, cripple), Blight (3x3, weakness/hunger, heals it), Reap (all 8 cells around it).
Wardens from old saves have `home = -1` and fight without an arena. Placeholder art = tinted armored statue. It drops one
`items/quest/SealFragment` (shown as "seal of the apocalypse"); `Prophet.FRAGMENTS` = 1 -> `WndProphet`,
pick one artifact. `SealBearer` (the old 4-champion version) is kept only for old saves. Art: items.png row 36.

## The seven sins

| Sin | Enum | Signature item | Buff (progress) | Hooks | Subclasses | Armor abilities |
|---|---|---|---|---|---|---|
| Gluttony 식탐 | GLUTTONY | PredatorMaw | PredatorStomach (consumable points + gourmet codex) | predator/PredatorHooks | GOURMAND, INHERITOR | abilities/predator/* |
| Greed 탐욕 | GREED | GoldenCard | GreedVault (stats from carried gold; payment gauge filled by gold pickups) | greed/GreedHooks | BANKER, USURER | abilities/greed/* |
| Wrath 분노 | WRATH | WarDrum | WrathGrudge (grudge from infighting) | wrath/WrathHooks | INSTIGATOR, AVENGER | abilities/wrath/* |
| Pride 교만 | PRIDE | PrideCrown | PrideGlory (glory from flawless kills, dignity) | pride/PrideHooks | MONARCH, PARAGON | abilities/pride/* |
| Lust 색욕 | LUST | Perfume | LustDesire (life stolen; 0.25 desire per HP) | lust/LustHooks | NIGHT_DANCER, CRIMSON_NOBLE | abilities/lust/* |
| Envy 시기 | ENVY | EnvyMirror | EnvyCovet (new bestiary entries, imitated growth, mimic slots) | envy/EnvyHooks | USURPER (doppelganger), COPYCAT | abilities/envy/* |
| Sloth 나태 | SLOTH | SlothPillow | SlothRepose (kills by dream puppets / auto-attacks; own kills 1/3) | sloth/SlothHooks | DREAM_LORD, SLEEPWALKER | abilities/sloth/* |

Paths are under `core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/`
(items in `items/`, buffs in `actors/buffs/`, abilities in `actors/hero/abilities/`).
Charge-based sins keep their item charges inside the buff. Each buff has a `mirrored` flag
set by the old vault mirror relics (`items/MirrorRelics.java`, no longer handed out).
Sin weapons: `items/weapon/melee/sins/` (`SinWeapons` helper, each extends a base weapon).

## How the mod plugs into the game

Each sin has a `*Hooks` class with static pass-through methods. Call sites (search for `Hooks.`):
- `Hero`: `updateHT` (bonusHT), `attackSkill` (accuracy), `defenseSkill` (evasion), `drRoll`,
  `attackProc` (onAttack), `defenseProc` (onDefense), `die` (Greed insurance).
- `Char.damage`: `modifyDamage` before damage (Wrath, Pride, Envy, Sloth Proxy redirect), `afterDamage` after HP is reduced.
- `Mob.die`: `onMobDeath`; `Mob.chooseEnemy`: Wrath discord / Sloth taunt targeting;
  `Char.attack` miss branch: Lust dodge.
- `Gold.doPickUp`: bonus gold. Per-class switches: `HeroClass`, `Talent` (trees + heroic energy
  icon), `ClassArmor.upgrade`, `Icons.get`, `WndHeroInfo`, `Badges` (null-safe for new classes),
  `VaultMirror`, `WndBlacksmith`.

Adding a new class means touching all of the above; follow an existing sin as the template.

## Assets

`tools/sinsart/make_sin_assets.py` regenerates all placeholder art from the Cleric's art
(hero sheet, splash, avatar, talent/hero icons, signature items, class armor, sin weapons,
mirror relics). Re-run after editing it: `python3 tools/sinsart/make_sin_assets.py`.
Reserved layout (must match the Java constants):
- `talent_icons.png`: row 8 + k (icon 256 + 32k + i), heroic energy at +26
- `hero_icons.png`: row 16 + k (icon 128 + 8k): sub1, sub2, ability 1-3 (`HeroIcon`)
- `items.png` (TX_HEIGHT in `ItemSpriteSheet` must equal the PNG height, now 608):
  row 33 signature item/class armor pairs, row 34 sin weapons + sigil, row 35 mirror relics,
  row 36 horsemen relics (gun, war sword, scales, scythe) + seal fragment (+0..4), sin remains (+5..11), mirror case (+12),
  row 37 sin essence, elixir of sin, sin bomb, sticky bomb, bandage, stimulant (`elite_art`), ... heavenly halo (+14), artifact shard (+15)
- `avatars.png`: frame = HeroClass ordinal (sins are 6..12)
k = sin index in `SINS` (gluttony 0, greed 1, wrath 2, pride 3, lust 4, envy 5, sloth 6).

## Strings

English and Korean only. Source copies live in `tools/strings/*.properties` and are appended to
`core/src/main/assets/messages/{actors,items,windows}/*.properties` under
`###### Seven Sins: ... ######` headers. After editing a source copy, run `python3 tools/strings/sync.py` to update the game files
(new keys land after the key that precedes them, a new file gets its own section at the end;
delete removed keys by hand). Keep both languages in sync; a missing key shows as
"!!!NO TEXT FOUND!!!" in game. Messages fall back to superclass keys, so sin weapons inherit
their base weapon's ability text. Gluttony's strings predate the source copies; only the keys in
`gluttony_*.properties` are mirrored there, the rest live only in the game files.

## Building and checking

- Normal PC: open in Android Studio, or `./gradlew android:assembleDebug`.
- Phone testing: push to GitHub; `.github/workflows/build-apk.yml` builds and publishes the APK.
- Versions (Nov 2026): the mod has its own version in `build.gradle` (`appVersionName` 1.0.0, `appVersionCode` 1000,
  `spdBaseVersion` 4.0.2; the title screen and menu show "Seven Sins v<version>"). To release: raise both (code +1 each
  time, never lower), push, then push a tag `v<appVersionName>`: the workflow checks the tag matches and publishes a
  "Seven Sins <version>" release with `SevenSins-<version>.apk` (same debug build: dev tools on, installs over test builds,
  owner's call). Branch pushes still update `test-build` (`SevenSins-<version>-test-<sha>.apk`).
  Debug builds are signed with the committed `android/sevensins-debug.keystore` (password `android`),
  so new APKs install over old ones. Don't replace it, or the owner has to uninstall (and lose saves).
- Sandboxes without Maven/Google access: `bash tools/compilecheck/check.sh` compiles against
  libGDX built from GitHub sources (prints `exit: 0` on success). Headless logic tests can be run
  with `java -cp <out>/spdcls:<deps.jar>`; anything touching Messages/UI needs the real game.
- After adding items: regenerate the dev-tools item list with
  `CLASSES=<spdcls> CP=<deps.jar> bash tools/devitems/regen.sh`
  (on a PC: `./gradlew :core:compileJava && bash tools/devitems/regen.sh`).

Debug builds (version suffix -INDEV) unlock all classes and show the dev tools; they install
alongside official SPD (`.indev` package suffix).
