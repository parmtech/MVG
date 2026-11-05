package com.parmtech.mvg

import android.content.Intent
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
    private val working = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                        onConnectionTest = ::checkConnection
                    )
                }
            }
        }
        store.clips.filter { it.status == "Running" || it.status == "Submitted" || it.status == "OutputReady" }
            .forEach { refreshGeneration(it) }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message.take(170), Toast.LENGTH_LONG).show()
    }

    private fun submitGeneration(project: FilmProject, prompt: String, options: VideoOptions) {
        val cloud = store.config
        val token = store.vault.read()
        try {
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
                    store.updateClip(clip.copy(operation = operation, status = "Running"))
                    showMessage("Video submitted. You can use AI Director.")
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
        if (clip.operation.isBlank() || !working.add(clip.id)) {
            if (clip.operation.isBlank()) showMessage("No operation ID; inspect Cloud console")
            return
        }
        val token = store.vault.read()
        val cloud = store.config
        background.execute {
            try {
                val result = VertexClient.pollVideo(cloud, token, clip)
                when {
                    !result.done -> runOnUiThread { store.updateClip(clip.copy(status = "Running", error = "")) }
                    result.error.isNotBlank() -> runOnUiThread {
                        store.updateClip(clip.copy(status = "Failed", error = result.error))
                    }
                    else -> {
                        val file = store.videoOutputFile(clip.id)
                        try {
                            VertexClient.writeVideo(token, result, file)
                            runOnUiThread { store.updateClip(clip.copy(
                                status = "Complete", remoteUri = result.remoteUri,
                                localPath = file.absolutePath, error = ""
                            )) }
                        } catch (e: Exception) {
                            runOnUiThread { store.updateClip(clip.copy(status = "OutputReady",
                                remoteUri = result.remoteUri, error = e.message ?: "Download failed")) }
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread { store.updateClip(clip.copy(
                    status = "Check again", error = e.message ?: "Temporary error")) }
            } finally { working.remove(clip.id) }
        }
    }

    private fun sendDirector(projectId: String, idea: String) {
        if (idea.isBlank()) return
        store.addLine(DirectorLine(projectId = projectId, role = "You", text = idea))
        val token = store.vault.read()
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
        val token = store.vault.read()
        background.execute {
            val message = try {
                VertexClient.validate(config, token)
                // Non-billable Vertex IAM/model eligibility cannot be fully proven here.
                "Settings look valid. Model IAM/billing verified on first actual request."
            } catch (e: Exception) { e.message ?: "Not configured" }
            runOnUiThread { showMessage(message) }
        }
    }

    private fun openVideo(clip: FilmClip) {
        val file = File(clip.localPath)
        if (!file.isFile) { showMessage("Local video not downloaded yet"); return }
        try {
            val uri = FileProvider.getUriForFile(this, packageName + ".files", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Preview MVG video"))
        } catch (e: Exception) { showMessage(e.message ?: "No video player available") }
    }

    override fun onDestroy() {
        if (isFinishing) background.shutdown()
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
    onConnectionTest: () -> Unit
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
                    "Scenes" -> ScenesView(store, project.id, onPoll, onPlay)
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
                    "Settings" -> SettingsView(store, onAvatarPhoto, onConnectionTest)
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
            val connected = store.config.projectId.isNotBlank() && store.vault.read().isNotBlank()
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
            if (!connected) Text("Open Settings to enter Google Cloud Project ID and a valid OAuth token.",
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
private fun ScenesView(store: StudioStore, projectId: String, onPoll: (FilmClip) -> Unit,
                       onPlay: (FilmClip) -> Unit) {
    val clips = store.clips.filter { it.projectId == projectId }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Eyebrow("SCENE BOARD")
        Text("Arrange your story", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Move scenes up or down. Source videos remain unchanged. " +
            "Multi-clip MP4 rendering is planned for the next build.",
            color = Muted, fontSize = 13.sp)
        if (clips.isEmpty()) Text("Create a video in Generate to start your scene list.", color = Muted)
        clips.forEachIndexed { index, clip ->
            Text("SHOT " + (index + 1).toString().padStart(2, '0'), color = Violet, fontSize = 12.sp)
            ClipCard(clip, onPoll, onPlay,
                if (index > 0) {{ store.moveClip(projectId, clip.id, true) }} else null,
                if (index < clips.lastIndex) {{ store.moveClip(projectId, clip.id, false) }} else null
            )
        }
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
    onConnectionTest: () -> Unit
) {
    var cloudProject by remember { mutableStateOf(store.config.projectId) }
    var region by remember { mutableStateOf(store.config.region) }
    var bucket by remember { mutableStateOf(store.config.outputBucket) }
    var token by remember { mutableStateOf("") }
    var avatarName by remember { mutableStateOf("") }
    var showRemove by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { onAvatarPhoto(avatarName, uri); avatarName = "" }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        StudioCard {
            Eyebrow("GOOGLE CLOUD · VERTEX AI")
            Text("Connect your account", fontWeight = FontWeight.Bold, fontSize = 21.sp)
            Text("Australian Google Cloud accounts can use us-central1 when the model and your " +
                "project permit it. Veo 3.1 video models are documented in us-central1.",
                fontSize = 12.sp, color = Muted)
            OutlinedTextField(value = cloudProject, onValueChange = { cloudProject = it.trim() },
                label = { Text("Google Cloud Project ID") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = region, onValueChange = { region = it.trim() },
                label = { Text("Video Region") }, singleLine = true,
                modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bucket, onValueChange = { bucket = it.trim() },
                label = { Text("Optional output GCS URI (gs://bucket/folder)") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Authentication (v0.1): paste a short-lived OAuth 2.0 access token obtained securely " +
                "for your Google Cloud user. It expires; repeat when needed. Never paste service-account JSON. " +
                "Google Sign-in will be added later.",
                color = Muted, fontSize = 12.sp)
            OutlinedTextField(value = token, onValueChange = { token = it.trim() },
                label = { Text("OAuth access token (NOT API key)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth())
            Text(if (store.vault.read().isNotBlank()) "Encrypted token saved on device" else "No token saved",
                color = if (store.vault.read().isNotBlank()) Color(0xFF65E2B3) else Muted, fontSize = 12.sp)
            Button(onClick = {
                store.updateConfig(CloudConfig(cloudProject, region, bucket))
                if (token.isNotBlank()) { store.vault.save(token); token = "" }
                status = "Settings saved on this device"
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Connection") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onConnectionTest) { Text("Check Setup") }
                OutlinedButton(onClick = { store.vault.clear(); status = "Token removed" }) {
                    Text("Clear Token")
                }
            }
            if (status.isNotBlank()) Text(status, color = Sky, fontSize = 12.sp)
            Text("This setup check validates format only, not billing or model access. " +
                "For approved token creation use your trusted Cloud SDK: gcloud auth print-access-token.",
                color = Muted, fontSize = 12.sp)
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
            Text("MVG · 0.1.0 — Early personal preview", fontWeight = FontWeight.Bold)
            Text("Projects, prompt generator, optional Vertex API call, AI Director, local avatar photos " +
                "and scene reorder are included. Full Google OAuth sign-in, automatic pricing, " +
                "background job service and final MP4 scene export are not yet shipped.",
                color = Muted, fontSize = 12.sp)
        }
    }
    if (showRemove.isNotBlank()) AlertDialog(
        onDismissRequest = { showRemove = "" }, title = { Text("Delete character photo?") },
        text = { Text("This removes the saved reference photo from this device.") },
        confirmButton = { TextButton(onClick = {
            store.removeAvatar(showRemove); showRemove = ""
        }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { showRemove = "" }) { Text("Cancel") } }
    )
}
