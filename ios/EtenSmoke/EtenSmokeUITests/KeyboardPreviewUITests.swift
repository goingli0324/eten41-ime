import XCTest

/// App 內「試用鍵盤」頁：打幾個字，把候選列拍下來（看候選字有沒有被系統效果遮住）
final class KeyboardPreviewUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testCandidateBarIsVisibleWhileTyping() {
        let app = XCUIApplication()
        app.launch()
        let open = app.buttons["openKeyboardPreview"]
        XCTAssertTrue(open.waitForExistence(timeout: 10))
        open.tap()

        for id in ["key-d", "key-x", "key--", "spaceKey", "key-c", "key-e", "key-1"] {
            let button = app.buttons[id]
            XCTAssertTrue(button.waitForExistence(timeout: 5), "找不到 \(id)")
            button.tap()
        }
        let firstCandidate = app.buttons["candidate-0"]
        XCTAssertTrue(firstCandidate.waitForExistence(timeout: 3), "候選列沒有候選")
        XCTAssertTrue(firstCandidate.isHittable, "候選字點不到")

        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = "preview-candidates"
        attachment.lifetime = .keepAlways
        add(attachment)
    }
}
