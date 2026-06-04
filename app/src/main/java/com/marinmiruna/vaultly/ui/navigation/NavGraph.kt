package com.marinmiruna.vaultly.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.marinmiruna.vaultly.ui.screens.FilesAuthScreen
import com.marinmiruna.vaultly.ui.screens.FilesListScreen
import com.marinmiruna.vaultly.ui.screens.HomeScreen
import com.marinmiruna.vaultly.ui.screens.NoteDetailScreen
import com.marinmiruna.vaultly.ui.screens.NotesListScreen
import com.marinmiruna.vaultly.ui.screens.PasswordDetailScreen
import com.marinmiruna.vaultly.ui.screens.PasswordsAuthScreen
import com.marinmiruna.vaultly.ui.screens.PasswordsListScreen
import com.marinmiruna.vaultly.ui.screens.PhotoViewerScreen
import com.marinmiruna.vaultly.ui.screens.PhotosGridScreen
import com.marinmiruna.vaultly.ui.screens.SettingsScreen
import com.marinmiruna.vaultly.ui.screens.PhotosAuthScreen

@Composable
fun VaultlyNavGraph(
    onTrustedSystemActivityStarted: () -> Unit = {},
    isPasswordsSessionValid: () -> Boolean = { false },
    onPasswordsAuthRequested: (onSuccess: () -> Unit) -> Unit = { onSuccess -> onSuccess() },
    onFilesAuthRequested: (onSuccess: () -> Unit) -> Unit = { onSuccess -> onSuccess() },
    onPhotosAuthRequested: (onSuccess: () -> Unit) -> Unit = { onSuccess -> onSuccess() },
    onExportAuthRequested: (onSuccess: () -> Unit) -> Unit = { onSuccess -> onSuccess() }
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {
        composable(Routes.Home.route) {
            HomeScreen(
                onOpenNotes = { navController.navigate(Routes.NotesList.route) },
                onOpenPasswords = { navController.navigate(Routes.PasswordsAuth.route) },
                onOpenPhotos = { navController.navigate(Routes.PhotosAuth.route) },
                onOpenFiles = { navController.navigate(Routes.FilesAuth.route) },
                onOpenSettings = { navController.navigate(Routes.Settings.route) }
            )
        }

        composable(Routes.NotesList.route) {
            NotesListScreen(
                onOpenNote = { noteId ->
                    navController.navigate(Routes.NoteDetail.createRoute(noteId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.NoteDetail.route,
            arguments = listOf(
                navArgument(Routes.NoteDetail.ARG_NOTE_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong(Routes.NoteDetail.ARG_NOTE_ID) ?: 0L

            NoteDetailScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PasswordsAuth.route) {
            PasswordsAuthScreen(
                onAuthenticateClick = {
                    onPasswordsAuthRequested {
                        navController.navigate(Routes.PasswordsList.route) {
                            popUpTo(Routes.PasswordsAuth.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onAuthenticated = {
                    navController.navigate(Routes.PasswordsList.route) {
                        popUpTo(Routes.PasswordsAuth.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.PasswordsList.route) {
            PasswordsListScreen(
                onOpenPassword = { passwordId ->
                    val openPasswordDetail = {
                        navController.navigate(Routes.PasswordDetail.createRoute(passwordId))
                    }

                    if (passwordId == 0L || isPasswordsSessionValid()) {
                        openPasswordDetail()
                    } else {
                        onPasswordsAuthRequested {
                            openPasswordDetail()
                        }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PasswordDetail.route,
            arguments = listOf(
                navArgument(Routes.PasswordDetail.ARG_PASSWORD_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val passwordId = backStackEntry.arguments?.getLong(
                Routes.PasswordDetail.ARG_PASSWORD_ID
            ) ?: 0L

            PasswordDetailScreen(
                passwordId = passwordId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.PhotosAuth.route) {
            PhotosAuthScreen(
                onAuthenticateClick = {
                    onPhotosAuthRequested {
                        navController.navigate(Routes.PhotosGrid.route) {
                            popUpTo(Routes.PhotosAuth.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onAuthenticated = {
                    navController.navigate(Routes.PhotosGrid.route) {
                        popUpTo(Routes.PhotosAuth.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.PhotosGrid.route) {
            PhotosGridScreen(
                onOpenPhoto = { photoId ->
                    navController.navigate(Routes.PhotoViewer.createRoute(photoId))
                },
                onBack = { navController.popBackStack() },
                onTrustedSystemActivityStarted = onTrustedSystemActivityStarted
            )
        }

        composable(
            route = Routes.PhotoViewer.route,
            arguments = listOf(
                navArgument(Routes.PhotoViewer.ARG_PHOTO_ID) {
                    type = NavType.LongType
                }
            )
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong(Routes.PhotoViewer.ARG_PHOTO_ID) ?: 0L

            PhotoViewerScreen(
                photoId = photoId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FilesAuth.route) {
            FilesAuthScreen(
                onAuthenticateClick = {
                    onFilesAuthRequested {
                        navController.navigate(Routes.FilesList.route) {
                            popUpTo(Routes.FilesAuth.route) {
                                inclusive = true
                            }
                        }
                    }
                },
                onAuthenticated = {
                    navController.navigate(Routes.FilesList.route) {
                        popUpTo(Routes.FilesAuth.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Routes.FilesList.route) {
            FilesListScreen(
                onBack = { navController.popBackStack() },
                onTrustedSystemActivityStarted = onTrustedSystemActivityStarted
            )
        }

        composable(Routes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onExportAuthRequested = onExportAuthRequested
            )
        }

    }
}
