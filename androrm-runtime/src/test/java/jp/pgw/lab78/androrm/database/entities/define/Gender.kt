package jp.pgw.lab78.androrm.database.entities.define

/**
 * テストデータで使用する性別を表す列挙型。
 * @author Masahiro Inoue
 * @since 2026-05-30
 */
enum class Gender {
    /** 男性 */
    MALE,

    /** 女性 */
    FEMALE,

    /** その他 */
    OTHER,

    /** 未回答 */
    UNSPECIFIED,
}
