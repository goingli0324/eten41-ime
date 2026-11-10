/// 畫面與測試共用的固定案例：倚天41按鍵串 → 預期上屏的字。
struct SmokeCase: Identifiable {
    let keys: String
    let reading: String
    let expected: String
    var id: String { keys }
}

let smokeCases: [SmokeCase] = [
    SmokeCase(keys: ",x- x92", reading: "ㄓㄨㄥ ㄨㄣˊ", expected: "中文"),
    SmokeCase(keys: ",x4e9 ", reading: "ㄓㄨˋ ㄧㄣ", expected: "注音"),
    SmokeCase(keys: "e3te8 ", reading: "ㄧˇ ㄊㄧㄢ", expected: "倚天"),
    SmokeCase(keys: "/x jx4fa3", reading: "ㄕㄨ ㄖㄨˋ ㄈㄚˇ", expected: "輸入法"),
]
