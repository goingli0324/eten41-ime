package com.muzigoing.eten41ime.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView

/**
 * 倚天41 螢幕鍵盤：上面候選列（組字區＋候選字），中間實體鍵盤 4 排（或符號面板），下面功能列。
 * 版面與 iOS 版 Shared/KeyboardView.swift 相同。每個鍵設 contentDescription（TalkBack 與 UI 測試共用）。
 */
class KeyboardView(
    context: Context,
    private val session: KeyboardSession,
    private val showsGlobeKey: Boolean = false,
    private val onGlobe: () -> Unit = {},
    /** 接了實體鍵盤：只顯示候選列（組字區＋候選字），不顯示螢幕鍵盤 */
    private val candidatesOnly: Boolean = false,
) : LinearLayout(context) {

    private val palette = Palette.forContext(context)
    private val candidateStrip = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(8), 0, dp(8), 0)
    }
    private val keyArea = LinearLayout(context).apply { orientation = VERTICAL }
    private val bottomRow = LinearLayout(context).apply { orientation = HORIZONTAL }

    init {
        orientation = VERTICAL
        setBackgroundColor(palette.background)
        setPadding(dp(3), dp(4), dp(3), dp(6))
        addView(
            HorizontalScrollView(context).apply {
                isHorizontalScrollBarEnabled = false
                addView(candidateStrip)
            },
            LayoutParams(LayoutParams.MATCH_PARENT, dp(CANDIDATE_BAR_DP)),
        )
        if (!candidatesOnly) {
            addView(keyArea, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            addView(bottomRow, LayoutParams(LayoutParams.MATCH_PARENT, dp(ROW_DP)).withMargins())
        }
        session.onChange = { render() }
        render()
        // Android 15 起輸入法視窗延伸到螢幕最底，系統把「收鍵盤／切換輸入法」按鈕畫在上面；
        // 不讓開的話最下排（換行、⇧）會被系統按鈕蓋住，點到的是系統的切換輸入法
        val basePaddingBottom = paddingBottom
        setOnApplyWindowInsetsListener { view, insets ->
            val navigationBottom = if (android.os.Build.VERSION.SDK_INT >= 30) {
                insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom
            } else {
                @Suppress("DEPRECATION") insets.systemWindowInsetBottom
            }
            view.setPadding(paddingLeft, paddingTop, paddingRight, basePaddingBottom + navigationBottom)
            insets
        }
    }

    private fun render() {
        renderCandidateStrip()
        if (candidatesOnly) return
        keyArea.removeAllViews()
        if (session.isShowingSymbols) renderSymbolPanel() else renderKeyRows()
        renderBottomRow()
    }

    // ---- 候選列 ----

    private fun renderCandidateStrip() {
        candidateStrip.removeAllViews()
        session.composingCharacters.forEachIndexed { index, character ->
            val focused = session.focusedIndex == index
            candidateStrip.addView(textItem(character, 20f).apply {
                setPadding(dp(3), dp(2), dp(3), dp(2))
                background = roundRect(if (focused) palette.focus else Color.TRANSPARENT)
                paintFlags = paintFlags or android.graphics.Paint.UNDERLINE_TEXT_FLAG
                contentDescription = "組字區 $character"
                isSelected = focused
                setOnClickListener { session.focusCharacter(index) }
            })
        }
        if (session.pendingBopomofo.isNotEmpty()) {
            candidateStrip.addView(textItem(session.pendingBopomofo, 20f).apply { setTextColor(palette.secondaryText) })
        }
        if (session.composingText.isNotEmpty()) {
            candidateStrip.addView(View(context).apply { setBackgroundColor(palette.secondaryText) },
                LayoutParams(dp(1), dp(22)).apply { setMargins(dp(10), 0, dp(10), 0) })
        }
        session.candidates.forEach { candidate ->
            candidateStrip.addView(textItem(candidate.text, 20f).apply {
                setPadding(dp(8), dp(4), dp(8), dp(4))
                contentDescription = candidate.text
                setOnClickListener { session.choose(candidate) }
            })
        }
    }

    // ---- 鍵盤 4 排 ----

    private fun renderKeyRows() {
        Eten41Layout.rows.forEachIndexed { index, row ->
            val isLast = index == Eten41Layout.rows.lastIndex
            val units = row.size + if (isLast) 1.5f else 0f
            keyArea.addView(keyRow(units) {
                row.forEach { key -> addView(characterKey(key), keyParams(1f)) }
                if (isLast) addView(functionKey("⌫", "刪除", session::tapBackspace), keyParams(1.5f))
            }, rowParams())
        }
    }

    /** 符號面板佔原本 4 排：3 排標點＋最後一排只有刪除鍵 */
    private fun renderSymbolPanel() {
        Eten41Layout.punctuationRows.forEach { row ->
            // 標點 7 個一排，平分整排寬度（同 iOS）
            keyArea.addView(keyRow(row.size.toFloat(), total = row.size.toFloat()) {
                row.forEach { symbol ->
                    addView(keycap(symbol.label, symbol.label, palette.key, 22f) { session.tapSymbol(symbol) }, keyParams(1f))
                }
            }, rowParams())
        }
        keyArea.addView(keyRow(12f) {
            addView(Space(context), keyParams(10.5f))
            addView(functionKey("⌫", "刪除", session::tapBackspace), keyParams(1.5f))
        }, rowParams())
    }

    /** 一排鍵：不足 total 格的兩側補空白置中，讓每個鍵寬度一致（主鍵盤以 12 格為準） */
    private fun keyRow(units: Float, total: Float = 12f, content: LinearLayout.() -> Unit): LinearLayout =
        LinearLayout(context).apply {
            orientation = HORIZONTAL
            weightSum = total
            val side = ((total - units) / 2f).coerceAtLeast(0f)
            if (side > 0f) addView(Space(context), keyParams(side))
            content()
            if (side > 0f) addView(Space(context), keyParams(side))
        }

    private fun characterKey(key: Eten41Key): TextView {
        val label = keycapLabel(key)
        return keycap(label, label, palette.key, 20f) { session.tap(key) }
    }

    /** 倚天41 使用者看英數鍵位打字：中文模式字母大寫；⇧ 時印這一鍵會打出的東西 */
    private fun keycapLabel(key: Eten41Key): String = when {
        session.isEnglishMode && session.isShifted -> key.englishShiftedText
        session.isEnglishMode -> key.ascii.toString()
        session.isShifted -> key.shiftedFullWidth ?: key.ascii.uppercase()
        else -> key.ascii.uppercase()
    }

    // ---- 功能列 ----

    private fun renderBottomRow() {
        bottomRow.removeAllViews()
        bottomRow.weightSum = 0f
        bottomRow.addView(functionKey(if (session.isEnglishMode) "英" else "中", "中英切換", session::toggleEnglishMode), keyParams(1.5f))
        bottomRow.addView(functionKey(if (session.isShifted) "⬆" else "⇧", "Shift", session::toggleShift, active = session.isShifted), keyParams(1.5f))
        bottomRow.addView(functionKey(if (session.isShowingSymbols) "返回" else "符", "標點符號", session::toggleSymbols, active = session.isShowingSymbols), keyParams(1.5f))
        if (showsGlobeKey) bottomRow.addView(functionKey("🌐", "下一個鍵盤", onGlobe), keyParams(1.5f))
        bottomRow.addView(functionKey(if (session.isEnglishMode) "space" else "一聲／空白", "空白", session::tapSpace), keyParams(5f))
        bottomRow.addView(functionKey("⏎", "換行", session::tapReturn), keyParams(1.5f))
    }

    // ---- 元件 ----

    private fun keycap(label: String, description: String, color: Int, sizeSp: Float, onTap: () -> Unit): TextView =
        textItem(label, sizeSp).apply {
            gravity = Gravity.CENTER
            background = pressable(color)
            contentDescription = description
            setOnClickListener { onTap() }
        }

    private fun functionKey(label: String, description: String, onTap: () -> Unit, active: Boolean = false): TextView =
        keycap(label, description, if (active) palette.key else palette.functionKey, 15f, onTap).apply { isSelected = active }

    private fun textItem(text: String, sizeSp: Float) = TextView(context).apply {
        this.text = text
        setTextColor(palette.text)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        typeface = Typeface.DEFAULT
    }

    private fun keyParams(weight: Float) =
        LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply { setMargins(dp(2), 0, dp(2), 0) }

    private fun rowParams() = LayoutParams(LayoutParams.MATCH_PARENT, dp(ROW_DP)).withMargins()

    private fun LayoutParams.withMargins() = apply { setMargins(0, dp(3), 0, dp(3)) }

    private fun roundRect(color: Int) = GradientDrawable().apply {
        cornerRadius = dp(5).toFloat()
        setColor(color)
    }

    private fun pressable(color: Int) = StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_pressed), roundRect(palette.pressed))
        addState(intArrayOf(), roundRect(color))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    /** 淺色／深色兩組顏色，跟著系統 */
    private data class Palette(
        val background: Int, val key: Int, val functionKey: Int, val pressed: Int,
        val text: Int, val secondaryText: Int, val focus: Int,
    ) {
        companion object {
            fun forContext(context: Context): Palette {
                val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
                return if (night) {
                    Palette(0xFF1C1C1E.toInt(), 0xFF5A5A5E.toInt(), 0xFF3A3A3C.toInt(), 0xFF7A7A7E.toInt(),
                        Color.WHITE, 0xFFAAAAAA.toInt(), 0x663A82F7)
                } else {
                    Palette(0xFFD7D9DE.toInt(), Color.WHITE, 0xFFADB2BC.toInt(), 0xFFC8CBD1.toInt(),
                        Color.BLACK, 0xFF6E6E73.toInt(), 0x403A82F7)
                }
            }
        }
    }

    companion object {
        const val ROW_DP = 48
        const val CANDIDATE_BAR_DP = 44
    }
}
