package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CommunityMember
import com.example.data.Task
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiefdomGameScreen(
    viewModel: FiefdomViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val taxLogs by viewModel.taxLogs.collectAsStateWithLifecycle()
    val player = viewModel.player
    val communityDebt = viewModel.communityDebt
    val currentDuel = viewModel.currentDuel
    val activeTab = viewModel.activeTab

    // Notification of challengeable representatives
    val challengeableNpcs = remember(members, player) {
        if (player == null) emptyList() else {
            members.filter { npc ->
                !npc.isPlayer && 
                npc.points < player.points && 
                isRankHigher(npc.title, player.title)
            }
        }
    }

    if (!viewModel.isAuthenticated) {
        AuthScreen(viewModel)
    } else {
        Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = "Fiefdom Quest Logo",
                            tint = GoldCrown,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Fiefdom Quest",
                            color = GoldCrown,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }
                },
                actions = {
                    player?.let {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CastleWall)
                                .border(1.dp, GoldCrown, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                it.avatarEmoji,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text(
                                it.title,
                                color = GoldCrown,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            VerticalDivider(modifier = Modifier.height(16.dp), color = KnightSilver)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${it.points} pts",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CastleSlate
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CastleWall,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "dashboard",
                    onClick = { viewModel.activeTab = "dashboard" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Fief") },
                    label = { Text("Mon Fief", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldCrown,
                        selectedTextColor = GoldCrown,
                        indicatorColor = CastleSlate,
                        unselectedIconColor = KnightSilver,
                        unselectedTextColor = KnightSilver
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "tasks",
                    onClick = { viewModel.activeTab = "tasks" },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = "Tâches") },
                    label = { Text("Tâches", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldCrown,
                        selectedTextColor = GoldCrown,
                        indicatorColor = CastleSlate,
                        unselectedIconColor = KnightSilver,
                        unselectedTextColor = KnightSilver
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "map",
                    onClick = { viewModel.activeTab = "map" },
                    icon = { Icon(Icons.Default.Map, contentDescription = "Carte") },
                    label = { Text("Carte", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldCrown,
                        selectedTextColor = GoldCrown,
                        indicatorColor = CastleSlate,
                        unselectedIconColor = KnightSilver,
                        unselectedTextColor = KnightSilver
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "leaderboard",
                    onClick = { viewModel.activeTab = "leaderboard" },
                    icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "Classement") },
                    label = { Text("Leaderboard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldCrown,
                        selectedTextColor = GoldCrown,
                        indicatorColor = CastleSlate,
                        unselectedIconColor = KnightSilver,
                        unselectedTextColor = KnightSilver
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "settings",
                    onClick = { viewModel.activeTab = "settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Paramètres") },
                    label = { Text("Paramètres", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldCrown,
                        selectedTextColor = GoldCrown,
                        indicatorColor = CastleSlate,
                        unselectedIconColor = KnightSilver,
                        unselectedTextColor = KnightSilver
                    )
                )
            }
        },
        containerColor = CastleSlate
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Warning Banner for vulnerable NPC / Usurpation option
                if (challengeableNpcs.isNotEmpty() && player != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VelvetPurple),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationImportant,
                                    contentDescription = "Duel notification",
                                    tint = GoldCrown,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${challengeableNpcs.size} titre(s) vulnérable(s) à un duel !",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = { viewModel.activeTab = "leaderboard" },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Défier", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Community Debt Pool Info Box
                if (communityDebt > 0) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoyalCrimson),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MonetizationOn,
                                    contentDescription = "Dette commune",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Dette de la Communauté : $communityDebt pts",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "La dette des dirigeants est à la charge de tous !",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            var showPayDebtDialog by remember { mutableStateOf(false) }
                            Button(
                                onClick = { showPayDebtDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Rembourser", color = RoyalCrimson, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            if (showPayDebtDialog) {
                                PayDebtDialog(
                                    currentDebt = communityDebt,
                                    playerPoints = player?.points ?: 0,
                                    onDismiss = { showPayDebtDialog = false },
                                    onConfirm = { amount ->
                                        viewModel.payDebt(amount)
                                        showPayDebtDialog = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Display Selected Tab Screen
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "TabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        "dashboard" -> DashboardTab(viewModel, player, taxLogs)
                        "tasks" -> TasksTab(viewModel, tasks)
                        "map" -> MapTab(viewModel, members)
                        "leaderboard" -> LeaderboardTab(viewModel, members, player)
                        "settings" -> SettingsTab(viewModel)
                    }
                }
            }

            // Duel Arena Dialog Overlay
            currentDuel?.let { duel ->
                DuelArenaDialog(
                    duelState = duel,
                    onGuessSubmitted = { guess -> viewModel.makeDuelGuess(guess) },
                    onQuitArena = { viewModel.endDuel() }
                )
            }

            // End of day tax result report dialog
            viewModel.taxLogResult?.let { log ->
                Dialog(onDismissRequest = { viewModel.clearTaxLogResult() }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = CastleWall),
                        border = BorderStroke(2.dp, GoldCrown),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HistoryEdu, "Taxes", tint = GoldCrown)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Journal des Taxes Féodales",
                                    color = GoldCrown,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = KnightSilver.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(modifier = Modifier.heightIn(max = 300.dp)) {
                                LazyColumn {
                                    item {
                                        Text(
                                            log,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.clearTaxLogResult() },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldCrown)
                            ) {
                                Text("Compris, Messire", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// --- DASHBOARD TAB ---
@Composable
fun DashboardTab(
    viewModel: FiefdomViewModel,
    player: CommunityMember?,
    taxLogs: List<com.example.data.TaxLog>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero / Header Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CastleWall),
                border = BorderStroke(1.dp, GoldCrown),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        player?.avatarEmoji ?: "🛡️",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        player?.name ?: "Noble Vagabond",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Display Rank Badge
                    player?.let {
                        RankBadge(title = it.title)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        player?.statusMessage ?: "",
                        color = KnightSilver,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = KnightSilver.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("POINTS DE POUVOIR", color = KnightSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${player?.points ?: 0}", color = GoldCrown, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        VerticalDivider(modifier = Modifier.height(40.dp), color = KnightSilver.copy(alpha = 0.3f))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PROCHAIN SEUIL", color = KnightSilver, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            val nextThreshold = getNextRankThreshold(player?.points ?: 0)
                            Text(nextThreshold, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Player's Daily Feudal Quest Card
        player?.dailyQuest?.let { quest ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CastleWall),
                    border = BorderStroke(1.dp, VelvetPurple),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = "Quest Icon",
                                    tint = GoldCrown,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Quête Féodale Quotidienne",
                                    color = GoldCrown,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            
                            // Quest status badge
                            val statusText = player.dailyQuestStatus ?: "En cours"
                            val statusColor = when (statusText) {
                                "Accompli" -> ForestGreen
                                "Échoué" -> RoyalCrimson
                                else -> VelvetPurple
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(statusColor.copy(alpha = 0.2f))
                                    .border(1.dp, statusColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    statusText.uppercase(),
                                    color = statusColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "\"$quest\"",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Récompense : +${player.dailyQuestReward} points de pouvoir",
                                color = KnightSilver,
                                fontSize = 11.sp
                            )

                            if (player.dailyQuestStatus == "En cours") {
                                Button(
                                    onClick = { viewModel.completePlayerDailyQuest() },
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetPurple),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("complete_player_quest_btn")
                                ) {
                                    Text("Accomplir", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action: End of Day / Taxes Simulation
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CastleWall),
                border = BorderStroke(1.dp, KnightSilver.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Cloche du Crépuscule",
                        color = GoldCrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Simulez la fin de journée. Si vous n'avez fait aucune tâche aujourd'hui, vous subirez une pénalité et paierez des impôts qui monteront en cascade féodale jusqu'à l'Empereur.",
                        color = KnightSilver,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.simulateEndOfDay() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("end_of_day_button")
                    ) {
                        Icon(Icons.Default.HourglassBottom, "End day", tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sauter la Journée & Lever les Taxes", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Ledger of Tax History
        item {
            Text(
                "Registre des Transactions Féodales",
                color = GoldCrown,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (taxLogs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CastleWall.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Aucun impôt n'a encore été collecté dans la sénéchaussée.",
                            color = KnightSilver,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(taxLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = CastleWall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HistoryEdu, "Log", tint = KnightSilver, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(log.payerName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Impôt levé : ${log.initialAmount} pts", color = GoldCrown, fontSize = 12.sp)
                                }
                            }
                            Icon(
                                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Expand",
                                tint = KnightSilver
                            )
                        }

                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = KnightSilver.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                log.details,
                                color = KnightSilver,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- TASKS TAB ---
@Composable
fun TasksTab(
    viewModel: FiefdomViewModel,
    tasks: List<Task>
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Headers and generator buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Registre des Devoirs",
                color = GoldCrown,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // AI generator button
                Button(
                    onClick = { viewModel.generateCommunityTasks() },
                    colors = ButtonDefaults.buttonColors(containerColor = VelvetPurple),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !viewModel.isGeneratingTasks,
                    modifier = Modifier.testTag("ai_generate_button")
                ) {
                    if (viewModel.isGeneratingTasks) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.AutoAwesome, "AI generate", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tâches IA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Add Personal task button
                Button(
                    onClick = { showAddTaskDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_task_button")
                ) {
                    Icon(Icons.Default.Add, "Add task", tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Créer", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Task,
                        contentDescription = "Empty tasks",
                        tint = KnightSilver.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Votre registre de devoirs est vide.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Générez des tâches IA (+10 pts) ou créez des tâches personnelles (+5 pts) !",
                        color = KnightSilver,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(tasks) { task ->
                    TaskRow(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskCompleted(task) },
                        onDeleteClick = { viewModel.deleteTask(task.id) }
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        CreateTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title ->
                viewModel.addPersonalTask(title)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskRow(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.completed) CastleWall.copy(alpha = 0.6f) else CastleWall
        ),
        border = BorderStroke(
            1.dp,
            if (task.completed) ForestGreen.copy(alpha = 0.5f) else if (task.type == "COMMUNITY") VelvetPurple.copy(alpha = 0.6f) else KnightSilver.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.completed,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = ForestGreen,
                        uncheckedColor = KnightSilver
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        task.title,
                        color = if (task.completed) KnightSilver else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        style = androidx.compose.ui.text.TextStyle(
                            textDecoration = if (task.completed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (task.type == "COMMUNITY") VelvetPurple else CastleSlate
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                if (task.type == "COMMUNITY") "Communautaire (IA)" else "Personnelle",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "+${task.pointsReward} pts",
                            color = GoldCrown,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Delete task", tint = RoyalCrimson.copy(alpha = 0.8f))
            }
        }
    }
}

// --- MAP TAB (COMMUNITY BOARD) ---
@Composable
fun MapTab(
    viewModel: FiefdomViewModel,
    members: List<CommunityMember>
) {
    var selectedMember by remember { mutableStateOf<CommunityMember?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Carte du Fief Communautaire",
            color = GoldCrown,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            "Les membres de la communauté règnent sur la carte. Sélectionnez une case pour communiquer, défier ou négocier.",
            color = KnightSilver,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 5x5 Map Grid
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CastleWall)
                .border(1.dp, KnightSilver.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            // Background Canvas Grid effect
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 5) {
                    Row(modifier = Modifier.weight(1f)) {
                        for (c in 0 until 5) {
                            val isSelectedCell = selectedMember?.gridX == c && selectedMember?.gridY == r
                            // Find member at c, r
                            val memberAtCell = members.find { it.gridX == c && it.gridY == r }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .border(
                                        0.5.dp,
                                        if (isSelectedCell) GoldCrown else KnightSilver.copy(alpha = 0.15f)
                                    )
                                    .background(
                                        if (isSelectedCell) GoldCrown.copy(alpha = 0.15f)
                                        else if (memberAtCell?.isPlayer == true) ForestGreen.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        if (memberAtCell != null) {
                                            selectedMember = memberAtCell
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (memberAtCell != null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            memberAtCell.avatarEmoji,
                                            fontSize = 24.sp,
                                            modifier = Modifier.animateContentSize()
                                        )
                                        Text(
                                            memberAtCell.name.substringBefore(" ").take(7),
                                            color = if (memberAtCell.isPlayer) GoldCrown else Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Selected Cell / Character Profile Panel
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (selectedMember != null) {
                val member = selectedMember!!
                val player = viewModel.player

                Card(
                    colors = CardDefaults.cardColors(containerColor = CastleWall),
                    border = BorderStroke(1.dp, GoldCrown),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxSize()
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(member.avatarEmoji, fontSize = 28.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            member.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        RankBadge(title = member.title)
                                    }
                                }

                                Text(
                                    "${member.points} pts",
                                    color = GoldCrown,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = KnightSilver.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Déclaration Féodale :",
                                color = KnightSilver,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "\"${member.statusMessage}\"",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                        }

                        // Daily Feudal Quest Section
                        item {
                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = KnightSilver.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Devoir Féodal du Jour :",
                                    color = KnightSilver,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                // Regenerate quest button (uses Gemini!)
                                if (!member.isPlayer) {
                                    Button(
                                        onClick = { viewModel.generateQuestForMember(member) },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelvetPurple),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(20.dp).testTag("regen_npc_quest_btn"),
                                        enabled = !viewModel.isGeneratingQuest
                                    ) {
                                        if (viewModel.isGeneratingQuest) {
                                            CircularProgressIndicator(modifier = Modifier.size(10.dp), color = Color.White, strokeWidth = 1.dp)
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, "Regen", tint = Color.White, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Régénérer (IA)", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            val qText = member.dailyQuest
                            val qReward = member.dailyQuestReward
                            val qStatus = member.dailyQuestStatus ?: "En cours"

                            if (qText != null) {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        "\"$qText\"",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Récompense : +$qReward pts",
                                            color = GoldCrown,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        val npcStatusColor = when (qStatus) {
                                            "Accompli" -> ForestGreen
                                            "Échoué" -> RoyalCrimson
                                            else -> VelvetPurple
                                        }
                                        Text(
                                            qStatus.uppercase(),
                                            color = npcStatusColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Aucun devoir pour le moment.",
                                    color = KnightSilver.copy(alpha = 0.6f),
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Duel Challenge Option
                        if (!member.isPlayer && player != null) {
                            item {
                                val userCanChallenge = player.points > member.points && isRankHigher(member.title, player.title)
                                
                                if (userCanChallenge) {
                                    Text(
                                        "🔔 Vous avez dépassé ce représentant en points ! Vous pouvez usurper son titre en le provoquant en duel.",
                                        color = GoldCrown,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    var showProvokeDialog by remember { mutableStateOf(false) }
                                    
                                    Button(
                                        onClick = { showProvokeDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = VelvetPurple),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.FlashOn, "Duel")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Provoquer en Duel d'Usurpation !", fontWeight = FontWeight.Bold)
                                    }

                                    if (showProvokeDialog) {
                                        ProvokeDuelDialog(
                                            opponentName = member.name,
                                            opponentTitle = member.title,
                                            onDismiss = { showProvokeDialog = false },
                                            onAction = { forfeit ->
                                                viewModel.startDuel(member, forceForfeit = forfeit)
                                                showProvokeDialog = false
                                            }
                                        )
                                    }
                                } else {
                                    val needsMorePoints = member.points - player.points
                                    Text(
                                        if (isRankHigher(member.title, player.title)) {
                                            "Pour défier ce représentant et prendre son titre, vous devez d'abord le dépasser de ${needsMorePoints + 1} points !"
                                        } else {
                                            "Ce membre de la communauté est de rang égal ou inférieur au vôtre."
                                        },
                                        color = KnightSilver,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CastleWall.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sélectionnez une case de la carte pour afficher l'édit du seigneur.",
                            color = KnightSilver,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// --- LEADERBOARD TAB ---
@Composable
fun LeaderboardTab(
    viewModel: FiefdomViewModel,
    members: List<CommunityMember>,
    player: CommunityMember?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Classement Public du Royaume",
            color = GoldCrown,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Text(
            "Classement officiel par points de pouvoir féodal. Repérez les seigneurs vulnérables pour les défier en duel d'esprit !",
            color = KnightSilver,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(members) { member ->
                val isSelf = member.isPlayer
                val canBeChallenged = player != null && !isSelf && member.points < player.points && isRankHigher(member.title, player.title)

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelf) ForestGreen.copy(alpha = 0.25f) else CastleWall
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (canBeChallenged) GoldCrown else if (isSelf) ForestGreen else KnightSilver.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Rank number based on index
                            val index = members.indexOf(member) + 1
                            Text(
                                "#$index",
                                color = if (index <= 3) GoldCrown else KnightSilver,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.width(32.dp)
                            )
                            Text(member.avatarEmoji, fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        member.name,
                                        color = if (isSelf) GoldCrown else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    if (isSelf) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("(Vous)", color = GoldCrown, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                RankBadge(title = member.title)
                                member.dailyQuest?.let { quest ->
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Quête : ${if (quest.length > 35) quest.take(35) + "..." else quest}",
                                        color = KnightSilver,
                                        fontSize = 10.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${member.points} pts",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )

                            if (canBeChallenged) {
                                var showProvokeDialog by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { showProvokeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = VelvetPurple),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Défier", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showProvokeDialog) {
                                    ProvokeDuelDialog(
                                        opponentName = member.name,
                                        opponentTitle = member.title,
                                        onDismiss = { showProvokeDialog = false },
                                        onAction = { forfeit ->
                                            viewModel.startDuel(member, forceForfeit = forfeit)
                                            showProvokeDialog = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPOSABLES & LOGIC ---

@Composable
fun RankBadge(title: String) {
    val (bgColor, strokeColor, textColor) = when (title) {
        "Empereur", "Roi", "Prince" -> Triple(VelvetPurple, GoldCrown, GoldCrown)
        "Duc", "Marquis", "Comte", "Vicomte", "Baron" -> Triple(CastleSlate, GoldCrown, Color.White)
        "Chevalier", "Seigneur" -> Triple(ForestGreen.copy(alpha = 0.3f), ForestGreen, Color.White)
        "Artisan" -> Triple(CastleWall, KnightSilver, KnightSilver)
        "Ouvrier" -> Triple(CastleWall, KnightSilver.copy(alpha = 0.6f), KnightSilver.copy(alpha = 0.8f))
        else -> Triple(Color.Black, RoyalCrimson.copy(alpha = 0.5f), KnightSilver.copy(alpha = 0.7f))
    }

    Box(
        modifier = Modifier
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(0.5.dp, strokeColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            title.uppercase(),
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

fun getNextRankThreshold(points: Int): String {
    return when {
        points < 50 -> "50 pts (Ouvrier)"
        points < 100 -> "100 pts (Artisan)"
        points < 200 -> "200 pts (Seigneur)"
        points < 350 -> "350 pts (Chevalier)"
        points < 500 -> "500 pts (Baron)"
        points < 700 -> "700 pts (Vicomte)"
        points < 900 -> "900 pts (Comte)"
        points < 1200 -> "1200 pts (Marquis)"
        points < 1500 -> "1500 pts (Duc)"
        points < 2000 -> "2000 pts (Prince)"
        points < 2500 -> "2500 pts (Roi)"
        points < 3500 -> "3500 pts (Empereur)"
        else -> "MAXIMUM"
    }
}

fun isRankHigher(target: String, current: String): Boolean {
    val ranks = listOf(
        "Esclave", "Ouvrier", "Artisan", "Seigneur", "Chevalier", 
        "Baron", "Vicomte", "Comte", "Marquis", "Duc", "Prince", "Roi", "Empereur"
    )
    val targetIndex = ranks.indexOf(target)
    val currentIndex = ranks.indexOf(current)
    return targetIndex > currentIndex
}

// --- DIALOGS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CastleWall),
            border = BorderStroke(1.dp, GoldCrown),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Décréter un devoir personnel",
                    color = GoldCrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Ex: Nettoyer mon écritoire...", color = KnightSilver) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CastleSlate,
                        unfocusedContainerColor = CastleSlate,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = GoldCrown,
                        focusedIndicatorColor = GoldCrown
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("task_input_field")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = KnightSilver)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (text.isNotBlank()) onConfirm(text) },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCrown)
                    ) {
                        Text("Décréter", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayDebtDialog(
    currentDebt: Int,
    playerPoints: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CastleWall),
            border = BorderStroke(1.dp, RoyalCrimson),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Rembourser la Dette Commune",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Dette actuelle : $currentDebt pts. Vos points de pouvoir : $playerPoints pts.",
                    color = KnightSilver,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                TextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Montant en points...", color = KnightSilver) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = CastleSlate,
                        unfocusedContainerColor = CastleSlate,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = RoyalCrimson,
                        focusedIndicatorColor = RoyalCrimson
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("debt_input_field")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Annuler", color = KnightSilver)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amount = text.toIntOrNull()
                            if (amount != null && amount > 0 && amount <= playerPoints) {
                                onConfirm(amount)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson)
                    ) {
                        Text("Payer l'Écot", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProvokeDuelDialog(
    opponentName: String,
    opponentTitle: String,
    onDismiss: () -> Unit,
    onAction: (Boolean) -> Unit // true for NPC forfeit, false for fight
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CastleWall),
            border = BorderStroke(2.dp, GoldCrown),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.FlashOn,
                    "Provoke duel",
                    tint = GoldCrown,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Provocation de $opponentName",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Vous provoquez le représentant $opponentTitle pour usurper son titre et prendre sa place en haut de la société. Le représentant va-t-il abdiquer ?",
                    color = KnightSilver,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                // Forfeit Button (NPC yields)
                Button(
                    onClick = { onAction(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = VelvetPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Exiger le forfait (Abdication automatique)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                // Real Guessing Game Button
                OutlinedButton(
                    onClick = { onAction(false) },
                    border = BorderStroke(1.dp, GoldCrown),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldCrown),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Engager le combat d'esprit (Devinette)", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = onDismiss) {
                    Text("Retirer le défi d'honneur", color = KnightSilver)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelArenaDialog(
    duelState: DuelState,
    onGuessSubmitted: (Int) -> Unit,
    onQuitArena: () -> Unit
) {
    var guessInput by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CastleSlate)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Arena
                Text(
                    "🏟️ L'ARÈNE DES DUELS D'ESPRIT 🏟️",
                    color = GoldCrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(top = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // Showdown Panel
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CastleWall)
                        .border(1.dp, GoldCrown, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🛡️", fontSize = 36.sp)
                        Text("VOUS (Challenger)", color = GoldCrown, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text("VS", color = RoyalCrimson, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(duelState.opponent.avatarEmoji, fontSize = 36.sp)
                        Text(duelState.opponent.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text(duelState.opponent.title, color = KnightSilver, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Arena State / Explanation banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = CastleWall),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            duelState.explanation,
                            color = Color.White,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (duelState.winner == null && duelState.attemptsLeft > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tentatives restantes : ${duelState.attemptsLeft}",
                                color = RoyalCrimson,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guess Feed
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Player guesses
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CastleWall)
                            .padding(8.dp)
                    ) {
                        Text("VOS ESTIMATIONS", color = GoldCrown, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = KnightSilver.copy(alpha = 0.1f))
                        LazyColumn {
                            items(duelState.playerGuesses) { (guess, feed) ->
                                Text("• Éstimé : $guess ($feed)", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }

                    // Opponent guesses
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(CastleWall)
                            .padding(8.dp)
                    ) {
                        Text("${duelState.opponent.name.uppercase()}", color = KnightSilver, fontWeight = FontWeight.Bold, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        Divider(color = KnightSilver.copy(alpha = 0.1f))
                        LazyColumn {
                            items(duelState.opponentGuesses) { (guess, feed) ->
                                Text("• Éstimé : $guess ($feed)", color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input Action panel
                if (duelState.winner == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = guessInput,
                            onValueChange = { guessInput = it },
                            placeholder = { Text("Votre devinette (0-1000)...", color = KnightSilver, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CastleWall,
                                unfocusedContainerColor = CastleWall,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = GoldCrown,
                                focusedIndicatorColor = GoldCrown
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("duel_guess_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val guess = guessInput.toIntOrNull()
                                if (guess != null && guess in 0..1000) {
                                    onGuessSubmitted(guess)
                                    guessInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                            modifier = Modifier.height(56.dp).testTag("duel_guess_submit_button")
                        ) {
                            Text("Défier !", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Duel Finished
                    Button(
                        onClick = onQuitArena,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(bottom = 16.dp)
                            .testTag("leave_arena_button")
                    ) {
                        Text("Quitter l'Arène", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- AUTHENTICATION SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(viewModel: FiefdomViewModel) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("⛓️") }

    val emojis = listOf("⛓️", "🔨", "🌾", "🗡️", "🏹", "🏇", "🏰", "👑")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CastleSlate),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CastleWall),
            border = BorderStroke(2.dp, GoldCrown),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .widthIn(max = 450.dp)
                .padding(16.dp)
                .testTag("auth_card")
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Crest / Shield
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(GoldCrown.copy(alpha = 0.1f), CircleShape)
                        .border(1.dp, GoldCrown, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = "Armoiries",
                        tint = GoldCrown,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Fiefdom Quest",
                    color = GoldCrown,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isRegisterMode) "Enregistrez votre lignée dans le registre du royaume" else "Présentez vos lettres de créance aux gardes du donjon",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (viewModel.authError != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = RoyalCrimson.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, RoyalCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = viewModel.authError ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nom de Vassal (Identifiant)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldCrown,
                        unfocusedBorderColor = KnightSilver.copy(alpha = 0.5f),
                        focusedLabelColor = GoldCrown,
                        unfocusedLabelColor = KnightSilver
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_username_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de Passe") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = GoldCrown,
                        unfocusedBorderColor = KnightSilver.copy(alpha = 0.5f),
                        focusedLabelColor = GoldCrown,
                        unfocusedLabelColor = KnightSilver
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_password_input")
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nom d'Affichage (ex: Godefroy)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldCrown,
                            unfocusedBorderColor = KnightSilver.copy(alpha = 0.5f),
                            focusedLabelColor = GoldCrown,
                            unfocusedLabelColor = KnightSilver
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("auth_name_input")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "Choisissez vos armoiries :",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        emojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedEmoji == emoji) GoldCrown else Color.Transparent)
                                    .clickable { selectedEmoji = emoji }
                                    .border(
                                        1.dp,
                                        if (selectedEmoji == emoji) GoldCrown else KnightSilver.copy(alpha = 0.3f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (viewModel.isAuthLoading) {
                    CircularProgressIndicator(color = GoldCrown)
                } else {
                    Button(
                        onClick = {
                            if (isRegisterMode) {
                                viewModel.register(username, password, name, selectedEmoji)
                            } else {
                                viewModel.login(username, password)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("auth_action_button")
                    ) {
                        Text(
                            text = if (isRegisterMode) "Prêter Serment et Commencer" else "Entrer dans la Forteresse",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            viewModel.authError = null
                        },
                        modifier = Modifier.testTag("auth_toggle_mode_button")
                    ) {
                        Text(
                            text = if (isRegisterMode) "Déjà vassal ? Se connecter" else "Nouveau dans le royaume ? S'enregistrer",
                            color = GoldCrown,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// --- SETTINGS TAB ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(viewModel: FiefdomViewModel) {
    var firebaseInput by remember { mutableStateOf(viewModel.firebaseBaseUrl) }
    var hfInput by remember { mutableStateOf(viewModel.huggingFaceToken) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Configurations Féodales",
                color = GoldCrown,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CastleWall),
                border = BorderStroke(1.dp, GoldCrown),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🔗 Base de Données Firebase",
                        color = GoldCrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Définissez l'URL de votre propre Firebase Realtime Database pour synchroniser vos quêtes et rivaliser avec d'autres vrais seigneurs.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = firebaseInput,
                        onValueChange = { firebaseInput = it },
                        label = { Text("Firebase RTDB Base URL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldCrown,
                            unfocusedBorderColor = KnightSilver.copy(alpha = 0.5f),
                            focusedLabelColor = GoldCrown,
                            unfocusedLabelColor = KnightSilver
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("settings_firebase_input")
                    )

                    Button(
                        onClick = {
                            viewModel.saveFirebaseConfig(firebaseInput)
                            saveSuccessMessage = "Base de données mise à jour !"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                        modifier = Modifier.align(Alignment.End).testTag("settings_save_firebase_button")
                    ) {
                        Text("Mettre à Jour URL", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CastleWall),
                border = BorderStroke(1.dp, GoldCrown),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "🤗 Intelligence Artificielle Hugging Face",
                        color = GoldCrown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        "Entrez un jeton d'accès Hugging Face (HF API Token) pour utiliser le modèle Llama-3 pour générer des quêtes. Si laissé vide, l'IA utilisera un moteur médiéval immersif hors-ligne.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    OutlinedTextField(
                        value = hfInput,
                        onValueChange = { hfInput = it },
                        label = { Text("Hugging Face API Token") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GoldCrown,
                            unfocusedBorderColor = KnightSilver.copy(alpha = 0.5f),
                            focusedLabelColor = GoldCrown,
                            unfocusedLabelColor = KnightSilver
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("settings_hf_input")
                    )

                    Button(
                        onClick = {
                            viewModel.saveHuggingFaceConfig(hfInput)
                            saveSuccessMessage = "Token Hugging Face enregistré !"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldCrown),
                        modifier = Modifier.align(Alignment.End).testTag("settings_save_hf_button")
                    ) {
                        Text("Enregistrer Token", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (saveSuccessMessage != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = GoldCrown.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, GoldCrown),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(saveSuccessMessage ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { saveSuccessMessage = null }) {
                            Text("Fermer", color = GoldCrown, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = RoyalCrimson),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("settings_logout_button")
            ) {
                Text("Se Déconnecter du Royaume", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
