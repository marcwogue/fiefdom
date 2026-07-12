package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.random.Random

class FiefdomRepository(private val dao: FiefdomDao) {

    val allTasks: Flow<List<Task>> = dao.getAllTasks()
    val allMembers: Flow<List<CommunityMember>> = dao.getAllMembers()
    val allTaxLogs: Flow<List<TaxLog>> = dao.getAllTaxLogs()

    // --- Dynamic Settings Getters/Setters stored in local state ---
    suspend fun getFirebaseUrl(): String {
        val state = dao.getGameState("firebase_url")
        return state?.value ?: FirebaseClient.DEFAULT_FIREBASE_URL
    }

    suspend fun saveFirebaseUrl(url: String) {
        dao.saveGameState(GameState("firebase_url", url))
    }

    suspend fun getHuggingFaceToken(): String {
        val state = dao.getGameState("hugging_face_token")
        return state?.value ?: ""
    }

    suspend fun saveHuggingFaceToken(token: String) {
        dao.saveGameState(GameState("hugging_face_token", token))
    }

    // --- Authentication System ---
    suspend fun registerUser(username: String, passwordRaw: String, name: String, avatarEmoji: String): Boolean {
        val cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() }
        if (cleanUsername.isEmpty()) return false
        
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        return try {
            val existing = service.getUser(cleanUsername)
            if (existing != null) {
                false // Username already exists
            } else {
                val newUser = CommunityMember(
                    id = cleanUsername,
                    name = name.trim(),
                    title = "Esclave",
                    points = 20,
                    gridX = Random.nextInt(1, 5),
                    gridY = Random.nextInt(1, 5),
                    statusMessage = "Esclave humble mais plein d'ambition !",
                    isPlayer = false, // Saved as false in Firebase
                    avatarEmoji = avatarEmoji,
                    dailyQuest = "Nettoyer les écuries du donjon",
                    dailyQuestReward = 15,
                    dailyQuestStatus = "En cours",
                    passwordHash = passwordRaw
                )
                // Save to Firebase
                service.saveUser(cleanUsername, newUser)
                
                // Trigger NPC population if database is empty/fresh
                val allUsers = service.getAllUsers()
                if (allUsers == null || allUsers.size <= 1) {
                    populateDefaultNpcsInFirebase(url)
                }

                // Cache locally as player
                dao.insertMember(newUser.copy(isPlayer = true))
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Offline/Fallback registration
            val newUser = CommunityMember(
                id = cleanUsername,
                name = name.trim(),
                title = "Esclave",
                points = 20,
                gridX = Random.nextInt(1, 5),
                gridY = Random.nextInt(1, 5),
                statusMessage = "Esclave humble mais plein d'ambition (Mode Hors-ligne) !",
                isPlayer = true,
                avatarEmoji = avatarEmoji,
                dailyQuest = "Nettoyer les écuries du donjon",
                dailyQuestReward = 15,
                dailyQuestStatus = "En cours",
                passwordHash = passwordRaw
            )
            dao.insertMember(newUser)
            true
        }
    }

    suspend fun loginUser(username: String, passwordRaw: String): CommunityMember? {
        val cleanUsername = username.trim().lowercase().filter { it.isLetterOrDigit() }
        if (cleanUsername.isEmpty()) return null
        
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        return try {
            val user = service.getUser(cleanUsername)
            if (user != null && user.passwordHash == passwordRaw) {
                // Clear old local player flags
                val allLocal = dao.getAllMembers().first()
                allLocal.forEach {
                    if (it.isPlayer) {
                        dao.insertMember(it.copy(isPlayer = false))
                    }
                }

                val loggedUser = user.copy(isPlayer = true)
                dao.insertMember(loggedUser)
                
                // Fully sync all other records from Firebase
                syncAllWithFirebase(cleanUsername)
                
                loggedUser
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Offline cache fallback login
            val localUser = dao.getMemberById(cleanUsername)
            if (localUser != null && localUser.passwordHash == passwordRaw) {
                // Set as active player
                val allLocal = dao.getAllMembers().first()
                allLocal.forEach {
                    if (it.isPlayer) {
                        dao.insertMember(it.copy(isPlayer = false))
                    }
                }
                val loggedUser = localUser.copy(isPlayer = true)
                dao.insertMember(loggedUser)
                loggedUser
            } else {
                null
            }
        }
    }

    suspend fun syncAllWithFirebase(username: String) {
        val cleanUsername = username.trim().lowercase()
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        try {
            // 1. Sync self
            val selfUser = service.getUser(cleanUsername)
            if (selfUser != null) {
                dao.insertMember(selfUser.copy(isPlayer = true))
            }

            // 2. Sync all other players/members
            val allUsers = service.getAllUsers()
            if (allUsers != null) {
                val membersList = allUsers.values.map { member ->
                    if (member.id == cleanUsername) {
                        member.copy(isPlayer = true)
                    } else {
                        member.copy(isPlayer = false)
                    }
                }
                dao.insertMembers(membersList)
            }

            // 3. Sync community debt
            val debt = service.getCommunityDebt() ?: 0
            saveCommunityDebt(debt)

            // 4. Sync tax logs
            val logsMap = service.getTaxLogs()
            if (logsMap != null) {
                logsMap.values.forEach { log ->
                    dao.insertTaxLog(log)
                }
            }

            // 5. Sync user tasks
            val tasksMap = service.getTasks(cleanUsername)
            if (tasksMap != null) {
                tasksMap.values.forEach { task ->
                    dao.insertTask(task)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun populateDefaultNpcsInFirebase(url: String) {
        val service = FirebaseClient.getService(url)
        val baseMembers = listOf(
            CommunityMember("npc_charlemagne", "L'Empereur Charlemagne", "Empereur", 5000, 0, 0, "L'Empire s'étend par la loi et l'honneur.", avatarEmoji = "👑"),
            CommunityMember("npc_louis", "Le Roi Louis XIV", "Roi", 3500, 4, 4, "L'État, c'est moi.", avatarEmoji = "⚜️"),
            CommunityMember("npc_arthur", "Le Prince Arthur", "Prince", 2600, 2, 0, "Pour la Table Ronde et la justice !", avatarEmoji = "🤴"),
            CommunityMember("npc_bourgogne", "Le Duc de Bourgogne", "Duc", 1800, 0, 4, "Mes terres regorgent de richesses.", avatarEmoji = "🏰"),
            CommunityMember("npc_lafayette", "Le Marquis de Lafayette", "Marquis", 1400, 4, 0, "La liberté guidera nos peuples.", avatarEmoji = "📯"),
            CommunityMember("npc_monte_cristo", "Le Comte de Monte-Cristo", "Comte", 1100, 1, 1, "Attendre et espérer.", avatarEmoji = "🎭"),
            CommunityMember("npc_robert", "Le Vicomte Robert", "Vicomte", 850, 3, 3, "Loyal serviteur de la couronne.", avatarEmoji = "🗡️"),
            CommunityMember("npc_godefroy", "Le Baron Godefroy", "Baron", 650, 1, 3, "Que trépasse si je faiblis !", avatarEmoji = "🏯"),
            CommunityMember("npc_bayard", "Le Chevalier Bayard", "Chevalier", 450, 3, 1, "Sans peur et sans reproche.", avatarEmoji = "🏇"),
            CommunityMember("npc_hugues", "Le Seigneur Hugues", "Seigneur", 300, 2, 4, "La terre nourrit mon autorité.", avatarEmoji = "🏹")
        )
        baseMembers.forEach { npc ->
            val questPair = HuggingFaceClient.getFallbackQuest(npc.title)
            val updatedNpc = npc.copy(
                dailyQuest = questPair.first,
                dailyQuestReward = questPair.second,
                dailyQuestStatus = "En cours"
            )
            try {
                service.saveUser(npc.id, updatedNpc)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Firebase Sync Backers for Tasks and States ---
    private suspend fun uploadTasksToFirebase(username: String) {
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        try {
            val localTasks = dao.getAllTasks().first()
            val tasksMap = localTasks.associateBy { it.id.toString() }
            service.saveTasks(username, tasksMap)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun uploadUserProfileToFirebase(user: CommunityMember) {
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        try {
            // Always save with isPlayer = false to Firebase database so other players see them as a neighbor
            service.saveUser(user.id, user.copy(isPlayer = false))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun uploadCommunityDebtToFirebase(debt: Int) {
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        try {
            service.saveCommunityDebt(debt)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun uploadTaxLogToFirebase(log: TaxLog) {
        val url = getFirebaseUrl()
        val service = FirebaseClient.getService(url)
        try {
            service.addTaxLog(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Tasks ---
    suspend fun insertTask(task: Task, playerUsername: String?) {
        dao.insertTask(task)
        if (playerUsername != null) {
            uploadTasksToFirebase(playerUsername)
        }
    }

    suspend fun updateTask(task: Task, playerUsername: String?) {
        dao.updateTask(task)
        if (playerUsername != null) {
            uploadTasksToFirebase(playerUsername)
        }
        
        // Reward points to player on completion
        if (task.completed) {
            val player = getPlayer()
            if (player != null) {
                val reward = task.pointsReward
                val newPoints = player.points + reward
                updateMemberPointsAndTitle(player.id, newPoints, playerUsername)
            }
        }
    }

    suspend fun deleteTask(id: Int, playerUsername: String?) {
        dao.deleteTaskById(id)
        if (playerUsername != null) {
            uploadTasksToFirebase(playerUsername)
        }
    }

    suspend fun getPlayer(): CommunityMember? {
        val allLocal = dao.getAllMembers().first()
        return allLocal.find { it.isPlayer }
    }

    suspend fun insertMember(member: CommunityMember, playerUsername: String?) {
        dao.insertMember(member)
        if (member.id == playerUsername) {
            uploadUserProfileToFirebase(member)
        }
    }

    fun getTitleForPoints(points: Int): String {
        return when {
            points < 50 -> "Esclave"
            points < 100 -> "Ouvrier"
            points < 200 -> "Artisan"
            points < 350 -> "Seigneur"
            points < 500 -> "Chevalier"
            points < 700 -> "Baron"
            points < 900 -> "Vicomte"
            points < 1200 -> "Comte"
            points < 1500 -> "Marquis"
            points < 2000 -> "Duc"
            points < 2500 -> "Prince"
            points < 3500 -> "Roi"
            else -> "Empereur"
        }
    }

    suspend fun updateMemberPointsAndTitle(id: String, newPoints: Int, playerUsername: String?) {
        val member = dao.getMemberById(id) ?: return
        val finalPoints = if (newPoints < 0) 0 else newPoints
        val newTitle = getTitleForPoints(finalPoints)
        
        // Handle representative debt
        if (newPoints < 0 && isRepresentative(member.title)) {
            val currentDebt = getCommunityDebt()
            saveCommunityDebt(currentDebt + abs(newPoints))
        }

        val updated = member.copy(points = finalPoints, title = newTitle)
        dao.insertMember(updated)
        
        if (id == playerUsername) {
            uploadUserProfileToFirebase(updated)
        } else {
            // Also upload if it's an NPC or another user we modified
            uploadUserProfileToFirebase(updated)
        }
    }

    private fun isRepresentative(title: String): Boolean {
        return title in listOf("Empereur", "Roi", "Prince", "Duc", "Marquis", "Comte", "Vicomte", "Baron")
    }

    suspend fun getCommunityDebt(): Int {
        val state = dao.getGameState("community_debt")
        return state?.value?.toIntOrNull() ?: 0
    }

    suspend fun saveCommunityDebt(debt: Int) {
        dao.saveGameState(GameState("community_debt", debt.toString()))
        uploadCommunityDebtToFirebase(debt)
    }

    suspend fun payCommunityDebt(amount: Int, playerUsername: String?): Boolean {
        val player = getPlayer() ?: return false
        val currentDebt = getCommunityDebt()
        if (player.points >= amount && currentDebt > 0) {
            val newDebt = (currentDebt - amount).coerceAtLeast(0)
            saveCommunityDebt(newDebt)
            updateMemberPointsAndTitle(player.id, player.points - amount, playerUsername)
            
            val log = TaxLog(
                payerName = player.name,
                payerTitle = player.title,
                initialAmount = amount,
                details = "A remboursé $amount points de la dette de la communauté. Dette restante : $newDebt."
            )
            dao.insertTaxLog(log)
            uploadTaxLogToFirebase(log)
            return true
        }
        return false
    }

    suspend fun initializeCommunity() {
        val existing = dao.getAllMembers().first()
        if (existing.isNotEmpty()) return

        // Fallback offline init if they aren't logged in yet
        val baseMembers = listOf(
            CommunityMember("offline_player", "Moi (Challenger)", "Artisan", 100, 2, 2, "En route vers la gloire féodale !", isPlayer = true, avatarEmoji = "🛡️")
        )
        dao.insertMembers(baseMembers)
        saveCommunityDebt(0)
    }

    // --- Feudal Cascade Tax Algorithm ---
    suspend fun performDailyTaxDeduction(playerUsername: String?): String {
        val members = dao.getAllMembers().first().toMutableList()
        val builder = java.lang.StringBuilder()

        // 1. Point Penalties & Gather Population Taxes
        val taxpayers = members.filter { !isRepresentative(it.title) }
        var totalPopulationTax = 0

        builder.append("=== DÉBUT DES TAXES FÉODALES ===\n")
        
        taxpayers.forEach { taxPayer ->
            val penalty = 5
            val taxAmount = (taxPayer.points * 0.15).toInt().coerceAtMost(taxPayer.points)
            val totalDeduction = penalty + taxAmount
            val newPoints = (taxPayer.points - totalDeduction).coerceAtLeast(0)
            
            if (taxPayer.isPlayer) {
                builder.append("• Vous payez $taxAmount pts de taxes + $penalty pts de pénalité de fin de journée.\n")
            } else {
                builder.append("• ${taxPayer.name} (${taxPayer.title}) paye $taxAmount pts de taxe.\n")
            }
            
            totalPopulationTax += taxAmount
            
            val updatedMember = taxPayer.copy(
                points = newPoints,
                title = getTitleForPoints(newPoints)
            )
            members[members.indexOf(taxPayer)] = updatedMember
        }

        builder.append("\nTaxe totale de la population collectée : $totalPopulationTax points.\n\n")

        // 2. Cascade taxes up the hierarchy
        val baron = members.find { it.title == "Baron" }
        val vicomte = members.find { it.title == "Vicomte" }
        val comte = members.find { it.title == "Comte" }
        val marquis = members.find { it.title == "Marquis" }
        val duc = members.find { it.title == "Duc" }
        val prince = members.find { it.title == "Prince" }
        val roi = members.find { it.title == "Roi" }
        val empereur = members.find { it.title == "Empereur" }

        var currentTaxFlow = totalPopulationTax

        // Baron
        if (baron != null && currentTaxFlow > 0) {
            val baronCut = (currentTaxFlow * 0.20).toInt()
            val payToVicomte = currentTaxFlow - baronCut
            val newPoints = baron.points + baronCut
            members[members.indexOf(baron)] = baron.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Baron (${baron.name}) perçoit $currentTaxFlow pts, garde son écot de $baronCut pts (20%), transmet $payToVicomte pts au Vicomte.\n")
            currentTaxFlow = payToVicomte
        }

        // Vicomte
        if (vicomte != null && currentTaxFlow > 0) {
            val vicomteCut = (currentTaxFlow * 0.20).toInt()
            val payToComte = currentTaxFlow - vicomteCut
            val newPoints = vicomte.points + vicomteCut
            members[members.indexOf(vicomte)] = vicomte.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Vicomte (${vicomte.name}) reçoit $currentTaxFlow pts, garde $vicomteCut pts (20%), transmet $payToComte pts au Comte.\n")
            currentTaxFlow = payToComte
        }

        // Comte
        if (comte != null && currentTaxFlow > 0) {
            val comteCut = (currentTaxFlow * 0.20).toInt()
            val payToMarquis = currentTaxFlow - comteCut
            val newPoints = comte.points + comteCut
            members[members.indexOf(comte)] = comte.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Comte (${comte.name}) reçoit $currentTaxFlow pts, garde $comteCut pts (20%), transmet $payToMarquis pts au Marquis.\n")
            currentTaxFlow = payToMarquis
        }

        // Marquis
        if (marquis != null && currentTaxFlow > 0) {
            val marquisCut = (currentTaxFlow * 0.20).toInt()
            val payToDuc = currentTaxFlow - marquisCut
            val newPoints = marquis.points + marquisCut
            members[members.indexOf(marquis)] = marquis.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Marquis (${marquis.name}) reçoit $currentTaxFlow pts, garde $marquisCut pts (20%), transmet $payToDuc pts au Duc.\n")
            currentTaxFlow = payToDuc
        }

        // Duc
        if (duc != null && currentTaxFlow > 0) {
            val ducCut = (currentTaxFlow * 0.20).toInt()
            val payToPrince = currentTaxFlow - ducCut
            val newPoints = duc.points + ducCut
            members[members.indexOf(duc)] = duc.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Duc (${duc.name}) reçoit $currentTaxFlow pts, garde $ducCut pts (20%), transmet $payToPrince pts au Prince.\n")
            currentTaxFlow = payToPrince
        }

        // Prince
        if (prince != null && currentTaxFlow > 0) {
            val princeCut = (currentTaxFlow * 0.20).toInt()
            val payToRoi = currentTaxFlow - princeCut
            val newPoints = prince.points + princeCut
            members[members.indexOf(prince)] = prince.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Prince (${prince.name}) reçoit $currentTaxFlow pts, garde $princeCut pts (20%), transmet $payToRoi pts au Roi.\n")
            currentTaxFlow = payToRoi
        }

        // Roi
        if (roi != null && currentTaxFlow > 0) {
            val roiCut = (currentTaxFlow * 0.20).toInt()
            val payToEmpereur = currentTaxFlow - roiCut
            val newPoints = roi.points + roiCut
            members[members.indexOf(roi)] = roi.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Roi (${roi.name}) reçoit $currentTaxFlow pts, garde $roiCut pts (20%), transmet $payToEmpereur pts à l'Empereur.\n")
            currentTaxFlow = payToEmpereur
        }

        // Empereur
        if (empereur != null && currentTaxFlow > 0) {
            val newPoints = empereur.points + currentTaxFlow
            members[members.indexOf(empereur)] = empereur.copy(points = newPoints, title = getTitleForPoints(newPoints))
            builder.append("👑 Empereur (${empereur.name}) perçoit le tribut final de $currentTaxFlow pts.\n")
        }

        // 3. Resolve active Daily Quests for non-player members
        val npcs = members.filter { it.id != playerUsername }
        if (npcs.isNotEmpty()) {
            builder.append("\n=== RÉSOLUTION DES QUÊTES DU JOUR ===\n")
            npcs.forEach { npc ->
                val quest = npc.dailyQuest
                if (quest != null && npc.dailyQuestStatus == "En cours") {
                    val success = Random.nextFloat() < 0.75f
                    if (success) {
                        val reward = npc.dailyQuestReward
                        val newPoints = npc.points + reward
                        val newTitle = getTitleForPoints(newPoints)
                        val updatedNpc = npc.copy(
                            points = newPoints,
                            title = newTitle,
                            dailyQuestStatus = "Accompli"
                        )
                        members[members.indexOf(npc)] = updatedNpc
                        builder.append("✅ ${npc.name} (${npc.title}) a ACCOMPLI sa quête : \"$quest\" (+${reward} pts).\n")
                    } else {
                        val updatedNpc = npc.copy(
                            dailyQuestStatus = "Échoué"
                        )
                        members[members.indexOf(npc)] = updatedNpc
                        builder.append("❌ ${npc.name} (${npc.title}) a ÉCHOUÉ sa quête : \"$quest\".\n")
                    }
                }
            }
        }

        // 4. Automatically roll / assign NEW Daily Quests for the next day
        builder.append("\n=== ATTRIBUTION DES NOUVELLES QUÊTES (LENDEMAIN) ===\n")
        members.toList().forEach { member ->
            val nextQuestPair = HuggingFaceClient.getFallbackQuest(member.title)
            val updatedMember = member.copy(
                dailyQuest = nextQuestPair.first,
                dailyQuestReward = nextQuestPair.second,
                dailyQuestStatus = "En cours"
            )
            val idx = members.indexOfFirst { it.id == member.id }
            if (idx != -1) {
                members[idx] = updatedMember
            }
            if (member.id == playerUsername) {
                builder.append("🔮 Nouvelle quête pour Vous (${member.title}) : \"${nextQuestPair.first}\" (+${nextQuestPair.second} pts).\n")
            } else {
                builder.append("🔮 Nouvelle quête pour ${member.name} (${member.title}) : \"${nextQuestPair.first}\" (+${nextQuestPair.second} pts).\n")
            }
        }

        // Save locally
        dao.insertMembers(members)

        // Sync all updated members to Firebase
        members.forEach { m ->
            uploadUserProfileToFirebase(m)
        }

        // Store log locally and on Firebase
        val logDetails = builder.toString()
        val taxLog = TaxLog(
            payerName = "La Population",
            payerTitle = "Féodalité",
            initialAmount = totalPopulationTax,
            details = logDetails
        )
        dao.insertTaxLog(taxLog)
        uploadTaxLogToFirebase(taxLog)

        return logDetails
    }

    // --- Duel Outcome Handling ---
    suspend fun resolveDuel(challengerId: String, representativeId: String, won: Boolean, playerUsername: String?): String {
        val challenger = dao.getMemberById(challengerId) ?: return "Erreur: Challenger non trouvé"
        val representative = dao.getMemberById(representativeId) ?: return "Erreur: Représentant non trouvé"

        return if (won) {
            val usurpedTitle = representative.title
            val usurpedPoints = representative.points

            val updatedChallenger = challenger.copy(
                title = usurpedTitle,
                points = usurpedPoints
            )
            val demotedRepresentative = representative.copy(
                title = "Esclave",
                points = 10,
                statusMessage = "J'ai tout perdu face à ${challenger.name} lors d'un duel royal..."
            )

            dao.insertMember(updatedChallenger)
            dao.insertMember(demotedRepresentative)

            // Sync both to Firebase
            uploadUserProfileToFirebase(updatedChallenger)
            uploadUserProfileToFirebase(demotedRepresentative)

            "VICTOIRE ! Vous avez vaincu ${representative.name} ! Vous usurpez le titre de ${usurpedTitle} et réclamez ses ${usurpedPoints} points. Le représentant déchu est banni au rang d'Esclave."
        } else {
            val lostPoints = challenger.points / 2
            val updatedChallenger = challenger.copy(
                points = lostPoints,
                title = getTitleForPoints(lostPoints)
            )
            dao.insertMember(updatedChallenger)
            
            // Sync to Firebase
            uploadUserProfileToFirebase(updatedChallenger)

            "DÉFAITE ! Vous avez perdu le duel face à ${representative.name}. Votre honneur est bafoué, vous perdez la moitié de vos points ($lostPoints pts perdus)."
        }
    }
}
