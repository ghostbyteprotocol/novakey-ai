package com.ghostbyte.novakey

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.ExtractedTextRequest
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class NovaKeyboardService : InputMethodService() {

    private var currentLang = "EN"
    private var isShift = false
    private var isCapsLock = false
    private var isSymbolMode = false
    private var activePanel = "KEYS"

    private val composingBuffer = StringBuilder()
    private val clipboardHistory = ArrayList<String>()

    private lateinit var rootLayout: LinearLayout
    private lateinit var suggestionContainer: LinearLayout
    private lateinit var mainPanelContainer: LinearLayout
    private lateinit var langButton: Button
    private lateinit var spaceButton: Button

    private var accentHex = "#00E5FF"
    private var bgHex = "#0B0F17"

    private val handler = Handler(Looper.getMainLooper())
    private var isBackspaceHeld = false

    private val backspaceRunnable = object : Runnable {
        override fun run() {
            if (isBackspaceHeld) {
                performBackspace()
                handler.postDelayed(this, 55)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.addPrimaryClipChangedListener {
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrBlank()) {
                clipboardHistory.remove(clipText)
                clipboardHistory.add(0, clipText)
                if (clipboardHistory.size > 15) clipboardHistory.removeAt(clipboardHistory.size - 1)
                if (activePanel == "CLIPBOARD") renderMainPanel()
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        loadTheme()
        if (::rootLayout.isInitialized) {
            rootLayout.setBackgroundColor(Color.parseColor(bgHex))
            renderMainPanel()
        }
    }

    private fun loadTheme() {
        val prefs = getSharedPreferences("novakey_prefs", Context.MODE_PRIVATE)
        accentHex = prefs.getString("theme_accent", "#00E5FF") ?: "#00E5FF"
        bgHex = prefs.getString("theme_bg", "#0B0F17") ?: "#0B0F17"
    }

    override fun onCreateInputView(): View {
        loadTheme()
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(bgHex))
            setPadding(8, 10, 8, 16)
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(6, 4, 6, 8)
        }

        val aiBtn = createToolPill("✨ AI", "#1E293B", accentHex).apply {
            setOnClickListener {
                vibrateKey()
                commitCurrentComposing()
                activePanel = if (activePanel == "AI") "KEYS" else "AI"
                renderMainPanel()
            }
        }

        langButton = createToolPill("🌐 EN", accentHex, "#0B0F17").apply {
            setOnClickListener {
                vibrateKey()
                commitCurrentComposing()
                currentLang = when (currentLang) {
                    "EN" -> "BN"
                    "BN" -> "HI"
                    else -> "EN"
                }
                updateLanguageUI()
                updateSuggestions()
            }
        }

        val clipBtn = createToolPill("📋 Clip", "#161B22", "#E6EDF3").apply {
            setOnClickListener {
                vibrateKey()
                activePanel = if (activePanel == "CLIPBOARD") "KEYS" else "CLIPBOARD"
                renderMainPanel()
            }
        }

        val emojiBtn = createToolPill("😀", "#161B22", "#E6EDF3").apply {
            setOnClickListener {
                vibrateKey()
                activePanel = if (activePanel == "EMOJI") "KEYS" else "EMOJI"
                renderMainPanel()
            }
        }

        val settingsBtn = createToolPill("⚙️", "#161B22", "#8B949E").apply {
            setOnClickListener {
                vibrateKey()
                val intent = Intent(this@NovaKeyboardService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        toolbar.addView(aiBtn)
        toolbar.addView(langButton)
        toolbar.addView(clipBtn)
        toolbar.addView(emojiBtn)
        toolbar.addView(settingsBtn)
        rootLayout.addView(toolbar)

        val suggestionScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(6, 2, 6, 10)
        }
        suggestionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        suggestionScroll.addView(suggestionContainer)
        rootLayout.addView(suggestionScroll)

        mainPanelContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(mainPanelContainer)

        updateSuggestions()
        renderMainPanel()

        return rootLayout
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        composingBuffer.clear()
        isBackspaceHeld = false
        activePanel = "KEYS"
    }

    private fun updateLanguageUI() {
        langButton.text = when (currentLang) {
            "BN" -> "🌐 বাং"
            "HI" -> "🌐 हिं"
            else -> "🌐 EN"
        }
        if (::spaceButton.isInitialized) {
            spaceButton.text = when (currentLang) {
                "BN" -> "NovaKey • বাংলা"
                "HI" -> "NovaKey • हिन्दी"
                else -> "NovaKey • English"
            }
        }
    }

    private fun updateSuggestions() {
        suggestionContainer.removeAllViews()
        val suggestions = if (composingBuffer.isNotEmpty()) {
            PhoneticEngine.getSuggestions(composingBuffer.toString(), currentLang)
        } else {
            when (currentLang) {
                "BN" -> listOf("ভাই", "কেমন আছিস?", "হ্যাঁ", "না", "একদম", "ভালো আছি")
                "HI" -> listOf("भाई", "कैसे हो?", "हाँ", "नहीं", "अच्छा", "ठीक है")
                else -> listOf("Bhai", "Okay", "Yes", "No", "Thanks", "On my way")
            }
        }

        suggestions.forEachIndexed { index, word ->
            val chip = TextView(this).apply {
                text = word
                textSize = 15f
                setTypeface(null, if (index == 0 && composingBuffer.isNotEmpty()) Typeface.BOLD else Typeface.NORMAL)
                setTextColor(if (index == 0 && composingBuffer.isNotEmpty()) Color.parseColor(accentHex) else Color.parseColor("#E6EDF3"))
                setPadding(28, 12, 28, 12)
                background = GradientDrawable().apply {
                    setColor(Color.parseColor("#161B22"))
                    cornerRadius = 20f
                    if (index == 0 && composingBuffer.isNotEmpty()) {
                        setStroke(2, Color.parseColor(accentHex))
                    }
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { rightMargin = 14 }
                setOnClickListener {
                    vibrateKey()
                    currentInputConnection?.commitText("$word ", 1)
                    composingBuffer.clear()
                    updateSuggestions()
                }
            }
            suggestionContainer.addView(chip)
        }
    }

    private fun renderMainPanel() {
        mainPanelContainer.removeAllViews()
        when (activePanel) {
            "AI" -> renderAiPanel()
            "CLIPBOARD" -> renderClipboardPanel()
            "EMOJI" -> renderEmojiPanel()
            else -> renderQwertyKeyboard()
        }
    }

    private fun renderAiPanel() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 20)
        }

        val statusText = TextView(this).apply {
            text = "✨ Gemini AI Action (ইনপুট বক্সের লেখা বদলে দেবে):"
            textSize = 13f
            setTextColor(Color.parseColor(accentHex))
            setPadding(4, 0, 4, 16)
        }
        container.addView(statusText)

        val actions = listOf(
            Pair("📝 Fix Grammar & Spelling", "GRAMMAR"),
            Pair("🇬🇧 Translate to English", "ENGLISH"),
            Pair("🇧🇩 বাংলায় অনুবাদ কর", "BENGALI"),
            Pair("🇮🇳 हिन्दी में अनुवाद करें", "HINDI"),
            Pair("😎 Savage / Bro Rewrite", "SAVAGE"),
            Pair("🎓 Formal / College Tone", "FORMAL")
        )

        actions.chunked(2).forEach { pairRow ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 14 }
            }

            pairRow.forEach { (label, mode) ->
                val btn = Button(this).apply {
                    text = label
                    textSize = 13f
                    isAllCaps = false
                    setTextColor(Color.WHITE)
                    setPadding(18, 24, 18, 24)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#161B22"))
                        cornerRadius = 20f
                        setStroke(1, Color.parseColor(accentHex))
                    }
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        leftMargin = 6
                        rightMargin = 6
                    }
                    setOnClickListener {
                        vibrateKey()
                        runAiAction(mode, statusText)
                    }
                }
                row.addView(btn)
            }
            container.addView(row)
        }

        val backBtn = Button(this).apply {
            text = "⌨️ Back to Keyboard"
            textSize = 14f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#0B0F17"))
            background = GradientDrawable().apply {
                setColor(Color.parseColor(accentHex))
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
            setOnClickListener {
                vibrateKey()
                activePanel = "KEYS"
                renderMainPanel()
            }
        }
        container.addView(backBtn)
        mainPanelContainer.addView(container)
    }

    private fun runAiAction(mode: String, statusView: TextView) {
        val ic = currentInputConnection ?: return
        val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)?.text?.toString()
            ?: ic.getSelectedText(0)?.toString()
            ?: ""

        if (extracted.isBlank()) {
            Toast.makeText(this, "আগে বক্সে কিছু লেখ ভাই!", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("novakey_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("gemini_api_key", "") ?: ""

        statusView.text = "⏳ Gemini AI ভাবছে..."

        GeminiHelper.processText(
            apiKey = apiKey,
            inputText = extracted,
            mode = mode,
            onResult = { newText ->
                handler.post {
                    ic.beginBatchEdit()
                    ic.deleteSurroundingText(10000, 10000)
                    ic.commitText(newText, 1)
                    ic.endBatchEdit()
                    activePanel = "KEYS"
                    renderMainPanel()
                    Toast.makeText(this, "✨ AI Done!", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { err ->
                handler.post {
                    statusView.text = "✨ Gemini AI Action:"
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun renderQwertyKeyboard() {
        val rows = if (isSymbolMode) {
            listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
                listOf("@", "#", "₹", "_", "&", "-", "+", "(", ")", "/"),
                listOf("=\\<", "*", "\"", "'", ":", ";", "!", "?", "⌫")
            )
        } else {
            listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
                listOf("⇧", "z", "x", "c", "v", "b", "n", "m", "⌫")
            )
        }

        rows.forEachIndexed { rowIndex, keys ->
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }

            if (rowIndex == 1 && !isSymbolMode) {
                rowLayout.setPadding(28, 0, 28, 0)
            }

            for (rawKey in keys) {
                val isSpecial = rawKey in listOf("⇧", "⌫", "=\\<")
                val displayKey = if (!isSpecial && !isSymbolMode && (isShift || isCapsLock)) {
                    rawKey.uppercase()
                } else {
                    rawKey
                }

                val weight = if (isSpecial) 1.45f else 1f
                val keyBtn = createKeyButton(displayKey, isSpecial, weight)

                if (rawKey == "⌫") {
                    keyBtn.setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                vibrateKey()
                                performBackspace()
                                isBackspaceHeld = true
                                handler.postDelayed(backspaceRunnable, 400)
                                true
                            }
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                isBackspaceHeld = false
                                handler.removeCallbacks(backspaceRunnable)
                                true
                            }
                            else -> false
                        }
                    }
                } else {
                    keyBtn.setOnClickListener {
                        vibrateKey()
                        handleKeyPress(displayKey)
                    }
                }
                rowLayout.addView(keyBtn)
            }
            mainPanelContainer.addView(rowLayout)
        }

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val symBtn = createKeyButton(if (isSymbolMode) "ABC" else "?123", true, 1.4f).apply {
            setOnClickListener {
                vibrateKey()
                commitCurrentComposing()
                isSymbolMode = !isSymbolMode
                renderMainPanel()
            }
        }

        val commaBtn = createKeyButton(",", false, 1f).apply {
            setOnClickListener {
                vibrateKey()
                commitCurrentComposing()
                currentInputConnection?.commitText(",", 1)
            }
        }

        spaceButton = createKeyButton(
            when (currentLang) {
                "BN" -> "NovaKey • বাংলা"
                "HI" -> "NovaKey • हिन्दी"
                else -> "NovaKey • English"
            },
            false,
            4.2f
        ).apply {
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 13f
            setOnClickListener {
                vibrateKey()
                if (composingBuffer.isNotEmpty()) {
                    val top = PhoneticEngine.getSuggestions(composingBuffer.toString(), currentLang).firstOrNull()
                        ?: composingBuffer.toString()
                    currentInputConnection?.commitText("$top ", 1)
                    composingBuffer.clear()
                    updateSuggestions()
                } else {
                    currentInputConnection?.commitText(" ", 1)
                }
            }
        }

        val dotBtn = createKeyButton(".", false, 1f).apply {
            setOnClickListener {
                vibrateKey()
                commitCurrentComposing()
                currentInputConnection?.commitText(".", 1)
            }
        }

        val enterBtn = createKeyButton("↵", true, 1.5f).apply {
            background = GradientDrawable().apply {
                setColor(Color.parseColor(accentHex))
                cornerRadius = 18f
            }
            setTextColor(Color.parseColor("#0B0F17"))
            setOnClickListener {
                vibrateKey()
                commitCurrentComposing()
                val imeAction = currentInputEditorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)
                if (imeAction != null && imeAction != EditorInfo.IME_ACTION_NONE) {
                    currentInputConnection?.performEditorAction(imeAction)
                } else {
                    currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                }
            }
        }

        bottomRow.addView(symBtn)
        bottomRow.addView(commaBtn)
        bottomRow.addView(spaceButton)
        bottomRow.addView(dotBtn)
        bottomRow.addView(enterBtn)
        mainPanelContainer.addView(bottomRow)
    }

    private fun renderClipboardPanel() {
        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 520)
        }
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 8, 12, 8)
        }

        if (clipboardHistory.isEmpty()) {
            val emptyMsg = TextView(this).apply {
                text = "ক্লিপবোর্ড এখন খালি। যেকোনো কিছু কপি করলে এখানে চলে আসবে!"
                setTextColor(Color.parseColor("#8B949E"))
                gravity = Gravity.CENTER
                setPadding(24, 120, 24, 120)
            }
            list.addView(emptyMsg)
        } else {
            clipboardHistory.forEach { item ->
                val card = TextView(this).apply {
                    text = item
                    maxLines = 2
                    setTextColor(Color.WHITE)
                    textSize = 14f
                    setPadding(26, 22, 26, 22)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor("#161B22"))
                        cornerRadius = 18f
                        setStroke(1, Color.parseColor("#30363D"))
                    }
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12 }
                    setOnClickListener {
                        vibrateKey()
                        currentInputConnection?.commitText(item, 1)
                        activePanel = "KEYS"
                        renderMainPanel()
                    }
                }
                list.addView(card)
            }
        }
        scroll.addView(list)
        mainPanelContainer.addView(scroll)
    }

    private fun renderEmojiPanel() {
        val emojis = listOf(
            "😂", "❤️", "🔥", "😭", "😎", "🙏", "👍", "💀",
            "🥰", "🤔", "🤣", "💯", "✨", "😤", "🎉", "👀",
            "🥺", "💪", "⚡", "🤝", "😴", "🤯", "🥳", "💔",
            "😇", "😈", "🙌", "👏", "🚀", "🌟", "☕", "🍛"
        )
        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
        }
        emojis.chunked(8).forEach { rowEmojis ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12 }
            }
            rowEmojis.forEach { emoji ->
                val tv = TextView(this).apply {
                    text = emoji
                    textSize = 24f
                    gravity = Gravity.CENTER
                    setPadding(0, 14, 0, 14)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        vibrateKey()
                        currentInputConnection?.commitText(emoji, 1)
                    }
                }
                row.addView(tv)
            }
            grid.addView(row)
        }
        mainPanelContainer.addView(grid)
    }

    private fun handleKeyPress(key: String) {
        when (key) {
            "⇧" -> {
                if (isShift) {
                    isCapsLock = !isCapsLock
                    isShift = false
                } else if (isCapsLock) {
                    isCapsLock = false
                } else {
                    isShift = true
                }
                renderMainPanel()
            }
            "=\\<" -> {}
            else -> {
                val isLetter = key.length == 1 && key[0].isLetter()
                if (currentLang != "EN" && isLetter && !isSymbolMode) {
                    composingBuffer.append(key.lowercase())
                    val top = PhoneticEngine.getSuggestions(composingBuffer.toString(), currentLang).firstOrNull()
                        ?: composingBuffer.toString()
                    currentInputConnection?.setComposingText(top, 1)
                    updateSuggestions()
                } else {
                    commitCurrentComposing()
                    currentInputConnection?.commitText(key, 1)
                }

                if (isShift && !isCapsLock) {
                    isShift = false
                    renderMainPanel()
                }
            }
        }
    }

    private fun performBackspace() {
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            if (composingBuffer.isEmpty()) {
                currentInputConnection?.commitText("", 1)
            } else {
                val top = PhoneticEngine.getSuggestions(composingBuffer.toString(), currentLang).firstOrNull()
                    ?: composingBuffer.toString()
                currentInputConnection?.setComposingText(top, 1)
            }
            updateSuggestions()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun commitCurrentComposing() {
        if (composingBuffer.isNotEmpty()) {
            val top = PhoneticEngine.getSuggestions(composingBuffer.toString(), currentLang).firstOrNull()
                ?: composingBuffer.toString()
            currentInputConnection?.commitText(top, 1)
            composingBuffer.clear()
            updateSuggestions()
        }
    }

    private fun createKeyButton(label: String, isSpecial: Boolean, weight: Float): Button {
        val bgColor = when {
            label == "⇧" && (isShift || isCapsLock) -> accentHex
            isSpecial -> "#1E2633"
            else -> "#161B22"
        }
        val txtColor = if (label == "⇧" && (isShift || isCapsLock)) "#0B0F17" else "#F0F6FC"

        return Button(this).apply {
            text = label
            textSize = if (isSpecial) 15f else 19f
            isAllCaps = false
            setTypeface(null, if (isSpecial) Typeface.BOLD else Typeface.NORMAL)
            setTextColor(Color.parseColor(txtColor))
            setPadding(0, 26, 0, 26)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgColor))
                cornerRadius = 18f
                setStroke(1, Color.parseColor("#30363D"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, weight).apply {
                leftMargin = 5
                rightMargin = 5
            }
        }
    }

    private fun createToolPill(label: String, bgHex: String, textHex: String): Button {
        return Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor(textHex))
            setPadding(16, 10, 16, 10)
            background = GradientDrawable().apply {
                setColor(Color.parseColor(bgHex))
                cornerRadius = 20f
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = 5
                rightMargin = 5
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateKey() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(16, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator?.vibrate(16)
            }
        } catch (_: Exception) {
        }
    }
}
