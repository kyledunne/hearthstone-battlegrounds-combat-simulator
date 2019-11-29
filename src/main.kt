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
        var attackResult: Pair<Pair<BoardState, BoardState>, Pair<Int, Int>> = if (isPlayer1Turn) attack(p1board, p2board, nextToAttackP1, nextToAttackP2)
                                                                               else attack(p2board, p1board, nextToAttackP2, nextToAttackP1)
        p1board = if (isPlayer1Turn) attackResult.first.first else attackResult.first.second
        p2board = if (isPlayer1Turn) attackResult.first.second else attackResult.first.first
        nextToAttackP1 = if (isPlayer1Turn) attackResult.second.first else attackResult.second.second
        nextToAttackP2 = if (isPlayer1Turn) attackResult.second.second else attackResult.second.first

        isPlayer1Turn = !isPlayer1Turn
    }

    // if both boards are empty, it's a tie, return 0
    if (p1board.isEmpty() && p2board.isEmpty()) {
        return 0
    }
    // if only p2's board is empty, p1 wins, return a positive number equal to damage done
    if (p2board.isEmpty()) {
        var totalDamage = p1Level
        for (minion in p1board.board) {
            totalDamage += minion.type.level
        }
        return totalDamage
    }
    // if only p1's board is empty, p2 wins, return a negative number equal to damage done
    var totalDamage = p2Level
    for (minion in p2board.board) {
        totalDamage += minion.type.level
    }
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
fun attack(attackerBoard: BoardState, defenderBoard: BoardState, attackingSlot: Int, nextDefendingSlot: Int): Pair<Pair<BoardState, BoardState>, Pair<Int, Int>> {
    var nextAttackingSlot = attackingSlot
    var nextDefendingSlot = nextDefendingSlot

    var defenderSlot = RAND.nextInt(defenderBoard.numMinions()) + 1
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

class MinionType(val level: Int, var tribe: Tribe = Tribe.NONE, var deathrattle: Deathrattle? = null,
                 var aura: Aura? = null, var summonEffect: SummonEffect? = null, var attackEffect: AttackEffect? = null,
                 var takesDamageEffect: TakesDamageEffect? = null, var deathEffect: DeathEffect? = null,
                 var startOfCombatEffect: StartOfCombatEffect? = null, var lossOfDivineShieldEffect: LossOfDivineShieldEffect? = null,
                 var anyFriendlyMinionAttacksEffect: AnyFriendlyMinionAttacksEffect? = null) {
    companion object {
        var beastToken = MinionType(1, Tribe.BEAST)
        var demonToken = MinionType(1, Tribe.DEMON)
        var mechToken = MinionType(1, Tribe.MECH)
        var murlocToken = MinionType(1, Tribe.MURLOC)
        var amalgamToken = MinionType(1, Tribe.ALL)

        var alleyCat = MinionType(1, Tribe.BEAST)
        var direWolfAlpha = MinionType(1, Tribe.BEAST, aura=Aura.direWolfAlphaAura)
        var mecharoo = MinionType(1, Tribe.MECH, deathrattle=Deathrattle.mecharooDeathrattle)
        var microMachine = MinionType(1, Tribe.MECH)
        var murlocTidecaller = MinionType(1, Tribe.MURLOC, summonEffect= SummonEffect.murlocTidecallerSummonEffect)
        var murlocTidehunter = MinionType(1, Tribe.MURLOC)
        var righteousProtector = MinionType(1, Tribe.NONE)
        var rockpoolHunter = MinionType(1, Tribe.MURLOC)
        var selflessHero = MinionType(1, Tribe.NONE, deathrattle=Deathrattle.selflessHeroDeathrattle)
        var voidwalker = MinionType(1, Tribe.DEMON)
        var vulgarHomunculus = MinionType(1, Tribe.DEMON)
        var wrathWeaver = MinionType(1, Tribe.NONE)

        var annoyOTron = MinionType(2, Tribe.MECH)
        var coldlightSeer = MinionType(2, Tribe.MURLOC)
        var harvestGolem = MinionType(2, Tribe.MECH, deathrattle=Deathrattle.harvestGolemDeathrattle)
        var kaboomBot = MinionType(2, Tribe.MECH, deathrattle=Deathrattle.kaboomBotDeathrattle)
        var kindlyGrandmother = MinionType(2, Tribe.BEAST, deathrattle=Deathrattle.kindlyGrandmotherDeathrattle)
        var metaltoothLeaper = MinionType(2, Tribe.MECH)
        var mountedRaptor = MinionType(2, Tribe.BEAST, deathrattle=Deathrattle.mountedRaptorDeathrattle)
        var murlocWarleader = MinionType(2, Tribe.MURLOC, aura=Aura.murlocWarleaderAura)
        var nathrezimOverseer = MinionType(2, Tribe.DEMON)
        var nightmareAmalgam = MinionType(2, Tribe.ALL)
        var oldMurkEye = MinionType(2, Tribe.MURLOC, aura=Aura.oldMurkEyeAura)
        var pogoHopper = MinionType(2, Tribe.MECH)
        var ratPack = MinionType(2, Tribe.BEAST, deathrattle=Deathrattle.ratPackDeathrattle)
        var scavengingHyena = MinionType(2, Tribe.BEAST, deathEffect=DeathEffect.scavengingHyenaDeathEffect)
        var shieldedMinibot = MinionType(2, Tribe.MECH)
        var spawnOfNzoth = MinionType(2, Tribe.NONE, deathrattle=Deathrattle.spawnOfNzothDeathrattle)
        var zoobot = MinionType(2, Tribe.MECH)

        var cobaltGuardian = MinionType(3, Tribe.MECH, summonEffect=SummonEffect.cobaltGuardianSummonEffect)
        var crowdFavorite = MinionType(3, Tribe.NONE)
        var crystalweaver = MinionType(3, Tribe.NONE)
        var houndmaster = MinionType(3, Tribe.NONE)
        var impGangBoss = MinionType(3, Tribe.DEMON, takesDamageEffect=TakesDamageEffect.impGangBossTakesDamageEffect)
        var infestedWolf = MinionType(3, Tribe.BEAST, deathrattle=Deathrattle.infestedWolfDeathrattle)
        var khadgar = MinionType(3, Tribe.NONE, summonEffect=SummonEffect.khadgarSummonEffect)
        var packLeader = MinionType(3, Tribe.NONE, summonEffect=SummonEffect.packLeaderSummonEffect)
        var phalanxCommander = MinionType(3, Tribe.NONE, aura=Aura.phalanxCommanderAura)
        var pilotedShredder = MinionType(3, Tribe.MECH, deathrattle=Deathrattle.pilotedShredderDeathrattle)
        var psychOTron = MinionType(3, Tribe.MECH)
        var replicatingMenace = MinionType(3, Tribe.MECH, deathrattle=Deathrattle.replicatingMenaceDeathrattle)
        var screwjankClunker = MinionType(3, Tribe.MECH)
        var shifterZerus = MinionType(3, Tribe.NONE)
        var soulJuggler = MinionType(3, Tribe.NONE, deathEffect= DeathEffect.soulJugglerDeathEffect)
        var tortollanShellraiser = MinionType(3, Tribe.NONE, deathrattle=Deathrattle.tortollanShellraiserDeathrattle)

        var annoyOModule = MinionType(4, Tribe.MECH)
        var bolvarFireblood = MinionType(4, Tribe.NONE, lossOfDivineShieldEffect= LossOfDivineShieldEffect.bolvarFirebloodLossOfDivineShieldEffect)
        var caveHydra = MinionType(4, Tribe.BEAST, attackEffect=AttackEffect.cleave)
        var defenderOfArgus = MinionType(4, Tribe.NONE)
        var festerrootHulk = MinionType(4, Tribe.NONE, anyFriendlyMinionAttacksEffect=AnyFriendlyMinionAttacksEffect.festerrootHulkEffect)
        var ironSensei = MinionType(4, Tribe.MECH)
        var menagerieMagician = MinionType(4, Tribe.NONE)
        var pilotedSkyGolem = MinionType(4, Tribe.MECH, deathrattle=Deathrattle.pilotedSkyGolemDeathrattle)
        var primalfinLookout = MinionType(4, Tribe.MURLOC)
        var securityRover = MinionType(4, Tribe.MECH, takesDamageEffect=TakesDamageEffect.securityRoverTakesDamageEffect)
        var siegebreaker = MinionType(4, Tribe.DEMON, aura=Aura.siegebreakerAura)
        var theBeast = MinionType(4, Tribe.BEAST, deathrattle=Deathrattle.theBeastDeathrattle)
        var toxfin = MinionType(4, Tribe.MURLOC)
        var virmenSensei = MinionType(4, Tribe.NONE)

        var annihilanBattlemaster = MinionType(5, Tribe.DEMON)
        var baronRivendare = MinionType(5, Tribe.NONE, deathEffect=DeathEffect.baronRivendareDeathEffect)
        var brannBronzebeard = MinionType(5, Tribe.NONE)
        var greatWolf = MinionType(5, Tribe.BEAST, deathrattle=Deathrattle.greatWolfDeathrattle)
        var ironhideDirehorn = MinionType(5, Tribe.BEAST, attackEffect=AttackEffect.ironhideDirehornAttackEffect)
        var junkbot = MinionType(5, Tribe.MECH, deathEffect=DeathEffect.junkbotDeathEffect)
        var lightfangEnforcer = MinionType(5, Tribe.NONE)
        var malganis = MinionType(5, Tribe.DEMON, aura=Aura.malganisAura)
        var mechanoEgg = MinionType(5, Tribe.MECH, deathrattle=Deathrattle.mechanoEggDeathrattle)
        var satedThreshadon = MinionType(5, Tribe.BEAST, deathrattle=Deathrattle.satedThreshadonDeathrattle)
        var savannahHighmane = MinionType(5, Tribe.BEAST, deathrattle=Deathrattle.savannahHighmaneDeathrattle)
        var strongshellScavenger = MinionType(5, Tribe.NONE)
        var theBoogeymonster = MinionType(5, Tribe.NONE, attackEffect=AttackEffect.boogeymonsterAttackEffect)
        var voidlord = MinionType(5, Tribe.DEMON, deathrattle=Deathrattle.voidlordDeathrattle)

        var foeReaper = MinionType(6, Tribe.MECH, attackEffect= AttackEffect.cleave)
        var gentleMegasaur = MinionType(6, Tribe.BEAST)
        var ghastcoiler = MinionType(6, Tribe.BEAST, deathrattle=Deathrattle.ghastcoilerDeathrattle)
        var kangorsApprentice = MinionType(6, Tribe.NONE, deathrattle=Deathrattle.kangorsApprenticeDeathrattle)
        var maexxna = MinionType(6, Tribe.BEAST)
        var mamaBear = MinionType(6, Tribe.BEAST, summonEffect=SummonEffect.mamaBearSummonEffect)
        var sneedsOldShredder = MinionType(6, Tribe.MECH, deathrattle=Deathrattle.sneedsOldShredderDeathrattle)
        var zappSlywick = MinionType(6, Tribe.NONE, attackEffect= AttackEffect.zappSlywickAttackEffect)

        //golden cards

        var goldAlleyCat = MinionType(1, Tribe.BEAST)
        var goldBeastToken = MinionType(1, Tribe.BEAST)
        var goldDireWolfAlpha = MinionType(1, Tribe.BEAST, aura=Aura.goldDireWolfAlphaAura)
        var goldMecharoo = MinionType(1, Tribe.MECH, deathrattle=Deathrattle.goldMecharooDeathrattle)
        var goldMicroMachine = MinionType(1, Tribe.MECH)
        var goldMurlocTidecaller = MinionType(1, Tribe.MURLOC, summonEffect= SummonEffect.goldMurlocTidecallerSummonEffect)
        var goldMurlocTidehunter = MinionType(1, Tribe.MURLOC)
        var goldMurlocToken = MinionType(1, Tribe.MURLOC)
        var goldRighteousProtector = MinionType(1, Tribe.NONE)
        var goldRockpoolHunter = MinionType(1, Tribe.MURLOC)
        var goldSelflessHero = MinionType(1, Tribe.NONE, deathrattle=Deathrattle.goldSelflessHeroDeathrattle)
        var goldVoidwalker = MinionType(1, Tribe.DEMON)
        var goldVulgarHomunculus = MinionType(1, Tribe.DEMON)
        var goldWrathWeaver = MinionType(1, Tribe.NONE)

        var goldAnnoyOTron = MinionType(2, Tribe.MECH)
        var goldColdlightSeer = MinionType(2, Tribe.MURLOC)
        var goldHarvestGolem = MinionType(2, Tribe.MECH, deathrattle=Deathrattle.goldHarvestGolemDeathrattle)
        var goldKaboomBot = MinionType(2, Tribe.MECH, deathrattle=Deathrattle.goldKaboomBotDeathrattle)
        var goldKindlyGrandmother = MinionType(2, Tribe.BEAST, deathrattle=Deathrattle.goldKindlyGrandmotherDeathrattle)
        var goldMetaltoothLeaper = MinionType(2, Tribe.MECH)
        var goldMountedRaptor = MinionType(2, Tribe.BEAST, deathrattle=Deathrattle.goldMountedRaptorDeathrattle)
        var goldMurlocWarleader = MinionType(2, Tribe.MURLOC, aura=Aura.goldMurlocWarleaderAura)
        var goldNathrezimOverseer = MinionType(2, Tribe.DEMON)
        var goldNightmareAmalgam = MinionType(2, Tribe.ALL)
        var goldOldMurkEye = MinionType(2, Tribe.MURLOC, aura=Aura.goldOldMurkEyeAura)
        var goldPogoHopper = MinionType(2, Tribe.MECH)
        var goldRatPack = MinionType(2, Tribe.BEAST, deathrattle=Deathrattle.goldRatPackDeathrattle)
        var goldScavengingHyena = MinionType(2, Tribe.BEAST, deathEffect=DeathEffect.goldScavengingHyenaDeathEffect)
        var goldShieldedMinibot = MinionType(2, Tribe.MECH)
        var goldSpawnOfNzoth = MinionType(2, Tribe.NONE, deathrattle=Deathrattle.goldSpawnOfNzothDeathrattle)
        var goldZoobot = MinionType(2, Tribe.MECH)

        var goldCobaltGuardian = MinionType(3, Tribe.MECH, summonEffect=SummonEffect.goldCobaltGuardianSummonEffect)
        var goldCrowdFavorite = MinionType(3, Tribe.NONE)
        var goldCrystalweaver = MinionType(3, Tribe.NONE)
        var goldHoundmaster = MinionType(3, Tribe.NONE)
        var goldImpGangBoss = MinionType(3, Tribe.DEMON, takesDamageEffect=TakesDamageEffect.goldImpGangBossTakesDamageEffect)
        var goldInfestedWolf = MinionType(3, Tribe.BEAST, deathrattle=Deathrattle.goldInfestedWolfDeathrattle)
        var goldKhadgar = MinionType(3, Tribe.NONE, summonEffect=SummonEffect.goldKhadgarSummonEffect)
        var goldPackLeader = MinionType(3, Tribe.NONE, summonEffect=SummonEffect.goldPackLeaderSummonEffect)
        var goldPhalanxCommander = MinionType(3, Tribe.NONE, aura=Aura.goldPhalanxCommanderAura)
        var goldPilotedShredder = MinionType(3, Tribe.MECH, deathrattle=Deathrattle.goldPilotedShredderDeathrattle)
        var goldPsychOTron = MinionType(3, Tribe.MECH)
        var goldReplicatingMenace = MinionType(3, Tribe.MECH, deathrattle=Deathrattle.goldReplicatingMenaceDeathrattle)
        var goldScrewjankClunker = MinionType(3, Tribe.MECH)
        var goldShifterZerus = MinionType(3, Tribe.NONE)
        var goldSoulJuggler = MinionType(3, Tribe.NONE, deathEffect= DeathEffect.goldSoulJugglerDeathEffect)
        var goldTortollanShellraiser = MinionType(3, Tribe.NONE, deathrattle=Deathrattle.goldTortollanShellraiserDeathrattle)

        var goldAnnoyOModule = MinionType(4, Tribe.MECH)
        var goldBolvarFireblood = MinionType(4, Tribe.NONE, lossOfDivineShieldEffect= LossOfDivineShieldEffect.goldBolvarFirebloodLossOfDivineShieldEffect)
        var goldCaveHydra = MinionType(4, Tribe.BEAST, attackEffect=AttackEffect.goldCleave)
        var goldDefenderOfArgus = MinionType(4, Tribe.NONE)
        var goldFesterrootHulk = MinionType(4, Tribe.NONE, anyFriendlyMinionAttacksEffect=AnyFriendlyMinionAttacksEffect.goldFesterrootHulkEffect)
        var goldIronSensei = MinionType(4, Tribe.MECH)
        var goldMenagerieMagician = MinionType(4, Tribe.NONE)
        var goldPilotedSkyGolem = MinionType(4, Tribe.MECH, deathrattle=Deathrattle.goldPilotedSkyGolemDeathrattle)
        var goldPrimalfinLookout = MinionType(4, Tribe.MURLOC)
        var goldSecurityRover = MinionType(4, Tribe.MECH, takesDamageEffect=TakesDamageEffect.goldSecurityRoverTakesDamageEffect)
        var goldSiegebreaker = MinionType(4, Tribe.DEMON, aura=Aura.goldSiegebreakerAura)
        var goldTheBeast = MinionType(4, Tribe.BEAST, deathrattle=Deathrattle.goldTheBeastDeathrattle)
        var goldToxfin = MinionType(4, Tribe.MURLOC)
        var goldVirmenSensei = MinionType(4, Tribe.NONE)

        var goldAnnihilanBattlemaster = MinionType(5, Tribe.DEMON)
        var goldBaronRivendare = MinionType(5, Tribe.NONE, deathEffect=DeathEffect.goldBaronRivendareDeathEffect)
        var goldBrannBronzebeard = MinionType(5, Tribe.NONE)
        var goldGreatWolf = MinionType(5, Tribe.BEAST, deathrattle=Deathrattle.goldGreatWolfDeathrattle)
        var goldIronhideDirehorn = MinionType(5, Tribe.BEAST, attackEffect=AttackEffect.goldIronhideDirehornAttackEffect)
        var goldJunkbot = MinionType(5, Tribe.MECH, deathEffect=DeathEffect.goldJunkbotDeathEffect)
        var goldLightfangEnforcer = MinionType(5, Tribe.NONE)
        var goldMalganis = MinionType(5, Tribe.DEMON, aura=Aura.goldMalganisAura)
        var goldMechanoEgg = MinionType(5, Tribe.MECH, deathrattle=Deathrattle.goldMechanoEggDeathrattle)
        var goldSatedThreshadon = MinionType(5, Tribe.BEAST, deathrattle=Deathrattle.goldSatedThreshadonDeathrattle)
        var goldSavannahHighmane = MinionType(5, Tribe.BEAST, deathrattle=Deathrattle.goldSavannahHighmaneDeathrattle)
        var goldStrongshellScavenger = MinionType(5, Tribe.NONE)
        var goldTheBoogeymonster = MinionType(5, Tribe.NONE, attackEffect=AttackEffect.goldBoogeymonsterAttackEffect)
        var goldVoidlord = MinionType(5, Tribe.DEMON, deathrattle=Deathrattle.goldVoidlordDeathrattle)

        var goldFoeReaper = MinionType(6, Tribe.MECH, attackEffect=AttackEffect.goldCleave)
        var goldGentleMegasaur = MinionType(6, Tribe.BEAST)
        var goldGhastcoiler = MinionType(6, Tribe.BEAST, deathrattle=Deathrattle.goldGhastcoilerDeathrattle)
        var goldKangorsApprentice = MinionType(6, Tribe.NONE, deathrattle=Deathrattle.goldKangorsApprenticeDeathrattle)
        var goldMaexxna = MinionType(6, Tribe.BEAST)
        var goldMamaBear = MinionType(6, Tribe.BEAST, summonEffect=SummonEffect.goldMamaBearSummonEffect)
        var goldSneedsOldShredder = MinionType(6, Tribe.MECH, deathrattle=Deathrattle.goldSneedsOldShredderDeathrattle)
        var goldZappSlywick = MinionType(6, Tribe.NONE, attackEffect= AttackEffect.goldZappSlywickAttackEffect)
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