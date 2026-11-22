package com.dslrmasterai.app

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

private val Night = Color(0xFF090B12)
private val Panel = Color(0xFF151A27)
private val Purple = Color(0xFF8B5CF6)
private val Cyan = Color(0xFF35D6E8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DSLRMasterApp() }
    }
}

private enum class Page { HOME, CAMERA, EDITOR, AI, GALLERY }

@Composable
fun DSLRMasterApp() {
    var page by remember { mutableStateOf(Page.HOME) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    var notice by remember { mutableStateOf("") }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { selectedBitmap = BitmapFactory.decodeStream(it) }
            if (selectedBitmap != null) page = Page.EDITOR
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Night, surface = Panel, primary = Purple, secondary = Cyan)) {
        Scaffold(containerColor = Night, bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                listOf(Page.HOME, Page.CAMERA, Page.EDITOR, Page.AI, Page.GALLERY).forEach { p ->
                    val icon = when (p) { Page.HOME -> Icons.Default.Home; Page.CAMERA -> Icons.Default.CameraAlt; Page.EDITOR -> Icons.Default.Edit; Page.AI -> Icons.Default.AutoAwesome; Page.GALLERY -> Icons.Default.PhotoLibrary }
                    NavigationBarItem(selected = page == p, onClick = { if (p == Page.EDITOR && selectedBitmap == null) picker.launch("image/*") else page = p }, icon = { Icon(icon, null) }, label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Cyan, selectedTextColor = Cyan, indicatorColor = Purple.copy(alpha = .2f), unselectedIconColor = Color.Gray, unselectedTextColor = Color.Gray))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    Page.HOME -> HomeScreen(onCamera = { page = Page.CAMERA }, onEdit = { picker.launch("image/*") }, onAI = { page = Page.AI }, onGallery = { page = Page.GALLERY })
                    Page.CAMERA -> CameraScreen(onBack = { page = Page.HOME }, onMessage = { notice = it })
                    Page.EDITOR -> EditorScreen(bitmap = selectedBitmap, brightness = brightness, contrast = contrast, onBrightness = { brightness = it }, onContrast = { contrast = it }, onPick = { picker.launch("image/*") }, onSave = { saveEdited(context, selectedBitmap, brightness, contrast); notice = "ছবি Gallery-তে সেভ করার চেষ্টা করা হয়েছে" })
                    Page.AI -> AIStudioScreen(onPick = { picker.launch("image/*") })
                    Page.GALLERY -> GalleryScreen(onPick = { picker.launch("image/*") })
                }
            }
            if (notice.isNotBlank()) { LaunchedEffect(notice) { kotlinx.coroutines.delay(2200); notice = "" }; SnackbarHostState().also { /* transient status is intentionally non-blocking */ } }
        }
    }
}

@Composable
private fun Header(title: String, subtitle: String? = null) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
        if (subtitle != null) Text(subtitle, color = Color.LightGray, fontSize = 13.sp)
    }
}

@Composable
private fun HomeScreen(onCamera: () -> Unit, onEdit: () -> Unit, onAI: () -> Unit, onGallery: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Purple, Cyan))), contentAlignment = Alignment.Center) { Icon(Icons.Default.Camera, null, tint = Color.White, modifier = Modifier.size(27.dp)) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("DSLR MASTER", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, letterSpacing = 1.sp); Text("AI PHOTO STUDIO", color = Cyan, fontSize = 10.sp, letterSpacing = 2.sp) }
            Icon(Icons.Default.Settings, "Settings", tint = Color.White.copy(.8f))
        }
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(26.dp)).background(Brush.linearGradient(listOf(Color(0xFF29204D), Color(0xFF102C3B), Color(0xFF151A27)))).padding(20.dp)) {
                Column(Modifier.align(Alignment.CenterStart)) {
                    Text("CREATE.\nCAPTURE.\nTRANSFORM.", color = Color.White, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(8.dp)); Text("Your creative studio, powered by AI", color = Color(0xFFD7D5E8), fontSize = 12.sp)
                }
                Icon(Icons.Default.AutoAwesome, null, tint = Cyan.copy(.8f), modifier = Modifier.align(Alignment.TopEnd).size(48.dp))
            }
            Spacer(Modifier.height(22.dp)); Text("YOUR CREATIVE TOOLS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToolCard("DSLR Camera", "Manual-style capture", Icons.Default.CameraAlt, Purple, Modifier.weight(1f), onCamera)
                ToolCard("Photo Editor", "Tune your photos", Icons.Default.Tune, Cyan, Modifier.weight(1f), onEdit)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ToolCard("AI Studio", "Smart photo tools", Icons.Default.AutoAwesome, Color(0xFFFFB454), Modifier.weight(1f), onAI)
                ToolCard("My Gallery", "Your creations", Icons.Default.Collections, Color(0xFF5ED6A5), Modifier.weight(1f), onGallery)
            }
            Spacer(Modifier.height(20.dp)); Text("PRO CREATION SUITE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(Modifier.height(9.dp)); Text("Advanced AI tools and pro camera controls will be connected in the next development stages.", color = Color.LightGray, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun ToolCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(118.dp).clip(RoundedCornerShape(20.dp)).background(Panel).border(1.dp, Color.White.copy(.06f), RoundedCornerShape(20.dp)).clickable { onClick() }.padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(27.dp)); Column { Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp); Text(sub, color = Color.Gray, fontSize = 10.sp) }
    }
}

@Composable
private fun CameraScreen(onBack: () -> Unit, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    LaunchedEffect(Unit) { permission.launch(android.Manifest.permission.CAMERA) }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) }; Text("DSLR CAMERA", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (hasPermission) {
                AndroidView(factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)
                    providerFuture.addListener({
                        val provider = providerFuture.get(); val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                        val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build(); imageCapture = capture
                        try { provider.unbindAll(); provider.bindToLifecycle(context as ComponentActivity, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture) } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx)); previewView
                }, modifier = Modifier.fillMaxSize())
            } else Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) { Text("Camera permission is needed", color = Color.White); Button(onClick = { permission.launch(android.Manifest.permission.CAMERA) }) { Text("Allow Camera") } }
        }
        Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            Text("AUTO", color = Cyan, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
                val capture = imageCapture
                if (capture == null) onMessage("ক্যামেরা প্রস্তুত হচ্ছে") else {
                    val name = "DSLR_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())}.jpg"
                    val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DSLRMasterAI") }
                    val output = ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
                    capture.takePicture(output, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback { override fun onImageSaved(result: ImageCapture.OutputFileResults) { onMessage("ছবি সেভ হয়েছে") }; override fun onError(exception: ImageCaptureException) { onMessage("ছবি তোলা যায়নি: ${exception.message}") } })
                }
            }, modifier = Modifier.size(76.dp).border(4.dp, Color.White, CircleShape).padding(7.dp).background(Color.White, CircleShape)) { }
            Text("PHOTO", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EditorScreen(bitmap: Bitmap?, brightness: Float, contrast: Float, onBrightness: (Float) -> Unit, onContrast: (Float) -> Unit, onPick: () -> Unit, onSave: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Header("Photo Editor", "Adjust your image with simple controls")
        if (bitmap == null) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Panel), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AddPhotoAlternate, null, tint = Cyan, modifier = Modifier.size(50.dp)); Text("Choose a photo to start", color = Color.White); Button(onClick = onPick) { Text("Select Photo") } } }
        } else {
            androidx.compose.foundation.Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Fit)
            Spacer(Modifier.height(14.dp)); Text("Brightness  ${(brightness * 100).toInt()}%", color = Color.White); Slider(value = brightness, onValueChange = onBrightness, valueRange = 0.3f..1.8f)
            Text("Contrast  ${(contrast * 100).toInt()}%", color = Color.White); Slider(value = contrast, onValueChange = onContrast, valueRange = 0.5f..1.8f)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onPick, modifier = Modifier.weight(1f)) { Text("Change Photo") }; Button(onClick = onSave, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.width(6.dp)); Text("Save") } }
        }
    }
}

@Composable
private fun AIStudioScreen(onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Header("AI Studio", "Intelligent tools for your best photos")
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Color(0xFF34235C), Color(0xFF153C4A)))).padding(18.dp)) { Column { Icon(Icons.Default.AutoAwesome, null, tint = Cyan, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(8.dp)); Text("MAKE EVERY PHOTO\nLOOK EXTRAORDINARY", color = Color.White, fontWeight = FontWeight.Black, fontSize = 21.sp); Text("AI services will be connected after choosing a provider and API.", color = Color.LightGray, fontSize = 12.sp) } }
        Spacer(Modifier.height(18.dp)); val tools = listOf("AI Photo Enhance" to Icons.Default.Hd, "Background Remover" to Icons.Default.LayersClear, "Object Eraser" to Icons.Default.AutoFixHigh, "Portrait Retouch" to Icons.Default.FaceRetouchingNatural, "AI Upscaler" to Icons.Default.ZoomIn, "Background Changer" to Icons.Default.Wallpaper)
        LazyVerticalGrid(columns = GridCells.Fixed(2), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(tools) { (name, icon) -> Column(Modifier.clip(RoundedCornerShape(18.dp)).background(Panel).clickable { onPick() }.padding(16.dp).height(88.dp), verticalArrangement = Arrangement.SpaceBetween) { Icon(icon, null, tint = Purple, modifier = Modifier.size(26.dp)); Text(name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) } } }
    }
}

@Composable
private fun GalleryScreen(onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) { Header("My Gallery", "Your edited and captured photos")
        Box(Modifier.fillMaxSize().clip(RoundedCornerShape(22.dp)).background(Panel), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.PhotoLibrary, null, tint = Cyan, modifier = Modifier.size(52.dp)); Spacer(Modifier.height(10.dp)); Text("Your creations will appear here", color = Color.White); Text("Open a photo from your device to edit", color = Color.Gray, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); Button(onClick = onPick) { Text("Choose from device") } } }
    }
}

private fun saveEdited(context: android.content.Context, source: Bitmap?, brightness: Float, contrast: Float) {
    if (source == null) return
    val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    val matrix = android.graphics.ColorMatrix(floatArrayOf(contrast,0f,0f,0f,128f*(1-contrast)+(brightness-1)*60, 0f,contrast,0f,0f,128f*(1-contrast)+(brightness-1)*60, 0f,0f,contrast,0f,128f*(1-contrast)+(brightness-1)*60, 0f,0f,0f,1f,0f))
    val paint = android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(matrix) }
    canvas.drawBitmap(source, 0f, 0f, paint)
    val name = "DSLR_EDIT_${System.currentTimeMillis()}.jpg"
    val values = ContentValues().apply { put(MediaStore.MediaColumns.DISPLAY_NAME, name); put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg"); put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DSLRMasterAI") }
    val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
    context.contentResolver.openOutputStream(uri)?.use { output.compress(Bitmap.CompressFormat.JPEG, 95, it) }
}
