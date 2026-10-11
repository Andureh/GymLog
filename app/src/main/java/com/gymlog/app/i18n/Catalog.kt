package com.gymlog.app.i18n

import com.gymlog.app.data.ExerciseType
import com.gymlog.app.data.ExerciseType.DURATION
import com.gymlog.app.data.ExerciseType.REPS_ONLY
import com.gymlog.app.data.ExerciseType.WEIGHT_REPS

/**
 * Catálogo traduzido de exercícios, músculos e equipamento.
 * Colunas dos nomes (separados por "|"): pt | en | es | fr | de | it
 */
object Catalog {
    const val LANGS = 6

    /** Aumentar sempre que se acrescentam exercícios, para os adicionar a instalações existentes. */
    const val VERSION = 2

    class Item(val key: String, val muscle: String, val equipment: String, val type: ExerciseType, names: String) {
        val names: List<String> = names.split("|")
    }

    private fun x(key: String, muscle: String, equip: String, names: String, type: ExerciseType = WEIGHT_REPS) =
        Item(key, muscle, equip, type, names)

    val muscles: Map<String, List<String>> = linkedMapOf(
        "chest" to "Peito|Chest|Pecho|Pectoraux|Brust|Petto",
        "back" to "Costas|Back|Espalda|Dos|Rücken|Schiena",
        "lower_back" to "Lombar|Lower Back|Lumbar|Lombaires|Unterer Rücken|Lombari",
        "shoulders" to "Ombros|Shoulders|Hombros|Épaules|Schultern|Spalle",
        "traps" to "Trapézio|Traps|Trapecio|Trapèzes|Trapez|Trapezio",
        "biceps" to "Bíceps|Biceps|Bíceps|Biceps|Bizeps|Bicipiti",
        "triceps" to "Tríceps|Triceps|Tríceps|Triceps|Trizeps|Tricipiti",
        "forearms" to "Antebraços|Forearms|Antebrazos|Avant-bras|Unterarme|Avambracci",
        "quads" to "Quadríceps|Quads|Cuádriceps|Quadriceps|Quadrizeps|Quadricipiti",
        "hamstrings" to "Isquiotibiais|Hamstrings|Isquiotibiales|Ischio-jambiers|Beinbeuger|Femorali",
        "glutes" to "Glúteos|Glutes|Glúteos|Fessiers|Gesäß|Glutei",
        "adductors" to "Adutores|Adductors|Aductores|Adducteurs|Adduktoren|Adduttori",
        "calves" to "Gémeos|Calves|Gemelos|Mollets|Waden|Polpacci",
        "abs" to "Abdominais|Abs|Abdominales|Abdominaux|Bauch|Addominali",
        "full_body" to "Corpo inteiro|Full Body|Cuerpo completo|Corps entier|Ganzkörper|Total body",
        "cardio" to "Cardio|Cardio|Cardio|Cardio|Cardio|Cardio",
    ).mapValues { it.value.split("|") }

    val equipment: Map<String, List<String>> = linkedMapOf(
        "barbell" to "Barra|Barbell|Barra|Barre|Langhantel|Bilanciere",
        "dumbbell" to "Halteres|Dumbbell|Mancuernas|Haltères|Kurzhantel|Manubri",
        "machine" to "Máquina|Machine|Máquina|Machine|Maschine|Macchina",
        "cable" to "Cabo|Cable|Polea|Poulie|Kabelzug|Cavi",
        "bodyweight" to "Peso corporal|Bodyweight|Peso corporal|Poids du corps|Körpergewicht|Corpo libero",
        "kettlebell" to "Kettlebell|Kettlebell|Kettlebell|Kettlebell|Kettlebell|Kettlebell",
        "ez_bar" to "Barra EZ|EZ Bar|Barra Z|Barre EZ|SZ-Stange|Bilanciere EZ",
        "smith" to "Smith|Smith|Smith|Smith|Smith|Smith",
        "band" to "Elástico|Band|Banda elástica|Élastique|Widerstandsband|Elastico",
        "none" to "Nenhum|None|Ninguno|Aucun|Keins|Nessuno",
    ).mapValues { it.value.split("|") }

    val exercises: List<Item> = listOf(
        // Peito
        x("bench_bb", "chest", "barbell", "Supino plano (barra)|Bench Press (Barbell)|Press de banca (barra)|Développé couché (barre)|Bankdrücken (Langhantel)|Panca piana (bilanciere)"),
        x("bench_db", "chest", "dumbbell", "Supino plano (halteres)|Bench Press (Dumbbell)|Press de banca (mancuernas)|Développé couché (haltères)|Bankdrücken (Kurzhantel)|Panca piana (manubri)"),
        x("incline_bench_bb", "chest", "barbell", "Supino inclinado (barra)|Incline Bench Press (Barbell)|Press inclinado (barra)|Développé incliné (barre)|Schrägbankdrücken (Langhantel)|Panca inclinata (bilanciere)"),
        x("incline_bench_db", "chest", "dumbbell", "Supino inclinado (halteres)|Incline Bench Press (Dumbbell)|Press inclinado (mancuernas)|Développé incliné (haltères)|Schrägbankdrücken (Kurzhantel)|Panca inclinata (manubri)"),
        x("decline_bench_bb", "chest", "barbell", "Supino declinado (barra)|Decline Bench Press (Barbell)|Press declinado (barra)|Développé décliné (barre)|Negativbankdrücken (Langhantel)|Panca declinata (bilanciere)"),
        x("incline_bench_smith", "chest", "smith", "Supino inclinado (Smith)|Incline Bench Press (Smith)|Press inclinado (Smith)|Développé incliné (Smith)|Schrägbankdrücken (Smith)|Panca inclinata (Smith)"),
        x("chest_press_machine", "chest", "machine", "Supino na máquina|Chest Press (Machine)|Press de pecho (máquina)|Presse pectorale (machine)|Brustpresse (Maschine)|Chest press (macchina)"),
        x("fly_db", "chest", "dumbbell", "Crucifixo (halteres)|Chest Fly (Dumbbell)|Aperturas (mancuernas)|Écarté (haltères)|Fliegende (Kurzhantel)|Croci (manubri)"),
        x("crossover_cable", "chest", "cable", "Crossover (cabo)|Cable Crossover|Cruce de poleas|Écarté à la poulie|Kabelzug-Crossover|Croci ai cavi"),
        x("pec_deck", "chest", "machine", "Peck deck|Pec Deck|Pec deck|Pec deck|Butterfly (Maschine)|Pec deck"),
        x("push_up", "chest", "bodyweight", "Flexões|Push-up|Flexiones|Pompes|Liegestütze|Piegamenti", REPS_ONLY),
        x("chest_dip", "chest", "bodyweight", "Dips para peito|Chest Dip|Fondos para pecho|Dips pour pectoraux|Dips (Brust)|Dip per il petto", REPS_ONLY),
        x("pullover_db", "chest", "dumbbell", "Pullover (halter)|Pullover (Dumbbell)|Pullover (mancuerna)|Pull-over (haltère)|Überzüge (Kurzhantel)|Pullover (manubrio)"),
        // Costas
        x("deadlift_bb", "back", "barbell", "Peso morto (barra)|Deadlift (Barbell)|Peso muerto (barra)|Soulevé de terre (barre)|Kreuzheben (Langhantel)|Stacco da terra (bilanciere)"),
        x("pull_up", "back", "bodyweight", "Elevações (pull-up)|Pull-up|Dominadas|Tractions|Klimmzüge|Trazioni", REPS_ONLY),
        x("chin_up", "back", "bodyweight", "Elevações pega supinada (chin-up)|Chin-up|Dominadas supinas|Tractions en supination|Klimmzüge im Untergriff|Trazioni presa supina", REPS_ONLY),
        x("weighted_pull_up", "back", "bodyweight", "Elevações com peso|Weighted Pull-up|Dominadas lastradas|Tractions lestées|Klimmzüge mit Gewicht|Trazioni zavorrate"),
        x("assisted_pull_up", "back", "machine", "Elevações assistidas|Assisted Pull-up (Machine)|Dominadas asistidas (máquina)|Tractions assistées (machine)|Unterstützte Klimmzüge (Maschine)|Trazioni assistite (macchina)"),
        x("lat_pulldown", "back", "cable", "Puxada à frente (cabo)|Lat Pulldown (Cable)|Jalón al pecho (polea)|Tirage vertical (poulie)|Latzug (Kabel)|Lat machine (cavo)"),
        x("close_grip_pulldown", "back", "cable", "Puxada pega fechada (cabo)|Close-Grip Lat Pulldown (Cable)|Jalón agarre estrecho (polea)|Tirage vertical prise serrée (poulie)|Latzug enger Griff (Kabel)|Lat machine presa stretta (cavo)"),
        x("bent_over_row_bb", "back", "barbell", "Remada curvada (barra)|Bent Over Row (Barbell)|Remo con barra|Rowing barre|Vorgebeugtes Rudern (Langhantel)|Rematore con bilanciere"),
        x("one_arm_row_db", "back", "dumbbell", "Remada unilateral (halter)|One-Arm Row (Dumbbell)|Remo a una mano (mancuerna)|Rowing unilatéral (haltère)|Einarmiges Rudern (Kurzhantel)|Rematore con manubrio"),
        x("seated_row_cable", "back", "cable", "Remada sentada (cabo)|Seated Cable Row|Remo sentado en polea|Tirage horizontal (poulie)|Rudern sitzend (Kabel)|Pulley basso (cavo)"),
        x("t_bar_row", "back", "barbell", "Remada T (barra)|T-Bar Row|Remo en T (barra)|Rowing T-bar|T-Bar-Rudern|Rematore T-bar"),
        x("row_machine", "back", "machine", "Remada na máquina|Seated Row (Machine)|Remo en máquina|Rowing (machine)|Rudern (Maschine)|Rematore (macchina)"),
        x("pullover_cable", "back", "cable", "Pullover (cabo)|Cable Pullover|Pullover en polea|Pull-over à la poulie|Überzüge (Kabel)|Pullover ai cavi"),
        x("rack_pull", "back", "barbell", "Rack pull|Rack Pull|Rack pull|Rack pull|Rack Pull|Rack pull"),
        x("hyperextension", "lower_back", "bodyweight", "Hiperextensões|Back Extension|Hiperextensiones|Extensions lombaires|Rückenstrecker|Iperestensioni", REPS_ONLY),
        x("good_morning", "lower_back", "barbell", "Good morning (barra)|Good Morning (Barbell)|Buenos días (barra)|Good morning (barre)|Good Morning (Langhantel)|Good morning (bilanciere)"),
        // Ombros
        x("ohp_bb", "shoulders", "barbell", "Press militar (barra)|Overhead Press (Barbell)|Press militar (barra)|Développé militaire (barre)|Military Press (Langhantel)|Military press (bilanciere)"),
        x("shoulder_press_db", "shoulders", "dumbbell", "Press de ombros (halteres)|Shoulder Press (Dumbbell)|Press de hombros (mancuernas)|Développé épaules (haltères)|Schulterdrücken (Kurzhantel)|Shoulder press (manubri)"),
        x("arnold_press", "shoulders", "dumbbell", "Press Arnold (halteres)|Arnold Press (Dumbbell)|Press Arnold (mancuernas)|Développé Arnold (haltères)|Arnold Press (Kurzhantel)|Arnold press (manubri)"),
        x("shoulder_press_machine", "shoulders", "machine", "Press de ombros (máquina)|Shoulder Press (Machine)|Press de hombros (máquina)|Développé épaules (machine)|Schulterdrücken (Maschine)|Shoulder press (macchina)"),
        x("lateral_raise_db", "shoulders", "dumbbell", "Elevações laterais (halteres)|Lateral Raise (Dumbbell)|Elevaciones laterales (mancuernas)|Élévations latérales (haltères)|Seitheben (Kurzhantel)|Alzate laterali (manubri)"),
        x("lateral_raise_cable", "shoulders", "cable", "Elevações laterais (cabo)|Lateral Raise (Cable)|Elevaciones laterales (polea)|Élévations latérales (poulie)|Seitheben (Kabel)|Alzate laterali (cavo)"),
        x("lateral_raise_machine", "shoulders", "machine", "Elevações laterais (máquina)|Lateral Raise (Machine)|Elevaciones laterales (máquina)|Élévations latérales (machine)|Seitheben (Maschine)|Alzate laterali (macchina)"),
        x("front_raise_db", "shoulders", "dumbbell", "Elevações frontais (halteres)|Front Raise (Dumbbell)|Elevaciones frontales (mancuernas)|Élévations frontales (haltères)|Frontheben (Kurzhantel)|Alzate frontali (manubri)"),
        x("face_pull", "shoulders", "cable", "Face pull (cabo)|Face Pull (Cable)|Face pull (polea)|Face pull (poulie)|Face Pull (Kabel)|Face pull (cavo)"),
        x("rear_delt_fly_db", "shoulders", "dumbbell", "Aberturas invertidas (halteres)|Rear Delt Fly (Dumbbell)|Pájaros (mancuernas)|Oiseau (haltères)|Reverse Flys (Kurzhantel)|Alzate posteriori (manubri)"),
        x("reverse_pec_deck", "shoulders", "machine", "Peck deck invertido|Reverse Pec Deck|Pec deck inverso|Pec deck inversé|Reverse Butterfly (Maschine)|Pec deck inverso"),
        x("upright_row_bb", "shoulders", "barbell", "Remada alta (barra)|Upright Row (Barbell)|Remo al mentón (barra)|Tirage menton (barre)|Aufrechtes Rudern (Langhantel)|Tirate al mento (bilanciere)"),
        x("shrug_db", "traps", "dumbbell", "Encolhimentos (halteres)|Shrug (Dumbbell)|Encogimientos (mancuernas)|Shrugs (haltères)|Shrugs (Kurzhantel)|Scrollate (manubri)"),
        x("shrug_bb", "traps", "barbell", "Encolhimentos (barra)|Shrug (Barbell)|Encogimientos (barra)|Shrugs (barre)|Shrugs (Langhantel)|Scrollate (bilanciere)"),
        // Bíceps
        x("curl_bb", "biceps", "barbell", "Curl com barra|Bicep Curl (Barbell)|Curl con barra|Curl barre|Bizepscurls (Langhantel)|Curl con bilanciere"),
        x("curl_ez", "biceps", "ez_bar", "Curl com barra EZ|Bicep Curl (EZ Bar)|Curl con barra Z|Curl barre EZ|Bizepscurls (SZ-Stange)|Curl con bilanciere EZ"),
        x("curl_db", "biceps", "dumbbell", "Curl com halteres|Bicep Curl (Dumbbell)|Curl con mancuernas|Curl haltères|Bizepscurls (Kurzhantel)|Curl con manubri"),
        x("hammer_curl", "biceps", "dumbbell", "Curl martelo (halteres)|Hammer Curl (Dumbbell)|Curl martillo (mancuernas)|Curl marteau (haltères)|Hammercurls (Kurzhantel)|Curl a martello (manubri)"),
        x("incline_curl", "biceps", "dumbbell", "Curl inclinado (halteres)|Incline Curl (Dumbbell)|Curl inclinado (mancuernas)|Curl incliné (haltères)|Schrägbank-Curls (Kurzhantel)|Curl su panca inclinata (manubri)"),
        x("concentration_curl", "biceps", "dumbbell", "Curl concentrado (halter)|Concentration Curl (Dumbbell)|Curl concentrado (mancuerna)|Curl concentré (haltère)|Konzentrationscurls (Kurzhantel)|Curl di concentrazione (manubrio)"),
        x("preacher_curl", "biceps", "ez_bar", "Curl no banco Scott|Preacher Curl|Curl predicador|Curl pupitre|Scottcurls|Curl alla panca Scott"),
        x("curl_cable", "biceps", "cable", "Curl no cabo|Bicep Curl (Cable)|Curl en polea|Curl à la poulie|Bizepscurls (Kabel)|Curl ai cavi"),
        x("curl_machine", "biceps", "machine", "Curl na máquina|Bicep Curl (Machine)|Curl en máquina|Curl (machine)|Bizepscurls (Maschine)|Curl (macchina)"),
        // Tríceps
        x("rope_pushdown", "triceps", "cable", "Extensão de tríceps com corda (cabo)|Triceps Rope Pushdown|Extensión de tríceps con cuerda (polea)|Extension triceps à la corde (poulie)|Trizepsdrücken mit Seil (Kabel)|Push down con corda (cavo)"),
        x("bar_pushdown", "triceps", "cable", "Pushdown com barra (cabo)|Triceps Pushdown (Bar)|Jalón de tríceps con barra (polea)|Pushdown barre (poulie)|Trizepsdrücken mit Stange (Kabel)|Push down con barra (cavo)"),
        x("overhead_ext_db", "triceps", "dumbbell", "Extensão sobre a cabeça (halter)|Overhead Triceps Extension (Dumbbell)|Extensión sobre la cabeza (mancuerna)|Extension au-dessus de la tête (haltère)|Trizepsstrecken über Kopf (Kurzhantel)|Estensioni sopra la testa (manubrio)"),
        x("overhead_ext_cable", "triceps", "cable", "Extensão sobre a cabeça (cabo)|Overhead Triceps Extension (Cable)|Extensión sobre la cabeza (polea)|Extension au-dessus de la tête (poulie)|Trizepsstrecken über Kopf (Kabel)|Estensioni sopra la testa (cavo)"),
        x("skull_crusher", "triceps", "ez_bar", "Skull crusher (barra EZ)|Skull Crusher (EZ Bar)|Press francés (barra Z)|Barre au front (barre EZ)|French Press (SZ-Stange)|French press (bilanciere EZ)"),
        x("close_grip_bench", "triceps", "barbell", "Supino pega fechada|Close-Grip Bench Press|Press de banca agarre estrecho|Développé couché prise serrée|Enges Bankdrücken|Panca presa stretta"),
        x("dips", "triceps", "bodyweight", "Dips (paralelas)|Dips|Fondos en paralelas|Dips (barres parallèles)|Dips (Barren)|Dip alle parallele", REPS_ONLY),
        x("kickback", "triceps", "dumbbell", "Kickback (halter)|Triceps Kickback (Dumbbell)|Patada de tríceps (mancuerna)|Kickback (haltère)|Trizeps-Kickback (Kurzhantel)|Kickback (manubrio)"),
        x("triceps_ext_machine", "triceps", "machine", "Extensão de tríceps (máquina)|Triceps Extension (Machine)|Extensión de tríceps (máquina)|Extension triceps (machine)|Trizepsstrecken (Maschine)|Estensioni tricipiti (macchina)"),
        // Antebraços
        x("wrist_curl", "forearms", "barbell", "Curl de pulso (barra)|Wrist Curl (Barbell)|Curl de muñeca (barra)|Curl poignets (barre)|Handgelenkcurls (Langhantel)|Curl per polsi (bilanciere)"),
        x("reverse_wrist_curl", "forearms", "barbell", "Curl de pulso invertido (barra)|Reverse Wrist Curl (Barbell)|Curl de muñeca inverso (barra)|Curl poignets inversé (barre)|Reverse Handgelenkcurls (Langhantel)|Curl inverso per polsi (bilanciere)"),
        x("farmers_walk", "forearms", "dumbbell", "Farmer's walk|Farmer's Walk|Paseo del granjero|Marche du fermier|Farmer's Walk|Farmer's walk"),
        // Pernas
        x("squat_bb", "quads", "barbell", "Agachamento (barra)|Squat (Barbell)|Sentadilla (barra)|Squat (barre)|Kniebeuge (Langhantel)|Squat (bilanciere)"),
        x("front_squat", "quads", "barbell", "Agachamento frontal (barra)|Front Squat (Barbell)|Sentadilla frontal (barra)|Squat avant (barre)|Frontkniebeuge (Langhantel)|Front squat (bilanciere)"),
        x("goblet_squat", "quads", "dumbbell", "Agachamento goblet|Goblet Squat|Sentadilla goblet|Goblet squat|Goblet Squat|Goblet squat"),
        x("squat_smith", "quads", "smith", "Agachamento (Smith)|Squat (Smith)|Sentadilla (Smith)|Squat (Smith)|Kniebeuge (Smith)|Squat (Smith)"),
        x("bulgarian_split_squat", "quads", "dumbbell", "Agachamento búlgaro (halteres)|Bulgarian Split Squat (Dumbbell)|Sentadilla búlgara (mancuernas)|Squat bulgare (haltères)|Bulgarische Kniebeuge (Kurzhantel)|Squat bulgaro (manubri)"),
        x("leg_press", "quads", "machine", "Leg press|Leg Press|Prensa de piernas|Presse à cuisses|Beinpresse|Leg press"),
        x("hack_squat", "quads", "machine", "Hack squat|Hack Squat|Sentadilla hack|Hack squat|Hackenschmidt-Kniebeuge|Hack squat"),
        x("leg_extension", "quads", "machine", "Extensão de pernas|Leg Extension|Extensión de cuádriceps|Leg extension|Beinstrecker|Leg extension"),
        x("lunge_db", "quads", "dumbbell", "Afundos (halteres)|Lunge (Dumbbell)|Zancadas (mancuernas)|Fentes (haltères)|Ausfallschritte (Kurzhantel)|Affondi (manubri)"),
        x("walking_lunge", "quads", "dumbbell", "Afundos a andar (halteres)|Walking Lunge (Dumbbell)|Zancadas caminando (mancuernas)|Fentes marchées (haltères)|Gehende Ausfallschritte (Kurzhantel)|Affondi camminati (manubri)"),
        x("step_up", "quads", "dumbbell", "Step-up (halteres)|Step-up (Dumbbell)|Step-up (mancuernas)|Step-up (haltères)|Step-ups (Kurzhantel)|Step-up (manubri)"),
        x("rdl_bb", "hamstrings", "barbell", "Peso morto romeno (barra)|Romanian Deadlift (Barbell)|Peso muerto rumano (barra)|Soulevé de terre roumain (barre)|Rumänisches Kreuzheben (Langhantel)|Stacco rumeno (bilanciere)"),
        x("rdl_db", "hamstrings", "dumbbell", "Peso morto romeno (halteres)|Romanian Deadlift (Dumbbell)|Peso muerto rumano (mancuernas)|Soulevé de terre roumain (haltères)|Rumänisches Kreuzheben (Kurzhantel)|Stacco rumeno (manubri)"),
        x("stiff_leg_deadlift", "hamstrings", "barbell", "Peso morto pernas rígidas|Stiff-Leg Deadlift|Peso muerto piernas rígidas|Soulevé de terre jambes tendues|Kreuzheben mit gestreckten Beinen|Stacco a gambe tese"),
        x("lying_leg_curl", "hamstrings", "machine", "Curl de pernas deitado|Lying Leg Curl|Curl femoral tumbado|Leg curl allongé|Beinbeuger liegend|Leg curl sdraiato"),
        x("seated_leg_curl", "hamstrings", "machine", "Curl de pernas sentado|Seated Leg Curl|Curl femoral sentado|Leg curl assis|Beinbeuger sitzend|Leg curl seduto"),
        x("sumo_deadlift", "glutes", "barbell", "Peso morto sumo (barra)|Sumo Deadlift (Barbell)|Peso muerto sumo (barra)|Soulevé de terre sumo (barre)|Sumo-Kreuzheben (Langhantel)|Stacco sumo (bilanciere)"),
        x("hip_thrust_bb", "glutes", "barbell", "Hip thrust (barra)|Hip Thrust (Barbell)|Hip thrust (barra)|Hip thrust (barre)|Hip Thrust (Langhantel)|Hip thrust (bilanciere)"),
        x("hip_thrust_machine", "glutes", "machine", "Hip thrust (máquina)|Hip Thrust (Machine)|Hip thrust (máquina)|Hip thrust (machine)|Hip Thrust (Maschine)|Hip thrust (macchina)"),
        x("glute_bridge", "glutes", "bodyweight", "Ponte de glúteos|Glute Bridge|Puente de glúteos|Pont fessier|Glute Bridge|Ponte per glutei", REPS_ONLY),
        x("hip_abduction", "glutes", "machine", "Abdução de anca (máquina)|Hip Abduction (Machine)|Abducción de cadera (máquina)|Abduction de hanche (machine)|Abduktoren (Maschine)|Abduzione anca (macchina)"),
        x("glute_kickback_cable", "glutes", "cable", "Coice de glúteo (cabo)|Glute Kickback (Cable)|Patada de glúteo (polea)|Kickback fessier (poulie)|Glute Kickback (Kabel)|Slanci posteriori (cavo)"),
        x("hip_adduction", "adductors", "machine", "Adução de anca (máquina)|Hip Adduction (Machine)|Aducción de cadera (máquina)|Adduction de hanche (machine)|Adduktoren (Maschine)|Adduzione anca (macchina)"),
        x("standing_calf_raise", "calves", "machine", "Gémeos em pé (máquina)|Standing Calf Raise (Machine)|Elevación de gemelos de pie (máquina)|Mollets debout (machine)|Wadenheben stehend (Maschine)|Calf raise in piedi (macchina)"),
        x("seated_calf_raise", "calves", "machine", "Gémeos sentado (máquina)|Seated Calf Raise (Machine)|Elevación de gemelos sentado (máquina)|Mollets assis (machine)|Wadenheben sitzend (Maschine)|Calf raise seduto (macchina)"),
        x("calf_press", "calves", "machine", "Gémeos no leg press|Calf Press (Leg Press)|Gemelos en prensa|Mollets à la presse|Wadenheben an der Beinpresse|Calf alla leg press"),
        // Abdominais
        x("plank", "abs", "bodyweight", "Prancha|Plank|Plancha|Gainage|Unterarmstütz|Plank", DURATION),
        x("side_plank", "abs", "bodyweight", "Prancha lateral|Side Plank|Plancha lateral|Gainage latéral|Seitstütz|Plank laterale", DURATION),
        x("crunch", "abs", "bodyweight", "Abdominais (crunch)|Crunch|Crunch abdominal|Crunch|Crunches|Crunch", REPS_ONLY),
        x("cable_crunch", "abs", "cable", "Crunch no cabo|Cable Crunch|Crunch en polea|Crunch à la poulie|Kabel-Crunches|Crunch ai cavi"),
        x("machine_crunch", "abs", "machine", "Crunch na máquina|Crunch (Machine)|Crunch en máquina|Crunch (machine)|Crunches (Maschine)|Crunch (macchina)"),
        x("hanging_leg_raise", "abs", "bodyweight", "Elevação de pernas suspenso|Hanging Leg Raise|Elevación de piernas colgado|Relevé de jambes suspendu|Hängendes Beinheben|Sollevamento gambe alla sbarra", REPS_ONLY),
        x("lying_leg_raise", "abs", "bodyweight", "Elevação de pernas deitado|Lying Leg Raise|Elevación de piernas tumbado|Relevé de jambes allongé|Beinheben liegend|Sollevamento gambe a terra", REPS_ONLY),
        x("russian_twist", "abs", "bodyweight", "Russian twist|Russian Twist|Giros rusos|Russian twist|Russian Twist|Russian twist", REPS_ONLY),
        x("ab_wheel", "abs", "none", "Roda abdominal|Ab Wheel|Rueda abdominal|Roue abdominale|Bauchroller|Ruota addominale", REPS_ONLY),
        x("mountain_climbers", "abs", "bodyweight", "Mountain climbers|Mountain Climbers|Escaladores|Mountain climbers|Mountain Climbers|Mountain climber", DURATION),
        // Corpo inteiro
        x("kb_swing", "full_body", "kettlebell", "Kettlebell swing|Kettlebell Swing|Swing con kettlebell|Kettlebell swing|Kettlebell Swing|Kettlebell swing"),
        x("power_clean", "full_body", "barbell", "Power clean|Power Clean|Cargada de potencia|Power clean|Power Clean|Power clean"),
        x("clean_and_press", "full_body", "barbell", "Clean and press|Clean and Press|Cargada y press|Épaulé et développé|Umsetzen und Drücken|Clean and press"),
        x("thruster", "full_body", "barbell", "Thruster (barra)|Thruster (Barbell)|Thruster (barra)|Thruster (barre)|Thruster (Langhantel)|Thruster (bilanciere)"),
        x("burpees", "full_body", "bodyweight", "Burpees|Burpees|Burpees|Burpees|Burpees|Burpees", REPS_ONLY),
        // Cardio
        x("treadmill", "cardio", "machine", "Passadeira|Treadmill|Cinta de correr|Tapis de course|Laufband|Tapis roulant", DURATION),
        x("stationary_bike", "cardio", "machine", "Bicicleta estática|Stationary Bike|Bicicleta estática|Vélo d'appartement|Heimtrainer|Cyclette", DURATION),
        x("elliptical", "cardio", "machine", "Elíptica|Elliptical|Elíptica|Vélo elliptique|Crosstrainer|Ellittica", DURATION),
        x("rowing_machine", "cardio", "machine", "Remo ergómetro|Rowing Machine|Remo ergómetro|Rameur|Rudergerät|Vogatore", DURATION),
        x("stair_climber", "cardio", "machine", "Máquina de escadas|Stair Climber|Escaladora|Simulateur d'escalier|Stepper|Stepper", DURATION),
        x("jump_rope", "cardio", "none", "Saltar à corda|Jump Rope|Saltar a la comba|Corde à sauter|Seilspringen|Salto con la corda", DURATION),
        // Acrescentados na versão 2 do catálogo
        x("bench_smith", "chest", "smith", "Supino plano (Smith)|Bench Press (Smith)|Press de banca (Smith)|Développé couché (Smith)|Bankdrücken (Smith)|Panca piana (Smith)"),
        x("iso_chest_press", "chest", "machine", "Supino iso-lateral (máquina)|Iso-Lateral Chest Press (Machine)|Press de pecho iso-lateral (máquina)|Presse pectorale iso-latérale (machine)|Iso-laterale Brustpresse (Maschine)|Chest press iso-laterale (macchina)"),
        x("chest_supported_row_db", "back", "dumbbell", "Remada com apoio no peito (halteres)|Chest Supported Incline Row (Dumbbell)|Remo con apoyo en el pecho (mancuernas)|Rowing buste appuyé (haltères)|Brustgestütztes Rudern (Kurzhantel)|Rematore con petto in appoggio (manubri)"),
        x("chest_supported_t_bar_row", "back", "machine", "Remada T com apoio no peito|Chest Supported T Bar Row|Remo en T con apoyo en el pecho|Rowing T-bar buste appuyé|Brustgestütztes T-Bar-Rudern|Rematore T-bar con petto in appoggio"),
        x("iso_high_row", "back", "machine", "High row iso-lateral (máquina)|Iso-Lateral High Row (Machine)|Remo alto iso-lateral (máquina)|Tirage haut iso-latéral (machine)|Iso-laterales High Row (Maschine)|High row iso-laterale (macchina)"),
        x("seated_lateral_raise_db", "shoulders", "dumbbell", "Elevações laterais sentado (halteres)|Seated Lateral Raise (Dumbbell)|Elevaciones laterales sentado (mancuernas)|Élévations latérales assis (haltères)|Seitheben sitzend (Kurzhantel)|Alzate laterali da seduto (manubri)"),
        x("leg_press_horizontal", "quads", "machine", "Leg press horizontal|Leg Press Horizontal (Machine)|Prensa horizontal (máquina)|Presse à cuisses horizontale (machine)|Horizontale Beinpresse (Maschine)|Leg press orizzontale (macchina)"),
        x("standing_calf_raise_smith", "calves", "smith", "Gémeos em pé (Smith)|Standing Calf Raise (Smith)|Elevación de gemelos de pie (Smith)|Mollets debout (Smith)|Wadenheben stehend (Smith)|Calf raise in piedi (Smith)"),
        x("decline_crunch", "abs", "bodyweight", "Crunch declinado (com peso)|Decline Crunch (Weighted)|Crunch declinado (con peso)|Crunch décliné (lesté)|Negativ-Crunches (mit Gewicht)|Crunch declinato (con peso)"),
        x("cycling", "cardio", "none", "Ciclismo|Cycling|Ciclismo|Vélo|Radfahren|Ciclismo", DURATION),
        x("walking", "cardio", "none", "Caminhada|Walking|Caminar|Marche|Gehen|Camminata", DURATION),
    )

    private val byKey = exercises.associateBy { it.key }

    fun exerciseName(key: String, col: Int): String? = byKey[key]?.names?.getOrNull(col)
    fun muscle(key: String, col: Int): String = muscles[key]?.getOrNull(col) ?: key
    fun equipment(key: String, col: Int): String = equipment[key]?.getOrNull(col) ?: key

    // ---------- Conversão de dados antigos (v1 guardava tudo em português) ----------

    val legacyExerciseKeys: Map<String, String> = exercises.associate { it.names[0] to it.key }
    val legacyMuscleKeys: Map<String, String> = muscles.entries.associate { it.value[0] to it.key }
    val legacyEquipmentKeys: Map<String, String> = equipment.entries.associate { it.value[0] to it.key }
}
