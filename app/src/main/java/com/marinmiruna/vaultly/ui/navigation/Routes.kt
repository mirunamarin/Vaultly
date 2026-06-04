package com.marinmiruna.vaultly.ui.navigation

sealed class Routes(val route: String) {
    data object Lock : Routes("lock")
    data object Home : Routes("home")

    data object NotesList : Routes("notes")
    data object NoteDetail : Routes("notes/detail?noteId={noteId}") {
        const val ARG_NOTE_ID = "noteId"

        fun createRoute(noteId: Long = 0L): String {
            return "notes/detail?noteId=$noteId"
        }
    }

    data object PasswordsAuth : Routes("passwords/auth")
    data object PasswordsList : Routes("passwords")
    data object PasswordDetail : Routes("passwords/detail?passwordId={passwordId}") {
        const val ARG_PASSWORD_ID = "passwordId"

        fun createRoute(passwordId: Long = 0L): String {
            return "passwords/detail?passwordId=$passwordId"
        }
    }

    data object PhotosAuth : Routes("photos/auth")
    data object PhotosGrid : Routes("photos")
    data object PhotoViewer : Routes("photos/viewer?photoId={photoId}") {
        const val ARG_PHOTO_ID = "photoId"

        fun createRoute(photoId: Long): String {
            return "photos/viewer?photoId=$photoId"
        }
    }

    data object FilesAuth : Routes("files/auth")
    data object FilesList : Routes("files")

    data object Settings : Routes("settings")

}
