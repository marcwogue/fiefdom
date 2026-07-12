package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HFMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class HFRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<HFMessage>,
    @Json(name = "temperature") val temperature: Double = 0.5,
    @Json(name = "max_tokens") val maxTokens: Int = 150
)

@JsonClass(generateAdapter = true)
data class HFChoice(
    @Json(name = "message") val message: HFMessage
)

@JsonClass(generateAdapter = true)
data class HFResponse(
    @Json(name = "choices") val choices: List<HFChoice>? = null
)

interface HuggingFaceApiService {
    @POST("models/{modelId}/v1/chat/completions")
    suspend fun chatCompletion(
        @Header("Authorization") authHeader: String,
        @Path("modelId", encoded = true) modelId: String,
        @Body request: HFRequest
    ): HFResponse
}

object HuggingFaceClient {
    private const val BASE_URL = "https://api-inference.huggingface.co/"
    
    // Default high-quality open model for chat completions on Hugging Face Serverless API
    const val DEFAULT_MODEL = "meta-llama/Llama-3.2-3B-Instruct"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()

    val service: HuggingFaceApiService by lazy {
        retrofit.create(HuggingFaceApiService::class.java)
    }

    suspend fun generateCommunityTasks(userTitle: String, hfToken: String?): List<String> {
        val token = hfToken?.trim() ?: ""
        if (token.isEmpty() || token == "MY_HF_TOKEN") {
            return getFallbackTasks(userTitle)
        }

        val prompt = """
            Génère exactement 5 tâches communautaires féodales médiévales amusantes et immersives adaptées pour un royaume où l'utilisateur a le titre de '$userTitle'.
            Le format de réponse DOIT être un tableau JSON de chaînes de caractères uniquement, par exemple :
            [
              "Aider les paysans à réparer le moulin à vent du village",
              "Patrouiller les douves du château pour le seigneur local"
            ]
            Ne mets aucune balise markdown, pas de ```json, pas d'explication, seulement le tableau JSON brut contenant exactement 5 phrases courtes.
        """.trimIndent()

        val request = HFRequest(
            model = DEFAULT_MODEL,
            messages = listOf(
                HFMessage(role = "user", content = prompt)
            ),
            temperature = 0.6,
            maxTokens = 250
        )

        return try {
            val authHeader = "Bearer $token"
            val response = service.chatCompletion(authHeader, DEFAULT_MODEL, request)
            val jsonText = response.choices?.firstOrNull()?.message?.content
            if (jsonText != null) {
                parseJsonArray(jsonText, userTitle)
            } else {
                getFallbackTasks(userTitle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackTasks(userTitle)
        }
    }

    suspend fun generateMemberQuest(memberName: String, memberTitle: String, hfToken: String?): Pair<String, Int> {
        val token = hfToken?.trim() ?: ""
        if (token.isEmpty() || token == "MY_HF_TOKEN") {
            return getFallbackQuest(memberTitle)
        }

        val prompt = """
            Génère une quête féodale médiévale amusante et unique pour un membre de la communauté nommé '$memberName' qui possède le rang de '$memberTitle'.
            La quête doit être directement liée à son rang. Par exemple, un Chevalier s'entraîne, un Esclave fait des corvées ingrates, un Roi prend des décisions royales.
            
            Renvoie la réponse au format JSON brut suivant :
            {
              "quest": "Description courte et de style moyenâgeux de la quête de 10 à 20 mots maximum",
              "reward": un nombre entier entre 10 et 50 représentant la récompense de points pour cette quête
            }
            Ne mets aucune balise markdown, pas de ```json, pas d'explication, seulement l'objet JSON brut.
        """.trimIndent()

        val request = HFRequest(
            model = DEFAULT_MODEL,
            messages = listOf(
                HFMessage(role = "user", content = prompt)
            ),
            temperature = 0.7,
            maxTokens = 200
        )

        return try {
            val authHeader = "Bearer $token"
            val response = service.chatCompletion(authHeader, DEFAULT_MODEL, request)
            val jsonText = response.choices?.firstOrNull()?.message?.content
            if (jsonText != null) {
                parseQuestJson(jsonText, memberTitle)
            } else {
                getFallbackQuest(memberTitle)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackQuest(memberTitle)
        }
    }

    private fun parseJsonArray(rawJson: String, userTitle: String): List<String> {
        var cleaned = rawJson.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        return try {
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
            val adapter = moshi.adapter<List<String>>(type)
            adapter.fromJson(cleaned) ?: getFallbackTasks(userTitle)
        } catch (e: Exception) {
            e.printStackTrace()
            cleaned.removePrefix("[").removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"").removeSurrounding("'") }
                .filter { it.isNotEmpty() }
                .ifEmpty { getFallbackTasks(userTitle) }
        }
    }

    private fun parseQuestJson(rawJson: String, memberTitle: String): Pair<String, Int> {
        var cleaned = rawJson.trim()
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
        return try {
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleaned)
            val quest = map?.get("quest") as? String ?: getFallbackQuest(memberTitle).first
            val rewardDouble = map?.get("reward")
            val reward = when (rewardDouble) {
                is Double -> rewardDouble.toInt()
                is Int -> rewardDouble
                is String -> rewardDouble.toIntOrNull() ?: 20
                else -> 20
            }
            quest to reward
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackQuest(memberTitle)
        }
    }

    fun getFallbackTasks(userTitle: String): List<String> {
        return when (userTitle) {
            "Esclave" -> listOf(
                "Nettoyer les écuries royales du château",
                "Transporter 20 blocs de pierre pour la muraille",
                "Cueillir des baies sauvages sous la pluie",
                "Polir l'armure rouillée du chevalier",
                "Éplucher 100 pommes de terre pour les cuisines"
            )
            "Ouvrier" -> listOf(
                "Réparer la toiture de la grange du bailli",
                "Aider à la coupe du bois pour l'hiver",
                "Creuser les tranchées de drainage des champs",
                "Décharger les cargaisons de sel au port fluvial",
                "Aider le forgeron à entretenir le soufflet"
            )
            "Artisan" -> listOf(
                "Forger 10 fers à cheval pour la garnison",
                "Tisser une tapisserie pour la salle d'armes",
                "Fabriquer des tonneaux de chêne pour le cidre",
                "Tailler des pierres d'ornement pour la chapelle",
                "Préparer un lot de pain de seigle pour le marché"
            )
            "Seigneur" -> listOf(
                "Arbitrer une dispute foncière entre paysans",
                "Organiser un banquet pour les récoltes d'été",
                "Inspecter la garde des tours de guet",
                "Financer la reconstruction du pont de pierre",
                "Rédiger un édit de protection pour les forêts"
            )
            "Chevalier" -> listOf(
                "S'entraîner au tournoi de joute de la Saint-Jean",
                "Chasser un loup féroce qui effraie les bergers",
                "Escorter une caravane de marchands de soie",
                "Prêter serment de fidélité devant l'autel",
                "Sécuriser les chemins de pèlerinage"
            )
            "Baron" -> listOf(
                "Rendre la justice dans le tribunal de la baronnie",
                "Lever une taxe spéciale sur les épices importées",
                "Négocier un accord commercial avec la cité voisine",
                "Fortifier les portes de la forteresse",
                "Allouer des parcelles de terre aux colons"
            )
            "Vicomte" -> listOf(
                "Superviser l'entretien des domaines viticoles",
                "Vérifier les comptes de la sénéchaussée",
                "Accueillir le messager de la cour royale",
                "Organiser une chasse à courre dans la forêt ducale",
                "Rénover la grande bibliothèque du château"
            )
            "Comte" -> listOf(
                "Mobiliser 50 hommes d'armes pour la défense",
                "Médiatiser un conflit entre deux ducs rivaux",
                "Consacrer une nouvelle cathédrale dans le comté",
                "Tracer de nouvelles routes royales",
                "Émettre des sauf-conduits pour les ambassadeurs"
            )
            "Marquis" -> listOf(
                "Sécuriser les frontières maritimes du marquisat",
                "Construire un fort de défense sur la colline",
                "Négocier une trêve avec les clans du nord",
                "Financer une guilde de cartographes",
                "Créer une milice de garde-frontières"
            )
            "Duc" -> listOf(
                "Lever le grand ban des vassaux de la province",
                "Rédiger la charte de franchise de la capitale",
                "Parrainer l'académie des arts et sciences",
                "Recevoir l'hommage féodal des barons locaux",
                "Organiser le grand conseil de la terre"
            )
            "Prince" -> listOf(
                "Mener la délégation diplomatique auprès de l'Empereur",
                "Prendre la régence du royaume en l'absence du Roi",
                "Inaugurer les grands canaux d'irrigation",
                "Passer en revue l'armée royale en parade",
                "Négocier une alliance de mariage princier"
            )
            "Roi" -> listOf(
                "Couronner le nouveau champion du grand tournoi",
                "Déclarer une trêve générale pour les moissons",
                "Signer des décrets de grâce pour les captifs",
                "Frapper une nouvelle monnaie d'or à son effigie",
                "Établir les lois du royaume pour le siècle"
            )
            "Empereur" -> listOf(
                "Présider le grand conclave des nations unies",
                "Décréter un an de paix impériale universelle",
                "Fonder une nouvelle ville impériale fortifiée",
                "Allouer des couronnes de laurier aux héros",
                "Rédiger le code de loi impérial pour l'histoire"
            )
            else -> listOf(
                "Réparer la charrette du village",
                "Nettoyer les douves de la forteresse",
                "Chasser les brigands des bois voisins",
                "Aider à la cuisine lors de la fête des récoltes",
                "Porter des sacs de grains jusqu'au moulin"
            )
        }
    }

    fun getFallbackQuest(memberTitle: String): Pair<String, Int> {
        val quests = when (memberTitle) {
            "Esclave" -> listOf(
                "Nettoyer de fond en comble la grand-salle du donjon" to 10,
                "Transporter de lourds sacs de grain pour le boulanger royal" to 12,
                "Polir les chaudrons en cuivre des cuisines du château" to 10,
                "Ramasser le bois mort dans les fourrés de la forêt" to 15
            )
            "Ouvrier" -> listOf(
                "Aider à fortifier les palissades du faubourg est" to 15,
                "Réparer l'essieu brisé de la charrette de foin du bailli" to 18,
                "Creuser un nouveau fossé de drainage près du moulin" to 15,
                "Tailler des poutres de chêne pour la charpente" to 20
            )
            "Artisan" -> listOf(
                "Forger 12 pointes de flèches en fer pour la garnison" to 20,
                "Tisser une couverture de laine pour la couche du prévôt" to 22,
                "Façonner un broc en étain gravé aux armes du fief" to 25,
                "Préparer dix miches de pain blanc pour l'intendant" to 20
            )
            "Seigneur" -> listOf(
                "Percevoir le cens sur les marchands de passage au péage" to 30,
                "Inspecter l'état des armures de la petite garde noble" to 25,
                "Donner audience à un fermier victime d'un vol de mouton" to 32,
                "Donner l'ordre de réparer le vieux pont de bois" to 28
            )
            "Chevalier" -> listOf(
                "S'exercer à la quintaine dans la cour d'honneur" to 35,
                "Traquer une meute de loups signalée près de la bergerie" to 40,
                "Escorter l'abbesse lors de son voyage vers le monastère" to 35,
                "Défier en duel un chevalier errant de passage" to 45
            )
            "Baron" -> listOf(
                "Présider la cour de justice locale de la baronnie" to 42,
                "Augmenter l'impôt sur le sel de deux deniers par minot" to 38,
                "Négocier l'achat de trois chevaux de guerre flamands" to 45,
                "Superviser les travaux de maçonnerie de la tour d'angle" to 40
            )
            "Vicomte" -> listOf(
                "Vérifier les registres de récolte de la sénéchaussée" to 45,
                "Organiser un banquet intime pour l'anniversaire du comte" to 50,
                "Arbitrer la querelle de bornage entre deux châtelains" to 48,
                "Allouer une bourse d'études au fils doué d'un scribe" to 40
            )
            "Comte" -> listOf(
                "Passer en revue les cinquante archers levés par le ban" to 50,
                "Rédiger une charte d'immunité pour la nouvelle abbaye" to 48,
                "Envoyer un placet de fidélité scellé d'or au souverain" to 52,
                "Ordonner la levée de l'arrière-ban contre les pillards" to 55
            )
            "Marquis" -> listOf(
                "Renforcer le guet sur la frontière contestée des marches" to 55,
                "Faire ériger une redoute en pierre sur l'éperon rocheux" to 60,
                "Inspecter la flottille de surveillance côtière" to 58,
                "Négocier un accord de non-agression avec le margrave" to 62
            )
            "Duc" -> listOf(
                "Réunir les barons de la province pour prêter serment" to 65,
                "Allouer une dotation royale pour le grand portail sculpté" to 60,
                "Recevoir l'envoyé plénipotentiaire de la couronne" to 70,
                "Publier un coutumier récapitulant les lois du duché" to 65
            )
            "Prince" -> listOf(
                "Mener la négociation de mariage avec l'ambassade" to 75,
                "Inspecter les arsenaux de la capitale provinciale" to 70,
                "Rédiger un mémoire de stratégie militaire pour le roi" to 80,
                "Présider le conseil restreint des finances du domaine" to 75
            )
            "Roi" -> listOf(
                "Couronner le vainqueur du grand tournoi de chevalerie" to 85,
                "Signer une ordonnance de paix perpétuelle avec l'Aragon" to 90,
                "Faire frapper un lot d'écus d'or au nouveau millésime" to 85,
                "Déclarer l'ouverture des assises solennelles du royaume" to 95
            )
            "Empereur" -> listOf(
                "Recevoir l'hommage solennel de trois ducs vassaux" to 100,
                "Promulguer la bulle d'or réglant l'élection impériale" to 110,
                "Décréter une amnistie générale pour les délits de chasse" to 95,
                "Fonder une chaire d'études théologiques à l'université" to 100
            )
            else -> listOf(
                "Aider un paysan à retrouver son veau égaré" to 15,
                "Nettoyer les douves encombrées d'algues du donjon" to 15,
                "Chasser les corbeaux qui pillent le champ de seigle" to 15
            )
        }
        return quests.random()
    }
}
