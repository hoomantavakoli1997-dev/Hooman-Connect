package com.hoomanconnect.app

import android.os.Bundle
import android.content.Intent
import android.net.VpnService
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.hoomanconnect.app.vpn.HoomanVpnService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.platform.LocalContext
import com.hoomanconnect.app.profiles.EncryptedProfileStore
import com.hoomanconnect.app.profiles.ServerProfile
import com.hoomanconnect.app.profiles.SingBoxConfigCheck
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.hoomanconnect.app.diagnostics.EndpointProbe

private val Navy = Color(0xFF071522)
private val Panel = Color(0xFF102638)
private val Cyan = Color(0xFF27C2D1)
private val Muted = Color(0xFF9FB4C3)

class MainActivity : ComponentActivity() {
    private var vpnMessage by mutableStateOf("اتصال VPN هنوز آماده نیست؛ هسته اتصال نصب نشده است.")
    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val intent = Intent(this, HoomanVpnService::class.java).setAction(HoomanVpnService.ACTION_START)
            ContextCompat.startForegroundService(this, intent)
            vpnMessage = "مجوز اندروید دریافت شد؛ در انتظار هسته VPN سازگار هستیم."
        } else {
            vpnMessage = "مجوز VPN صادر نشد؛ هیچ اتصالی برقرار نشد."
        }
    }

    private fun requestVpnStart() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) vpnPermission.launch(prepareIntent)
        else {
            val intent = Intent(this, HoomanVpnService::class.java).setAction(HoomanVpnService.ACTION_START)
            ContextCompat.startForegroundService(this, intent)
            vpnMessage = "مجوز قبلاً صادر شده؛ هسته VPN هنوز نصب نشده است."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HoomanApp(vpnMessage, ::requestVpnStart) }
    }
}

private enum class Page(val title: String) {
    HOME("خانه"), SERVERS("سرورها"), SMART("تونل هوشمند"), GOOGLE("دستیار گوگل"), SETTINGS("تنظیمات")
}

@Composable
private fun HoomanApp(vpnMessage: String, onVpnStart: () -> Unit) {
    var page by remember { mutableStateOf(Page.HOME) }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = darkColorScheme(
            background = Navy, surface = Panel, primary = Cyan,
            onBackground = Color.White, onSurface = Color.White
        )) {
            Scaffold(containerColor = Navy, bottomBar = {
                NavigationBar(containerColor = Panel) {
                    Page.values().forEach { item ->
                        NavigationBarItem(
                            selected = page == item,
                            onClick = { page = item },
                            icon = { Icon(when (item) {
                                Page.HOME -> Icons.Default.Home
                                Page.SERVERS -> Icons.Default.Public
                                Page.SMART -> Icons.Default.Tune
                                Page.GOOGLE -> Icons.Default.Language
                                Page.SETTINGS -> Icons.Default.Settings
                            }, contentDescription = item.title) },
                            label = { Text(item.title, fontSize = 10.sp) }
                        )
                    }
                }
            }) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("♜", color = Cyan, fontSize = 32.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("HOOMAN CONNECT", color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            Text("اتصال هوشمند و امن", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    when (page) {
                        Page.HOME -> HomeScreen(vpnMessage, onVpnStart, onGoogle = { page = Page.GOOGLE })
                        Page.SERVERS -> ServerProfilesScreen()
                        Page.SMART -> PlaceholderScreen("تونل هوشمند", "قواعد مسیریابی برنامه‌ها و دامنه‌ها در مرحله بعد پیاده‌سازی می‌شود.")
                        Page.GOOGLE -> GoogleAssistantScreen()
                        Page.SETTINGS -> PlaceholderScreen("تنظیمات", "زبان، پوسته، حریم خصوصی و تنظیمات اتصال.")
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(vpnMessage: String, onVpnStart: () -> Unit, onGoogle: () -> Unit) {
    Text("خوش آمدی", color = Muted, fontSize = 14.sp)
    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🦅", fontSize = 72.sp)
            Text("Hooman Connect", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(vpnMessage, color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onVpnStart, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("آزمایش آماده‌سازی اتصال VPN")
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text("دستیار سرویس‌های گوگل", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Text("بررسی DNS و دسترسی شبکه به دامنه‌های رسمی گوگل", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onGoogle) { Text("باز کردن دستیار") }
        }
    }
}

private data class DomainResult(val domain: String, val detail: String, val ok: Boolean)

@Composable
private fun GoogleAssistantScreen() {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<DomainResult>>(emptyList()) }
    var message by remember { mutableStateOf("برای شروع، بررسی شبکه را اجرا کن.") }
    val domains = listOf("accounts.google.com", "gemini.google.com", "flow.google.com")

    Text("Google Service Assistant", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("این ابزار فقط وضعیت DNS و دسترسی پایه شبکه را بررسی می‌کند؛ وضعیت حساب، سن، اشتراک یا مجاز بودن منطقه را تغییر نمی‌دهد.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(18.dp))
    Button(onClick = {
        loading = true
        message = "در حال بررسی DNS و HTTPS…"
        scope.launch {
            results = withContext(Dispatchers.IO) {
                domains.map { domain ->
                    val probe = EndpointProbe.check(domain)
                    val detail = if (probe.dnsOk) {
                        "${probe.message} | HTTP ${probe.httpStatus ?: "نامشخص"} | ${probe.addresses.size} نشانی DNS"
                    } else probe.message
                    DomainResult(domain, detail, probe.dnsOk && probe.httpStatus != null)
                }
            }
            loading = false
            message = "بررسی تمام شد. این نتیجه تضمین‌کننده دسترسی به خود سرویس نیست."
        }
    }, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        else Icon(Icons.Default.NetworkCheck, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(if (loading) "در حال بررسی" else "بررسی اتصال")
    }
    Spacer(Modifier.height(14.dp))
    Text(message, color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(10.dp))
    results.forEach { result ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(if (result.ok) Cyan else Color(0xFFFFA45B), CircleShape))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(result.domain, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text(result.detail, color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("نکته: موفقیت DNS فقط یعنی نام دامنه به نشانی شبکه تبدیل شده است؛ دسترسی نهایی ممکن است به عوامل دیگری مثل حساب کاربری، منطقه و شرایط خود سرویس وابسته باشد.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
}


@Composable
private fun ServerProfilesScreen() {
    val context = LocalContext.current
    val store = remember { EncryptedProfileStore(context) }
    var name by remember { mutableStateOf("") }
    var config by remember { mutableStateOf("") }
    var profiles by remember { mutableStateOf(runCatching { store.load() }.getOrDefault(emptyList())) }
    var message by remember { mutableStateOf("پیکربندی sing-box را از ارائه‌دهنده‌ای که مجاز به استفاده از آن هستی دریافت کن.") }
    var error by remember { mutableStateOf(false) }

    Text("پروفایل‌های سرور", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text("پروفایل‌ها روی دستگاه با AES-GCM و کلید Android Keystore رمزگذاری می‌شوند. این صفحه هنوز اتصال را اجرا نمی‌کند.", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("نام پروفایل") }, singleLine = true)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(value = config, onValueChange = { config = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp), label = { Text("پیکربندی JSON هسته") }, minLines = 5)
    Spacer(Modifier.height(10.dp))
    Button(onClick = {
        val check = SingBoxConfigCheck.check(config)
        if (!check.valid || name.isBlank()) {
            error = true
            message = if (name.isBlank()) "نام پروفایل را وارد کن." else check.message
        } else {
            runCatching { store.save(ServerProfile(name = name.trim(), configJson = config)) }
                .onSuccess {
                    profiles = store.load()
                    name = ""
                    config = ""
                    error = false
                    message = "پروفایل به‌صورت رمزگذاری‌شده ذخیره شد. ${check.message}"
                }
                .onFailure { error = true; message = it.message ?: "ذخیره ناموفق بود." }
        }
    }, modifier = Modifier.fillMaxWidth()) { Text("اعتبارسنجی اولیه و ذخیره امن") }
    Spacer(Modifier.height(8.dp))
    Text(message, color = if (error) Color(0xFFFFA45B) else Muted, fontSize = 12.sp, lineHeight = 18.sp)
    Spacer(Modifier.height(18.dp))
    Text("پروفایل‌های ذخیره‌شده (${profiles.size})", color = Color.White, fontWeight = FontWeight.Bold)
    profiles.forEach { profile ->
        Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("ذخیره‌شده روی همین دستگاه", color = Muted, fontSize = 11.sp)
                }
                IconButton(onClick = {
                    runCatching { store.delete(profile.id); profiles = store.load() }
                        .onSuccess { message = "پروفایل حذف شد."; error = false }
                        .onFailure { message = it.message ?: "حذف ناموفق بود."; error = true }
                }) { Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color(0xFFFFA45B)) }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String) {
    Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Text(description, Modifier.padding(18.dp), color = Muted, fontSize = 14.sp, lineHeight = 22.sp)
    }
}
