import SwiftUI

private extension View {
    /// iOS 26 起捲動區碰到容器邊緣會加模糊漸層；在鍵盤擴充最上緣會把候選列整排糊掉（iOS 27 實機回報）
    @ViewBuilder
    func hidingScrollEdgeEffect() -> some View {
        if #available(iOS 26.0, *) {
            scrollEdgeEffectHidden(true, for: .all)
        } else {
            self
        }
    }
}

/// 倚天41 螢幕鍵盤：上面是組字區＋候選字，中間是實體鍵盤 4 排（或符號面板），下面是功能列。
struct KeyboardView: View {
    @ObservedObject var session: KeyboardSession
    /// 鍵盤擴充在需要時才顯示地球鍵（iPhone 有實體 Home 鍵或系統要求時）
    var showsGlobeKey = false
    var onGlobe: () -> Void = {}

    static let keySpacing: CGFloat = 4
    static let rowHeight: CGFloat = 46
    static let candidateBarHeight: CGFloat = 40
    static let totalHeight: CGFloat = candidateBarHeight + rowHeight * 5 + keySpacing * 6

    var body: some View {
        GeometryReader { geometry in
            let keyWidth = (geometry.size.width - Self.keySpacing * 13) / 12
            VStack(spacing: Self.keySpacing) {
                candidateBar
                if session.isShowingSymbols {
                    symbolPanel(totalWidth: geometry.size.width, keyWidth: keyWidth)
                } else {
                    keyRows(keyWidth: keyWidth)
                }
                bottomRow(keyWidth: keyWidth, totalWidth: geometry.size.width)
            }
            .padding(.vertical, Self.keySpacing)
            .frame(width: geometry.size.width)
        }
        .frame(height: Self.totalHeight)
        .background(Color(uiColor: .systemGray5))
    }

    private var candidateBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 14) {
                composingArea
                ForEach(Array(session.candidates.enumerated()), id: \.element) { index, candidate in
                    Button(candidate.text) { session.choose(candidate) }
                        .font(.title3)
                        .foregroundStyle(.primary)
                        .accessibilityIdentifier("candidate-\(index)")
                }
            }
            .padding(.horizontal, 8)
        }
        .hidingScrollEdgeEffect()
        .frame(height: Self.candidateBarHeight)
    }

    /// 組字區：每個已轉好的字都能點（叫出那個字的候選），點中的字加底色；後面接還在打的注音
    @ViewBuilder
    private var composingArea: some View {
        if !session.composingText.isEmpty {
            HStack(spacing: 0) {
                ForEach(Array(session.composingCharacters.enumerated()), id: \.offset) { index, character in
                    let isFocused = session.focusedIndex == index
                    Button {
                        session.focusCharacter(at: index)
                    } label: {
                        Text(character)
                            .font(.title3)
                            .padding(.horizontal, 2)
                            .padding(.vertical, 4)
                            .background(isFocused ? Color.accentColor.opacity(0.25) : Color.clear,
                                        in: RoundedRectangle(cornerRadius: 4))
                            .overlay(alignment: .bottom) {
                                Rectangle().frame(height: 1).foregroundStyle(.secondary.opacity(0.5))
                            }
                            .foregroundStyle(.primary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("composing-\(index)")
                    .accessibilityLabel(character)
                    .accessibilityAddTraits(isFocused ? .isSelected : [])
                }
                if !session.pendingBopomofo.isEmpty {
                    Text(session.pendingBopomofo)
                        .font(.title3)
                        .foregroundStyle(.secondary)
                        .padding(.leading, 2)
                }
            }
            // 不要在這層掛 accessibilityIdentifier：SwiftUI 會蓋掉每個字自己的 composing-N
            Divider().frame(height: 24)
        }
    }

    private func keyRows(keyWidth: CGFloat) -> some View {
        ForEach(Array(Eten41Layout.rows.enumerated()), id: \.offset) { index, row in
            HStack(spacing: Self.keySpacing) {
                ForEach(row, id: \.self) { key in
                    characterKey(key, width: keyWidth)
                }
                if index == Eten41Layout.rows.count - 1 {
                    backspaceKey(width: keyWidth * 1.5)
                }
            }
        }
    }

    /// 符號面板佔原本 4 排的高度：3 排標點＋最後一排只有刪除鍵
    private func symbolPanel(totalWidth: CGFloat, keyWidth: CGFloat) -> some View {
        let columns = CGFloat(Eten41Layout.punctuationRows.map(\.count).max() ?? 7)
        let symbolWidth = (totalWidth - Self.keySpacing * (columns + 1)) / columns
        return Group {
            ForEach(Array(Eten41Layout.punctuationRows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: Self.keySpacing) {
                    ForEach(row, id: \.self) { symbol in
                        Button {
                            session.tapSymbol(symbol)
                        } label: {
                            Text(symbol.label)
                                .font(.system(size: 22))
                                .frame(width: symbolWidth, height: Self.rowHeight)
                                .background(Color(uiColor: .systemBackground), in: RoundedRectangle(cornerRadius: 5))
                                .foregroundStyle(.primary)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("symbol-\(symbol.label)")
                        .accessibilityLabel(symbol.label)
                    }
                }
            }
            HStack {
                Spacer()
                backspaceKey(width: keyWidth * 1.5)
            }
            .padding(.trailing, Self.keySpacing)
        }
    }

    private func characterKey(_ key: Eten41Key, width: CGFloat) -> some View {
        Button {
            session.tap(key)
        } label: {
            // 倚天41的使用者看英數鍵位打字，鍵帽只印英數（中文模式字母大寫，同實體鍵盤）；
            // ⇧ 按下時改印這一鍵會打出的東西（中文模式是全形標點）
            Text(keycapLabel(key))
                .font(.system(size: 20))
                .frame(width: width, height: Self.rowHeight)
                .background(Color(uiColor: .systemBackground), in: RoundedRectangle(cornerRadius: 5))
                .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("key-\(key.ascii)")
        .accessibilityLabel(keycapLabel(key))
    }

    private func keycapLabel(_ key: Eten41Key) -> String {
        switch (session.isEnglishMode, session.isShifted) {
        case (true, false): return String(key.ascii)
        case (true, true): return key.englishShiftedText
        case (false, false): return String(key.ascii).uppercased()
        case (false, true): return key.shiftedFullWidth ?? String(key.ascii).uppercased()
        }
    }

    private func backspaceKey(width: CGFloat) -> some View {
        functionKey(systemImage: "delete.left", label: "刪除", id: "backspaceKey",
                    width: width, action: session.tapBackspace)
    }

    private func functionKey(systemImage: String? = nil, title: String? = nil, label: String, id: String,
                             width: CGFloat, isActive: Bool = false,
                             action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Group {
                if let systemImage {
                    Image(systemName: systemImage)
                } else {
                    Text(title ?? label).font(.system(size: 15))
                }
            }
            .frame(width: width, height: Self.rowHeight)
            .background(Color(uiColor: isActive ? .systemBackground : .systemGray3),
                        in: RoundedRectangle(cornerRadius: 5))
            .foregroundStyle(.primary)
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(id)
        .accessibilityLabel(label)
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }

    private func bottomRow(keyWidth: CGFloat, totalWidth: CGFloat) -> some View {
        let sideWidth = keyWidth * 1.5
        let sideKeyCount: CGFloat = showsGlobeKey ? 5 : 4
        let spaceWidth = totalWidth - Self.keySpacing * (sideKeyCount + 2) - sideWidth * sideKeyCount
        return HStack(spacing: Self.keySpacing) {
            functionKey(title: session.isEnglishMode ? "英" : "中", label: "中英切換", id: "modeKey",
                        width: sideWidth, action: session.toggleEnglishMode)
            functionKey(systemImage: session.isShifted ? "shift.fill" : "shift", label: "Shift", id: "shiftKey",
                        width: sideWidth, isActive: session.isShifted, action: session.toggleShift)
            functionKey(title: session.isShowingSymbols ? "返回" : "符", label: "標點符號", id: "symbolKey",
                        width: sideWidth, isActive: session.isShowingSymbols, action: session.toggleSymbols)
            if showsGlobeKey {
                functionKey(systemImage: "globe", label: "下一個鍵盤", id: "globeKey",
                            width: sideWidth, action: onGlobe)
            }
            functionKey(title: session.isEnglishMode ? "space" : "一聲／空白", label: "空白", id: "spaceKey",
                        width: max(spaceWidth, keyWidth), action: session.tapSpace)
            functionKey(systemImage: "return", label: "換行", id: "returnKey",
                        width: sideWidth, action: session.tapReturn)
        }
    }
}
