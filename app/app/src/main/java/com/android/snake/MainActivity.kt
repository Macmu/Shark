package com.android.snake

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.itos.coui.theme.CouiTheme
import com.itos.coui.theme.LocalCouiContentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import roro.stellar.Stellar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val STAGES = listOf(
    "检查设备与授权",
    "加载漏洞利用",
    "注入内核模块",
    "挂载与隐藏",
    "收尾"
)

private data class Contributor(val name: String, val role: String)

/** 全部署名均取自仓库的 README 与 NOTICE，没有虚构条目。 */
private val CONTRIBUTORS = listOf(
    Contributor("Yunnijian", "Snake / 原 Shark 作者"),
    Contributor("AzirX1337", "CVE-2026-98260 公开实现"),
    Contributor("YuKongA", "漏洞研究与披露"),
    Contributor("RikkaApps", "Shizuku API"),
    Contributor("Muntashir Akon", "libadb-android"),
    Contributor("inforcqb", "SUSFS LKM"),
    Contributor("SukiSU Ultra", "内核管理器"),
    Contributor("ItosEO", "COUI-Kit")
)

private enum class Page { HOME, COMMANDS, EXTRAS, ABOUT, MANUAL }

private sealed interface CmdLine {
    data class Input(val text: String) : CmdLine
    data class Output(val text: String) : CmdLine
}

class MainActivity : ComponentActivity() {

    /* ===== 运行状态 ===== */
    private val logs = mutableStateListOf<String>()
    private val cmdLines = mutableStateListOf<CmdLine>()
    private val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    private var page by mutableStateOf(Page.HOME)
    private var busy by mutableStateOf(false)
    private var authorized by mutableStateOf(false)
    private var binderReady by mutableStateOf(false)
    private var ksuActive by mutableStateOf(false)
    private var exploit by mutableStateOf(ExploitKind.UNSUPPORTED)
    private var manager by mutableStateOf(ManagerKind.KERNELSU)
    private var kernelVersion by mutableStateOf("未知")
    private var deviceModel by mutableStateOf("未知")
    private var selinuxText by mutableStateOf("未知")
    private var managerPickerOpen by mutableStateOf(false)

    /* ===== 进度 ===== */
    private var stageIndex by mutableStateOf(0)
    private var elapsedMs by mutableStateOf(0L)
    private var totalMs by mutableStateOf<Long?>(null)
    private var finishedOk by mutableStateOf<Boolean?>(null)
    private var ticker: Job? = null

    /* ===== 扩展 ===== */
    private var antiRootGuard by mutableStateOf(true)
    private var softRebootAfter by mutableStateOf(false)
    private var unloadGuard by mutableStateOf(true)
    private var susfsSupport by mutableStateOf(false)
    private var vfsSupport by mutableStateOf(false)
    private var universalLoaded by mutableStateOf(false)
    private var hotManager by mutableStateOf(ManagerKind.KERNELSU)
    private var hotSwitchBusy by mutableStateOf(false)
    private var hotSwitchError by mutableStateOf<String?>(null)

    /* ===== 命令页 ===== */
    private var cmdInput by mutableStateOf("")
    private var cmdRunning by mutableStateOf(false)

    /* ===== 常用命令 ===== */
    private var savedCmds by mutableStateOf<List<SavedCmd>>(emptyList())
    private var cmdSheetOpen by mutableStateOf(false)
    /** 正在编辑的条目；非空即弹出编辑对话框。 */
    private var cmdEditing by mutableStateOf<SavedCmd?>(null)
    private var cmdEditingNew by mutableStateOf(false)
    /** 需要二次确认才能执行的条目。 */
    private var cmdPending by mutableStateOf<SavedCmd?>(null)

    /* ===== 对话框 ===== */
    private var showDroid by mutableStateOf(false)
    private var droidLoading by mutableStateOf(false)
    private var droidRelease by mutableStateOf<DroidspacesRelease?>(null)
    private var droidDownloading by mutableStateOf(false)
    private var droidProgress by mutableStateOf(0f)
    private var droidError by mutableStateOf<String?>(null)
    private var droidSavedPath by mutableStateOf<String?>(null)

    /* ===== 授权后端监听 ===== */
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { code, result ->
        if (code == REQ_SHIZUKU) onPermissionResult(result == PackageManager.PERMISSION_GRANTED)
    }
    private val stellarPermissionListener = Stellar.OnRequestPermissionResultListener { code, allowed, _ ->
        if (code == REQ_SHIZUKU) onPermissionResult(allowed)
    }
    private val binderListener = Shizuku.OnBinderReceivedListener {
        binderReady = PrivService.pick() != null
        refreshAuth()
        refreshKsuAsync()
    }
    private val stellarBinderListener = Stellar.OnBinderReceivedListener {
        binderReady = PrivService.pick() != null
        refreshAuth()
        refreshKsuAsync()
    }

    private fun onPermissionResult(ok: Boolean) {
        authorized = ok
        addLog(if (ok) "${PrivService.pick()?.label ?: "Backend"} authorized" else "permission denied")
        if (ok) refreshKsuAsync()
    }

    /* ===== 生命周期 ===== */

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(permissionListener)
        Shizuku.addBinderReceivedListenerSticky(binderListener)
        Stellar.addRequestPermissionResultListener(stellarPermissionListener)
        Stellar.addBinderReceivedListenerSticky(stellarBinderListener)

        binderReady = PrivService.pick() != null
        refreshAuth()
        // 已配对过的设备可静默重连无线调试，无需再次输入配对码。
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { AdbAuth.connect(applicationContext) }
            withContext(Dispatchers.Main) {
                binderReady = PrivService.pick() != null
                refreshAuth()
            }
        }

        val prefs = getSharedPreferences("pyb110", MODE_PRIVATE)
        antiRootGuard = prefs.getBoolean("antiroot_guard", true)
        softRebootAfter = prefs.getBoolean("soft_reboot_after", false)
        unloadGuard = prefs.getBoolean("unload_guard", true)
        susfsSupport = prefs.getBoolean("susfs_support", false)
        vfsSupport = prefs.getBoolean("vfs_support", false)
        manager = runCatching { ManagerKind.valueOf(prefs.getString("manager", "") ?: "") }
            .getOrDefault(ManagerKind.KERNELSU)
        savedCmds = CmdStore.load(this)
        kernelVersion = runCatching { android.system.Os.uname().release }.getOrDefault("未知")
        exploit = PROFILE_BY_RELEASE[kernelVersion] ?: ExploitKind.UNSUPPORTED
        if (manager == ManagerKind.DYNAMIC && !supportsUniversal()) manager = ManagerKind.KERNELSU
        hotManager = if (manager == ManagerKind.DYNAMIC) ManagerKind.KERNELSU else manager
        deviceModel = resolveDeviceName()
        refreshStaticStateAsync()
        refreshKsuAsync()

        setContent {
            CouiTheme(darkTheme = androidx.compose.foundation.isSystemInDarkTheme()) {
                Root()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binderReady = PrivService.pick() != null
        refreshAuth()
        // 配对常在通知栏完成，App 未必收到回调；回到前台时若已就绪就主动收起
        // 配对输入通知，避免它一直挂在通知栏。
        if (binderReady && authorized) {
            runCatching { AdbAuth.dismissPairingNotification(applicationContext) }
        }
    }

    override fun onDestroy() {
        ticker?.cancel()
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        Shizuku.removeBinderReceivedListener(binderListener)
        Stellar.removeRequestPermissionResultListener(stellarPermissionListener)
        Stellar.removeBinderReceivedListener(stellarBinderListener)
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_ADB_PERM) return
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBuiltinAuth()
        } else {
            addLog("本地网络/通知权限被拒绝，无法发现无线调试服务")
        }
    }

    /* ============================ UI ============================ */

    @Composable
    private fun Root() {
        val dark = CouiTheme.isDark
        val scheme = CouiTheme.colorScheme
        val background = if (dark) Color(0xFF101114) else scheme.background
        val ink = if (dark) Color(0xFFF2F3F5) else scheme.onSurface
        val dim = if (dark) Color(0xFF8B9098) else scheme.onSurfaceVariant
        val accent = if (dark) Color(0xFF7FB4FF) else scheme.accent
        val line = if (dark) Color(0xFF23262B) else ink.copy(alpha = 0.08f)
        val surface = if (dark) Color(0xFF1B1D22) else Color(0xFFF2F3F6)

        val palette = Palette(background, surface, ink, dim, accent, line, dark)

        BackHandler(enabled = page != Page.HOME || cmdSheetOpen) {
            when {
                cmdSheetOpen -> cmdSheetOpen = false
                else -> page = Page.HOME
            }
        }
        // 离开命令页时把抽屉收起，避免下次进来浮在上面。
        LaunchedEffect(page) {
            if (page != Page.COMMANDS) cmdSheetOpen = false
        }

        Box(Modifier.fillMaxSize().background(palette.background)) {
            // 页面切换：轻微横向位移 + 淡入，退出的页面只淡出，避免来回抢视线。
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val dir = if (targetState == Page.HOME) -1 else 1
                    (fadeIn(tween(220)) + slideInHorizontally(
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) { it / 24 * dir }) togetherWith fadeOut(tween(140))
                },
                label = "page"
            ) { current ->
                if (current == Page.HOME) {
                    HomePage(palette)
                } else {
                    Column(Modifier.fillMaxSize()) {
                        SubPageHeader(palette, current)
                        when (current) {
                            Page.COMMANDS -> CommandPage(palette)
                            Page.EXTRAS -> ExtrasPage(palette)
                            Page.ABOUT -> AboutPage(palette)
                            Page.MANUAL -> ManualPage(palette)
                            Page.HOME -> Unit
                        }
                    }
                }
            }

            // 抽屉和页面内容同一个 Box，保证遮罩一定盖在内容之上。
            CmdSheetHost(palette)
        }

        cmdEditing?.let { target ->
            CmdEditorDialog(palette, target, cmdEditingNew)
        }
        cmdPending?.let { target ->
            CmdConfirmDialog(palette, target)
        }

        if (managerPickerOpen) ManagerPickerDialog(palette) { managerPickerOpen = false }
        if (showDroid) DroidspacesDialog(palette)
    }

    @Composable
    private fun SubPageHeader(palette: Palette, current: Page) {
        val title = when (current) {
            Page.COMMANDS -> "命令"
            Page.EXTRAS -> "扩展"
            Page.ABOUT -> "关于"
            Page.MANUAL -> "使用手册"
            Page.HOME -> ""
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Txt(title, 22, palette.ink, FontWeight.Bold, modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { page = Page.HOME },
                contentAlignment = Alignment.Center
            ) {
                Glyph(Icons.Filled.Close, null, Modifier.size(19.dp), palette.dim)
            }
        }
    }

    /* -------- 首页 -------- */

    @Composable
    private fun HomePage(palette: Palette) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp)
        ) {
            Spacer(Modifier.height(14.dp))
            HeaderRow(palette)
            Spacer(Modifier.height(16.dp))
            DeviceBlock(palette)
            Spacer(Modifier.height(14.dp))
            ProgressBlock(palette)
            Spacer(Modifier.height(12.dp))
            OutputBlock(palette, Modifier.weight(1f))
            ActionRow(palette)
        }
    }

    @Composable
    private fun HeaderRow(palette: Palette) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Txt("Snake", 30, palette.ink, FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Txt(
                    if (exploit == ExploitKind.UNSUPPORTED) "当前内核不受支持 · $kernelVersion"
                    else exploit.fullLabel,
                    11.5f,
                    palette.dim,
                    maxLines = 1
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.ink.copy(alpha = 0.05f))
                    .clickable { page = Page.ABOUT },
                contentAlignment = Alignment.Center
            ) {
                Glyph(
                    Icons.Outlined.Info,
                    "关于",
                    Modifier.size(21.dp),
                    palette.ink.copy(alpha = 0.72f)
                )
            }
        }
    }

    @Composable
    private fun DeviceBlock(palette: Palette) {
        Column {
            InfoRow("设备", deviceModel, palette)
            InfoRow("内核", shortKernel(kernelVersion), palette)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { managerPickerOpen = true }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt("管理器", 13, palette.dim, modifier = Modifier.width(64.dp))
                Txt(manager.fullLabel, 13, palette.ink, FontWeight.Medium, modifier = Modifier.weight(1f))
                Txt("切换", 12, palette.accent)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
        }
    }

    @Composable
    private fun ManagerPickerDialog(palette: Palette, onDismiss: () -> Unit) {
        SnakeDialog(palette, "选择 ROOT 管理器", onDismiss) {
            managerChoices().forEach { kind ->
                val selected = kind == manager
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            selectManager(kind)
                            onDismiss()
                        }
                        .padding(horizontal = 10.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Txt(
                        kind.fullLabel,
                        14,
                        if (selected) palette.accent else palette.ink,
                        if (selected) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) Txt("当前", 11.5f, palette.accent)
                }
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String, palette: Palette) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt(label, 13, palette.dim, modifier = Modifier.width(64.dp))
                Txt(
                    value,
                    13,
                    palette.ink,
                    FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
        }
    }

    @Composable
    private fun ProgressBlock(palette: Palette) {
        val progress = when {
            totalMs != null -> 1f
            busy -> (stageIndex + 1f) / (STAGES.size + 1f)
            else -> 0f
        }
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Txt(
                    when {
                        totalMs != null && finishedOk == true -> "已完成"
                        totalMs != null -> "未完成"
                        busy -> STAGES[stageIndex.coerceIn(0, STAGES.lastIndex)]
                        binderReady && authorized -> "待命"
                        else -> "未授权"
                    },
                    15,
                    palette.ink,
                    FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Txt(formatDuration(totalMs ?: elapsedMs), 12, palette.dim, mono = true)
            }
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(palette.line)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(progress.coerceIn(0.001f, 1f))
                        .fillMaxHeight()
                        .background(if (finishedOk == false) Color(0xFFE5484D) else palette.accent)
                )
            }
            Spacer(Modifier.height(10.dp))
            StageList(palette)
        }
    }

    @Composable
    private fun StageList(palette: Palette) {
        val current = if (totalMs != null) STAGES.size else stageIndex
        // 只露三行，窗口跟随当前环节下移，形成滚动感。
        val windowStart = (current - 1).coerceIn(0, (STAGES.size - 3).coerceAtLeast(0))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            STAGES.drop(windowStart).take(3).forEach { label ->
                val index = STAGES.indexOf(label)
                val state = when {
                    totalMs != null -> 2
                    index < current -> 2
                    index == current && busy -> 1
                    else -> 0
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(if (state == 1) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    1 -> palette.accent
                                    2 -> palette.dim.copy(alpha = 0.55f)
                                    else -> palette.line
                                }
                            )
                    )
                    Spacer(Modifier.width(9.dp))
                    Txt(
                        label,
                        12,
                        when (state) {
                            1 -> palette.accent
                            2 -> palette.dim
                            else -> palette.dim.copy(alpha = 0.5f)
                        },
                        if (state == 1) FontWeight.Medium else FontWeight.Normal
                    )
                }
            }
        }
    }

    @Composable
    private fun OutputBlock(palette: Palette, modifier: Modifier = Modifier) {
        val state = rememberLazyListState()
        val snapshot = logs.toList()
        LaunchedEffect(snapshot.size) {
            if (snapshot.isNotEmpty()) state.scrollToItem(snapshot.size - 1)
        }
        Box(
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(palette.surface)
        ) {
            if (snapshot.isEmpty()) {
                Box(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Txt(
                        if (binderReady && authorized) "已就绪，点右下角开始越狱。"
                        else "尚未授权。点右下角「一键授权」，按提示输入无线调试的六位配对码。",
                        11.5f,
                        palette.dim,
                        mono = true,
                        align = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(snapshot) { line ->
                        Txt(line, 10.5f, palette.dim, mono = true)
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionRow(palette: Palette) {
        val ready = binderReady && authorized
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GhostAction("扩展", Icons.Filled.Extension, palette) { page = Page.EXTRAS }
            GhostAction("命令", Icons.Filled.Terminal, palette) { page = Page.COMMANDS }
            val mainInteraction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1.2f)
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (busy) palette.accent.copy(alpha = 0.45f) else palette.accent)
                    .pressScale(mainInteraction)
                    .clickable(
                        interactionSource = mainInteraction,
                        indication = LocalIndication.current,
                        enabled = !busy
                    ) { onAuthAction() },
                contentAlignment = Alignment.Center
            ) {
                Txt(
                    when {
                        busy -> "运行中"
                        !ready -> "一键授权"
                        else -> "一键越狱"
                    },
                    14,
                    Color.White,
                    FontWeight.Bold
                )
            }
        }
    }

    @Composable
    private fun GhostAction(
        label: String,
        icon: ImageVector,
        palette: Palette,
        onClick: () -> Unit
    ) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.ink.copy(alpha = 0.05f))
                .pressScale(interaction)
                .clickable(interactionSource = interaction, indication = LocalIndication.current) { onClick() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(icon, null, Modifier.size(17.dp), palette.ink.copy(alpha = 0.8f))
            Spacer(Modifier.width(5.dp))
            Txt(label, 13, palette.ink, FontWeight.Medium)
        }
    }

    /* -------- 命令页 -------- */

    @Composable
    private fun CommandPage(palette: Palette) {
        val state = rememberLazyListState()
        val snapshot = cmdLines.toList()
        LaunchedEffect(snapshot.size) {
            if (snapshot.isNotEmpty()) state.scrollToItem(snapshot.size - 1)
        }
        val ready = binderReady && authorized
        val focusRequester = remember { FocusRequester() }
        val keyboard = LocalSoftwareKeyboardController.current
        // 输入条固定在顶部：键盘弹起时只有输出区收缩，输入框位置不动，
        // 不会再出现「一打字上面就全被顶没」的情况。
        Column(Modifier.fillMaxSize().imePadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (ready) Color(0xFF36D167) else palette.dim)
                )
                Spacer(Modifier.width(6.dp))
                Txt(
                    if (ready) "root" else "shell",
                    11,
                    if (ready) Color(0xFF36D167) else palette.dim,
                    mono = true
                )
                Spacer(Modifier.weight(1f))
                if (snapshot.isNotEmpty()) {
                    Txt("清空", 12, palette.dim, modifier = Modifier.clickable { cmdLines.clear() })
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(palette.surface)
                    .padding(start = 14.dp, end = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = cmdInput,
                    onValueChange = { cmdInput = it },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .focusRequester(focusRequester)
                        // 外接键盘 / 遥控器的物理回车也要能执行，不能只依赖软键盘的「发送」。
                        .onPreviewKeyEvent { event ->
                            if (event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                runCommand()
                                true
                            } else {
                                false
                            }
                        },
                    singleLine = true,
                    enabled = !cmdRunning,
                    cursorBrush = SolidColor(palette.accent),
                    textStyle = TextStyle(
                        color = palette.ink,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { runCommand() }),
                    decorationBox = { inner ->
                        // # 号放进装饰层：整块输入区（含 # 号）都归 TextField 管，
                        // 点在哪里都能聚焦并自动弹出键盘。
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Txt("#", 15, palette.accent, FontWeight.Bold, mono = true)
                            Spacer(Modifier.width(9.dp))
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (cmdInput.isEmpty()) {
                                    Txt("输入命令", 13, palette.dim.copy(alpha = 0.7f), mono = true)
                                }
                                inner()
                            }
                        }
                    }
                )
                // 存为常用：只在输入框有内容时出现，点开就是编辑对话框（新建）。
                AnimatedVisibility(
                    visible = cmdInput.isNotBlank() && !cmdRunning,
                    enter = fadeIn(tween(140)),
                    exit = fadeOut(tween(120))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(palette.accent.copy(alpha = 0.13f))
                            .clickable {
                                val text = cmdInput.trim()
                                cmdEditingNew = true
                                cmdEditing = SavedCmd(
                                    id = 0L,
                                    nick = CmdStore.defaultNick(text),
                                    cmd = text,
                                    confirm = CmdStore.looksRisky(text)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Glyph(Icons.Outlined.BookmarkBorder, "存为常用", Modifier.size(17.dp), palette.accent)
                    }
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.accent.copy(alpha = if (cmdRunning) 0.4f else 1f))
                        .clickable(enabled = !cmdRunning) { runCommand() },
                    contentAlignment = Alignment.Center
                ) {
                    Txt(if (cmdRunning) "…" else "\u2197", 16, Color.White, FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.surface)
            ) {
                if (snapshot.isEmpty()) {
                    Box(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Txt("snake:/ #", 11.5f, palette.accent, mono = true)
                            Spacer(Modifier.height(6.dp))
                            Txt(
                                "输入命令后回车执行，需要 root 的指令会通过内置授权执行。",
                                11,
                                palette.dim,
                                mono = true,
                                align = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(snapshot) { line ->
                            when (line) {
                                is CmdLine.Input -> Txt(
                                    "snake:/ # ${line.text}",
                                    10.5f,
                                    palette.accent,
                                    mono = true
                                )
                                is CmdLine.Output -> Txt(line.text, 10.5f, palette.dim, mono = true)
                            }
                        }
                    }
                }
            }
            // 底栏只留一个入口：点开从底部升起常用命令抽屉。
            val entryInteraction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.ink.copy(alpha = 0.055f))
                        .pressScale(entryInteraction)
                        .clickable(interactionSource = entryInteraction, indication = LocalIndication.current) {
                            keyboard?.hide()
                            cmdSheetOpen = true
                        }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Glyph(Icons.Outlined.BookmarkBorder, null, Modifier.size(16.dp), palette.accent)
                    Spacer(Modifier.width(8.dp))
                    Txt("常用命令", 13, palette.ink, FontWeight.Medium)
                    Spacer(Modifier.weight(1f))
                    if (savedCmds.isNotEmpty()) {
                        Txt("${savedCmds.size}", 11.5f, palette.dim, mono = true)
                        Spacer(Modifier.width(4.dp))
                    }
                    Glyph(Icons.Filled.KeyboardArrowUp, null, Modifier.size(17.dp), palette.dim)
                }
            }
        }
    }

    /* -------- 常用命令抽屉 -------- */

    /**
     * 抽屉：点底栏入口从下方升起，点遮罩收起。
     * 用 Animatable 而不是 AnimatedVisibility，是因为遮罩只需要淡、抽屉只需要滑，
     * 两者动画曲线不同，塞进同一个 AnimatedVisibility 里会互相迁就。
     */
    @Composable
    private fun CmdSheetHost(palette: Palette) {
        val anim = remember { Animatable(0f) }
        LaunchedEffect(cmdSheetOpen) {
            if (cmdSheetOpen) {
                anim.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
            } else {
                anim.animateTo(0f, tween(200, easing = FastOutLinearInEasing))
            }
        }
        val progress = anim.value
        // 收起且动画结束时彻底不参与布局，否则会一直拦着下面的点击。
        if (!cmdSheetOpen && progress == 0f) return

        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.46f * progress))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { cmdSheetOpen = false }
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = (1f - progress) * size.height
                        alpha = progress
                    }
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(if (palette.dark) Color(0xFF17191E) else Color.White)
                    .navigationBarsPadding()
                    .padding(top = 8.dp, bottom = 12.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 10.dp)
                        .width(34.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.ink.copy(alpha = 0.16f))
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Txt("常用命令", 15, palette.ink, FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                cmdEditingNew = true
                                cmdEditing = SavedCmd(0L, "", "", false)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Txt("＋ 新建", 12.5f, palette.accent, FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(2.dp))
                if (savedCmds.isEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 26.dp)) {
                        Txt("还没有常用命令。", 13, palette.ink, FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Txt("在输入框里输命令，点右侧书签就能存下来。", 11.5f, palette.dim)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 392.dp)) {
                        items(savedCmds, key = { it.id }) { item ->
                            CmdSheetRow(palette, item)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Txt(
                        "点一行直接执行 · 右侧 ⋯ 可改名、置顶或删除",
                        10.5f,
                        palette.dim,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun CmdSheetRow(palette: Palette, item: SavedCmd) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { runSavedCmd(item) }
                .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Txt(item.nick, 14, palette.ink, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Txt(item.cmd, 11, palette.dim, mono = true, maxLines = 1)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.accent.copy(alpha = 0.13f))
                    .clickable { runSavedCmd(item) },
                contentAlignment = Alignment.Center
            ) {
                Glyph(Icons.Filled.PlayArrow, "执行", Modifier.size(15.dp), palette.accent)
            }
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable {
                        cmdEditingNew = false
                        cmdEditing = item
                    },
                contentAlignment = Alignment.Center
            ) {
                Glyph(Icons.Filled.MoreVert, "更多", Modifier.size(17.dp), palette.dim)
            }
        }
    }

    @Composable
    private fun CmdEditorDialog(palette: Palette, target: SavedCmd, isNew: Boolean) {
        var nick by remember(target.id, isNew) { mutableStateOf(target.nick) }
        var cmd by remember(target.id, isNew) { mutableStateOf(target.cmd) }
        var guarded by remember(target.id, isNew) { mutableStateOf(target.confirm) }

        SnakeDialog(palette, if (isNew) "存为常用命令" else "编辑常用命令", { cmdEditing = null }) {
            Txt("昵称", 11.5f, palette.dim)
            Spacer(Modifier.height(6.dp))
            CmdField(nick, "例如：清微信缓存", palette, mono = false) { nick = it }
            Spacer(Modifier.height(14.dp))
            Txt("命令", 11.5f, palette.dim)
            Spacer(Modifier.height(6.dp))
            CmdField(cmd, "例如：pm clear com.tencent.mm", palette, mono = true) { text ->
                cmd = text
                // 新建时跟着命令走：危险命令自动勾上确认，不用用户自己想起这件事。
                if (isNew && CmdStore.looksRisky(text)) guarded = true
            }
            Spacer(Modifier.height(8.dp))
            SwitchRow("执行前确认", guarded, palette) { guarded = it }
            if (!isNew) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Txt(
                        "置顶",
                        12.5f,
                        palette.dim,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                pinCommand(target.id)
                                cmdEditing = null
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                    Txt(
                        "删除",
                        12.5f,
                        Color(0xFFE5484D),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                deleteCommand(target.id)
                                cmdEditing = null
                            }
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("取消", false, palette, Modifier.weight(1f)) { cmdEditing = null }
                DialogButton("保存", true, palette, Modifier.weight(1f)) {
                    val clean = cmd.trim()
                    if (clean.isNotEmpty()) {
                        upsertCommand(target.copy(nick = nick, cmd = clean, confirm = guarded), isNew)
                        cmdEditing = null
                    }
                }
            }
        }
    }

    @Composable
    private fun CmdConfirmDialog(palette: Palette, target: SavedCmd) {
        SnakeDialog(palette, "确认执行", { cmdPending = null }) {
            Txt("这条命令被标记为执行前需要确认：", 11.5f, palette.dim)
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.ink.copy(alpha = 0.05f))
                    .padding(12.dp)
            ) {
                Txt(target.cmd, 11.5f, palette.ink, mono = true)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("取消", false, palette, Modifier.weight(1f)) { cmdPending = null }
                DialogButton("执行", true, palette, Modifier.weight(1f)) {
                    val text = target.cmd
                    cmdPending = null
                    runCommand(text)
                }
            }
        }
    }

    @Composable
    private fun CmdField(
        value: String,
        placeholder: String,
        palette: Palette,
        mono: Boolean,
        onChange: (String) -> Unit
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.ink.copy(alpha = 0.06f))
                .padding(horizontal = 13.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                cursorBrush = SolidColor(palette.accent),
                textStyle = TextStyle(
                    color = palette.ink,
                    fontSize = if (mono) 13.5.sp else 14.sp,
                    fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default
                ),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Txt(placeholder, 12.5f, palette.dim.copy(alpha = 0.7f), mono = mono)
                        }
                        inner()
                    }
                }
            )
        }
    }

    /* -------- 扩展页 -------- */

    @Composable
    private fun ExtrasPage(palette: Palette) {
        LaunchedEffect(Unit) {
            if (!supportsUniversal()) {
                universalLoaded = false
                return@LaunchedEffect
            }
            val (rc, text) = withContext(Dispatchers.IO) {
                runRootCommand("cat /proc/kernelsu_manager", 20)
            }
            universalLoaded = rc == 0 && text.isNotEmpty()
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            if (exploit != ExploitKind.UNSUPPORTED) {
                if (!isVivoV2505a()) {
                    SwitchRow("越狱后禁用安全弹窗", antiRootGuard, palette) { updateAntiRootGuard(it) }
                }
                SwitchRow("越狱后软重启", softRebootAfter, palette) { updateSoftRebootAfter(it) }
                if (!isVivoV2505a()) {
                    SwitchRow("越狱后卸载内核安全防护", unloadGuard, palette) { updateUnloadGuard(it) }
                }
            }
            if (supportsSusfs()) {
                SwitchRow("SUSFS 支持", susfsSupport, palette) { updateSusfsSupport(it) }
            }
            if (exploit == ExploitKind.CVE61821) {
                SwitchRow("VFS 支持", vfsSupport, palette) { updateVfsSupport(it) }
            }
            if (supportsUniversal() && universalLoaded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !hotSwitchBusy) {
                            val list = hotManagerChoices()
                            val next = list[(list.indexOf(hotManager) + 1) % list.size]
                            hotManager = next
                            switchHotManager(next)
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Txt("管理器热切换", 14, palette.ink)
                        if (hotSwitchBusy) {
                            Spacer(Modifier.height(2.dp))
                            Txt("正在切换…", 11.5f, palette.dim)
                        }
                    }
                    Txt(hotManager.fullLabel, 13, palette.accent, FontWeight.Medium)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
            }
            hotSwitchError?.let {
                Spacer(Modifier.height(8.dp))
                Txt(it, 11.5f, Color(0xFFE5484D), mono = true)
            }
            if (exploit == ExploitKind.CVE61821) {
                Spacer(Modifier.height(24.dp))
                Txt("DROIDSPACES", 10.5f, palette.dim, FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Txt(
                    "下载最新的 droidspaces_lkm 模块到 Download/Snake，之后到内核管理器里刷入。",
                    12,
                    palette.dim
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.ink.copy(alpha = 0.05f))
                        .clickable { openDroidspacesDialog() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Glyph(Icons.Filled.Download, null, Modifier.size(16.dp), palette.accent)
                    Spacer(Modifier.width(7.dp))
                    Txt("下载模块", 13, palette.ink, FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(24.dp))
            Txt("以上开关在下次越狱时生效。", 11.5f, palette.dim)
            Spacer(Modifier.height(20.dp))
        }
    }

    @Composable
    private fun SwitchRow(
        label: String,
        checked: Boolean,
        palette: Palette,
        onChange: (Boolean) -> Unit
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChange(!checked) }
                    .padding(vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt(label, 14, palette.ink, modifier = Modifier.weight(1f))
                SnakeSwitch(checked, palette) { onChange(it) }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
        }
    }

    @Composable
    private fun SnakeSwitch(checked: Boolean, palette: Palette, onChange: (Boolean) -> Unit) {
        val offset by animateDpAsState(if (checked) 18.dp else 0.dp, label = "knob")
        Box(
            modifier = Modifier
                .size(44.dp, 26.dp)
                .clip(CircleShape)
                .background(if (checked) palette.accent else palette.ink.copy(alpha = 0.14f))
                .clickable { onChange(!checked) }
                .padding(3.dp)
        ) {
            Box(
                Modifier
                    .offset(x = offset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }

    /* -------- 关于页 -------- */

    @Composable
    private fun AboutPage(palette: Palette) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(if (palette.dark) Color(0xFF24331E) else Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    // 前景图是 108dp 画布，图形只占中间约 66%，所以这里放大到 60dp，
                    // 尾巴才不会贴边被裁。
                    Image(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Txt("Snake", 20, palette.ink, FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Txt(
                        "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · 内置授权",
                        11.5f,
                        palette.dim
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Txt(
                "在设备上直接完成无线调试配对与内核提权，不需要额外安装 Shizuku。",
                12.5f,
                palette.dim
            )
            Spacer(Modifier.height(24.dp))
            Txt("致谢", 10.5f, palette.dim, FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            CONTRIBUTORS.forEach { contributor ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Txt(
                        contributor.name,
                        13.5f,
                        palette.ink,
                        FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Txt(contributor.role, 11.5f, palette.dim)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
            }
            Spacer(Modifier.height(16.dp))
            Txt(
                "感谢以上项目与作者公开的实现与文档。本项目的授权链路、界面组件与内核模块都建立在这些工作之上。",
                12,
                palette.dim
            )
            Spacer(Modifier.height(22.dp))
            LinkRow("使用手册", palette) { page = Page.MANUAL }
            Spacer(Modifier.height(24.dp))
            Txt("GPL-3.0 · 仅供学习研究", 11, palette.dim)
            Spacer(Modifier.height(24.dp))
        }
    }

    @Composable
    private fun LinkRow(label: String, palette: Palette, onClick: () -> Unit) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Txt(label, 14, palette.ink, modifier = Modifier.weight(1f))
                Txt("›", 16, palette.dim)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
        }
    }

    /* -------- 手册页 -------- */

    @Composable
    private fun ManualPage(palette: Palette) {
        val context = LocalContext.current
        val blocks = remember {
            val text = runCatching {
                context.assets.open("manual.md").bufferedReader().use { it.readText() }
            }.getOrElse { "# 使用手册\n\n手册内容读取失败。" }
            parseManualMarkdown(text)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            itemsIndexed(blocks) { _, block -> MdBlockView(block, palette) }
        }
    }

    @Composable
    private fun MdBlockView(block: MdBlock, palette: Palette) {
        when (block) {
            is MdBlock.Heading -> Txt(
                block.text,
                when (block.level) {
                    1 -> 20f
                    2 -> 16f
                    else -> 14f
                },
                palette.ink,
                FontWeight.Bold,
                modifier = Modifier.padding(top = if (block.level == 1) 6.dp else 12.dp)
            )
            is MdBlock.Paragraph -> MdRich(block.text, 13, palette.dim, palette)
            is MdBlock.ListItem -> Row {
                Txt(block.marker, 13, palette.dim, modifier = Modifier.width(18.dp))
                MdRich(block.text, 13, palette.dim, palette, Modifier.weight(1f))
            }
            is MdBlock.Callout -> Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.accent.copy(alpha = 0.08f))
                    .padding(10.dp)
            ) { MdRich(block.text, 12, palette.dim, palette) }
            is MdBlock.Code -> Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.surface)
                    .padding(10.dp)
            ) { Txt(block.text, 11, palette.dim, mono = true) }
            is MdBlock.Table -> Column {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    block.headers.forEach { header ->
                        Txt(header, 11.5f, palette.ink, FontWeight.Bold, modifier = Modifier.weight(1f))
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(palette.line))
                block.rows.forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        row.forEach { cell ->
                            Txt(cell, 11.5f, palette.dim, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    /* -------- 对话框 -------- */

    /** 手册正文需要 inline markdown（**加粗**、`代码`），BasicText 支持 AnnotatedString。 */
    @Composable
    private fun MdRich(
        text: String,
        size: Number,
        color: Color,
        palette: Palette,
        modifier: Modifier = Modifier
    ) {
        val px = size.toFloat()
        val annotated = remember(text) {
            markdownInline(text, palette.ink.copy(alpha = 0.10f))
        }
        BasicText(
            text = annotated,
            modifier = modifier,
            style = TextStyle(
                color = color,
                fontSize = px.sp,
                lineHeight = (px * 1.55f).sp
            )
        )
    }

    @Composable
    private fun SnakeDialog(
        palette: Palette,
        title: String,
        onDismiss: () -> Unit,
        content: @Composable () -> Unit
    ) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            // 对话框进场：轻微放大 + 淡入，避免「啪」地铺满屏幕。
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val scale by animateFloatAsState(
                targetValue = if (shown) 1f else 0.94f,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "dlgScale"
            )
            val alpha by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(150),
                label = "dlgAlpha"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.surface)
                    .padding(20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Txt(title, 16, palette.ink, FontWeight.Bold, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) { Glyph(Icons.Filled.Close, null, Modifier.size(16.dp), palette.dim) }
                    }
                    Spacer(Modifier.height(12.dp))
                    content()
                }
            }
        }
    }

    @Composable
    private fun DroidspacesDialog(palette: Palette) {
        SnakeDialog(palette, "Droidspaces 模块", { showDroid = false }) {
            val release = droidRelease
            when {
                droidLoading -> Txt("正在获取最新版本…", 12, palette.dim)
                droidError != null -> Txt(droidError ?: "", 12, Color(0xFFE5484D))
                release == null -> Txt("暂无可用版本。", 12, palette.dim)
                else -> Column {
                    Txt("版本 ${release.version}", 13, palette.ink, FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Txt(
                        formatSize(release.assetSize) + " · " + release.assetName,
                        11.5f,
                        palette.dim,
                        mono = true
                    )
                    if (release.changelog.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Txt(
                            release.changelog,
                            11.5f,
                            palette.dim,
                            modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
            if (droidDownloading) {
                Spacer(Modifier.height(12.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.line)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(droidProgress.coerceIn(0.001f, 1f))
                            .fillMaxHeight()
                            .background(palette.accent)
                    )
                }
            }
            droidSavedPath?.let {
                Spacer(Modifier.height(10.dp))
                Txt("已保存：$it", 11, Color(0xFF36D167), mono = true)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DialogButton("关闭", false, palette, Modifier.weight(1f)) { showDroid = false }
                DialogButton(
                    if (droidDownloading) "下载中" else "下载",
                    true,
                    palette,
                    Modifier.weight(1f)
                ) { if (!droidDownloading) downloadDroidspacesModule() }
            }
        }
    }

    @Composable
    private fun DialogButton(
        label: String,
        primary: Boolean,
        palette: Palette,
        modifier: Modifier = Modifier,
        onClick: () -> Unit
    ) {
        Box(
            modifier = modifier
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (primary) palette.accent else palette.ink.copy(alpha = 0.07f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Txt(label, 13.5f, if (primary) Color.White else palette.ink, FontWeight.Medium)
        }
    }

    /* ============================ 基础组件 ============================ */

    private data class Palette(
        val background: Color,
        val surface: Color,
        val ink: Color,
        val dim: Color,
        val accent: Color,
        val line: Color,
        val dark: Boolean
    )

    /**
     * 按下时轻微回弹。配合 ripple 用，手指按下的瞬间有个「压实」的反馈，
     * 比只有水波纹更跟手。
     */
    @Composable
    private fun Modifier.pressScale(
        interaction: MutableInteractionSource,
        pressedScale: Float = 0.96f
    ): Modifier {
        val pressed by interaction.collectIsPressedAsState()
        val scale by animateFloatAsState(
            targetValue = if (pressed) pressedScale else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "press"
        )
        return this.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    }

    @Composable
    private fun Txt(
        text: String,
        size: Number,
        color: Color,
        weight: FontWeight = FontWeight.Normal,
        modifier: Modifier = Modifier,
        mono: Boolean = false,
        align: TextAlign = TextAlign.Start,
        maxLines: Int = Int.MAX_VALUE
    ) {
        val px = size.toFloat()
        BasicText(
            text = text,
            modifier = modifier,
            style = TextStyle(
                color = color,
                fontSize = px.sp,
                fontWeight = weight,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                textAlign = align,
                lineHeight = (px * 1.45f).sp
            ),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }

    @Composable
    private fun Glyph(icon: ImageVector, label: String?, modifier: Modifier, tint: Color) {
        val color = tint.takeIf { it != Color.Unspecified }
            ?: LocalCouiContentColor.current.takeIf { it != Color.Unspecified }
            ?: CouiTheme.colorScheme.onSurface
        Image(
            imageVector = icon,
            contentDescription = label,
            modifier = modifier,
            colorFilter = ColorFilter.tint(color)
        )
    }

    /* ============================ 业务 ============================ */

    private fun onAuthAction() {
        if (busy) return
        when {
            !binderReady -> startBuiltinAuth()
            !authorized -> PrivService.request(REQ_SHIZUKU)
            else -> runFlow()
        }
    }

    private fun addLog(line: String) {
        Log.i("SNAKE", line)
        val stamped = "${timeFmt.format(Date())}  $line"
        runOnUiThread {
            logs.add(stamped)
            if (logs.size > 1200) logs.removeRange(0, logs.size - 1200)
        }
    }

    private fun refreshAuth() {
        binderReady = PrivService.pick() != null
        authorized = PrivService.granted()
    }

    private fun startBuiltinAuth() {
        if (!AdbAuth.hasPermissions(applicationContext)) {
            addLog("申请本地网络与通知权限…")
            requestPermissions(AdbAuth.requiredPermissions(), REQ_ADB_PERM)
            return
        }
        busy = true
        addLog("正在连接内置授权（无线调试）…")
        lifecycleScope.launch(Dispatchers.IO) {
            val outcome = AdbAuth.startPairingFlow(applicationContext)
            withContext(Dispatchers.Main) {
                busy = false
                outcome.fold(
                    onSuccess = { state ->
                        when (state) {
                            AdbAuth.StartState.CONNECTED -> {
                                refreshAuth()
                                addLog("内置授权已连接（无线调试）")
                            }
                            AdbAuth.StartState.PAIRING -> {
                                AdbAuth.lastError()?.let { addLog("连接未就绪：$it") }
                                addLog("请打开「无线调试 → 使用配对码配对设备」，保持弹窗开启，在通知栏输入六位配对码；完成后回到本页")
                            }
                            AdbAuth.StartState.NEED_PERMISSIONS -> {
                                addLog("申请本地网络与通知权限…")
                                requestPermissions(AdbAuth.requiredPermissions(), REQ_ADB_PERM)
                            }
                        }
                    },
                    onFailure = { t -> addLog("内置授权失败：${t.message ?: t.javaClass.simpleName}") }
                )
            }
        }
    }

    private fun refreshKsuAsync() {
        if (PrivService.pick() == null) return
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                val (active, se) = readDeviceState()
                withContext(Dispatchers.Main) {
                    ksuActive = active
                    selinuxText = se
                }
            }
        }
    }

    private fun readDeviceState(): Pair<Boolean, String> {
        val p = PrivService.newProcess(
            arrayOf(
                "sh", "-c",
                "if grep -q '^kernelsu .* Live ' /proc/modules; then echo YES; elif timeout 3 su -c id 2>/dev/null | grep -q uid=0; then echo YES; else echo NO; fi; getenforce"
            ),
            shizukuEnv(), "/"
        )
        val lines = p.getInputStream().bufferedReader().readLines()
        p.waitFor()
        // ADB shell 会话可能夹带回显或空行，不能假设状态在固定行号。
        val active = lines.any { it.trim().equals("YES", ignoreCase = true) }
        val se = lines.map { mapSelinux(it.trim()) }.firstOrNull { it != "未知" } ?: "未知"
        return active to se
    }

    private fun updateAntiRootGuard(value: Boolean) {
        antiRootGuard = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("antiroot_guard", value).apply()
    }

    private fun updateSoftRebootAfter(value: Boolean) {
        softRebootAfter = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("soft_reboot_after", value).apply()
    }

    private fun updateUnloadGuard(value: Boolean) {
        unloadGuard = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("unload_guard", value).apply()
    }

    private fun updateSusfsSupport(value: Boolean) {
        susfsSupport = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("susfs_support", value).apply()
    }

    private fun updateVfsSupport(value: Boolean) {
        vfsSupport = value
        getSharedPreferences("pyb110", MODE_PRIVATE).edit().putBoolean("vfs_support", value).apply()
    }

    private fun selectManager(kind: ManagerKind) {
        manager = kind
        if (kind == ManagerKind.DYNAMIC) hotManager = ManagerKind.KERNELSU
        getSharedPreferences("pyb110", MODE_PRIVATE).edit()
            .putString("manager", kind.name)
            .apply()
        addLog("ROOT 管理器：${kind.fullLabel}")
    }

    private fun runRootCommand(script: String, timeoutSeconds: Long = 8): Pair<Int, String> {
        var directFailure: Pair<Int, String>? = null
        runCatching {
            val p = ProcessBuilder("/system/bin/su", "-c", script).redirectErrorStream(true).start()
            if (p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                val out = p.inputStream.bufferedReader().readText().trim()
                val result = p.exitValue() to out
                if (result.first == 0) return result
                directFailure = result
            } else {
                p.destroyForcibly()
                directFailure = -2 to "su timeout"
            }
        }
        val fallback = runCatching {
            val p = PrivService.newProcess(arrayOf("su", "-c", script), shizukuEnv(), "/")
            val out = p.getInputStream().bufferedReader().readText().trim()
            p.waitFor() to out
        }.getOrElse { -1 to (it.message ?: "root command failed") }
        return if (fallback.first == 0) fallback else (directFailure ?: fallback)
    }

    private fun switchHotManager(kind: ManagerKind) {
        if (hotSwitchBusy || kind == ManagerKind.DYNAMIC) return
        hotSwitchBusy = true
        hotSwitchError = null
        lifecycleScope.launch(Dispatchers.IO) {
            val pkg = kind.pkg
            val appInfo = runCatching {
                packageManager.getPackageInfo(pkg, 0).applicationInfo
            }.getOrNull()
            if (appInfo == null) {
                withContext(Dispatchers.Main) {
                    hotSwitchBusy = false
                    hotSwitchError = "${kind.fullLabel} 未安装"
                }
                return@launch
            }
            val script =
                "printf '%s %u %s' '$pkg' ${appInfo.uid} '${appInfo.sourceDir}' > /proc/kernelsu_manager"
            val (rc, out) = runRootCommand(script, 20)
            if (rc == 0) runRootCommand("am force-stop '$pkg'", 10)
            withContext(Dispatchers.Main) {
                hotSwitchBusy = false
                if (rc == 0) {
                    hotSwitchError = null
                    addLog("已切换到 ${kind.fullLabel}")
                    packageManager.getLaunchIntentForPackage(pkg)?.let {
                        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                        runCatching { startActivity(it) }
                    }
                } else {
                    hotSwitchError = "切换失败：rc=$rc ${out.take(100)}"
                }
            }
        }
    }

    /* -------- 常用命令的增删改 -------- */

    private fun runSavedCmd(item: SavedCmd) {
        cmdSheetOpen = false
        if (item.confirm) {
            cmdPending = item
        } else {
            runCommand(item.cmd)
        }
    }

    private fun upsertCommand(item: SavedCmd, isNew: Boolean) {
        val clean = item.cmd.trim()
        if (clean.isEmpty()) return
        // id 同时是 LazyColumn 的 key，同一毫秒连存两条会撞，这里往后挪到空位。
        val id = if (isNew || item.id == 0L) {
            generateSequence(System.currentTimeMillis()) { it + 1 }
                .first { candidate -> savedCmds.none { it.id == candidate } }
        } else {
            item.id
        }
        val normalized = item.copy(
            id = id,
            nick = item.nick.trim().ifBlank { CmdStore.defaultNick(clean) },
            cmd = clean
        )
        val list = savedCmds.toMutableList()
        val index = list.indexOfFirst { it.id == id }
        if (index >= 0) list[index] = normalized else list.add(0, normalized)
        savedCmds = list
        CmdStore.save(this, list)
    }

    private fun deleteCommand(id: Long) {
        val list = savedCmds.filterNot { it.id == id }
        savedCmds = list
        CmdStore.save(this, list)
    }

    private fun pinCommand(id: Long) {
        val item = savedCmds.firstOrNull { it.id == id } ?: return
        val list = listOf(item) + savedCmds.filterNot { it.id == id }
        savedCmds = list
        CmdStore.save(this, list)
    }

    private fun runCommand(text: String? = null) {
        val cmd = (text ?: cmdInput).trim()
        if (cmd.isEmpty() || cmdRunning) return
        // 从常用命令直接跑时不动输入框，避免把用户正在敲的内容冲掉。
        if (text == null) cmdInput = ""
        cmdLines.add(CmdLine.Input(cmd))
        cmdRunning = true
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val p = PrivService.newProcess(arrayOf("sh", "-c", cmd), shizukuEnv(), "/")
                val out = p.getInputStream().bufferedReader().readText()
                val err = p.getErrorStream().bufferedReader().readText()
                val rc = p.waitFor()
                Triple(rc, out, err)
            }.getOrElse {
                runCatching {
                    val p = ProcessBuilder("/system/bin/sh", "-c", cmd)
                        .redirectErrorStream(true)
                        .start()
                    val out = p.inputStream.bufferedReader().readText()
                    Triple(p.waitFor(), out, "")
                }.getOrElse { Triple(-1, "", it.message ?: "执行失败") }
            }
            val text = (result.second + result.third).trimEnd()
            withContext(Dispatchers.Main) {
                if (text.isEmpty()) {
                    cmdLines.add(CmdLine.Output("(无输出，exit=${result.first})"))
                } else {
                    text.lineSequence().take(400).forEach { cmdLines.add(CmdLine.Output(it)) }
                }
                cmdRunning = false
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        val started = System.currentTimeMillis()
        elapsedMs = 0
        totalMs = null
        finishedOk = null
        ticker = lifecycleScope.launch {
            while (true) {
                elapsedMs = System.currentTimeMillis() - started
                delay(120)
            }
        }
    }

    private fun stopTicker(ok: Boolean, started: Long) {
        ticker?.cancel()
        totalMs = System.currentTimeMillis() - started
        finishedOk = ok
    }

    private fun advanceStage(index: Int) {
        if (index > stageIndex) runOnUiThread { stageIndex = index }
    }

    private fun detectStage(line: String) {
        val lower = line.lowercase()
        when {
            lower.contains("module_loaded") || lower.contains("module loaded") -> advanceStage(2)
            lower.contains("susfs") || lower.contains("hybridmount") || lower.contains("mount") -> advanceStage(3)
            line.trim() == "DONE" || lower.contains("success") -> advanceStage(4)
        }
    }

    private fun runFlow() {
        if (busy) return
        busy = true
        stageIndex = 0
        val softReboot = softRebootAfter
        val started = System.currentTimeMillis()
        startTicker()
        lifecycleScope.launch(Dispatchers.IO) {
            var success = false
            var exit = -1
            try {
                if (exploit == ExploitKind.UNSUPPORTED) {
                    addLog("当前机型/内核不受支持：$kernelVersion")
                    return@launch
                }
                if (PrivService.pick() == null) {
                    addLog("未连接内置授权：请先点右侧按钮完成无线调试配对")
                    return@launch
                }
                val ksudPreflight = PrivService.newProcess(
                    arrayOf(
                        "sh", "-c",
                        "[ -n \"$(pm path '${manager.pkg}' 2>/dev/null)\" ] || [ -x /data/adb/ksu/bin/ksud ] || [ -x /data/adb/ksud ]"
                    ),
                    shizukuEnv(), "/"
                ).waitFor()
                if (ksudPreflight != 0) {
                    addLog("未安装管理器 ${manager.pkg}，请先安装对应管理器 APK")
                    return@launch
                }
                advanceStage(1)
                val nativeDir = applicationInfo.nativeLibraryDir
                addLog("Starting ${exploit.fullLabel}")
                val cmd = when (exploit) {
                    ExploitKind.CVE61821 -> arrayOf("$nativeDir/libk61821.so", "run")
                    ExploitKind.CVE61269 -> arrayOf("$nativeDir/libk61269.so", "run")
                    ExploitKind.CVE61238 -> arrayOf("$nativeDir/libk61238.so", "run")
                    ExploitKind.CVE61258 -> arrayOf("$nativeDir/libk61258.so", "run")
                    ExploitKind.CVE61223 -> arrayOf("$nativeDir/libk61223.so", "run")
                    ExploitKind.CVE66118 -> arrayOf("$nativeDir/libk66118.so", "run")
                    ExploitKind.CVE66127 -> arrayOf("$nativeDir/libk66127.so", "run")
                    ExploitKind.UNSUPPORTED -> null
                } ?: return@launch
                val (rc, transcript) = runStreaming(cmd, nativeDir)
                exit = rc
                addLog("exploit exit=$rc")
                val (active, se) = readDeviceState()
                // runner 打印的 DONE 是越狱成功最直接的证据；ADB shell 回显
                // 可能让状态检测误判，任一成立即视为已加载。
                val runnerOk = transcript.lineSequence().any { it.trim() == "DONE" }
                val loaded = active || runnerOk
                success = (rc == 0 || runnerOk) && loaded
                withContext(Dispatchers.Main) {
                    ksuActive = loaded
                    selinuxText = se
                }
                addLog(if (loaded) "KernelSU module loaded" else "KernelSU module not loaded")
                if (loaded) advanceStage(4)
                if (rc == 0 && loaded && !softReboot) withContext(Dispatchers.Main) { launchManager() }
                if (softReboot && rc == 0 && loaded) {
                    addLog("越狱完成，正在重启 zygote")
                    runCatching {
                        PrivService.newProcess(
                            arrayOf("su", "-c", "setprop ctl.restart zygote"),
                            shizukuEnv(), "/"
                        ).waitFor()
                    }.onFailure { addLog("软重启失败：${it.message}") }
                }
            } catch (t: Throwable) {
                addLog("ERROR: $t")
            } finally {
                withContext(Dispatchers.Main) {
                    busy = false
                    stopTicker(success, started)
                }
            }
        }
    }

    private fun runStreaming(cmd: Array<String>, dir: String): Pair<Int, String> {
        val susfsKo = if (exploit == ExploitKind.CVE61821) {
            "${applicationInfo.nativeLibraryDir}/libsusfs_guard_lkm.so"
        } else {
            "${applicationInfo.nativeLibraryDir}/libsusfs_guard_lkm_612.so"
        }
        val susfsActive = susfsSupport && supportsSusfs()
        val universal = manager == ManagerKind.DYNAMIC && supportsUniversal()
        val initialManager = if (manager == ManagerKind.DYNAMIC) ManagerKind.KERNELSU else manager
        val env = shizukuEnv() + "PYB110_DIR=$dir" + "PYB110_MANAGER=${initialManager.manager}" +
            "PYB110_LATE_LOAD=1" +
            "PYB110_UNLOAD_GUARD=${if (unloadGuard) "1" else "0"}" +
            "PYB110_GATE=${if (isGateKernel()) "1" else "0"}" +
            "PYB110_DISABLE_ANTIROOT=${if (antiRootGuard) "1" else "0"}" +
            "PYB110_SUSFS=${if (susfsActive) "1" else "0"}" +
            "PYB110_VFS=${if (vfsSupport) "1" else "0"}" +
            "PYB110_UNIVERSAL=${if (universal) "1" else "0"}" +
            "PYB110_UNIVERSAL_KSUD=${applicationInfo.nativeLibraryDir}/libksud_universal.so" +
            "PYB110_FILES_DIR=${filesDir.absolutePath}" +
            "PYB110_SUSFS_KO=$susfsKo" +
            "PYB110_VFS_KO=${applicationInfo.nativeLibraryDir}/libhybridmount.so"
        val p = PrivService.newProcess(cmd, env, dir)
        val out = p.getInputStream()
        val err = p.getErrorStream()
        val transcript = StringBuilder()
        fun record(line: String) {
            addLog(line)
            detectStage(line)
            synchronized(transcript) { transcript.append(line).append('\n') }
        }
        val t1 = Thread {
            runCatching { out.bufferedReader().forEachLine { record(it) } }
        }.apply { isDaemon = true; start() }
        val t2 = Thread {
            runCatching { err.bufferedReader().forEachLine { record(it) } }
        }.apply { isDaemon = true; start() }
        val rc = p.waitFor()
        runCatching { out.close() }
        runCatching { err.close() }
        t1.join(2000)
        t2.join(2000)
        return rc to synchronized(transcript) { transcript.toString() }
    }

    private fun launchManager() {
        val intent = packageManager.getLaunchIntentForPackage(manager.pkg)
        if (intent == null) {
            addLog("找不到管理器入口：${manager.pkg}")
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        runCatching { startActivity(intent) }
            .onFailure { addLog("打开管理器失败：$it") }
    }

    private fun openDroidspacesDialog() {
        if (droidDownloading) return
        showDroid = true
        droidRelease = null
        droidSavedPath = null
        droidError = null
        droidLoading = true
        lifecycleScope.launch {
            runCatching { DroidspacesModule.latest() }
                .onSuccess { droidRelease = it }
                .onFailure { droidError = it.message ?: "获取最新版本失败" }
            droidLoading = false
        }
    }

    private fun downloadDroidspacesModule() {
        val release = droidRelease ?: return
        if (droidDownloading) return
        droidDownloading = true
        droidProgress = 0f
        droidError = null
        lifecycleScope.launch {
            runCatching {
                DroidspacesModule.download(applicationContext, release) { progress ->
                    droidProgress = progress
                }
            }.onSuccess { droidSavedPath = it }
                .onFailure { droidError = it.message ?: "下载失败" }
            droidDownloading = false
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes <= 0 -> "未知大小"
        bytes >= 1024 * 1024 -> "%.2f MB".format(bytes / 1024.0 / 1024.0)
        else -> "%.0f KB".format(bytes / 1024.0)
    }

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }

    /**
     * 完整 uname 形如 `6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k`，
     * 中间那串 commit hash 对本机识别没有意义，一行也放不下。
     * 这里只保留内核版本、Android 版本与构建号，长度降到原来的一半左右。
     */
    private fun shortKernel(raw: String): String {
        val version = Regex("""^(\d+\.\d+\.\d+)""").find(raw)?.groupValues?.get(1)
        val android = Regex("""android(\d+)""").find(raw)?.groupValues?.get(1)
        val build = Regex("""ab[a-z]*\d+""").findAll(raw).lastOrNull()?.value
        val parts = listOfNotNull(version, android?.let { "android$it" }, build)
        return if (parts.size >= 2) parts.joinToString(" · ") else raw
    }

    private fun resolveDeviceName(): String {
        val marketName = when (Build.MANUFACTURER.orEmpty().lowercase()) {
            "xiaomi" -> getSystemProperty("ro.product.marketname")
            "oppo", "oneplus", "realme", "oplus" -> getSystemProperty("ro.vendor.oplus.market.name")
            "vivo" -> getSystemProperty("ro.vivo.market.name")
            "honor", "huawei" -> getSystemProperty("ro.config.marketing_name")
            "zte", "nubia" -> getSystemProperty("ro.vendor.product.ztename")
            else -> ""
        }.trim().takeIf {
            it.isNotEmpty() &&
                !it.equals(Build.UNKNOWN, ignoreCase = true) &&
                !it.equals("null", ignoreCase = true)
        }
        if (marketName != null) return marketName
        return buildString {
            append(Build.MANUFACTURER)
            if (!Build.BRAND.equals(Build.MANUFACTURER, ignoreCase = true)) {
                append(" ").append(Build.BRAND)
            }
            append(" ").append(Build.MODEL)
        }
    }

    private fun getSystemProperty(key: String): String = try {
        val props = Class.forName("android.os.SystemProperties")
        props.getMethod("get", String::class.java).invoke(null, key) as? String ?: ""
    } catch (t: Throwable) {
        ""
    }

    private fun refreshStaticStateAsync() {
        lifecycleScope.launch(Dispatchers.IO) {
            val raw = getSELinuxStatusRaw()
            withContext(Dispatchers.Main) { selinuxText = mapSelinux(raw) }
        }
    }

    private fun getSELinuxStatusRaw(): String = try {
        val process = ProcessBuilder("getenforce").start()
        val stdout = process.inputStream.bufferedReader().readText().trim()
        val stderr = process.errorStream.bufferedReader().readText().trim()
        val ok = process.waitFor() == 0
        if (ok) {
            when (stdout) {
                "Enforcing", "Permissive", "Disabled" -> stdout
                else -> "Unknown"
            }
        } else if (stderr.endsWith("Permission denied")) {
            "Enforcing"
        } else {
            "Unknown"
        }
    } catch (t: Throwable) {
        "Unknown"
    }

    private fun mapSelinux(raw: String): String = when (raw) {
        "Enforcing" -> "强制执行"
        "Permissive" -> "宽容模式"
        "Disabled" -> "已禁用"
        else -> "未知"
    }

    private fun shizukuEnv(): Array<String> = arrayOf(
        "PATH=/system/bin:/system/xbin:/data/adb/ksu/bin",
        "HOME=/"
    )

    private fun isVivoV2505a(): Boolean =
        (exploit == ExploitKind.CVE61223 || exploit == ExploitKind.CVE61258) &&
            kernelVersion.contains("abogki")

    private fun supportsUniversal(): Boolean =
        exploit == ExploitKind.CVE61821 ||
            exploit == ExploitKind.CVE61269 ||
            exploit == ExploitKind.CVE61238 ||
            exploit == ExploitKind.CVE61258 ||
            exploit == ExploitKind.CVE61223

    /** OriginOS 7 的内核通过 hooks_defend 门加载 KSU LKM，不能按常规路径处理。 */
    private fun isGateKernel(): Boolean =
        exploit == ExploitKind.CVE66127 ||
            (exploit == ExploitKind.CVE61258 &&
                (kernelVersion.contains("gdd6fd5c98918") ||
                    kernelVersion.contains("g690108013dcc")))

    private fun supportsSusfs(): Boolean =
        !isGateKernel() && exploit != ExploitKind.CVE66118 &&
            exploit != ExploitKind.CVE66127 &&
            exploit != ExploitKind.UNSUPPORTED

    private fun managerChoices(): List<ManagerKind> =
        if (supportsUniversal()) ManagerKind.entries
        else ManagerKind.entries.filter { it != ManagerKind.DYNAMIC }

    private fun hotManagerChoices(): List<ManagerKind> =
        ManagerKind.entries.filter { it != ManagerKind.DYNAMIC }

    private enum class ExploitKind(val fullLabel: String) {
        CVE61821("CVE-2026-98260-6.18"),
        CVE61269("CVE-2026-98260-6.12.69"),
        CVE61238("CVE-2026-98260-6.12.38"),
        CVE61258("CVE-2026-98260-6.12.58"),
        CVE61223("CVE-2026-98260-vivo-6.12.23"),
        CVE66118("CVE-2026-98260-6.6.118"),
        CVE66127("CVE-2026-98260-vivo-6.6.127"),
        UNSUPPORTED("当前机型不支持")
    }

    private enum class ManagerKind(val fullLabel: String, val pkg: String, val manager: String) {
        KERNELSU("KernelSU", "me.weishu.kernelsu", "kernelsu"),
        FINASU("FinaSU", "me.yuki.fina", "finasu"),
        NEXT("KernelSU Next", "com.rifsxd.ksunext", "next"),
        BAKASU("BakaSU", "com.resukisu.resukisu", "bakasu"),
        SUKISU("SukiSU Ultra", "com.sukisu.ultra", "sukisu"),
        DYNAMIC("Dynamic Switching", "me.weishu.kernelsu", "kernelsu")
    }

    companion object {
        private const val REQ_SHIZUKU = 1001
        private const val REQ_ADB_PERM = 1002
        private val PROFILE_BY_RELEASE: Map<String, ExploitKind> = mapOf(
            "6.18.21-android17-5-gf957accaf563-ab15926176-4k" to ExploitKind.CVE61821,
            "6.18.21-android17-5-g7fcbffcad3e1-ab16353812-4k" to ExploitKind.CVE61821,
            "6.12.69-android16-6-g242e1a07f878-ab15865446-4k" to ExploitKind.CVE61269,
            "6.12.69-android16-6-g5fd39602326e-ab16372235-4k" to ExploitKind.CVE61269,
            "6.12.38-android16-5-g844001fb8721-ab14552068-4k" to ExploitKind.CVE61238,
            "6.12.58-android16-6-g7704a1ae279b-ab15213644-4k" to ExploitKind.CVE61258,
            "6.12.58-android16-6-g0a092cc0037a-abogki521987229-4k" to ExploitKind.CVE61258,
            "6.12.58-android16-6-gdd6fd5c98918-abogki543627742-4k" to ExploitKind.CVE61258,
            "6.12.23-android16-5-g41f4cb1fa7c7-abogki465038634-4k" to ExploitKind.CVE61223,
            "6.6.118-android15-8-g9bc34d5b0c79-abogki537459655-4k" to ExploitKind.CVE66118,
            "6.6.118-android15-8-gc4127a25dcf3-ab15863337-4k" to ExploitKind.CVE66118,
            "6.6.127-android15-8-gd489d4ad2a70-abogki567717946-4k" to ExploitKind.CVE66127,
            "6.12.58-android16-6-g690108013dcc-abogki576129896-4k" to ExploitKind.CVE61258,
        )
    }
}
