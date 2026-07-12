package com.example.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

data class DuelState(
    val opponent: CommunityMember,
    val secretNumber: Int,
    val attemptsLeft: Int = 7,
    val playerGuesses: List<Pair<Int, String>> = emptyList(), // Pair of (Guess, Feedback)
    val opponentGuesses: List<Pair<Int, String>> = emptyList(),
    val winner: String? = null, // "PLAYER", "OPPONENT", or null
    val explanation: String = "Le duel commence ! Devinez le nombre mystère entre 0 et 1000."
)

class FiefdomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FiefdomRepository
    private val prefs = application.getSharedPreferences("fiefdom_prefs", Context.MODE_PRIVATE)
    
    // UI state streams
    val tasks: StateFlow<List<Task>>
    val members: StateFlow<List<CommunityMember>>
    val taxLogs: StateFlow<List<TaxLog>>
    
    // Player details
    var player by mutableStateOf<CommunityMember?>(null)
        private set
        
    var communityDebt by mutableStateOf(0)
        private set

    var isGeneratingTasks by mutableStateOf(false)
        private set

    var isGeneratingQuest by mutableStateOf(false)
        private set

    var taxLogResult by mutableStateOf<String?>(null)
        private set

    var currentDuel by mutableStateOf<DuelState?>(null)
        private set

    var activeTab by mutableStateOf("dashboard") // "dashboard", "tasks", "map", "leaderboard", "settings"

    // Authentication States
    var playerUsername by mutableStateOf<String?>(null)
        private set
    var isAuthenticated by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)
    var isAuthLoading by mutableStateOf(false)

    // AI & Connection Config
    var firebaseBaseUrl by mutableStateOf(FirebaseClient.DEFAULT_FIREBASE_URL)
        private set
    var huggingFaceToken by mutableStateOf("")
        private set

    // Opponent state bounds for smart guessing
    private var opponentMin = 0
    private var opponentMax = 1000

    init {
        val database = FiefdomDatabase.getDatabase(application)
        repository = FiefdomRepository(database.fiefdomDao())

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        members = repository.allMembers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        taxLogs = repository.allTaxLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Load configs
        viewModelScope.launch {
            firebaseBaseUrl = repository.getFirebaseUrl()
            huggingFaceToken = repository.getHuggingFaceToken()
            
            // Check active login session
            val savedUsername = prefs.getString("username", null)
            if (savedUsername != null) {
                playerUsername = savedUsername
                isAuthenticated = true
                repository.syncAllWithFirebase(savedUsername)
                updatePlayerAndDebt()
            } else {
                repository.initializeCommunity()
                updatePlayerAndDebt()
            }
        }

        // Listen for database changes to keep player & debt state updated
        viewModelScope.launch {
            repository.allMembers.collect {
                updatePlayerAndDebt()
            }
        }
    }

    private suspend fun updatePlayerAndDebt() {
        player = repository.getPlayer()
        communityDebt = repository.getCommunityDebt()
    }

    // --- Authentication Actions ---
    fun register(username: String, passwordRaw: String, name: String, avatarEmoji: String) {
        val cleanUser = username.trim().lowercase().filter { it.isLetterOrDigit() }
        if (cleanUser.isEmpty()) {
            authError = "Le nom d'utilisateur doit être composé de lettres ou de chiffres."
            return
        }
        if (passwordRaw.length < 4) {
            authError = "Le mot de passe doit contenir au moins 4 caractères."
            return
        }
        if (name.trim().isEmpty()) {
            authError = "Veuillez entrer un nom d'affichage médiéval."
            return
        }

        viewModelScope.launch {
            isAuthLoading = true
            authError = null
            try {
                val success = repository.registerUser(cleanUser, passwordRaw, name, avatarEmoji)
                if (success) {
                    prefs.edit().putString("username", cleanUser).apply()
                    playerUsername = cleanUser
                    isAuthenticated = true
                    repository.syncAllWithFirebase(cleanUser)
                    updatePlayerAndDebt()
                } else {
                    authError = "Ce nom d'utilisateur est déjà pris par un autre seigneur !"
                }
            } catch (e: Exception) {
                authError = "Erreur de connexion médiévale : ${e.message}"
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun login(username: String, passwordRaw: String) {
        val cleanUser = username.trim().lowercase().filter { it.isLetterOrDigit() }
        if (cleanUser.isEmpty() || passwordRaw.isEmpty()) {
            authError = "Veuillez remplir tous les champs."
            return
        }

        viewModelScope.launch {
            isAuthLoading = true
            authError = null
            try {
                val user = repository.loginUser(cleanUser, passwordRaw)
                if (user != null) {
                    prefs.edit().putString("username", cleanUser).apply()
                    playerUsername = cleanUser
                    isAuthenticated = true
                    updatePlayerAndDebt()
                } else {
                    authError = "Identifiants incorrects ou seigneur inconnu !"
                }
            } catch (e: Exception) {
                authError = "Erreur de connexion : ${e.message}"
            } finally {
                isAuthLoading = false
            }
        }
    }

    fun logout() {
        prefs.edit().remove("username").apply()
        playerUsername = null
        isAuthenticated = false
        player = null
        activeTab = "dashboard"
    }

    // --- Config Editing Actions ---
    fun saveFirebaseConfig(url: String) {
        viewModelScope.launch {
            repository.saveFirebaseUrl(url)
            firebaseBaseUrl = url
            playerUsername?.let { repository.syncAllWithFirebase(it) }
        }
    }

    fun saveHuggingFaceConfig(token: String) {
        viewModelScope.launch {
            repository.saveHuggingFaceToken(token)
            huggingFaceToken = token
        }
    }

    // --- Add/Toggle Tasks ---
    fun addPersonalTask(title: String) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    type = "PERSONAL",
                    pointsReward = 5
                ),
                playerUsername
            )
        }
    }

    fun generateCommunityTasks() {
        val currentTitle = player?.title ?: "Artisan"
        viewModelScope.launch {
            isGeneratingTasks = true
            try {
                // Generates from HuggingFace Serverless Inference API!
                val generated = HuggingFaceClient.generateCommunityTasks(currentTitle, huggingFaceToken)
                generated.forEach { taskTitle ->
                    repository.insertTask(
                        Task(
                            title = taskTitle,
                            type = "COMMUNITY",
                            pointsReward = 10
                        ),
                        playerUsername
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGeneratingTasks = false
            }
        }
    }

    fun generateQuestForMember(member: CommunityMember) {
        viewModelScope.launch {
            isGeneratingQuest = true
            try {
                // Generates quest using Hugging Face instead of Gemini!
                val (questDescription, reward) = HuggingFaceClient.generateMemberQuest(member.name, member.title, huggingFaceToken)
                val updated = member.copy(
                    dailyQuest = questDescription,
                    dailyQuestReward = reward,
                    dailyQuestStatus = "En cours"
                )
                repository.insertMember(updated, playerUsername)
                updatePlayerAndDebt()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isGeneratingQuest = false
            }
        }
    }

    fun completePlayerDailyQuest() {
        val p = player ?: return
        val quest = p.dailyQuest ?: return
        if (p.dailyQuestStatus == "En cours") {
            viewModelScope.launch {
                val reward = p.dailyQuestReward
                val updated = p.copy(
                    points = p.points + reward,
                    title = repository.getTitleForPoints(p.points + reward),
                    dailyQuestStatus = "Accompli"
                )
                repository.insertMember(updated, playerUsername)
                updatePlayerAndDebt()
            }
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(completed = !task.completed), playerUsername)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id, playerUsername)
        }
    }

    // --- End of Day (Taxes & Penalty) ---
    fun simulateEndOfDay() {
        viewModelScope.launch {
            val log = repository.performDailyTaxDeduction(playerUsername)
            taxLogResult = log
            updatePlayerAndDebt()
        }
    }

    fun clearTaxLogResult() {
        taxLogResult = null
    }

    // --- Pay Community Debt ---
    fun payDebt(amount: Int) {
        viewModelScope.launch {
            repository.payCommunityDebt(amount, playerUsername)
            updatePlayerAndDebt()
        }
    }

    // --- Duel Operations ---
    fun startDuel(opponent: CommunityMember, forceForfeit: Boolean) {
        val challengerId = playerUsername ?: "offline_player"
        if (forceForfeit) {
            viewModelScope.launch {
                val report = repository.resolveDuel(challengerId, opponent.id, won = true, playerUsername)
                currentDuel = DuelState(
                    opponent = opponent,
                    secretNumber = 0,
                    attemptsLeft = 0,
                    winner = "PLAYER",
                    explanation = "Le représentant a décliné le duel ! Il abdique immédiatement.\n\n$report"
                )
                updatePlayerAndDebt()
            }
        } else {
            opponentMin = 0
            opponentMax = 1000
            val secret = Random.nextInt(0, 1001)
            currentDuel = DuelState(
                opponent = opponent,
                secretNumber = secret,
                explanation = "Le combat d'esprit commence ! Trouvez le nombre mystère entre 0 et 1000 avant ${opponent.name}."
            )
        }
    }

    fun makeDuelGuess(playerGuess: Int) {
        val state = currentDuel ?: return
        if (state.winner != null || state.attemptsLeft <= 0) return

        val secret = state.secretNumber
        
        // 1. Process Player's Guess
        val playerFeedback = when {
            playerGuess == secret -> "TROUVÉ !"
            playerGuess < secret -> "Trop petit ⬆️"
            else -> "Trop grand ⬇️"
        }
        val updatedPlayerGuesses = state.playerGuesses + (playerGuess to playerFeedback)

        if (playerGuess == secret) {
            resolveDuelOutcome(state, updatedPlayerGuesses, state.opponentGuesses, "PLAYER")
            return
        }

        // 2. Process Opponent's Smart Guess
        val opponentGuess = makeOpponentGuess(secret)
        val opponentFeedback = when {
            opponentGuess == secret -> "TROUVÉ !"
            opponentGuess < secret -> {
                opponentMin = (opponentGuess + 1).coerceAtMost(1000)
                "Trop petit ⬆️"
            }
            else -> {
                opponentMax = (opponentGuess - 1).coerceAtLeast(0)
                "Trop grand ⬇️"
            }
        }
        val updatedOpponentGuesses = state.opponentGuesses + (opponentGuess to opponentFeedback)

        if (opponentGuess == secret) {
            resolveDuelOutcome(state, updatedPlayerGuesses, updatedOpponentGuesses, "OPPONENT")
            return
        }

        // 3. Check attempts
        val nextAttempts = state.attemptsLeft - 1
        if (nextAttempts <= 0) {
            val lastPlayerGuess = playerGuess
            val lastOpponentGuess = opponentGuess
            
            val playerDiff = abs(lastPlayerGuess - secret)
            val opponentDiff = abs(lastOpponentGuess - secret)

            val finalWinner = if (playerDiff <= opponentDiff) "PLAYER" else "OPPONENT"
            resolveDuelOutcome(
                state.copy(attemptsLeft = 0),
                updatedPlayerGuesses,
                updatedOpponentGuesses,
                finalWinner,
                suffix = " (Fin des 7 tentatives. Le plus proche gagne !)"
            )
        } else {
            currentDuel = state.copy(
                attemptsLeft = nextAttempts,
                playerGuesses = updatedPlayerGuesses,
                opponentGuesses = updatedOpponentGuesses,
                explanation = "Tentative ${8 - nextAttempts}/7 : Votre estimation était $playerGuess ($playerFeedback). L'adversaire a estimé $opponentGuess ($opponentFeedback)."
            )
        }
    }

    private fun makeOpponentGuess(secret: Int): Int {
        val mid = (opponentMin + opponentMax) / 2
        val deviationRange = ((opponentMax - opponentMin) * 0.10).toInt().coerceAtLeast(1)
        
        val guess = if (Random.nextFloat() < 0.85f) {
            mid
        } else {
            (mid + Random.nextInt(-deviationRange, deviationRange + 1)).coerceIn(opponentMin, opponentMax)
        }
        return guess.coerceIn(0, 1000)
    }

    private fun resolveDuelOutcome(
        currentState: DuelState,
        pGuesses: List<Pair<Int, String>>,
        oGuesses: List<Pair<Int, String>>,
        winner: String,
        suffix: String = ""
    ) {
        viewModelScope.launch {
            val playerWon = winner == "PLAYER"
            val challengerId = playerUsername ?: "offline_player"
            val report = repository.resolveDuel(challengerId, currentState.opponent.id, won = playerWon, playerUsername)
            
            currentDuel = currentState.copy(
                winner = winner,
                playerGuesses = pGuesses,
                opponentGuesses = oGuesses,
                explanation = if (playerWon) {
                    "VICTOIRE ROYALE ! Le nombre mystère était ${currentState.secretNumber}.$suffix\n\n$report"
                } else {
                    "DÉFAITE CUISANTE... Le nombre mystère était ${currentState.secretNumber}.$suffix\n\n$report"
                }
            )
            updatePlayerAndDebt()
        }
    }

    fun endDuel() {
        currentDuel = null
    }
}
