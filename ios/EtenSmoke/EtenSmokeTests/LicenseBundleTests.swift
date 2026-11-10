import XCTest
@testable import EtenSmoke

/// LGPL 散布義務：App 裡要帶 libchewing 的授權全文與作者名單
final class LicenseBundleTests: XCTestCase {
    func testLicenseFilesAreBundled() throws {
        let directory = Bundle.main.bundleURL.appendingPathComponent("licenses")
        let copying = try String(contentsOf: directory.appendingPathComponent("libchewing-COPYING.txt"), encoding: .utf8)
        XCTAssertTrue(copying.contains("GNU LESSER GENERAL PUBLIC LICENSE"))
        XCTAssertTrue(copying.contains("Version 2.1"))
        let authors = try String(contentsOf: directory.appendingPathComponent("libchewing-AUTHORS.txt"), encoding: .utf8)
        XCTAssertFalse(authors.isEmpty)
    }
}
