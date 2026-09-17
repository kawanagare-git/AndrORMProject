package jp.pgw.lab78.androrm.ksp.projectoin

import jp.pgw.lab78.androrm.common.EntityConstants.DMLInterfaceEnum
import jp.pgw.lab78.androrm.common.annotation.ColumnProjection
import jp.pgw.lab78.androrm.common.annotation.FunctionProjection

/**
 * ## プロジェクション定義クラス
 * ### @Projection アノテーションの引数を格納するためのデータクラス
 * @param entityNameExtend エンティティ名の拡張部分
 * @param aliasExtend エイリアスの拡張部分（省略可能）
 * @param properties プロパティのリスト
 * @param functions 関数のリスト
 * @param andrOrmSubPackages 共通インターフェースのリスト
 * @param customSubPackages カスタム生成先サブパッケージのリスト
 * @author Masahiro Inoue
 * @since 2026-04-17
 */
data class ProjectionDefinition(
    val entityNameExtend: String,
    val aliasExtend: String = "",
    val properties: List<ColumnProjection> = emptyList(),
    val functions: List<FunctionProjection> = emptyList(),
    val andrOrmSubPackages: List<DMLInterfaceEnum> = emptyList(),
    val customSubPackages: List<String> = emptyList(),
)
