package com.parmtech.mvg

import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.Executors
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private val Navy = Color(0xFF090D18)
private val Panel = Color(0xFF151D2D)
private val Elevated = Color(0xFF202B41)
private val Sky = Color(0xFF62DCF5)
private val Violet = Color(0xFFA57DFE)
private val Muted = Color(0xFFAFBDD0)
private val TextWhite = Color(0xFFF3F7FF)
private val gradient = Brush.horizontalGradient(listOf(Sky, Violet))

class MainActivity : ComponentActivity() {
    private val store by lazy { StudioStore(this) }
    private val background = Executors.newFixedThreadPool(3)
    private val sceneExporter by lazy { SceneExporter(this) }
    private lateinit var googleAuthorization: GoogleCloudAuthorization

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuthorization = GoogleCloudAuthorization(this) { token, message ->
            if (token != null) {
                store.saveGoogleAuthorization(token)
                store.updateConfig(store.config.copy(authMode = "GOOGLE"))
            }
            store.googleAuthorizationStatus = message
            showMessage(message)
        }
        window.statusBarColor = android.graphics.Color.rgb(9, 13, 24)
        window.navigationBarColor = android.graphics.Color.rgb(9, 13, 24)
        setContent {
            val scheme = darkColorScheme(
                primary = Sky, secondary = Violet, background = Navy,
                surface = Panel, onBackground = TextWhite, onSurface = TextWhite,
                onPrimary = Navy
            )
            MaterialTheme(colorScheme = scheme) {
                Surface(color = Navy, modifier = Modifier.fillMaxSize()) {
                    MVGApp(store,
                        onGenerate = ::submitGeneration,
                        onPoll = ::refreshGeneration,
                        onChat = ::sendDirector,
                        onPlay = ::openVideo,
                        onAvatarPhoto = ::importAvatar,
                        onConnectionTest = ::checkConnection,
                        onGoogleLogin = { googleAuthorization.request() },
                        onExportScenes = ::exportScenes,
                        onCancelExport = ::cancelExport,
                        onPreviewMovie = ::previewMovie,
                        onSaveMovie = ::saveMovie,
                        onShareMovie = ::shareMovie,
                        onDeleteMovie = ::deleteMovie,
                        onCreateBackup = ::createPortableBackup,
                        onRestoreBackup = ::restorePortableBackup
                    )
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    // Worker may update SharedPreferences while this Activity is paused.
                    // Refresh only when the stored JSON has actually changed.
                    store.refreshClipsFromDisk()
                    delay(2500)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        store.refreshClipsFromDisk()
        store.clips.filter { it.operation.isNotBlank() &&
            it.status !in listOf("Complete", "Failed") }
            .forEach { VideoJobs.enqueue(this, it) }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message.take(170), Toast.LENGTH_LONG).show()
    }

    private fun submitGeneration(project: FilmProject, prompt: String, options: VideoOptions) {
        val cloud = store.config
        val token = store.activeCredential()
        try {
            require(store.isGoogleAuthorizationFresh()) {
                "Google Login has expired or is missing. Open Settings and tap Continue with Google."
            }
            VertexClient.validate(cloud, token)
            val issues = ModelGuard.validate(options)
            require(issues.isEmpty()) { issues.joinToString("; ") }
        } catch (e: Exception) { showMessage(e.message ?: "Check Settings"); return }
        val clip = FilmClip(projectId = project.id, title = "Scene " + (store.clips.count { it.projectId == project.id } + 1),
            prompt = prompt, modelId = options.model, duration = options.duration,
            ratio = options.ratio, resolution = options.resolution, status = "Submitting")
        store.addClip(clip)
        background.execute {
            try {
                val operation = VertexClient.startVideo(cloud, token, prompt, options)
                runOnUiThread {
                    val active = clip.copy(operation = operation, status = "Running")
                    store.updateClip(active)
                    VideoJobs.enqueue(this@MainActivity, active)
                    showMessage("Video submitted. Job tracking continues in background.")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    store.updateClip(clip.copy(
                        status = "Needs review",
                        error = (e.message ?: "Unknown submission result") +
                            " — Never re-submit without checking Cloud job status."
                    ))
                    showMessage("Submission not confirmed. Review job; no automatic charged retry.")
                }
            }
        }
    }

    private fun refreshGeneration(clip: FilmClip) {
        if (clip.operation.isBlank()) {
            showMessage("No operation ID. Check the job in Google Cloud before any retry.")
            return
        }
        VideoJobs.enqueue(this, clip)
        showMessage("Checking existing video operation; no new generation charge.")
    }

    private fun sendDirector(projectId: String, idea: String) {
        if (idea.isBlank()) return
        store.addLine(DirectorLine(projectId = projectId, role = "You", text = idea))
        val token = store.activeCredential()
        val cloud = store.config
        background.execute {
            val response = try { VertexClient.directorPrompt(cloud, token, idea) }
                catch (e: Exception) { "AI Director unavailable: " + (e.message ?: "Check account and model access") }
            runOnUiThread { store.addLine(DirectorLine(projectId = projectId, role = "AI Director", text = response)) }
        }
    }

    private fun importAvatar(name: String, uri: android.net.Uri) {
        try {
            val path = store.importImage(uri)
            store.addAvatar(CharacterPack(nickname = name.trim().ifBlank { "My Character" }, photoPath = path))
            showMessage("Character reference saved privately")
        } catch (e: Exception) { showMessage(e.message ?: "Import failed") }
    }

    private fun checkConnection() {
        val config = store.config
        val credential = store.activeCredential()
        background.execute {
            val message = try {
                VertexClient.validate(config, credential, video = false)
                if (config.authMode == "API_KEY") {
                    "API Key saved. Gemini AI Director only; Veo requires Google Login. " +
                        "Actual model access is checked when you send a prompt."
                } else {
                    "Google Login saved. Check Cloud Project ID and Veo permissions " +
                        "before generation. This is a format check, not a billed API test."
                }
            } catch (e: Exception) { e.message ?: "Not configured" }
            runOnUiThread { store.googleAuthorizationStatus = message; showMessage(message) }
        }
    }

    private fun createPortableBackup(uri: android.net.Uri, password: CharArray) {
        if (store.backupBusy || store.movieExportRunning) {
            password.fill(' ')
            showMessage("Finish your current operation before creating a backup")
            return
        }
        val snapshot = try {
            store.portableBackupSnapshot()
        } catch (e: Exception) {
            password.fill(' ')
            store.backupStatus = e.localizedMessage ?: "Could not collect project backup"
            return
        }
        store.backupBusy = true
        store.backupStatus = "Saving project archive and media to selected location..."
        background.execute {
            try {
                val result = ProjectBackup.create(this, uri, snapshot, password)
                runOnUiThread {
                    store.backupBusy = false
                    store.backupStatus = "Encrypted backup saved: " + result.projects +
                        " projects, " + result.clips + " clips, " + result.files + " media files."
                }
            } catch (e: Exception) {
                // Document providers may leave a partially written file; attempt cleanup.
                try { contentResolver.delete(uri, null, null) } catch (_: Exception) {}
                runOnUiThread {
                    store.backupBusy = false
                    store.backupStatus = "Backup failed: " +
                        (e.localizedMessage ?: "Storage unavailable") +
                        ". Delete any partial encrypted archive at your chosen location."
                }
            } finally { password.fill(' ') }
        }
    }

    private fun restorePortableBackup(uri: android.net.Uri, password: CharArray) {
        if (store.backupBusy || store.movieExportRunning) {
            password.fill(' ')
            showMessage("Finish your current operation before restoring a backup")
            return
        }
        store.backupBusy = true
        store.backupStatus = "Reading and verifying project archive..."
        background.execute {
            val plan = try {
                ProjectBackup.read(this, uri, password)
            } catch (e: Exception) {
                runOnUiThread {
                    store.backupBusy = false
                    store.backupStatus = "Restore rejected: " +
                        (e.localizedMessage ?: "Invalid or corrupted archive")
                }
                return@execute
            } finally { password.fill(' ') }
            runOnUiThread {
                if (isFinishing || isDestroyed) {
                    plan.discardCreatedFiles()
                    store.backupBusy = false
                    return@runOnUiThread
                }
                try {
                    store.commitBackupImport(plan)
                    store.backupStatus = "Restore complete: " + plan.summary.projects +
                        " project copies, " + plan.summary.clips + " clips, " +
                        plan.summary.files + " media files. Sign in to Google again."
                } catch (e: Exception) {
                    plan.discardCreatedFiles()
                    store.backupStatus = "Restore failed without overwriting projects: " +
                        (e.localizedMessage ?: "Storage error")
                } finally { store.backupBusy = false }
            }
        }
    }

    private fun openVideo(clip: FilmClip) = openMp4(File(clip.localPath))

    private fun openMp4(file: File) {
        if (!file.isFile) { showMessage("Local MP4 not available yet"); return }
        try {
            val uri = FileProvider.getUriForFile(this, packageName + ".files", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Preview MVG video"))
        } catch (e: Exception) { showMessage(e.message ?: "No video player available") }
    }

    private fun exportScenes(projectId: String) {
        if (store.backupBusy) {
            showMessage("Wait for the project backup or restore to finish")
            return
        }
        if (store.movieExportRunning || sceneExporter.isRunning()) {
            showMessage("An export is already in progress")
            return
        }
        // Storyboard order matters. Only completed and downloaded clips are included.
        val finished = store.clips.filter { it.projectId == projectId &&
            it.status == "Complete" && File(it.localPath).isFile }
        if (finished.size < 2) {
            showMessage("Download at least two complete clips before exporting")
            return
        }
        store.movieExportRunning = true
        store.movieExportProgress = 0
        store.movieExportError = ""
        try {
            sceneExporter.export(projectId, finished,
                onProgress = { percent -> store.movieExportProgress = percent },
                onComplete = { output, error ->
                    store.movieExportRunning = false
                    if (output != null) {
                        try {
                            check(store.projects.any { it.id == projectId }) {
                                output.delete()
                                "Project was deleted while exporting"
                            }
                            store.addMovie(FilmMovie(
                                projectId = projectId, filePath = output.absolutePath,
                                sceneCount = finished.size
                            ))
                            store.movieExportProgress = 100
                            showMessage("Movie exported locally. Save to Movies or share it.")
                        } catch (e: Exception) {
                            store.movieExportError = e.message ?: "Could not save movie record"
                            showMessage(store.movieExportError)
                        }
                    } else {
                        store.movieExportError = error ?: "Export failed"
                        showMessage(store.movieExportError)
                    }
                }
            )
        } catch (e: Exception) {
            store.movieExportRunning = false
            store.movieExportError = e.message ?: "Unable to start movie export"
            showMessage(store.movieExportError)
        }
    }

    private fun cancelExport() {
        sceneExporter.cancel()
        store.movieExportRunning = false
        store.movieExportProgress = 0
        store.movieExportError = "Movie export cancelled; source scenes are unchanged."
    }

    private fun previewMovie(movie: FilmMovie) = openMp4(File(movie.filePath))

    private fun saveMovie(movie: FilmMovie) {
        background.execute {
            val result = try {
                val uri = SceneExporter.copyToMovies(this, File(movie.filePath))
                "Saved to Movies/MVG: " + uri.lastPathSegment.orEmpty().take(35)
            } catch (e: Exception) {
                "Could not save movie: " + (e.localizedMessage ?: "device error")
            }
            runOnUiThread { showMessage(result) }
        }
    }

    private fun shareMovie(movie: FilmMovie) {
        val file = File(movie.filePath)
        if (!file.isFile) { showMessage("Movie file no longer exists"); return }
        try {
            val uri = FileProvider.getUriForFile(this, packageName + ".files", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = android.content.ClipData.newRawUri("MVG movie", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share MVG movie"))
        } catch (e: Exception) { showMessage(e.message ?: "Sharing unavailable") }
    }

    private fun deleteMovie(movie: FilmMovie) {
        store.removeMovie(movie.id)
        showMessage("Export removed from private project storage")
    }

    override fun onDestroy() {
        if (sceneExporter.isRunning()) sceneExporter.cancel()
        background.shutdown()
        super.onDestroy()
    }
}

@Composable
private fun MVGApp(
    store: StudioStore,
    onGenerate: (FilmProject, String, VideoOptions) -> Unit,
    onPoll: (FilmClip) -> Unit,
    onChat: (String, String) -> Unit,
    onPlay: (FilmClip) -> Unit,
    onAvatarPhoto: (String, android.net.Uri) -> Unit,
    onConnectionTest: () -> Unit,
    onGoogleLogin: () -> Unit,
    onExportScenes: (String) -> Unit,
    onCancelExport: () -> Unit,
    onPreviewMovie: (FilmMovie) -> Unit,
    onSaveMovie: (FilmMovie) -> Unit,
    onShareMovie: (FilmMovie) -> Unit,
    onDeleteMovie: (FilmMovie) -> Unit,
    onCreateBackup: (android.net.Uri, CharArray) -> Unit,
    onRestoreBackup: (android.net.Uri, CharArray) -> Unit
) {
    var root by rememberSaveable { mutableStateOf("Projects") }
    var projectId by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf("Generate") }
    val project = store.projects.firstOrNull { it.id == projectId }
    Column(Modifier.fillMaxSize()) {
        BrandHeader(
            title = project?.title ?: if (root == "Projects") "Your Studio" else root,
            subtitle = if (project != null) "Your private AI film workspace" else "MVG · AI VIDEO STUDIO",
            back = project != null,
            onBack = { projectId = ""; tab = "Generate" }
        )
        if (project != null) {
            TabRow(selectedTabIndex = listOf("Generate", "AI Director", "Scenes", "Assets").indexOf(tab)) {
                listOf("Generate", "AI Director", "Scenes", "Assets").forEach { name ->
                    Tab(
                        selected = tab == name, onClick = { tab = name },
                        text = { Text(name, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                var draft by rememberSaveable(project.id) { mutableStateOf(store.getDraft(project.id)) }
                when (tab) {
                    "Generate" -> GeneratorView(store, project, draft, {
                        draft = it
                        store.saveDraft(project.id, it)
                    }, onGenerate, onPoll, onPlay)
                    "AI Director" -> DirectorView(store, project.id, onChat) {
                        draft = it
                        store.saveDraft(project.id, it)
                        tab = "Generate"
                    }
                    "Scenes" -> ScenesView(store, project.id, onPoll, onPlay,
                        onExportScenes, onCancelExport, onPreviewMovie, onSaveMovie,
                        onShareMovie, onDeleteMovie)
                    "Assets" -> AssetsView(store, project.id, onPlay)
                }
                if (tab != "AI Director") {
                    FloatingActionButton(onClick = { tab = "AI Director" },
                        containerColor = Elevated, contentColor = Sky,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Open AI Director")
                    }
                }
            }
        } else {
            Box(Modifier.weight(1f)) {
                when (root) {
                    "Projects" -> ProjectsView(store) { id -> projectId = id }
                    "Library" -> LibraryView(store, onPlay)
                    "Settings" -> SettingsView(store, onAvatarPhoto, onConnectionTest,
                        onGoogleLogin, onCreateBackup, onRestoreBackup)
                }
            }
            NavigationBar(containerColor = Panel) {
                listOf("Projects", "Library", "Settings").forEach { name ->
                    NavigationBarItem(
                        selected = root == name,
                        onClick = { root = name },
                        label = { Text(name) },
                        icon = {
                            Icon(when(name) {
                                "Projects" -> Icons.Default.Movie
                                "Library" -> Icons.Default.VideoLibrary
                                else -> Icons.Default.Settings
                            }, contentDescription = name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(title: String, subtitle: String, back: Boolean, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Navy).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (back) IconButton(onClick = onBack) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back to projects", tint = Sky)
        } else Box(
            Modifier.size(43.dp).clip(RoundedCornerShape(13.dp)).background(gradient),
            contentAlignment = Alignment.Center
        ) { Text("M", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Navy) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextWhite, fontWeight = FontWeight.Bold,
                fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        if (!back) Icon(Icons.Default.AutoAwesome, tint = Violet, contentDescription = null)
    }
}

@Composable
private fun StudioCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
private fun Eyebrow(value: String) {
    Text(value.uppercase(), color = Sky, fontSize = 11.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProjectsView(store: StudioStore, open: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioCard {
            Eyebrow("CREATE SOMETHING NEW")
            Text("Every film starts with a project.", fontWeight = FontWeight.Bold, fontSize = 23.sp)
            Text("Keep your videos, prompts, characters and shots together.", color = Muted, fontSize = 14.sp)
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New project")
            }
        }
        Eyebrow("YOUR PROJECTS · " + store.projects.size)
        if (store.projects.isEmpty()) {
            Text("No projects yet. Create one to open your film studio.", color = Muted)
        }
        store.projects.forEach { p ->
            StudioCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(66.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF183A63), Color(0xFF4A235E)))),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Default.MovieCreation, null, tint = Sky, modifier = Modifier.size(32.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Text(store.clips.count { it.projectId == p.id }.toString() + " clips",
                            color = Muted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { deleteId = p.id }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete project", tint = Muted)
                    }
                }
                OutlinedButton(onClick = { open(p.id) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Open Studio")
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, null)
                }
            }
        }
    }
    if (creating) AlertDialog(
        onDismissRequest = { creating = false },
        title = { Text("New Film Project") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it.take(80) },
                label = { Text("Project name") }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    val p = store.createProject(name)
                    creating = false
                    name = ""
                    open(p.id)
                }
            }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = { creating = false }) { Text("Cancel") } }
    )
    if (deleteId.isNotBlank()) AlertDialog(
        onDismissRequest = { deleteId = "" },
        title = { Text("Delete project?") },
        text = { Text("This removes its project records and scene list from MVG. Exported clips already on your device are not deleted.") },
        confirmButton = { TextButton(onClick = {
            store.removeProject(deleteId); deleteId = ""
        }) { Text("Delete", color = Color(0xFFFF708F)) } },
        dismissButton = { TextButton(onClick = { deleteId = "" }) { Text("Cancel") } }
    )
}

@Composable
private fun ChoiceRow(label: String, choices: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { choice ->
                FilterChip(
                    selected = selected == choice,
                    onClick = { onSelect(choice) },
                    label = { Text(choice) }
                )
            }
        }
    }
}

@Composable
private fun GeneratorView(
    store: StudioStore,
    project: FilmProject,
    prompt: String,
    onPrompt: (String) -> Unit,
    onGenerate: (FilmProject, String, VideoOptions) -> Unit,
    onPoll: (FilmClip) -> Unit,
    onPlay: (FilmClip) -> Unit
) {
    var model by rememberSaveable(project.id) { mutableStateOf("veo-3.1-fast-generate-001") }
    var duration by rememberSaveable(project.id) { mutableIntStateOf(8) }
    var ratio by rememberSaveable(project.id) { mutableStateOf("16:9") }
    var resolution by rememberSaveable(project.id) { mutableStateOf("720p") }
    var audio by rememberSaveable(project.id) { mutableStateOf(true) }
    var avatarId by rememberSaveable(project.id) { mutableStateOf("") }
    var accepted by rememberSaveable(project.id) { mutableStateOf(false) }
    var avatarConsent by rememberSaveable(project.id) { mutableStateOf(false) }
    var askConfirm by remember { mutableStateOf(false) }
    val selectedAvatar = store.avatars.firstOrNull { it.id == avatarId }
    val selectedOptions = VideoOptions(
        model = model, duration = duration, ratio = ratio, resolution = resolution,
        audio = audio, firstFramePath = selectedAvatar?.photoPath ?: ""
    )
    val modelErrors = ModelGuard.validate(selectedOptions)
    val estimate = ModelGuard.quoteText(selectedOptions)
    val projectClips = store.clips.filter { it.projectId == project.id }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        StudioCard {
            Eyebrow("VIDEO GENERATOR")
            Text("Direct your next shot", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Choose a model, describe the scene and review the paid API request.",
                color = Muted, fontSize = 13.sp)
            var showModels by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { showModels = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(model.removePrefix("veo-").replace("-generate-001","").uppercase(),
                        modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Choose model")
                }
                DropdownMenu(expanded = showModels, onDismissRequest = { showModels = false }) {
                    VertexClient.VIDEO_MODELS.forEach {
                        DropdownMenuItem(text = { Text(it.removePrefix("veo-").replace("-generate-001","")) },
                            onClick = {
                                model = it
                                if (resolution !in ModelGuard.resolutions(model)) resolution = "720p"
                                if (duration !in ModelGuard.durations(resolution)) duration = 8
                                accepted = false
                                showModels = false
                            })
                    }
                }
            }
            OutlinedTextField(
                value = prompt, onValueChange = { onPrompt(it.take(5000)); accepted = false },
                label = { Text("Cinematic prompt") },
                placeholder = { Text("A moody cinematic tracking shot...") },
                minLines = 5, maxLines = 10,
                modifier = Modifier.fillMaxWidth()
            )
            ChoiceRow("DURATION", ModelGuard.durations(resolution).map { it.toString() }, duration.toString()) {
                duration = it.toInt()
                accepted = false
            }
            ChoiceRow("ASPECT RATIO", listOf("16:9", "9:16"), ratio) { ratio = it; accepted = false }
            ChoiceRow("RESOLUTION", ModelGuard.resolutions(model), resolution) {
                resolution = it
                if (duration !in ModelGuard.durations(it)) duration = 8
                accepted = false
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Generate native audio", modifier = Modifier.weight(1f))
                Switch(checked = audio, onCheckedChange = { audio = it; accepted = false })
            }
            var showAvatars by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { showAvatars = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(store.avatars.firstOrNull { it.id == avatarId }?.nickname
                        ?: "First-frame image (optional)", modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Image, contentDescription = "Choose image")
                }
                DropdownMenu(expanded = showAvatars, onDismissRequest = { showAvatars = false }) {
                    DropdownMenuItem(text = { Text("Text-only (no image)") },
                        onClick = { avatarId = ""; showAvatars = false })
                    store.avatars.forEach { a ->
                        DropdownMenuItem(text = { Text(a.nickname) },
                            onClick = { avatarId = a.id; showAvatars = false; accepted = false })
                    }
                }
            }
            Text("Selected character photo is used as a FIRST FRAME, not a trained/locked identity. " +
                "Subject-reference packs are not implemented in this preview.",
                fontSize = 12.sp, color = Muted)
            if (selectedAvatar != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = avatarConsent, onCheckedChange = { avatarConsent = it })
                    Text("I have permission to use this person's image.", fontSize = 12.sp)
                }
            }
        }
        StudioCard {
            Eyebrow("COST & SAFETY")
            Text(estimate, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Sky)
            Text("Public Google Cloud list-price calculation, checked " + ModelGuard.RATE_DATE +
                ". USD per generated second, single output; excludes tax, storage and discounts. " +
                "Your account bill may differ. No automatic paid retries.",
                color = Muted, fontSize = 12.sp)
            if (modelErrors.isNotEmpty()) Text(modelErrors.joinToString("; "), color = Color(0xFFFFB77A))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                Text("I understand the paid API charge and authorize this single request.",
                    fontSize = 12.sp)
            }
            val connected = store.credentialRevision >= 0 &&
                store.config.authMode == "GOOGLE" &&
                store.config.projectId.isNotBlank() &&
                store.isGoogleAuthorizationFresh()
            Button(
                onClick = { askConfirm = true },
                enabled = prompt.isNotBlank() && accepted && connected && modelErrors.isEmpty() &&
                    (selectedAvatar == null || avatarConsent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate video")
            }
            if (!connected) Text("Veo needs Google Login and Cloud Project ID. Connect in Settings.",
                color = Color(0xFFFFCA7A), fontSize = 12.sp)
        }
        Eyebrow("GENERATED CLIPS · " + projectClips.size)
        if (projectClips.isEmpty()) Text("Your generated clips will appear here.", color = Muted)
        projectClips.forEach { clip -> ClipCard(clip, onPoll, onPlay, null, null) }
        Spacer(Modifier.height(58.dp))
    }
    if (askConfirm) AlertDialog(
        onDismissRequest = { askConfirm = false },
        title = { Text("Confirm Google Cloud charge") },
        text = {
            Text("Model: " + model + "\nLength: " + duration + "s\nOutput: " + resolution +
                " " + ratio + "\nAudio: " + audio +
                "\nEstimate: " + estimate + " (public USD list price, " + ModelGuard.RATE_DATE + ")" +
                "\n\nYour Google Cloud account may be billed a different amount. Submit ONCE?")
        },
        confirmButton = {
            TextButton(onClick = {
                askConfirm = false
                accepted = false
                onGenerate(project, prompt, selectedOptions)
            }) { Text("Confirm & Generate") }
        },
        dismissButton = { TextButton(onClick = { askConfirm = false }) { Text("Cancel") } }
    )
}

@Composable
private fun ClipCard(
    clip: FilmClip,
    onPoll: (FilmClip) -> Unit,
    onPlay: (FilmClip) -> Unit,
    onUp: (() -> Unit)?,
    onDown: (() -> Unit)?
) {
    StudioCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VideoFile, tint = Sky, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(clip.title, fontWeight = FontWeight.Bold)
                Text(clip.status + " · " + clip.duration + "s · " + clip.resolution,
                    color = if (clip.status == "Complete") Color(0xFF65E2B3) else Muted, fontSize = 12.sp)
            }
            if (onUp != null) IconButton(onClick = onUp) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move scene up")
            }
            if (onDown != null) IconButton(onClick = onDown) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move scene down")
            }
        }
        Text(clip.prompt, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        if (clip.error.isNotBlank()) Text(clip.error, color = Color(0xFFFFB77A), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (clip.operation.isNotBlank() && clip.status != "Complete") {
                OutlinedButton(onClick = { onPoll(clip) }) { Text("Check / Download") }
            }
            if (clip.localPath.isNotBlank()) {
                Button(onClick = { onPlay(clip) }) { Icon(Icons.Default.PlayArrow, null); Text("Play") }
            }
        }
    }
}

@Composable
private fun ScenesView(
    store: StudioStore,
    projectId: String,
    onPoll: (FilmClip) -> Unit,
    onPlay: (FilmClip) -> Unit,
    onExport: (String) -> Unit,
    onCancelExport: () -> Unit,
    onPreviewMovie: (FilmMovie) -> Unit,
    onSaveMovie: (FilmMovie) -> Unit,
    onShareMovie: (FilmMovie) -> Unit,
    onDeleteMovie: (FilmMovie) -> Unit
) {
    val clips = store.clips.filter { it.projectId == projectId }
    val finished = clips.filter { it.status == "Complete" && File(it.localPath).isFile }
    val exports = store.movies.filter { it.projectId == projectId }
    var confirmDelete by remember { mutableStateOf<FilmMovie?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Eyebrow("SCENE BOARD")
        Text("Build your movie", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Rearrange shots, then stitch completed videos into one local MP4. " +
            "No extra Vertex AI generation request is made for export.",
            color = Muted, fontSize = 13.sp)
        StudioCard {
            Eyebrow("MOVIE EXPORT · " + finished.size + " READY")
            Text("Combine scenes in order", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("At least 2 downloaded clips are required. Unfinished or missing " +
                "clips are excluded. Output quality depends on source videos and phone codecs.",
                color = Muted, fontSize = 12.sp)
            if (store.movieExportRunning) {
                Text("Rendering locally: " + store.movieExportProgress + "%",
                    fontWeight = FontWeight.Bold, color = Sky)
                LinearProgressIndicator(
                    progress = { (store.movieExportProgress / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = onCancelExport, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cancel export")
                }
            } else {
                Button(
                    onClick = { onExport(projectId) },
                    enabled = finished.size >= 2 && !store.backupBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MovieCreation, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Export " + finished.size + " scenes as MP4")
                }
            }
            if (store.movieExportError.isNotBlank()) {
                Text(store.movieExportError, color = Color(0xFFFFB77A), fontSize = 12.sp)
            }
        }

        if (exports.isNotEmpty()) {
            Eyebrow("EXPORTED MOVIES · " + exports.size)
            exports.forEach { movie ->
                StudioCard {
                    val file = File(movie.filePath)
                    Text(file.name, color = Sky, fontWeight = FontWeight.SemiBold)
                    Text(movie.sceneCount.toString() + " scenes • " +
                        if (file.isFile) ((file.length() / (1024L * 1024L)).toString() + " MB")
                        else "File missing", color = Muted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onPreviewMovie(movie) },
                            enabled = file.isFile,
                            modifier = Modifier.weight(1f)
                        ) { Text("Preview") }
                        OutlinedButton(
                            onClick = { onSaveMovie(movie) },
                            enabled = file.isFile,
                            modifier = Modifier.weight(1f)
                        ) { Text("Save to Movies") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onShareMovie(movie) },
                            enabled = file.isFile,
                            modifier = Modifier.weight(1f)
                        ) { Text("Share") }
                        OutlinedButton(
                            onClick = { confirmDelete = movie },
                            modifier = Modifier.weight(1f)
                        ) { Text("Delete export") }
                    }
                }
            }
        }

        Eyebrow("STORYBOARD · " + clips.size + " SHOTS")
        if (clips.isEmpty()) Text("Generate a few clips first to build your movie.", color = Muted)
        clips.forEachIndexed { index, clip ->
            Text("SHOT " + (index + 1).toString().padStart(2, '0'),
                color = Violet, fontSize = 12.sp)
            ClipCard(
                clip, onPoll, onPlay,
                if (index > 0) ({ store.moveClip(projectId, clip.id, true) }) else null,
                if (index < clips.lastIndex) ({ store.moveClip(projectId, clip.id, false) }) else null
            )
        }
        Text("Export is performed on this device and can stop if the app is closed. " +
            "4K and mixed video formats may exceed your phone's editing capability.",
            color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(50.dp))
    }
    confirmDelete?.let { movie ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete exported movie?") },
            text = { Text("Only this exported MP4 will be removed from MVG. " +
                "Original generated clips and any copy saved in Movies remain unchanged.") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteMovie(movie)
                    confirmDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DirectorView(store: StudioStore, projectId: String, onChat: (String, String) -> Unit,
                         applyPrompt: (String) -> Unit) {
    var idea by rememberSaveable(projectId) { mutableStateOf("") }
    val lines = store.messages.filter { it.projectId == projectId }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        StudioCard {
            Eyebrow("AI DIRECTOR")
            Text("Creative partner", fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Text("Powered by your Vertex Gemini account. Messages can incur API charges. " +
                "Use Apply to Generator to review prompts before video generation.",
                color = Muted, fontSize = 12.sp)
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (lines.isEmpty()) item {
                Text("Describe a scene: characters, action, mood and camera movement. " +
                    "I'll craft a cinematic prompt.", color = Muted)
            }
            items(lines, key = { it.id }) { line ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = if (line.role == "You") Elevated else Panel)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(line.role, color = if (line.role == "You") Sky else Violet,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(line.text, fontSize = 14.sp)
                        if (line.role == "AI Director" && !line.text.startsWith("AI Director unavailable")) {
                            TextButton(onClick = { applyPrompt(line.text) }) {
                                Icon(Icons.Default.ArrowForward, null)
                                Text("Use in Generator")
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = idea, onValueChange = { idea = it.take(3500) },
                placeholder = { Text("Describe your scene...") },
                maxLines = 4, modifier = Modifier.weight(1f))
            IconButton(onClick = { if (idea.isNotBlank()) { onChat(projectId, idea); idea = "" } },
                enabled = idea.isNotBlank()) {
                Icon(Icons.Default.Send, contentDescription = "Send message", tint = Sky)
            }
        }
    }
}

@Composable
private fun AssetsView(store: StudioStore, projectId: String, onPlay: (FilmClip) -> Unit) {
    val clips = store.clips.filter { it.projectId == projectId }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("PROJECT ASSETS")
        Text("Video library", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Locally downloaded media plays without internet.", color = Muted)
        clips.forEach { clip ->
            StudioCard {
                Text(clip.title, fontWeight = FontWeight.SemiBold)
                Text(clip.status + " • " + clip.modelId, color = Muted, fontSize = 12.sp)
                if (clip.localPath.isNotBlank()) Button(onClick = { onPlay(clip) }) { Text("Open Video") }
            }
        }
        if (clips.isEmpty()) Text("No assets yet.", color = Muted)
    }
}

@Composable
private fun LibraryView(store: StudioStore, onPlay: (FilmClip) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("YOUR MEDIA LIBRARY")
        Text("All videos", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        store.clips.forEach { clip ->
            StudioCard {
                Text(clip.title, fontWeight = FontWeight.SemiBold)
                Text(clip.status + " • " + clip.duration + "s", color = Muted)
                if (clip.localPath.isNotBlank()) Button(onClick = { onPlay(clip) }) { Text("Play") }
            }
        }
        if (store.clips.isEmpty()) Text("Your generated videos will appear here.", color = Muted)
    }
}

@Composable
private fun SettingsView(
    store: StudioStore,
    onAvatarPhoto: (String, android.net.Uri) -> Unit,
    onConnectionTest: () -> Unit,
    onGoogleLogin: () -> Unit,
    onCreateBackup: (android.net.Uri, CharArray) -> Unit,
    onRestoreBackup: (android.net.Uri, CharArray) -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val signingSha1 = remember(context) { SigningIdentity.certificateSha1(context) }
    var cloudProject by remember { mutableStateOf(store.config.projectId) }
    var region by remember { mutableStateOf(store.config.region) }
    var bucket by remember { mutableStateOf(store.config.outputBucket) }
    var authMode by remember { mutableStateOf(store.config.authMode) }
    var apiKey by remember { mutableStateOf("") }
    var avatarName by remember { mutableStateOf("") }
    var showRemove by remember { mutableStateOf("") }
    var showBackupWarning by remember { mutableStateOf(false) }
    var showRestoreWarning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ProjectBackup.MIME_TYPE)
    ) { uri -> if (uri != null) onCreateBackup(uri) }
    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onRestoreBackup(uri) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { onAvatarPhoto(avatarName, uri); avatarName = "" }
    }
    fun saveCloudConfig() {
        store.updateConfig(CloudConfig(cloudProject.trim(), region.trim(), bucket.trim(), authMode))
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioCard {
            Eyebrow("CONNECT TO GOOGLE AI")
            Text("Choose how to connect", fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text("Only two sign-in options. Google Login is recommended for Veo videos. " +
                "API Key is available for compatible Gemini AI Director requests.",
                fontSize = 13.sp, color = Muted)

            ChoiceRow("CONNECTION METHOD", listOf("GOOGLE", "API_KEY"), authMode) { selected ->
                authMode = selected
                saveCloudConfig()
                status = ""
            }

            if (authMode == "GOOGLE") {
                Text("Google account sign-in", fontWeight = FontWeight.SemiBold)
                Button(onClick = {
                    saveCloudConfig()
                    onGoogleLogin()
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Continue with Google")
                }
                Text(
                    if (store.credentialRevision >= 0 && store.isGoogleAuthorizationFresh())
                        "Google authorization active (short-lived)"
                    else "Connect or renew Google Login to generate videos",
                    color = if (store.isGoogleAuthorizationFresh()) Color(0xFF65E2B3) else Muted,
                    fontSize = 12.sp
                )
                Text("Google Cloud authorization requires an Android OAuth client " +
                    "registered for this exact APK signing certificate.",
                    color = Muted, fontSize = 12.sp)
                Text("Android package: com.parmtech.mvg",
                    color = Muted, fontSize = 12.sp)
                Text("Signing certificate SHA-1", color = Muted, fontSize = 12.sp)
                Text(signingSha1, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = Sky)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val clip = ClipData.newPlainText("MVG signing SHA-1", signingSha1)
                        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        manager.setPrimaryClip(clip)
                        status = "SHA-1 copied. Register it in Google OAuth Android client."
                    }, enabled = signingSha1 != "Unavailable", modifier = Modifier.weight(1f)) {
                        Text("Copy SHA-1")
                    }
                    OutlinedButton(onClick = {
                        uriHandler.openUri("https://console.cloud.google.com/auth/clients")
                    }, modifier = Modifier.weight(1f)) {
                        Text("OAuth Setup")
                    }
                }
                Text("Your Google Cloud billing project must have the eligible $300 " +
                    "Welcome Credit and Veo quota. Google Login alone does not select a " +
                    "billing account. A Free Trial can have service restrictions.",
                    color = Color(0xFFFFCA7A), fontSize = 12.sp)
            } else {
                Text("Google Cloud API Key", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it.trim() },
                    label = { Text("Paste API Key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(onClick = {
                    try {
                        require(apiKey.isNotBlank()) { "Paste a valid API Key" }
                        store.apiKeyVault.save(apiKey)
                        apiKey = ""
                        saveCloudConfig()
                        store.credentialsChanged()
                        status = "API Key saved privately on your device"
                    } catch (e: Exception) { status = e.message ?: "Could not save API Key" }
                }, enabled = apiKey.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Key, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save API Key")
                }
                Text(
                    if (store.credentialRevision >= 0 && store.apiKeyVault.read().isNotBlank())
                        "Encrypted API Key saved on this device" else "No API Key saved",
                    color = Muted,
                    fontSize = 12.sp
                )
                Text("API Key mode supports documented Gemini generateContent calls " +
                    "for AI Director. Veo long-running video generation requires Google Login. " +
                    "Never paste a key into the public GitHub repository.",
                    color = Color(0xFFFFCA7A), fontSize = 12.sp)
            }

            HorizontalDivider()
            Eyebrow("CLOUD PROJECT SETTINGS")
            Text("Project ID and video region configure the Cloud destination; " +
                "they are not extra login methods.", color = Muted, fontSize = 12.sp)
            OutlinedTextField(
                value = cloudProject,
                onValueChange = { cloudProject = it.trim() },
                label = { Text("Google Cloud Project ID") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = region, onValueChange = { region = it.trim() },
                label = { Text("Video region") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bucket, onValueChange = { bucket = it.trim() },
                label = { Text("Optional output folder (gs://bucket/folder)") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(onClick = {
                saveCloudConfig()
                status = "Cloud project settings saved"
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Cloud Settings") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    saveCloudConfig()
                    onConnectionTest()
                }, modifier = Modifier.weight(1f)) { Text("Check Setup") }
                OutlinedButton(onClick = {
                    if (authMode == "GOOGLE") store.clearGoogleAuthorization()
                    else { store.apiKeyVault.clear(); store.credentialsChanged() }
                    status = "Selected connection cleared"
                }, modifier = Modifier.weight(1f)) { Text("Disconnect") }
            }
            if (status.isNotBlank()) Text(status, color = Sky, fontSize = 12.sp)
            if (store.googleAuthorizationStatus.isNotBlank()) {
                Text(store.googleAuthorizationStatus, color = Muted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = {
                uriHandler.openUri("https://console.cloud.google.com/billing")
            }, modifier = Modifier.fillMaxWidth()) { Text("Check $300 Credit in Cloud Billing") }
            Text("The Cloud Billing page shows credits and their expiry; MVG cannot " +
                "read your balance. This setup check is NOT a paid test or proof " +
                "of model access. Video requests still require individual cost approval.",
                color = Muted, fontSize = 12.sp)
        }
        StudioCard {
            Eyebrow("PROJECT BACKUP & RESTORE")
            Text("Keep your films when upgrading", fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text("Export all MVG projects, scene order, drafts, Director messages, " +
                "imported reference photos, downloaded clips and rendered movies. " +
                "The backup can be opened later on another device.",
                fontSize = 12.sp, color = Muted)
            Text("Security: The portable ZIP is NOT ENCRYPTED and may contain personal " +
                "photos and prompts. Store it somewhere private. Google login credentials, " +
                "API keys and signing secrets are NEVER included.",
                color = Color(0xFFFFCA7A), fontSize = 12.sp)
            if (store.backupBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Working with your backup...", color = Sky, fontSize = 12.sp)
            }
            Button(
                onClick = { showBackupWarning = true },
                enabled = !store.backupBusy && !store.movieExportRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.SaveAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create complete ZIP backup")
            }
            OutlinedButton(
                onClick = { showRestoreWarning = true },
                enabled = !store.backupBusy && !store.movieExportRunning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Restore ZIP (add as new projects)")
            }
            Text("Restore checks ZIP paths, file checksums and size limits. Existing " +
                "projects are not overwritten. Google Login must be connected again.",
                fontSize = 12.sp, color = Muted)
            if (store.backupStatus.isNotBlank()) {
                Text(store.backupStatus, color = Sky, fontSize = 12.sp)
            }
        }
        StudioCard {
            Eyebrow("CHARACTER LIBRARY")
            Text("Reusable avatars", fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text("Store multiple private character photos. In this build a selected photo can be " +
                "sent as a first-frame image, not persistent face identity training.",
                color = Muted, fontSize = 12.sp)
            OutlinedTextField(value = avatarName, onValueChange = { avatarName = it.take(60) },
                label = { Text("Character name") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = { launcher.launch("image/*") },
                enabled = avatarName.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import character photo")
            }
            store.avatars.forEach { a ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Face, tint = Violet, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(a.nickname, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showRemove = a.id }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete avatar")
                    }
                }
            }
        }
        StudioCard {
            Eyebrow("BUILD INFORMATION")
            Text("MVG · 0.6.0 — Portable project backup", fontWeight = FontWeight.Bold)
            Text("Projects, prompt generator, optional Vertex API call, AI Director, local avatar photos " +
                "and scene reorder are included. Google Login and API Key are separate connection methods; " +
                "background video checks, local MP4 export and manual ZIP project backup/restore are included. " +
                "Physical device testing is still required.",
                color = Muted, fontSize = 12.sp)
        }
    }
    if (showBackupWarning) AlertDialog(
        onDismissRequest = { showBackupWarning = false },
        title = { Text("Export private project ZIP?") },
        text = { Text("This portable backup contains your project text, photos " +
            "and downloaded videos. It is NOT password-protected. Choose a private " +
            "folder and keep the ZIP away from public file-sharing locations.") },
        confirmButton = {
            TextButton(onClick = {
                showBackupWarning = false
                backupLauncher.launch("MVG-backup-" + System.currentTimeMillis() + ".zip")
            }) { Text("Choose backup location") }
        },
        dismissButton = {
            TextButton(onClick = { showBackupWarning = false }) { Text("Cancel") }
        }
    )
    if (showRestoreWarning) AlertDialog(
        onDismissRequest = { showRestoreWarning = false },
        title = { Text("Import MVG project archive?") },
        text = { Text("Only import ZIP files you trust. The contents will be validated " +
            "and added as separate restored projects, without overwriting the originals. " +
            "You will need to connect Google again to use Vertex AI.") },
        confirmButton = {
            TextButton(onClick = {
                showRestoreWarning = false
                restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
            }) { Text("Choose ZIP") }
        },
        dismissButton = {
            TextButton(onClick = { showRestoreWarning = false }) { Text("Cancel") }
        }
    )
    if (showRemove.isNotBlank()) AlertDialog(
        onDismissRequest = { showRemove = "" }, title = { Text("Delete character photo?") },
        text = { Text("This removes the saved reference photo from this device.") },
        confirmButton = { TextButton(onClick = {
            store.removeAvatar(showRemove); showRemove = ""
        }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { showRemove = "" }) { Text("Cancel") } }
    )
}
