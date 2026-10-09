package com.example.data.model

enum class UserRole(val displayName: String, val badgeTitle: String) {
    DIRECTION("Administrateur", "Direction Générale"),
    ENSEIGNANT("Professeur", "Corps Enseignant"),
    EDUCATEUR("Éducateur", "Vie Scolaire"),
    CAISSE("Comptable", "Service Comptabilité"),
    PARENT("Parent d'élève", "Espace Famille"),
    ELEVE("Élève", "Espace Élève");

    val tabs: List<String>
        get() = when (this) {
            DIRECTION -> listOf(
                "Administration",
                "Comptes & Accès",
                "Cours en ligne",
                "Emploi du temps",
                "Finances",
                "Notes",
                "Devoirs & PDF",
                "Présence",
                "Messagerie",
                "Notifications"
            )
            ENSEIGNANT -> listOf(
                "Cours en ligne",
                "Notes",
                "Devoirs & PDF",
                "Présence",
                "Emploi du temps",
                "Messagerie"
            )
            EDUCATEUR -> listOf(
                "Emploi du temps",
                "Présence",
                "Messagerie",
                "Notifications"
            )
            CAISSE -> listOf(
                "Finances",
                "Messagerie",
                "Notifications"
            )
            PARENT -> listOf(
                "Cours en ligne",
                "Bulletin",
                "Paiements",
                "Devoirs & PDF",
                "Absences",
                "Emploi du temps",
                "Messagerie"
            )
            ELEVE -> listOf(
                "Cours en ligne",
                "Mon Bulletin",
                "Devoirs & PDF",
                "Assiduité",
                "Emploi du temps",
                "Messagerie",
                "Notifications"
            )
        }
}
