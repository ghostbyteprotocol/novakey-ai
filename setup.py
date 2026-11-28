import os

files = {
    "gradle.properties": """org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
""",
    "settings.gradle": """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "NovaKeyAIPro"
include ':app'
""",
    "build.gradle": """plugins {
    id 'com.android.application' version '8.5.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.24' apply false
}
""",
    "app/build.gradle": """plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.ghostbyte.novakey'
    compileSdk 34

    defaultConfig {
        applicationId "com.ghostbyte.novakey"
        minSdk 24
        targetSdk 34
        versionCode 2
        versionName "2.0-pro"
    }

    buildTypes {
        release {
            minifyEnabled false
        }
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.13.1'
}
""",
    "app/src/main/AndroidManifest.xml": """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.VIBRATE" />

    <application
        android:allowBackup="true"
        android:label="NovaKey AI Pro"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".NovaKeyboardService"
            android:label="NovaKey AI Pro"
            android:permission="android.permission.BIND_INPUT_METHOD"
            android:exported="true">
            <intent-filter>
                <action android:name="android.view.InputMethod" />
            </intent-filter>
            <meta-data
                android:name="android.view.im"
                android:resource="@xml/method" />
        </service>

    </application>
</manifest>
""",
    "app/src/main/res/xml/method.xml": """<?xml version="1.0" encoding="utf-8"?>
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity="com.ghostbyte.novakey.MainActivity"
    android:supportsSwitchingToNextInputMethod="true" />
""",
    "app/src/main/java/com/ghostbyte/novakey/MainActivity.kt": """package com.ghostbyte.novakey

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("novakey_prefs", Context.MODE_PRIVATE)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0B0F17"))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 70, 50, 70)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "⚡ NovaKey AI Pro"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#00E5FF"))
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Offline Phonetic + Online Gemini AI Engine"
            textSize = 13f
            setTextColor(Color.parseColor("#8B949E"))
            gravity = Gravity.CENTER
            setPadding(0, 10, 0, 45)
        }

        val btnEnable = createStyledButton("1. Enable NovaKey in Settings", "#161B22", "#00E5FF").apply {
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        }

        val btnSwitch = createStyledButton("2. Select NovaKey Keyboard", "#00E5FF", "#0B0F17").apply {
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        }

        val apiLabel = TextView(this).apply {
            text = "🔑 Gemini API Key (অনলাইন AI ফিচারের জন্য):"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E6EDF3"))
            setPadding(0, 35, 0, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val apiInput = EditText(this).apply {
            hint = "AIzaSy... এখানে তোর API Key পেস্ট কর"
            setText(prefs.getString("gemini_api_key", ""))
            setHintTextColor(Color.parseColor("#484F58"))
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(32, 28, 32, 28)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161B22"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#30363D"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20 }
        }

        val btnSaveApi = createStyledButton("Save API Key & Theme", "#1F6FEB", "#FFFFFF").apply {
            setOnClickListener {
                val key = apiInput.text.toString().trim()
                prefs.edit().putString("gemini_api_key", key).apply()
                Toast.makeText(this@MainActivity, "API Key সেভ হয়ে গেছে ভাই!", Toast.LENGTH_SHORT).show()
            }
        }

        val themeLabel = TextView(this).apply {
            text = "🎨 কিবোর্ড থিম কালার বেছে নে:"
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#E6EDF3"))
            setPadding(0, 30, 0, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val themeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 35 }
        }

        val themes = listOf(
            Triple("Cyan", "#00E5FF", "#0B0F17"),
            Triple("Neon Green", "#00FF88", "#07130D"),
            Triple("Crimson", "#FF2A5F", "#14080C")
        )

        themes.forEach { (name, accent, bg) ->
            val tBtn = Button(this).apply {
                text = name
                textSize = 13f
                isAllCaps = false
                setTextColor(Color.parseColor(accent))
                background = GradientDrawable().apply {
                    setColor(Color.parseColor(bg))
                    cornerRadius = 20f
                    setStroke(2, Color.parseColor(accent))
                }
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                    leftMargin = 8
                    rightMargin = 8
                }
                setOnClickListener {
                    prefs.edit()
                        .putString("theme_accent", accent)
                        .putString("theme_bg", bg)
                        .apply()
                    Toast.makeText(this@MainActivity, "$name থিম সেট হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            }
            themeRow.addView(tBtn)
        }

        val testLabel = TextView(this).apply {
            text = "নিচে টাইপ করে টেস্ট কর (EN / বাং / हिं + ✨ AI):"
            textSize = 14f
            setTextColor(Color.parseColor("#C9D1D9"))
            setPadding(0, 10, 0, 14)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val testBox = EditText(this).apply {
            hint = "এখানে কিছু লিখে ✨ AI বাটনে চাপ দে..."
            setHintTextColor(Color.parseColor("#484F58"))
            setTextColor(Color.WHITE)
            textSize = 16f
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
            setPadding(34, 34, 34, 34)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#161B22"))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#30363D"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(btnEnable)
        root.addView(btnSwitch)
        root.addView(apiLabel)
        root.addView(apiInput)
        root.addView(btnSaveApi)
        root.addView(themeLabel)
        root.addView(themeRow)
        root.addView(testLabel)
        root.addView(testBox)

        scroll.addView(root)
        setContentView(scroll)
    }

    private fun createStyledButton(label: String, bgHex: String, textHex: String): Button {
        return Button(this).apply {
            text = label
            textSize = 15f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(textHex))
            setPadding(30, 32, 30, 32)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = 24f
                setStroke(2, Color.parseColor("#00E5FF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 22
            }
        }
    }
}
""",
    "app/src/main/java/com/ghostbyte/novakey/GeminiHelper.kt": """package com.ghostbyte.novakey

import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object GeminiHelper {
    fun processText(
        apiKey: String,
        inputText: String,
        mode: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (apiKey.isBlank()) {
            onError("আগে NovaKey অ্যাপ খুলে Gemini API Key সেভ কর ভাই!")
            return
        }
        if (inputText.isBlank()) {
            onError("আগে বক্সে কিছু টাইপ কর!")
            return
        }

        val prompt = when (mode) {
            "GRAMMAR" -> "Fix grammar and spelling naturally. Keep the original language and tone. Output ONLY the corrected text without quotes or explanation:\\n$inputText"
            "ENGLISH" -> "Translate this naturally into fluent modern English. Output ONLY the translated text:\\n$inputText"
            "BENGALI" -> "Translate this into natural conversational Indian Bengali (West Bengal style). Output ONLY the Bengali text:\\n$inputText"
            "HINDI" -> "Translate this into natural conversational Hindi. Output ONLY the Hindi text:\\n$inputText"
            "SAVAGE" -> "Rewrite or reply to this in a witty, savage, confident bro-style tone (keep same language). Output ONLY the final text:\\n$inputText"
            "FORMAL" -> "Rewrite this professionally and politely for college/official use. Output ONLY the final text:\\n$inputText"
            else -> "Improve this text clearly. Output ONLY the final text:\\n$inputText"
        }

        thread {
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.doOutput = true

                val part = JSONObject().put("text", prompt)
                val parts = JSONArray().put(part)
                val content = JSONObject().put("parts", parts)
                val contents = JSONArray().put(content)
                val payload = JSONObject().put("contents", contents)

                OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

                val code = conn.responseCode
                if (code in 200..299) {
                    val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val json = JSONObject(responseStr)
                    val reply = json.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                        .trim()
                    onResult(reply)
                } else {
                    onError("API Error ($code): নেট বা API Key চেক কর!")
                }
            } catch (e: Exception) {
                onError("অফলাইন আছিস বা নেট স্লো! অনলাইন হলে AI কাজ করবে।")
            }
        }
    }
}
""",
    "app/src/main/java/com/ghostbyte/novakey/PhoneticEngine.kt": """package com.ghostbyte.novakey

object PhoneticEngine {
    private val bnDict = mapOf(
        "ami" to listOf("আমি"), "tumi" to listOf("তুমি"), "tui" to listOf("তুই"),
        "tor" to listOf("তোর"), "amar" to listOf("আমার"), "tomar" to listOf("তোমার"),
        "bhai" to listOf("ভাই"), "dada" to listOf("দাদা"), "baba" to listOf("বাবা"),
        "ma" to listOf("মা"), "ki" to listOf("কী", "কি"), "kemon" to listOf("কেমন"),
        "achi" to listOf("আছি"), "achis" to listOf("আছিস"), "ache" to listOf("আছে"),
        "bhalo" to listOf("ভালো", "ভাল"), "valo" to listOf("ভালো"), "kharap" to listOf("খারাপ"),
        "na" to listOf("না"), "ha" to listOf("হ্যাঁ"), "hyan" to listOf("হ্যাঁ"),
        "keno" to listOf("কেন"), "kothay" to listOf("কোথায়"), "ekhon" to listOf("এখন"),
        "kore" to listOf("করে"), "korchis" to listOf("করছিস"), "korbo" to listOf("করবো"),
        "hobe" to listOf("হবে"), "habo" to listOf("হবো"), "ekdom" to listOf("একদম"),
        "sundor" to listOf("সুন্দর"), "purulia" to listOf("পুরুলিয়া"), "bankura" to listOf("বাঁকুড়া"),
        "kolkata" to listOf("কলকাতা"), "bangla" to listOf("বাংলা"), "aj" to listOf("আজ"),
        "kal" to listOf("কাল"), "cha" to listOf("চা"), "taka" to listOf("টাকা"),
        "bari" to listOf("বাড়ি"), "mess" to listOf("মেস"), "college" to listOf("কলেজ"),
        "bondhu" to listOf("বন্ধু"), "chol" to listOf("চল"), "bol" to listOf("বল"),
        "dekh" to listOf("দেখ"), "thik" to listOf("ঠিক"), "eta" to listOf("এটা"), "ota" to listOf("ওটা")
    )

    private val hiDict = mapOf(
        "kya" to listOf("क्या"), "kaise" to listOf("कैसे"), "bhai" to listOf("भाई"),
        "hai" to listOf("है"), "hain" to listOf("हैं"), "ho" to listOf("हो"),
        "hun" to listOf("हूँ"), "main" to listOf("मैं"), "mein" to listOf("में"),
        "tum" to listOf("तुम"), "tu" to listOf("तू"), "aap" to listOf("आप"),
        "tera" to listOf("तेरा"), "mera" to listOf("मेरा"), "nahi" to listOf("नहीं"),
        "haan" to listOf("हाँ"), "kar" to listOf("कर"), "raha" to listOf("रहा"),
        "bhi" to listOf("भी"), "aur" to listOf("और"), "accha" to listOf("अच्छा"),
        "theek" to listOf("ठीक"), "dost" to listOf("दोस्त"), "ghar" to listOf("घर"),
        "sab" to listOf("सब"), "kuch" to listOf("कुछ"), "abhi" to listOf("अभी"),
        "kal" to listOf("कल"), "aaj" to listOf("आज"), "kyun" to listOf("क्यों")
    )

    fun getSuggestions(raw: String, lang: String): List<String> {
        if (raw.isEmpty()) return emptyList()
        if (lang == "EN") return listOf(raw)

        val lower = raw.lowercase()
        val results = LinkedHashSet<String>()

        if (lang == "BN") {
            bnDict[lower]?.let { results.addAll(it) }
            results.add(transliterateBengali(lower))
        } else if (lang == "HI") {
            hiDict[lower]?.let { results.addAll(it) }
            results.add(transliterateHindi(lower))
        }
        results.add(raw)
        return results.toList()
    }

    private fun transliterateBengali(input: String): String {
        val sb = StringBuilder()
        var i = 0
        var lastWasConsonant = false
        val vowels = mapOf("o" to "অ", "a" to "আ", "i" to "ই", "ee" to "ঈ", "u" to "উ", "oo" to "ঊ", "e" to "এ", "oi" to "ঐ", "ou" to "ঔ")
        val matras = mapOf("a" to "া", "i" to "ি", "ee" to "ী", "u" to "ু", "oo" to "ূ", "e" to "ে", "oi" to "ৈ", "o" to "ো", "ou" to "ৌ")
        val consonants = mapOf(
            "kh" to "খ", "gh" to "ঘ", "ng" to "ঙ", "chh" to "ছ", "ch" to "চ", "jh" to "ঝ",
            "th" to "থ", "dh" to "ধ", "ph" to "ফ", "bh" to "ভ", "sh" to "শ",
            "k" to "ক", "g" to "গ", "c" to "চ", "j" to "জ", "t" to "ত", "d" to "দ", "n" to "ন",
            "p" to "প", "f" to "ফ", "b" to "ব", "v" to "ভ", "m" to "ম",
            "y" to "য়", "r" to "র", "l" to "ল", "s" to "স", "h" to "হ"
        )
        while (i < input.length) {
            var matched = false
            for (len in 3 downTo 1) {
                if (i + len <= input.length) {
                    val sub = input.substring(i, i + len)
                    if (consonants.containsKey(sub)) {
                        sb.append(consonants[sub]); lastWasConsonant = true; i += len; matched = true; break
                    } else if (lastWasConsonant && matras.containsKey(sub)) {
                        sb.append(matras[sub]); lastWasConsonant = false; i += len; matched = true; break
                    } else if (!lastWasConsonant && vowels.containsKey(sub)) {
                        sb.append(vowels[sub]); lastWasConsonant = false; i += len; matched = true; break
                    }
                }
            }
            if (!matched) { sb.append(input[i]); lastWasConsonant = false; i++ }
        }
        return sb.toString()
    }

    private fun transliterateHindi(input: String): String {
        val sb = StringBuilder()
        var i = 0
        var lastWasConsonant = false
        val vowels = mapOf("a" to "अ", "aa" to "आ", "i" to "इ", "ee" to "ई", "u" to "उ", "oo" to "ऊ", "e" to "ए", "ai" to "ऐ", "o" to "ओ", "au" to "औ")
        val matras = mapOf("aa" to "ा", "a" to "ा", "i" to "ि", "ee" to "ी", "u" to "ु", "oo" to "ू", "e" to "े", "ai" to "ै", "o" to "ो", "au" to "ौ")
        val consonants = mapOf(
            "kh" to "ख", "gh" to "घ", "chh" to "छ", "ch" to "च", "jh" to "झ",
            "th" to "थ", "dh" to "ध", "ph" to "फ", "bh" to "भ", "sh" to "श",
            "k" to "क", "g" to "ग", "j" to "ज", "t" to "त", "d" to "द", "n" to "न",
            "p" to "प", "f" to "फ", "b" to "ब", "v" to "व", "m" to "म",
            "y" to "य", "r" to "र", "l" to "ल", "s" to "स", "h" to "ह"
        )
        while (i < input.length) {
            var matched = false
            for (len in 3 downTo 1) {
                if (i + len <= input.length) {
                    val sub = input.substring(i, i + len)
                    if (consonants.containsKey(sub)) {
                        sb.append(consonants[sub]); lastWasConsonant = true; i += len; matched = true; break
                    } else if (lastWasConsonant && matras.containsKey(sub)) {
                        sb.append(matras[sub]); lastWasConsonant = false; i += len; matched = true; break
                    } else if (!lastWasConsonant && vowels.containsKey(sub)) {
                        sb.append(vowels[sub]); lastWasConsonant = false; i += len; matched = true; break
                    }
                }
            }
            if (!matched) { sb.append(input[i]); lastWasConsonant = false; i++ }
        }
        return sb.toString()
    }
}
"""
}

for path, content in files.items():
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(content)

print("Core files generated successfully!")
