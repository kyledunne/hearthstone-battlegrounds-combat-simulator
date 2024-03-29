/**
 * Takes 2 players' board states,
 * returns damage done by player 1.
 * In case of tie, returns 0.
 * If player 2 wins, returns a negative number equal to player 2's damage
 */
fun simulate(p1board: BoardState, p2board: BoardState, p1Level: Int, p2Level: Int, doesP1goFirst: Boolean? = null,
             p1StartOfCombatEffect: StartOfCombatEffect? = null, p2StartOfCombatEffect: StartOfCombatEffect? = null): Int {
    var p1board = p1board
    var p2board = p2board
    //if null, set to random boolean
    var isPlayer1Turn = doesP1goFirst ?: RAND.nextBoolean()
    var nextToAttackP1 = 1
    var nextToAttackP2 = 1

    //TODO figure out how order is decided
    if (p1StartOfCombatEffect != null) {
        var boards = p1StartOfCombatEffect.trigger(p1board, p2board)
        p1board = boards.first
        p2board = boards.second
    }
    if (p2StartOfCombatEffect != null) {
        var boards = p2StartOfCombatEffect.trigger(p2board, p1board)
        p1board = boards.second
        p2board = boards.first
    }

    while (!(p1board.isEmpty() || p2board.isEmpty())) {
        var attackResult: Pair<Pair<BoardState, BoardState>, Pair<Int, Int>> = if (isPlayer1Turn) attack(p1board, p2board, nextToAttackP1, nextToAttackP2, true)
                                                                               else attack(p2board, p1board, nextToAttackP2, nextToAttackP1, false)
        p1board = if (isPlayer1Turn) attackResult.first.first else attackResult.first.second
        p2board = if (isPlayer1Turn) attackResult.first.second else attackResult.first.first
        nextToAttackP1 = if (isPlayer1Turn) attackResult.second.first else attackResult.second.second
        nextToAttackP2 = if (isPlayer1Turn) attackResult.second.second else attackResult.second.first

        isPlayer1Turn = !isPlayer1Turn
    }

    // if both boards are empty, it's a tie, return 0
    if (p1board.isEmpty() && p2board.isEmpty()) {
        println("Result: 0")
        return 0
    }
    // if only p2's board is empty, p1 wins, return a positive number equal to damage done
    if (p2board.isEmpty()) {
        var totalDamage = p1Level
        for (minion in p1board.board) {
            totalDamage += minion.type.level
        }
        println("Result: $totalDamage")
        return totalDamage
    }
    // if only p1's board is empty, p2 wins, return a negative number equal to damage done
    var totalDamage = p2Level
    for (minion in p2board.board) {
        totalDamage += minion.type.level
    }
    println("Result: " + ((-1) * totalDamage))
    return (-1) * totalDamage
}

/**
 * Returns a pair of pairs:
 *     Pair 1:
 *         First: attackerBoardState after the attack
 *         Second: defenderBoardState after the attack
 *     Pair 2:
 *         First: Attacking player's next attacking slot
 *         Second: Defending player's next attacking slot
 */
fun attack(attackerBoard: BoardState, defenderBoard: BoardState, attackingSlot: Int, nextDefendingSlot: Int, attackerIsP1: Boolean): Pair<Pair<BoardState, BoardState>, Pair<Int, Int>> {
    var nextAttackingSlot = attackingSlot
    var nextDefendingSlot = nextDefendingSlot

    var defenderSlot = RAND.nextInt(defenderBoard.numMinions()) + 1

    if (attackerIsP1) {
        MainClass.updateState(attackerBoard, defenderBoard, true, attackingSlot, defenderSlot)
    } else {
        MainClass.updateState(defenderBoard, attackerBoard, false, attackingSlot, defenderSlot)
    }

    var attacker = attackerBoard.get(attackingSlot)
    var defender = defenderBoard.get(defenderSlot)
    if (!(attacker.divineShield)) {
        attacker.health -= defender.attack
    } else {
        attacker.divineShield = false
        // trigger any attacking Bolvars
        for (minion in attackerBoard.board) {
            if (minion.type.lossOfDivineShieldEffect != null) {
                minion.type.lossOfDivineShieldEffect!!.trigger(minion)
            }
        }
    }
    if (!(defender.divineShield)) {
        defender.health -= attacker.attack
        // trigger any defending Bolvars
        for (minion in attackerBoard.board) {
            if (minion.type.lossOfDivineShieldEffect != null) {
                minion.type.lossOfDivineShieldEffect!!.trigger(minion)
            }
        }
    }
    if (attacker.health <= 0) {
        attackerBoard.remove(attackingSlot)
        if (attackingSlot > attackerBoard.numMinions()) {
            nextAttackingSlot = 1
        }
    } else {
        if (attackingSlot == attackerBoard.numMinions()) {
            nextAttackingSlot = 1
        } else {
            nextAttackingSlot += 1
        }
    }
    if (defender.health <= 0) {
        defenderBoard.remove(defenderSlot)
        if (nextDefendingSlot > defenderBoard.numMinions()) {
            nextDefendingSlot = 1
        }
    } else {
        if (nextDefendingSlot == defenderBoard.numMinions()) {
            nextDefendingSlot = 1
        } else {
            nextDefendingSlot += 1
        }
    }

    return Pair(Pair(attackerBoard, defenderBoard), Pair(nextAttackingSlot, nextDefendingSlot))
}

val RAND = java.util.Random()

class BoardState(slot1: BoardMinion, slot2: BoardMinion, slot3: BoardMinion, slot4: BoardMinion, slot5: BoardMinion, slot6: BoardMinion, slot7: BoardMinion) {
    var board: ArrayList<BoardMinion> = ArrayList()
    init {
        board.add(slot1)
        board.add(slot2)
        board.add(slot3)
        board.add(slot4)
        board.add(slot5)
        board.add(slot6)
        board.add(slot7)
    }

    fun isEmpty(): Boolean {
        return board.isEmpty()
    }

    fun numMinions(): Int {
        return board.size
    }

    /**
     * 1 through 7
     */
    fun get(slot: Int): BoardMinion {
        return board.get(slot - 1)
    }

    fun remove(slot: Int) {
        board.removeAt(slot - 1)
    }
}

class BoardMinion(var type: MinionType, var attack: Int, var health: Int, var divineShield: Boolean = false,
                  var taunt: Boolean = false, var poisonous: Boolean = false) {
}

class MinionType(val name: String, val level: Int, var tribe: Tribe = Tribe.NONE, var deathrattle: Deathrattle? = null,
                 var aura: Aura? = null, var summonEffect: SummonEffect? = null, var attackEffect: AttackEffect? = null,
                 var takesDamageEffect: TakesDamageEffect? = null, var deathEffect: DeathEffect? = null,
                 var startOfCombatEffect: StartOfCombatEffect? = null, var lossOfDivineShieldEffect: LossOfDivineShieldEffect? = null,
                 var anyFriendlyMinionAttacksEffect: AnyFriendlyMinionAttacksEffect? = null, val gold: Boolean = false) {
    companion object {
        var beastToken = MinionType("beastToken", 1, Tribe.BEAST)
        var demonToken = MinionType("demonToken", 1, Tribe.DEMON)
        var mechToken = MinionType("mechToken", 1, Tribe.MECH)
        var murlocToken = MinionType("murlocToken", 1, Tribe.MURLOC)
        var amalgamToken = MinionType("amalgamToken", 1, Tribe.ALL)

        var alleyCat = MinionType("alleyCat", 1, Tribe.BEAST)
        var direWolfAlpha = MinionType("direWolfAlpha", 1, Tribe.BEAST, aura=Aura.direWolfAlphaAura)
        var mecharoo = MinionType("mecharoo", 1, Tribe.MECH, deathrattle=Deathrattle.mecharooDeathrattle)
        var microMachine = MinionType("microMachine", 1, Tribe.MECH)
        var murlocTidecaller = MinionType("murlocTidecaller", 1, Tribe.MURLOC, summonEffect= SummonEffect.murlocTidecallerSummonEffect)
        var murlocTidehunter = MinionType("murlocTidehunter", 1, Tribe.MURLOC)
        var righteousProtector = MinionType("righteousProtector", 1, Tribe.NONE)
        var rockpoolHunter = MinionType("rockpoolHunter", 1, Tribe.MURLOC)
        var selflessHero = MinionType("seflessHero", 1, Tribe.NONE, deathrattle=Deathrattle.selflessHeroDeathrattle)
        var voidwalker = MinionType("voidwalker", 1, Tribe.DEMON)
        var vulgarHomunculus = MinionType("vulgarHomunculus", 1, Tribe.DEMON)
        var wrathWeaver = MinionType("wrathWeaver", 1, Tribe.NONE)

        var annoyOTron = MinionType("annoyOTron", 2, Tribe.MECH)
        var coldlightSeer = MinionType("coldlightSeer", 2, Tribe.MURLOC)
        var harvestGolem = MinionType("harvestGolem", 2, Tribe.MECH, deathrattle=Deathrattle.harvestGolemDeathrattle)
        var kaboomBot = MinionType("kaboomBot", 2, Tribe.MECH, deathrattle=Deathrattle.kaboomBotDeathrattle)
        var kindlyGrandmother = MinionType("kindlyGrandmother", 2, Tribe.BEAST, deathrattle=Deathrattle.kindlyGrandmotherDeathrattle)
        var metaltoothLeaper = MinionType("metaltoothLeaper", 2, Tribe.MECH)
        var mountedRaptor = MinionType("mountedRaptor", 2, Tribe.BEAST, deathrattle=Deathrattle.mountedRaptorDeathrattle)
        var murlocWarleader = MinionType("murlocWarleader", 2, Tribe.MURLOC, aura=Aura.murlocWarleaderAura)
        var nathrezimOverseer = MinionType("nathrezimOverseer", 2, Tribe.DEMON)
        var nightmareAmalgam = MinionType("nightmareAmalgam", 2, Tribe.ALL)
        var oldMurkEye = MinionType("oldMurkEye", 2, Tribe.MURLOC, aura=Aura.oldMurkEyeAura)
        var pogoHopper = MinionType("pogoHopper", 2, Tribe.MECH)
        var ratPack = MinionType("ratPack", 2, Tribe.BEAST, deathrattle=Deathrattle.ratPackDeathrattle)
        var scavengingHyena = MinionType("scavengingHyena", 2, Tribe.BEAST, deathEffect=DeathEffect.scavengingHyenaDeathEffect)
        var shieldedMinibot = MinionType("shieldedMinibot", 2, Tribe.MECH)
        var spawnOfNzoth = MinionType("spawnOfNzoth", 2, Tribe.NONE, deathrattle=Deathrattle.spawnOfNzothDeathrattle)
        var zoobot = MinionType("zoobot", 2, Tribe.MECH)

        var cobaltGuardian = MinionType("cobaltGuardian", 3, Tribe.MECH, summonEffect=SummonEffect.cobaltGuardianSummonEffect)
        var crowdFavorite = MinionType("crowdFavorite", 3, Tribe.NONE)
        var crystalweaver = MinionType("crystalweaver", 3, Tribe.NONE)
        var houndmaster = MinionType("houndmaster", 3, Tribe.NONE)
        var impGangBoss = MinionType("impGangBoss", 3, Tribe.DEMON, takesDamageEffect=TakesDamageEffect.impGangBossTakesDamageEffect)
        var infestedWolf = MinionType("infestedWolf", 3, Tribe.BEAST, deathrattle=Deathrattle.infestedWolfDeathrattle)
        var khadgar = MinionType("khadgar", 3, Tribe.NONE, summonEffect=SummonEffect.khadgarSummonEffect)
        var packLeader = MinionType("packLeader", 3, Tribe.NONE, summonEffect=SummonEffect.packLeaderSummonEffect)
        var phalanxCommander = MinionType("phalanxCommander", 3, Tribe.NONE, aura=Aura.phalanxCommanderAura)
        var pilotedShredder = MinionType("pilotedShredder", 3, Tribe.MECH, deathrattle=Deathrattle.pilotedShredderDeathrattle)
        var psychOTron = MinionType("psychOTron", 3, Tribe.MECH)
        var replicatingMenace = MinionType("replicatingMenace", 3, Tribe.MECH, deathrattle=Deathrattle.replicatingMenaceDeathrattle)
        var screwjankClunker = MinionType("screwjankClunker", 3, Tribe.MECH)
        var shifterZerus = MinionType("shifterZerus", 3, Tribe.NONE)
        var soulJuggler = MinionType("soulJuggler", 3, Tribe.NONE, deathEffect= DeathEffect.soulJugglerDeathEffect)
        var tortollanShellraiser = MinionType("tortollanShellraiser", 3, Tribe.NONE, deathrattle=Deathrattle.tortollanShellraiserDeathrattle)

        var annoyOModule = MinionType("annoyOModule", 4, Tribe.MECH)
        var bolvarFireblood = MinionType("bolvarFireblood", 4, Tribe.NONE, lossOfDivineShieldEffect= LossOfDivineShieldEffect.bolvarFirebloodLossOfDivineShieldEffect)
        var caveHydra = MinionType("caveHydra", 4, Tribe.BEAST, attackEffect=AttackEffect.cleave)
        var defenderOfArgus = MinionType("defenderOfArgus", 4, Tribe.NONE)
        var festerrootHulk = MinionType("festerrootHulk", 4, Tribe.NONE, anyFriendlyMinionAttacksEffect=AnyFriendlyMinionAttacksEffect.festerrootHulkEffect)
        var ironSensei = MinionType("ironSensei", 4, Tribe.MECH)
        var menagerieMagician = MinionType("menagerieMagician", 4, Tribe.NONE)
        var pilotedSkyGolem = MinionType("pilotedSkyGolem", 4, Tribe.MECH, deathrattle=Deathrattle.pilotedSkyGolemDeathrattle)
        var primalfinLookout = MinionType("primalfinLookout", 4, Tribe.MURLOC)
        var securityRover = MinionType("securityRover", 4, Tribe.MECH, takesDamageEffect=TakesDamageEffect.securityRoverTakesDamageEffect)
        var siegebreaker = MinionType("siegebreaker", 4, Tribe.DEMON, aura=Aura.siegebreakerAura)
        var theBeast = MinionType("theBeast", 4, Tribe.BEAST, deathrattle=Deathrattle.theBeastDeathrattle)
        var toxfin = MinionType("toxfin", 4, Tribe.MURLOC)
        var virmenSensei = MinionType("virmenSensei", 4, Tribe.NONE)

        var annihilanBattlemaster = MinionType("annihilanBattlemaster", 5, Tribe.DEMON)
        var baronRivendare = MinionType("baronRivendare", 5, Tribe.NONE, deathEffect=DeathEffect.baronRivendareDeathEffect)
        var brannBronzebeard = MinionType("brannBronzebeard", 5, Tribe.NONE)
        var greatWolf = MinionType("greatWolf", 5, Tribe.BEAST, deathrattle=Deathrattle.greatWolfDeathrattle)
        var ironhideDirehorn = MinionType("ironhideDirehorn", 5, Tribe.BEAST, attackEffect=AttackEffect.ironhideDirehornAttackEffect)
        var junkbot = MinionType("junkbot", 5, Tribe.MECH, deathEffect=DeathEffect.junkbotDeathEffect)
        var lightfangEnforcer = MinionType("lightfangEnforcer", 5, Tribe.NONE)
        var malganis = MinionType("malganis", 5, Tribe.DEMON, aura=Aura.malganisAura)
        var mechanoEgg = MinionType("mechanoEgg", 5, Tribe.MECH, deathrattle=Deathrattle.mechanoEggDeathrattle)
        var satedThreshadon = MinionType("satedThreshadon", 5, Tribe.BEAST, deathrattle=Deathrattle.satedThreshadonDeathrattle)
        var savannahHighmane = MinionType("savannahHighmane", 5, Tribe.BEAST, deathrattle=Deathrattle.savannahHighmaneDeathrattle)
        var strongshellScavenger = MinionType("strongshellScavenger", 5, Tribe.NONE)
        var theBoogeymonster = MinionType("theBoogeymonster", 5, Tribe.NONE, attackEffect=AttackEffect.boogeymonsterAttackEffect)
        var voidlord = MinionType("voidlord", 5, Tribe.DEMON, deathrattle=Deathrattle.voidlordDeathrattle)

        var foeReaper = MinionType("foeReaper", 6, Tribe.MECH, attackEffect= AttackEffect.cleave)
        var gentleMegasaur = MinionType("gentleMegasaur", 6, Tribe.BEAST)
        var ghastcoiler = MinionType("ghastcoiler", 6, Tribe.BEAST, deathrattle=Deathrattle.ghastcoilerDeathrattle)
        var kangorsApprentice = MinionType("kangorsApprentice", 6, Tribe.NONE, deathrattle=Deathrattle.kangorsApprenticeDeathrattle)
        var maexxna = MinionType("maexxna", 6, Tribe.BEAST)
        var mamaBear = MinionType("mamaBear", 6, Tribe.BEAST, summonEffect=SummonEffect.mamaBearSummonEffect)
        var sneedsOldShredder = MinionType("sneedsOldShredder", 6, Tribe.MECH, deathrattle=Deathrattle.sneedsOldShredderDeathrattle)
        var zappSlywick = MinionType("zappSlywick", 6, Tribe.NONE, attackEffect= AttackEffect.zappSlywickAttackEffect)

        //golden cards

        var goldAlleyCat = MinionType("alleyCat", 1, Tribe.BEAST, gold=true)
        var goldDireWolfAlpha = MinionType("direWolfAlpha", 1, Tribe.BEAST, aura=Aura.direWolfAlphaAura, gold=true)
        var goldMecharoo = MinionType("mecharoo", 1, Tribe.MECH, deathrattle=Deathrattle.mecharooDeathrattle, gold=true)
        var goldMicroMachine = MinionType("microMachine", 1, Tribe.MECH, gold=true)
        var goldMurlocTidecaller = MinionType("murlocTidecaller", 1, Tribe.MURLOC, summonEffect= SummonEffect.murlocTidecallerSummonEffect, gold=true)
        var goldMurlocTidehunter = MinionType("murlocTidehunter", 1, Tribe.MURLOC, gold=true)
        var goldRighteousProtector = MinionType("righteousProtector", 1, Tribe.NONE, gold=true)
        var goldRockpoolHunter = MinionType("rockpoolHunter", 1, Tribe.MURLOC, gold=true)
        var goldSelflessHero = MinionType("seflessHero", 1, Tribe.NONE, deathrattle=Deathrattle.selflessHeroDeathrattle, gold=true)
        var goldVoidwalker = MinionType("voidwalker", 1, Tribe.DEMON, gold=true)
        var goldVulgarHomunculus = MinionType("vulgarHomunculus", 1, Tribe.DEMON, gold=true)
        var goldWrathWeaver = MinionType("wrathWeaver", 1, Tribe.NONE, gold=true)

        var goldAnnoyOTron = MinionType("annoyOTron", 2, Tribe.MECH, gold=true)
        var goldColdlightSeer = MinionType("coldlightSeer", 2, Tribe.MURLOC, gold=true)
        var goldHarvestGolem = MinionType("harvestGolem", 2, Tribe.MECH, deathrattle=Deathrattle.harvestGolemDeathrattle, gold=true)
        var goldKaboomBot = MinionType("kaboomBot", 2, Tribe.MECH, deathrattle=Deathrattle.kaboomBotDeathrattle, gold=true)
        var goldKindlyGrandmother = MinionType("kindlyGrandmother", 2, Tribe.BEAST, deathrattle=Deathrattle.kindlyGrandmotherDeathrattle, gold=true)
        var goldMetaltoothLeaper = MinionType("metaltoothLeaper", 2, Tribe.MECH, gold=true)
        var goldMountedRaptor = MinionType("mountedRaptor", 2, Tribe.BEAST, deathrattle=Deathrattle.mountedRaptorDeathrattle, gold=true)
        var goldMurlocWarleader = MinionType("murlocWarleader", 2, Tribe.MURLOC, aura=Aura.murlocWarleaderAura, gold=true)
        var goldNathrezimOverseer = MinionType("nathrezimOverseer", 2, Tribe.DEMON, gold=true)
        var goldNightmareAmalgam = MinionType("nightmareAmalgam", 2, Tribe.ALL, gold=true)
        var goldOldMurkEye = MinionType("oldMurkEye", 2, Tribe.MURLOC, aura=Aura.oldMurkEyeAura, gold=true)
        var goldPogoHopper = MinionType("pogoHopper", 2, Tribe.MECH, gold=true)
        var goldRatPack = MinionType("ratPack", 2, Tribe.BEAST, deathrattle=Deathrattle.ratPackDeathrattle, gold=true)
        var goldScavengingHyena = MinionType("scavengingHyena", 2, Tribe.BEAST, deathEffect=DeathEffect.scavengingHyenaDeathEffect, gold=true)
        var goldShieldedMinibot = MinionType("shieldedMinibot", 2, Tribe.MECH, gold=true)
        var goldSpawnOfNzoth = MinionType("spawnOfNzoth", 2, Tribe.NONE, deathrattle=Deathrattle.spawnOfNzothDeathrattle, gold=true)
        var goldZoobot = MinionType("zoobot", 2, Tribe.MECH, gold=true)

        var goldCobaltGuardian = MinionType("cobaltGuardian", 3, Tribe.MECH, summonEffect=SummonEffect.cobaltGuardianSummonEffect, gold=true)
        var goldCrowdFavorite = MinionType("crowdFavorite", 3, Tribe.NONE, gold=true)
        var goldCrystalweaver = MinionType("crystalweaver", 3, Tribe.NONE, gold=true)
        var goldHoundmaster = MinionType("houndmaster", 3, Tribe.NONE, gold=true)
        var goldImpGangBoss = MinionType("impGangBoss", 3, Tribe.DEMON, takesDamageEffect=TakesDamageEffect.impGangBossTakesDamageEffect, gold=true)
        var goldInfestedWolf = MinionType("infestedWolf", 3, Tribe.BEAST, deathrattle=Deathrattle.infestedWolfDeathrattle, gold=true)
        var goldKhadgar = MinionType("khadgar", 3, Tribe.NONE, summonEffect=SummonEffect.khadgarSummonEffect, gold=true)
        var goldPackLeader = MinionType("packLeader", 3, Tribe.NONE, summonEffect=SummonEffect.packLeaderSummonEffect, gold=true)
        var goldPhalanxCommander = MinionType("phalanxCommander", 3, Tribe.NONE, aura=Aura.phalanxCommanderAura, gold=true)
        var goldPilotedShredder = MinionType("pilotedShredder", 3, Tribe.MECH, deathrattle=Deathrattle.pilotedShredderDeathrattle, gold=true)
        var goldPsychOTron = MinionType("psychOTron", 3, Tribe.MECH, gold=true)
        var goldReplicatingMenace = MinionType("replicatingMenace", 3, Tribe.MECH, deathrattle=Deathrattle.replicatingMenaceDeathrattle, gold=true)
        var goldScrewjankClunker = MinionType("screwjankClunker", 3, Tribe.MECH, gold=true)
        var goldShifterZerus = MinionType("shifterZerus", 3, Tribe.NONE, gold=true)
        var goldSoulJuggler = MinionType("soulJuggler", 3, Tribe.NONE, deathEffect= DeathEffect.soulJugglerDeathEffect, gold=true)
        var goldTortollanShellraiser = MinionType("tortollanShellraiser", 3, Tribe.NONE, deathrattle=Deathrattle.tortollanShellraiserDeathrattle, gold=true)

        var goldAnnoyOModule = MinionType("annoyOModule", 4, Tribe.MECH)
        var goldBolvarFireblood = MinionType("bolvarFireblood", 4, Tribe.NONE, lossOfDivineShieldEffect= LossOfDivineShieldEffect.bolvarFirebloodLossOfDivineShieldEffect, gold=true)
        var goldCaveHydra = MinionType("caveHydra", 4, Tribe.BEAST, attackEffect=AttackEffect.cleave, gold=true)
        var goldDefenderOfArgus = MinionType("defenderOfArgus", 4, Tribe.NONE, gold=true)
        var goldFesterrootHulk = MinionType("festerrootHulk", 4, Tribe.NONE, anyFriendlyMinionAttacksEffect=AnyFriendlyMinionAttacksEffect.festerrootHulkEffect, gold=true)
        var goldIronSensei = MinionType("ironSensei", 4, Tribe.MECH, gold=true)
        var goldMenagerieMagician = MinionType("menagerieMagician", 4, Tribe.NONE, gold=true)
        var goldPilotedSkyGolem = MinionType("pilotedSkyGolem", 4, Tribe.MECH, deathrattle=Deathrattle.pilotedSkyGolemDeathrattle, gold=true)
        var goldPrimalfinLookout = MinionType("primalfinLookout", 4, Tribe.MURLOC, gold=true)
        var goldSecurityRover = MinionType("securityRover", 4, Tribe.MECH, takesDamageEffect=TakesDamageEffect.securityRoverTakesDamageEffect, gold=true)
        var goldSiegebreaker = MinionType("siegebreaker", 4, Tribe.DEMON, aura=Aura.siegebreakerAura, gold=true)
        var goldTheBeast = MinionType("theBeast", 4, Tribe.BEAST, deathrattle=Deathrattle.theBeastDeathrattle, gold=true)
        var goldToxfin = MinionType("toxfin", 4, Tribe.MURLOC, gold=true)
        var goldVirmenSensei = MinionType("virmenSensei", 4, Tribe.NONE, gold=true)

        var goldAnnihilanBattlemaster = MinionType("annihilanBattlemaster", 5, Tribe.DEMON, gold=true)
        var goldBaronRivendare = MinionType("baronRivendare", 5, Tribe.NONE, deathEffect=DeathEffect.baronRivendareDeathEffect, gold=true)
        var goldBrannBronzebeard = MinionType("brannBronzebeard", 5, Tribe.NONE, gold=true)
        var goldGreatWolf = MinionType("greatWolf", 5, Tribe.BEAST, deathrattle=Deathrattle.greatWolfDeathrattle, gold=true)
        var goldIronhideDirehorn = MinionType("ironhideDirehorn", 5, Tribe.BEAST, attackEffect=AttackEffect.ironhideDirehornAttackEffect, gold=true)
        var goldJunkbot = MinionType("junkbot", 5, Tribe.MECH, deathEffect=DeathEffect.junkbotDeathEffect, gold=true)
        var goldLightfangEnforcer = MinionType("lightfangEnforcer", 5, Tribe.NONE, gold=true)
        var goldMalganis = MinionType("malganis", 5, Tribe.DEMON, aura=Aura.malganisAura, gold=true)
        var goldMechanoEgg = MinionType("mechanoEgg", 5, Tribe.MECH, deathrattle=Deathrattle.mechanoEggDeathrattle, gold=true)
        var goldSatedThreshadon = MinionType("satedThreshadon", 5, Tribe.BEAST, deathrattle=Deathrattle.satedThreshadonDeathrattle, gold=true)
        var goldSavannahHighmane = MinionType("savannahHighmane", 5, Tribe.BEAST, deathrattle=Deathrattle.savannahHighmaneDeathrattle, gold=true)
        var goldStrongshellScavenger = MinionType("strongshellScavenger", 5, Tribe.NONE, gold=true)
        var goldTheBoogeymonster = MinionType("theBoogeymonster", 5, Tribe.NONE, attackEffect=AttackEffect.boogeymonsterAttackEffect, gold=true)
        var goldVoidlord = MinionType("voidlord", 5, Tribe.DEMON, deathrattle=Deathrattle.voidlordDeathrattle, gold=true)

        var goldFoeReaper = MinionType("foeReaper", 6, Tribe.MECH, attackEffect= AttackEffect.cleave, gold=true)
        var goldGentleMegasaur = MinionType("gentleMegasaur", 6, Tribe.BEAST, gold=true)
        var goldGhastcoiler = MinionType("ghastcoiler", 6, Tribe.BEAST, deathrattle=Deathrattle.ghastcoilerDeathrattle, gold=true)
        var goldKangorsApprentice = MinionType("kangorsApprentice", 6, Tribe.NONE, deathrattle=Deathrattle.kangorsApprenticeDeathrattle, gold=true)
        var goldMaexxna = MinionType("maexxna", 6, Tribe.BEAST, gold=true)
        var goldMamaBear = MinionType("mamaBear", 6, Tribe.BEAST, summonEffect=SummonEffect.mamaBearSummonEffect, gold=true)
        var goldSneedsOldShredder = MinionType("sneedsOldShredder", 6, Tribe.MECH, deathrattle=Deathrattle.sneedsOldShredderDeathrattle, gold=true)
        var goldZappSlywick = MinionType("zappSlywick", 6, Tribe.NONE, attackEffect= AttackEffect.zappSlywickAttackEffect, gold=true)
    }
}

enum class Tribe {
    NONE, MECH, DEMON, MURLOC, BEAST, ALL //Slywick
}

class Deathrattle {
    companion object {
        var mecharooDeathrattle = Deathrattle()//TODO
        var selflessHeroDeathrattle = Deathrattle()//TODO
        var harvestGolemDeathrattle = Deathrattle()//TODO
        var kaboomBotDeathrattle = Deathrattle()//TODO
        var kindlyGrandmotherDeathrattle = Deathrattle()//TODO
        var mountedRaptorDeathrattle = Deathrattle()//TODO
        var ratPackDeathrattle = Deathrattle()//TODO
        var spawnOfNzothDeathrattle = Deathrattle()//TODO
        var infestedWolfDeathrattle = Deathrattle()//TODO
        var pilotedShredderDeathrattle = Deathrattle()//TODO
        var replicatingMenaceDeathrattle = Deathrattle()//TODO
        var tortollanShellraiserDeathrattle = Deathrattle()//TODO
        var pilotedSkyGolemDeathrattle = Deathrattle()//TODO
        var theBeastDeathrattle = Deathrattle()//TODO
        var greatWolfDeathrattle = Deathrattle()//TODO
        var mechanoEggDeathrattle = Deathrattle()//TODO
        var satedThreshadonDeathrattle = Deathrattle()//TODO
        var savannahHighmaneDeathrattle = Deathrattle()//TODO
        var voidlordDeathrattle = Deathrattle()//TODO
        var ghastcoilerDeathrattle = Deathrattle()//TODO
        var kangorsApprenticeDeathrattle = Deathrattle()//TODO
        var sneedsOldShredderDeathrattle = Deathrattle()//TODO

        var goldMecharooDeathrattle = Deathrattle()//TODO
        var goldSelflessHeroDeathrattle = Deathrattle()//TODO
        var goldHarvestGolemDeathrattle = Deathrattle()//TODO
        var goldKaboomBotDeathrattle = Deathrattle()//TODO
        var goldKindlyGrandmotherDeathrattle = Deathrattle()//TODO
        var goldMountedRaptorDeathrattle = Deathrattle()//TODO
        var goldRatPackDeathrattle = Deathrattle()//TODO
        var goldSpawnOfNzothDeathrattle = Deathrattle()//TODO
        var goldInfestedWolfDeathrattle = Deathrattle()//TODO
        var goldPilotedShredderDeathrattle = Deathrattle()//TODO
        var goldReplicatingMenaceDeathrattle = Deathrattle()//TODO
        var goldTortollanShellraiserDeathrattle = Deathrattle()//TODO
        var goldPilotedSkyGolemDeathrattle = Deathrattle()//TODO
        var goldTheBeastDeathrattle = Deathrattle()//TODO
        var goldGreatWolfDeathrattle = Deathrattle()//TODO
        var goldMechanoEggDeathrattle = Deathrattle()//TODO
        var goldSatedThreshadonDeathrattle = Deathrattle()//TODO
        var goldSavannahHighmaneDeathrattle = Deathrattle()//TODO
        var goldVoidlordDeathrattle = Deathrattle()//TODO
        var goldGhastcoilerDeathrattle = Deathrattle()//TODO
        var goldKangorsApprenticeDeathrattle = Deathrattle()//TODO
        var goldSneedsOldShredderDeathrattle = Deathrattle()//TODO
    }
}

class Aura {
    companion object {
        var direWolfAlphaAura = Aura()//TODO
        var murlocWarleaderAura = Aura()//TODO
        var oldMurkEyeAura = Aura()//TODO
        var phalanxCommanderAura = Aura()//TODO
        var siegebreakerAura = Aura()//TODO
        var malganisAura = Aura()//TODO

        var goldDireWolfAlphaAura = Aura()//TODO
        var goldMurlocWarleaderAura = Aura()//TODO
        var goldOldMurkEyeAura = Aura()//TODO
        var goldPhalanxCommanderAura = Aura()//TODO
        var goldSiegebreakerAura = Aura()//TODO
        var goldMalganisAura = Aura()//TODO
    }
}

/**
 * Old Murk-Eye: has a StartOfCombatEffect that adds 1 attack for each murloc on the board,
 * a SummonEffect that adds 1 attack when a murloc is summoned,
 * and a DeathEffect that removes 1 attack when a murloc dies
 */

/**
 * Effect that triggers when a friendly minion is summoned
 */
class SummonEffect {
    companion object {
        var murlocTidecallerSummonEffect = SummonEffect()//TODO
        var cobaltGuardianSummonEffect = SummonEffect()//TODO
        var khadgarSummonEffect = SummonEffect()//TODO
        var packLeaderSummonEffect = SummonEffect()//TODO
        var mamaBearSummonEffect = SummonEffect()//TODO

        var goldMurlocTidecallerSummonEffect = SummonEffect()//TODO
        var goldCobaltGuardianSummonEffect = SummonEffect()//TODO
        var goldKhadgarSummonEffect = SummonEffect()//TODO
        var goldPackLeaderSummonEffect = SummonEffect()//TODO
        var goldMamaBearSummonEffect = SummonEffect()//TODO
    }
}

/**
 * Effect that triggers when this minion attacks
 */
class AttackEffect {
    companion object {
        var cleave = AttackEffect()//TODO
        var ironhideDirehornAttackEffect = AttackEffect()//TODO
        var boogeymonsterAttackEffect = AttackEffect()//TODO
        var zappSlywickAttackEffect = AttackEffect()//TODO

        var goldCleave = AttackEffect()//TODO
        var goldIronhideDirehornAttackEffect = AttackEffect()//TODO
        var goldBoogeymonsterAttackEffect = AttackEffect()//TODO
        var goldZappSlywickAttackEffect = AttackEffect()//TODO
    }
}

class TakesDamageEffect {
    companion object {
        var impGangBossTakesDamageEffect = TakesDamageEffect()//TODO
        var securityRoverTakesDamageEffect = TakesDamageEffect()//TODO

        var goldImpGangBossTakesDamageEffect = TakesDamageEffect()//TODO
        var goldSecurityRoverTakesDamageEffect = TakesDamageEffect()//TODO
    }
}

/**
 * Effect that triggers when a different friendly minion dies
 */
class DeathEffect {
    companion object {
        var scavengingHyenaDeathEffect = DeathEffect()//TODO
        var soulJugglerDeathEffect = DeathEffect()//TODO
        var baronRivendareDeathEffect = DeathEffect()//TODO
        var junkbotDeathEffect = DeathEffect()//TODO

        var goldScavengingHyenaDeathEffect = DeathEffect()//TODO
        var goldSoulJugglerDeathEffect = DeathEffect()//TODO
        var goldBaronRivendareDeathEffect = DeathEffect()//TODO
        var goldJunkbotDeathEffect = DeathEffect()//TODO
    }
}

/**
 * Friendly board state, opponent's board state
 */
//TODO figure out lots of special cases wrt these hero powers
/* TODO
For example: If Nefarian's hero power kills a replicating menace,
it shouldn't also damage the microbots that come out. Make sure that doesn't happen.

Also, for patches and ragnaros:
If there are no minions, the hero powers should do nothing
If there is 1 minion, the hero power should only deal damage once.
 */
class StartOfCombatEffect(var trigger: (BoardState, BoardState) -> Pair<BoardState, BoardState>) {
    companion object {
        var nefarianStartOfCombatEffect = StartOfCombatEffect { nefariansBoard: BoardState, opponentsBoard: BoardState ->
            var newBoard = opponentsBoard
            for (minion in newBoard.board) {
                if (!minion.divineShield) {
                    //TODO
                } else {
                    minion.divineShield = false
                    //Bolvar
                    for (m in newBoard.board) {
                        if (m.type.lossOfDivineShieldEffect != null) {
                            m.type.lossOfDivineShieldEffect!!.trigger(m)
                        }
                    }
                }
            }
            Pair(nefariansBoard, newBoard)
        }

        var patchesStartOfCombatEffect = StartOfCombatEffect { patchesBoard: BoardState, opponentsBoard: BoardState ->
            var newBoard = opponentsBoard
            var numMinions = newBoard.numMinions()
            var damageSlot1 = RAND.nextInt(numMinions) + 1
            var damageSlot2 = RAND.nextInt(numMinions)
            if (damageSlot2 >= damageSlot1) {
                if (damageSlot2 == numMinions) {
                    damageSlot2 = 1
                } else {
                    damageSlot2 += 1
                }
            }
            if (damageSlot2 > damageSlot1) {
                var temp = damageSlot1
                damageSlot1 = damageSlot2
                damageSlot2 = temp
            }

            var firstMinion = newBoard.get(damageSlot1)
            if (!firstMinion.divineShield) {
                //TODO
            } else {
                firstMinion.divineShield = false
                //Bolvar
                for (m in newBoard.board) {
                    if (m.type.lossOfDivineShieldEffect != null) {
                        m.type.lossOfDivineShieldEffect!!.trigger(m)
                    }
                }
            }

            var secondMinion = newBoard.get(damageSlot1)
            if (!secondMinion.divineShield) {
                //TODO
            } else {
                secondMinion.divineShield = false
                //Bolvar
                for (m in newBoard.board) {
                    if (m.type.lossOfDivineShieldEffect != null) {
                        m.type.lossOfDivineShieldEffect!!.trigger(m)
                    }
                }
            }

            Pair(patchesBoard, newBoard)
        }

        var professorPutricideStartOfCombatEffect = StartOfCombatEffect { putricidesBoard: BoardState, opponentsBoard: BoardState ->
            var newBoard = putricidesBoard
            if (putricidesBoard.numMinions() >= 1) {
                var firstMinion = putricidesBoard.get(1)
                firstMinion.attack += 10
            }
            Pair(newBoard, opponentsBoard)
        }

        var ragnarosStartOfCombatEffect = StartOfCombatEffect { ragnarosBoard: BoardState, opponentsBoard: BoardState ->
            var newBoard = opponentsBoard
            var numMinions = newBoard.numMinions()
            var damageSlot1 = RAND.nextInt(numMinions) + 1
            var damageSlot2 = RAND.nextInt(numMinions)
            if (damageSlot2 >= damageSlot1) {
                if (damageSlot2 == numMinions) {
                    damageSlot2 = 1
                } else {
                    damageSlot2 += 1
                }
            }
            if (damageSlot2 > damageSlot1) {
                var temp = damageSlot1
                damageSlot1 = damageSlot2
                damageSlot2 = temp
            }

            var firstMinion = newBoard.get(damageSlot1)
            if (!firstMinion.divineShield) {
                //TODO
            } else {
                firstMinion.divineShield = false
                //Bolvar
                for (m in newBoard.board) {
                    if (m.type.lossOfDivineShieldEffect != null) {
                        m.type.lossOfDivineShieldEffect!!.trigger(m)
                    }
                }
            }

            var secondMinion = newBoard.get(damageSlot1)
            if (!secondMinion.divineShield) {
                //TODO
            } else {
                secondMinion.divineShield = false
                //Bolvar
                for (m in newBoard.board) {
                    if (m.type.lossOfDivineShieldEffect != null) {
                        m.type.lossOfDivineShieldEffect!!.trigger(m)
                    }
                }
            }

            Pair(ragnarosBoard, newBoard)
        }

        var theGreatAkazamzarakStartOfCombatEffect = StartOfCombatEffect { akazamzaraksBoard: BoardState, opponentsBoard: BoardState ->
            //TODO
            Pair(akazamzaraksBoard, opponentsBoard)
        }

        var theLichKingsStartOfCombatEffect = StartOfCombatEffect { lichKingsBoard: BoardState, opponentsBoard: BoardState ->
            //TODO
            Pair(lichKingsBoard, opponentsBoard)
        }
    }
}

class LossOfDivineShieldEffect(var trigger: ((BoardMinion) -> Unit)) {

    companion object {
        var bolvarFirebloodLossOfDivineShieldEffect = LossOfDivineShieldEffect { minion: BoardMinion ->
            minion.attack += 2
        }//TODO

        var goldBolvarFirebloodLossOfDivineShieldEffect = LossOfDivineShieldEffect { minion: BoardMinion ->
            minion.attack += 2
        }//TODO
    }
}

class AnyFriendlyMinionAttacksEffect {
    companion object {
        var festerrootHulkEffect = AnyFriendlyMinionAttacksEffect()//TODO

        var goldFesterrootHulkEffect = AnyFriendlyMinionAttacksEffect()//TODO
    }
}