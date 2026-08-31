package jp.pgw.lab78.androrm.database.support

import jp.pgw.lab78.androrm.database.entities.select.EmployeeEntity
import jp.pgw.lab78.androrm.database.reference.ColumnRef
import jp.pgw.lab78.androrm.database.reference.TableRef

/**
 * ## テストサポートクラス
 * ### テストコードで使用する共通関数を定義するクラス
 * @author Masahiro Inoue
 * @since 2026-05-13
 */
object SupportOperation {
    /**
     * ## EmployeeEntity プロパティ取得関数
     * ### EmployeeEntity クラスプロパティを文字列で指定して KProperty1 オブジェクトを取得する
     * @param propertyName 取得したいプロパティの名前を文字列で指定する
     * @return 指定されたプロパティに対応する KProperty1 オブジェクトを返す
     * @throws IllegalStateException 指定されたプロパティ名が EmployeeEntity クラスのプロパティに存在しない場合
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    fun changeProperty(
        propertyName: String,
    ) =
        when (propertyName) {
            "employeeId" -> EmployeeEntity::employeeId
            "EmployeeEntity::employeeSubId",
            "employeeSubId" -> EmployeeEntity::employeeSubId

            "name" -> EmployeeEntity::name
            else -> error("Unknown EmployeeEntity property: $propertyName")
        }

    /**
     * ## EmployeeEntity カラム参照取得関数
     * ### EmployeeEntity クラスプロパティを文字列で指定して ColumnRef オブジェクトを取得する
     * @param propertyName 取得したいプロパティの名前を文字列で指定する
     * @return 指定されたプロパティに対応する ColumnRef オブジェクトを返す
     * @throws IllegalStateException 指定されたプロパティ名が EmployeeEntity クラスのプロパティに存在しない場合
     * @author Masahiro Inoue
     * @since 2026-05-13
     */
    fun changeColumnRef(
        propertyName: String,
    ): ColumnRef<EmployeeEntity, Any> =
        when (propertyName) {
            "EmployeeEntity::employeeId" -> ColumnRef(
                TableRef(EmployeeEntity::class, "EMP"),
                EmployeeEntity::employeeId
            )

            else -> error("Unknown EmployeeEntity property: $propertyName")
        }
}