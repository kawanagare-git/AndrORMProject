package jp.pgw.lab78.androrm.database

import jp.pgw.lab78.androrm.common.dml.interfaces.Entity

/**
 * ## 文字列MapとEntityの相互変換Mapper契約
 * ### KSPが生成したEntity専用のMap転送処理を公開する
 * ### MapのキーはEntityのDBカラム名を使用する
 * @param T 変換対象のEntity型
 * @author Masahiro Inoue
 * @since 2026-09-18
 */
interface StringMapEntityMapper<T : Entity> {
    /**
     * ## MapからEntityへ変換
     * ### Entityに存在しないMapキーは、ignoreUnknownColumns=falseの場合はエラー、trueの場合は無視する
     * @param row 1Entity分のカラム名と文字列値
     * @param ignoreUnknownColumns Entityに存在しないMapキーを無視する場合true。デフォルトfalse
     * @return 生成されたEntity
     */
    fun fromMap(
        row: Map<String, String?>,
        ignoreUnknownColumns: Boolean = false,
    ): T

    /**
     * ## EntityからMapへ変換
     * ### Entityのコンストラクタ定義順にDBカラム名と文字列値へ変換する
     * @param entity 変換元Entity
     * @return DBカラム名と文字列値のMap
     */
    fun toMap(entity: T): Map<String, String?>

    /**
     * ## Map一覧からEntity一覧へ変換
     * @param rows 変換元Map一覧
     * @param ignoreUnknownColumns Entityに存在しないMapキーを無視する場合true。デフォルトfalse
     * @return 生成されたEntity一覧
     */
    fun fromMapList(
        rows: List<Map<String, String?>>,
        ignoreUnknownColumns: Boolean = false,
    ): List<T> = rows.map { row ->
        fromMap(row, ignoreUnknownColumns)
    }

    /**
     * ## Entity一覧からMap一覧へ変換
     * @param entities 変換元Entity一覧
     * @return DBカラム名と文字列値のMap一覧
     */
    fun toMapList(entities: List<T>): List<Map<String, String?>> =
        entities.map(::toMap)
}
