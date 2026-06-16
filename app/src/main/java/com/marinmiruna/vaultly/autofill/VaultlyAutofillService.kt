package com.marinmiruna.vaultly.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import com.marinmiruna.vaultly.R
import android.util.Log
import com.marinmiruna.vaultly.data.repository.PasswordRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import com.marinmiruna.vaultly.domain.model.PasswordEntry
import android.app.PendingIntent
import android.content.Intent

@Suppress("DEPRECATION")
@AndroidEntryPoint
class VaultlyAutofillService : AutofillService() {

    private val tag = "VaultlyAutofill"

    @Inject
    lateinit var passwordRepository: PasswordRepository

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        Log.e(tag, "onFillRequest called")
        val structure = request.fillContexts.lastOrNull()?.structure
        Log.e(tag, "structure exists: ${structure != null}")
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        val textFieldIds = findAllTextFields(structure)
        Log.e(tag, "text fields found: ${textFieldIds.size}")

        val passwordEntries = runBlocking {
            passwordRepository.getAllPasswordEntries()
        }

        val requestHints = extractRequestHints(structure)
        val matchingEntries = filterMatchingEntries(
            entries = passwordEntries,
            hints = requestHints
        )

        val entriesToShow = matchingEntries

        if (entriesToShow.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        Log.e(tag, "request hints: $requestHints")
        Log.e(tag, "matching entries found: ${matchingEntries.size}")

        Log.e(tag, "password entries found: ${passwordEntries.size}")

        if (passwordEntries.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        if (textFieldIds.isEmpty()) {
            callback.onSuccess(null)
            return
        }

        val responseBuilder = FillResponse.Builder()

        entriesToShow.forEach { passwordEntry ->
            val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
                setTextViewText(
                    android.R.id.text1,
                    "${passwordEntry.service} - ${passwordEntry.username}"
                )
            }

            val requestId = System.currentTimeMillis() + passwordEntry.id
            autofillIdStore[requestId] = textFieldIds

            val authIntent = Intent(this, VaultlyAutofillAuthActivity::class.java).apply {
                putExtra(EXTRA_PASSWORD_ID, passwordEntry.id)
                putExtra(EXTRA_AUTOFILL_REQUEST_ID, requestId)
            }

            val authPendingIntent = PendingIntent.getActivity(
                this,
                requestId.toInt(),
                authIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val datasetBuilder = Dataset.Builder(presentation)

            textFieldIds.forEach { autofillId ->
                datasetBuilder.setValue(
                    autofillId,
                    null,
                    presentation
                )
            }

            val dataset = datasetBuilder
                .setAuthentication(authPendingIntent.intentSender)
                .build()

            responseBuilder.addDataset(dataset)
        }

        val response = responseBuilder.build()

        Log.e(tag, "returning real password datasets")

        callback.onSuccess(response)
    }

    override fun onSaveRequest(
        request: SaveRequest,
        callback: SaveCallback
    ) {
        callback.onSuccess()
    }

    private fun findAllTextFields(structure: AssistStructure): List<AutofillId> {
        val ids = mutableListOf<AutofillId>()

        for (windowIndex in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(windowIndex)
            val rootNode = windowNode.rootViewNode

            rootNode.traverse { node ->
                val autofillId = node.autofillId ?: return@traverse

                if (node.autofillType == View.AUTOFILL_TYPE_TEXT) {
                    ids += autofillId
                }
            }
        }

        return ids
    }

    private fun extractRequestHints(structure: AssistStructure): Set<String> {
        val hints = mutableSetOf<String>()

        for (windowIndex in 0 until structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(windowIndex)

            windowNode.rootViewNode.traverse { node ->
                node.webDomain?.let { domain ->
                    hints += normalizeHint(domain)
                }

                node.idPackage?.let { packageName ->
                    hints += normalizeHint(packageName)
                    hints += packageName.substringAfterLast('.').lowercase()
                }

                node.hint?.let { hint ->
                    hints += normalizeHint(hint)
                }

                node.idEntry?.let { idEntry ->
                    hints += normalizeHint(idEntry)
                }
            }
        }

        return hints.filter { it.length >= 3 }.toSet()
    }

    private fun filterMatchingEntries(
        entries: List<PasswordEntry>,
        hints: Set<String>
    ): List<PasswordEntry> {
        if (hints.isEmpty()) {
            return emptyList()
        }

        return entries.filter { entry ->
            val entryHints = buildSet {
                add(normalizeHint(entry.service))
                add(normalizeHint(entry.url))
                add(normalizeHint(entry.username))
                extractDomainParts(entry.url).forEach { add(it) }
            }.filter { it.length >= 3 }

            entryHints.any { entryHint ->
                hints.any { requestHint ->
                    entryHint.contains(requestHint) || requestHint.contains(entryHint)
                }
            }
        }
    }

    private fun extractDomainParts(value: String): Set<String> {
        val normalized = normalizeHint(value)
        val withoutProtocol = normalized
            .removePrefix("https")
            .removePrefix("http")
            .removePrefix("www")

        return withoutProtocol
            .split('.', '/', '-', '_')
            .map { it.trim() }
            .filter { it.length >= 3 }
            .toSet()
    }

    private fun normalizeHint(value: String): String {
        return value
            .lowercase()
            .replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
            .replace(Regex("[^a-z0-9._-]"), "")
            .trim()
    }

    private fun AssistStructure.ViewNode.traverse(
        visitor: (AssistStructure.ViewNode) -> Unit
    ) {
        visitor(this)

        for (index in 0 until childCount) {
            getChildAt(index)?.traverse(visitor)
        }
    }

    private fun AssistStructure.ViewNode.looksLikePasswordField(): Boolean {
        val hints = autofillHints?.joinToString(" ").orEmpty().lowercase()
        val hint = hint?.lowercase().orEmpty()
        val idEntry = idEntry?.lowercase().orEmpty()
        val text = text?.toString()?.lowercase().orEmpty()

        return autofillType == View.AUTOFILL_TYPE_TEXT &&
                (
                        "password" in hints ||
                                "password" in hint ||
                                "password" in idEntry ||
                                "parola" in hint ||
                                "parolă" in hint ||
                                "pass" in idEntry ||
                                "password" in text
                        )
    }

    private fun AssistStructure.ViewNode.looksLikeUsernameField(): Boolean {
        val hints = autofillHints?.joinToString(" ").orEmpty().lowercase()
        val hint = hint?.lowercase().orEmpty()
        val idEntry = idEntry?.lowercase().orEmpty()
        val text = text?.toString()?.lowercase().orEmpty()

        return "username" in hints ||
                "email" in hints ||
                "user" in hints ||
                "login" in hints ||
                "username" in hint ||
                "email" in hint ||
                "utilizator" in hint ||
                "user" in idEntry ||
                "email" in idEntry ||
                "login" in idEntry ||
                "username" in text ||
                "email" in text
    }

    private data class AutofillFields(
        val usernameId: AutofillId?,
        val passwordId: AutofillId?
    )

    companion object {
        const val EXTRA_PASSWORD_ID = "extra_password_id"
        const val EXTRA_AUTOFILL_REQUEST_ID = "extra_autofill_request_id"
        val autofillIdStore = mutableMapOf<Long, List<AutofillId>>()
    }
}