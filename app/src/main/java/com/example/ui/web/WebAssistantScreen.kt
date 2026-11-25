package com.example.ui.web

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.notifications.AerisNotificationManager
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanBright
import com.example.ui.theme.CyanDeep
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.CyanTeal
import com.example.ui.theme.HoloBlack
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloDark
import com.example.ui.theme.HoloSurface
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextCoolGrey
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.Typography

data class WebQuickTarget(val title: String, val url: String, val subtitle: String)

@Composable
fun WebAssistantScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifManager = remember { AerisNotificationManager(context) }

    var urlInput by remember { mutableStateOf("https://accounts.google.com") }
    var activeUrl by remember { mutableStateOf<String?>("https://accounts.google.com") }
    var isLoading by remember { mutableStateOf(false) }

    var showGmailDialog by remember { mutableStateOf(false) }
    var selectedGmailAccount by remember { mutableStateOf<String?>(null) }
    var discoveredAccounts by remember { mutableStateOf<List<String>>(emptyList()) }

    val quickTargets = listOf(
        WebQuickTarget("Google Sign-In", "https://accounts.google.com", "Official Google Login"),
        WebQuickTarget("GitHub", "https://github.com/login", "Developer Portal"),
        WebQuickTarget("Wikipedia", "https://en.wikipedia.org", "Knowledge Base"),
        WebQuickTarget("Hugging Face", "https://huggingface.co", "GGUF Model Hub")
    )

    // Activity Result Launcher for native Android Account Picker
    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val chosen = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!chosen.isNullOrBlank()) {
            selectedGmailAccount = chosen
            Toast.makeText(context, "Account selected: $chosen", Toast.LENGTH_SHORT).show()
        }
    }

    fun initiateGmailSelection() {
        val am = AccountManager.get(context)
        val googleAccounts = try {
            am.getAccountsByType("com.google").map { it.name }
        } catch (_: Exception) {
            emptyList()
        }

        discoveredAccounts = if (googleAccounts.isNotEmpty()) {
            googleAccounts
        } else {
            listOf("ayyanmukadam005@gmail.com") // fallback / primary user
        }

        showGmailDialog = true
        notifManager.showAccountSelectionPrompt("Sir ji, kis Gmail account se login karna hai? Please select your account.")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBlack)
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .testTag("web_assistant_screen")
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = CyanNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "WEB ASSISTANT",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = TextWhite
                    )
                    Text(
                        text = "Controlled HTTPS & Login Gateway",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = CyanBright
                    )
                }
            }

            // Google Sign-In Action Button
            Surface(
                modifier = Modifier
                    .clickable { initiateGmailSelection() }
                    .border(1.dp, CyanNeon, RoundedCornerShape(12.dp))
                    .testTag("choose_gmail_button"),
                color = CyanDeep.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Gmail Account Selection",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Select Account",
                        style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = CyanBright
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 2. Privacy & Security Banner
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = HoloSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Zero-Knowledge",
                    tint = StatusSuccess,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero-Knowledge: AERIS never sees or stores passwords, OTPs, or 2FA secrets.",
                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextCoolGrey
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. URL Bar & Navigation Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("web_url_field"),
                leadingIcon = {
                    Icon(Icons.Default.Language, contentDescription = null, tint = CyanTeal, modifier = Modifier.size(16.dp))
                },
                placeholder = { Text("https://example.com", fontSize = 11.sp, color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextPrimary,
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = HoloCardBorder,
                    focusedContainerColor = HoloSurface,
                    unfocusedContainerColor = HoloSurface
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = {
                    val formatted = if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                        "https://$urlInput"
                    } else urlInput
                    activeUrl = formatted
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanDeep),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon),
                modifier = Modifier.height(48.dp)
            ) {
                Text("GO", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Open in external default browser button
            IconButton(
                onClick = {
                    val target = activeUrl ?: urlInput
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                        context.startActivity(browserIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open browser: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Open in Chrome/Browser",
                    tint = CyanBright,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Targets Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickTargets.forEach { target ->
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            urlInput = target.url
                            activeUrl = target.url
                        },
                    shape = RoundedCornerShape(8.dp),
                    color = HoloSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, HoloCardBorder.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = target.title,
                            style = Typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                            color = CyanBright,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active Account Indicator (if selected)
        selectedGmailAccount?.let { acc ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = CyanDeep.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanNeon)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selected Login: $acc",
                            style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium),
                            color = TextWhite
                        )
                    }
                    Text(
                        text = "Change",
                        style = Typography.labelSmall.copy(fontSize = 10.sp, color = CyanBright),
                        modifier = Modifier.clickable { showGmailDialog = true }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 4. Secure Controlled In-App WebView
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .border(1.dp, CyanGlow.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            activeUrl?.let { url ->
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW // Enforce HTTPS
                                userAgentString = "${settings.userAgentString} AERIS-Android-Assistant"
                            }
                            CookieManager.getInstance().setAcceptCookie(true)

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { urlInput = it }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let { urlInput = it }
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    super.onProgressChanged(view, newProgress)
                                    isLoading = newProgress < 100
                                }
                            }

                            loadUrl(url)
                        }
                    },
                    update = { webView ->
                        if (webView.url != url) {
                            webView.loadUrl(url)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Gmail Account Selection Dialog
    if (showGmailDialog) {
        AlertDialog(
            onDismissRequest = { showGmailDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Sir ji, kis Gmail account se login karna hai?",
                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanNeon
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select your desired Google account. Authentication takes place directly through Google's official interface. AERIS does not collect or store your password.",
                        style = Typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextCoolGrey
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    discoveredAccounts.forEach { accountName ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedGmailAccount = accountName
                                    showGmailDialog = false
                                    Toast.makeText(context, "Selected: $accountName", Toast.LENGTH_SHORT).show()
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = if (selectedGmailAccount == accountName) CyanDeep else HoloSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selectedGmailAccount == accountName) CyanNeon else HoloCardBorder
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = if (selectedGmailAccount == accountName) CyanNeon else TextCoolGrey,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = accountName,
                                    style = Typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                                    color = TextWhite
                                )
                            }
                        }
                    }

                    // Choose / Add another account button
                    Button(
                        onClick = {
                            showGmailDialog = false
                            try {
                                val intent = AccountManager.newChooseAccountIntent(
                                    null,
                                    null,
                                    arrayOf("com.google"),
                                    false,
                                    null,
                                    null,
                                    null,
                                    null
                                )
                                accountPickerLauncher.launch(intent)
                            } catch (_: Exception) {
                                val addIntent = Intent(android.provider.Settings.ACTION_ADD_ACCOUNT).apply {
                                    putExtra(android.provider.Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                                }
                                context.startActivity(addIntent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HoloSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyanGlow),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = CyanBright, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choose / Add Another Account", color = CyanBright, style = Typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGmailDialog = false }) {
                    Text("CLOSE", color = CyanNeon)
                }
            },
            containerColor = HoloSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
