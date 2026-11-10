import XCTest

/// 端到端：在「設定」加入41注音鍵盤（倚天41排列），回到 App 的輸入框切到它，用螢幕鍵盤打出「東西」。
/// 模擬器語言是繁體中文（台灣），設定裡的選項名稱照中文找。
final class KeyboardExtensionUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testTypeWithEten41KeyboardInAnotherTextField() throws {
        try addEten41KeyboardInSettings()

        let app = XCUIApplication()
        app.launch()
        let field = app.textFields["typedKeysField"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()

        let firstKey = app.buttons["key-d"]
        try switchToEten41Keyboard(in: app, until: firstKey)
        snapshot(app, "eten41-keyboard-shown")

        for id in ["key-d", "key-x", "key--", "spaceKey", "key-c", "key-e", "key-1"] {
            let button = app.buttons[id]
            XCTAssertTrue(button.waitForExistence(timeout: 3), "找不到 \(id)")
            button.tap()
        }
        // 點組字區第 1 個字「東」，候選列換成它的候選，選「冬」
        let firstCharacter = app.buttons["composing-0"]
        XCTAssertTrue(firstCharacter.waitForExistence(timeout: 3), "找不到組字區的字")
        firstCharacter.tap()
        let winter = app.buttons.matching(NSPredicate(format: "label == '冬'")).firstMatch
        XCTAssertTrue(winter.waitForExistence(timeout: 3), "候選列沒有「冬」")
        snapshot(app, "focused-candidates")
        winter.tap()
        app.buttons["returnKey"].tap()
        XCTAssertEqual(field.value as? String, "冬西")

        // 標點：⇧＋, 打出「，」；符號面板點「、」
        for id in ["shiftKey", "key-,", "returnKey", "symbolKey", "symbol-、"] {
            let button = app.buttons[id]
            XCTAssertTrue(button.waitForExistence(timeout: 3), "找不到 \(id)")
            button.tap()
        }
        snapshot(app, "after-typing")
        XCTAssertEqual(field.value as? String, "冬西，、")
    }

    /// 鍵盤已經加好（例如用 defaults 寫入 AppleKeyboards）時，只拍候選列：
    /// iOS 27 實機回報候選列被模糊效果蓋住，App 內預覽頁不會，只有鍵盤擴充會
    func testCandidateBarInKeyboardExtension() throws {
        let app = XCUIApplication()
        app.launch()
        let field = app.textFields["typedKeysField"]
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        try switchToEten41Keyboard(in: app, until: app.buttons["key-d"])
        for id in ["key-d", "key-x", "key--", "spaceKey", "key-c", "key-e", "key-1"] {
            let button = app.buttons[id]
            XCTAssertTrue(button.waitForExistence(timeout: 3), "找不到 \(id)")
            button.tap()
        }
        XCTAssertTrue(app.buttons["candidate-0"].waitForExistence(timeout: 3), "候選列沒有候選")
        snapshot(app, "extension-candidates")
    }

    private func addEten41KeyboardInSettings() throws {
        let settings = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        settings.terminate()
        settings.launch()
        tapCell(settings, "一般")
        tapCell(settings, "鍵盤")
        // 「鍵盤」頁裡第一列也叫「鍵盤」（顯示目前幾個）
        let keyboardsRow = settings.cells.containing(.staticText, identifier: "鍵盤").firstMatch
        XCTAssertTrue(keyboardsRow.waitForExistence(timeout: 5))
        keyboardsRow.tap()
        if settings.staticTexts["41注音"].waitForExistence(timeout: 2) {
            return  // 之前已加過
        }
        tapCell(settings, "新增鍵盤")
        snapshot(settings, "add-keyboard-list")
        // iOS 27 的「新增鍵盤」是長清單＋搜尋框，第三方鍵盤在最底下；用搜尋縮小（打 ASCII 才不受測試鍵盤影響）
        let search = settings.searchFields.firstMatch
        if search.waitForExistence(timeout: 2) {
            search.tap()
            search.typeText("41")
            snapshot(settings, "after-search")
        }
        let thirdParty = settings.cells.containing(NSPredicate(format: "label CONTAINS '41注音'")).firstMatch
        XCTAssertTrue(thirdParty.waitForExistence(timeout: 5), "第三方鍵盤清單裡找不到41注音")
        thirdParty.tap()
        snapshot(settings, "after-add")
    }

    private func tapCell(_ app: XCUIApplication, _ text: String) {
        let predicate = NSPredicate(format: "label BEGINSWITH %@", text)
        let cell = app.cells.containing(predicate).firstMatch
        if cell.waitForExistence(timeout: 5) {
            cell.tap()
            return
        }
        let anything = app.descendants(matching: .any).matching(predicate).firstMatch
        XCTAssertTrue(anything.waitForExistence(timeout: 3), "設定裡找不到「\(text)」")
        anything.tap()
    }

    /// 切到41注音：先長按地球鍵開鍵盤清單直接點（使用者的做法）；叫不出清單才輪流按地球鍵。
    /// 鍵盤多的時候，連按地球鍵只會在最近用過的兩個鍵盤之間來回，繞不到排在後面的倚天41。
    private func switchToEten41Keyboard(in app: XCUIApplication, until target: XCUIElement) throws {
        if target.waitForExistence(timeout: 2) { return }
        // 地球鍵不在 app.keyboards 底下，是跟鍵盤並列的按鈕
        let globe = app.buttons.matching(
            NSPredicate(format: "label IN %@", ["下一個鍵盤", "Next keyboard", "地球", "切換鍵盤"])).firstMatch
        guard globe.waitForExistence(timeout: 3) else {
            XCTFail("找不到地球鍵")
            return
        }
        globe.press(forDuration: 1.2)
        let menuItem = app.descendants(matching: .any)
            .matching(NSPredicate(format: "label == '41注音' OR label BEGINSWITH '41注音,' OR label BEGINSWITH '41注音，'")).firstMatch
        snapshot(app, "globe-menu")
        if menuItem.waitForExistence(timeout: 3) {
            menuItem.tap()
            if target.waitForExistence(timeout: 3) { return }
        }
        for attempt in 0..<8 {
            if target.waitForExistence(timeout: 2) { return }
            snapshot(app, "switch-attempt-\(attempt)")
            guard globe.exists else { break }
            globe.tap()
        }
        XCTAssertTrue(target.waitForExistence(timeout: 3), "切不到41注音鍵盤")
    }

    private func snapshot(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
