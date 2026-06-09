package jp.pgw.lab78.androrm.common

/**
 * ## メッセージ定数
 * ### 使用するエラー・警告メッセージを定義する
 * - app 用 Error   : AE00001～
 * - app 用 Warning : AW00001～
 * - androrm-common 用 Error   : CE00001～
 * - androrm-common 用 Warning : CW00001～
 * @author Masahiro Inoue
 * @since 2026-05-23
 */
object MessageConstants {
    /**
     * ## AE00001
     * ### テーブル名を決定できない場合
     */
    const val AE00001 = "Unable to determine table name"

    /**
     * ## AE00002
     * ### raw 条件式内のプレースホルダ数とバインド値数が一致しない場合
     *
     * 第1引数: 条件式文字列
     * 第2引数: プレースホルダ数
     * 第3引数: バインド値数
     */
    const val AE00002 =
        "The number of placeholders '?' and bind values does not match. " +
                "text='%s', placeholders=%d, values=%d"

    /**
     * ## AE00003
     * ### 同一 Select 内でテーブルエイリアスが重複した場合
     *
     * 第1引数: テーブルエイリアス
     * 第2引数: テーブル名
     */
    const val AE00003 =
        "Duplicate table alias '%s' was detected. " +
                "tableName='%s'. " +
                "Use a different alias with table(..., alias = \"...\")."

    /**
     * ## AE00004
     * ### limitValue に 0 未満の値が指定された場合
     */
    const val AE00004 = "limitValue must be greater than or equal to 0."

    /**
     * ## AE00005
     * ### offsetValue に 0 未満の値が指定された場合
     */
    const val AE00005 = "offsetValue must be greater than or equal to 0."

    /**
     * ## AE00006
     * ### FunctionProjection 用プロパティに functionType が設定されていない場合
     *
     * 第1引数: プロパティ名
     */
    const val AE00006 = "Function type is missing for property '%s'."

    /**
     * ## AE00007
     * ### Entity クラスのプライマリコンストラクタを取得できない場合
     *
     * 第1引数: Entity クラス名、または完全修飾名
     */
    const val AE00007 = "No primary constructor for %s"

    /**
     * ## AE00008
     * ### コンストラクタ引数に対応するプロパティが Entity クラスに存在しない場合
     *
     * 第1引数: プロパティ名
     * 第2引数: Entity クラス完全修飾名
     */
    const val AE00008 = "Property '%s' is not declared in %s"

    /**
     * ## AE00009
     * ### Kotlin 型から SQLite 型への変換対象外の型が指定された場合
     *
     * 第1引数: 対象型、または対象値
     */
    const val AE00009 = "Unsupported type: %s"

    /**
     * ## AE00010
     * ### QueryBuilder 系メソッドが同一インスタンス内で重複指定された場合
     *
     * 第1引数: 所有クラス名
     * 第2引数: メソッド名
     */
    const val AE00010 = "%s.%s() has already been specified."

    /**
     * ## AE00011
     * ### Insert に使用する entity が設定されていない場合
     */
    const val AE00011 = "Insert requires at least one value."

    /**
     * ## AE00012
     * ### Update の更新値 Entity が指定されていない場合
     */
    const val AE00012 = "Update requires an entity."

    /**
     * ## AE00013
     * ### Update で WHERE 条件が指定されていない場合
     */
    const val AE00013 = "Update requires a WHERE condition."

    /**
     * ## AE00014
     * ### Update の SET 句が指定されていない場合
     */
    const val AE00014 = "Update requires at least one SET assignment."

    /**
     * ## AE00015
     * ### Update の join が from 指定前に呼び出された場合
     */
    const val AE00015 = "Update.join() requires from() before join()."

    /**
     * ## AE00016
     * ### Upsert 対象 Entity が指定されていない場合
     */

    const val AE00016 = "Upsert requires at least one value."

    /**
     * ## AE00017
     * ### Upsert の ON CONFLICT 対象カラムが指定されていない場合
     */
    const val AE00017 = "Upsert requires at least one conflict column."

    /**
     * ## AE00018
     * ### Upsert の DO UPDATE SET 対象カラムが指定されていない場合
     */
    const val AE00018 = "Upsert requires at least one update column."

    /**
     * ## AE00019
     * ### 対象プロパティが指定されていない場合
     */
    const val AE00019 = "Index requires at least one property."

    /**
     * ## AE00020
     * ### Index / Unique の索引名が重複している場合
     *
     * 第1引数: 重複した索引名
     */
    const val AE00020 = "Duplicate index name: %s"

    /**
     * ## AE00021
     * ### resolveColumnMappings メソッド内で使用されたエンティティが存在しない場合
     *
     * 第1引数: 存在し無いエンティティ
     */
    const val AE00021 = "Entity specified in the resolveColumnMappings method was not found: %s"

    /**
     * ## AE00022
     * ### resolveColumnMappings メソッド内で使用されたカラムが存在しない場合
     *
     * 第1引数: 存在し無いカラム
     */
    const val AE00022 = "Column mapping does not exist: %s"

    /**
     * ## AE00023
     * ### セレクト句に使用するバインド変数に null が設定されている場合
     *
     * 第1引数: 該当インデックス
     */
    const val AE00023 = "Null bind value is not supported for executeSelect. index=%s"

    /**
     * ## AE00024
     * ### セレクト句に使用するバインド変数に BLOB(ByteArray) が設定されている場合
     *
     * 第1引数: 該当インデックス
     */
    const val AE00024 = "BLOB bind value is not supported for executeSelect. index=%s"

    /**
     * ## AE00025
     * ### 取得対象のカラムの型が存在しない（事実上あり得ない）
     *
     * 第1引数: 該当インデックス
     * 第2引数: カラムの型
     */
    const val AE00025 = "Unsupported cursor field type. index=%s, type=%s"

    /**
     * ## AE00026
     * ### 取得対象のテーブルにカラムのメタデータが存在しない場合
     *
     * 第1引数: 該当インデックス
     * 第2引数: カラムの型
     */
    const val AE00026 = "Column metadata not found. table=%s, alias=%s"

    /**
     * ## AE00027
     * ### 取得対象のテーブルに特定プロパティのメタデータが存在しない場合
     *
     * 第1引数: プロパティ名
     * 第2引数: テーブル名
     * 第3引数: テーブルエイリアス
     */
    const val AE00027 =
        "Column metadata not found. property=%s, table=%s, alias=%s"

    /**
     * ## AE00028
     * ### 取得対象のカラムが見つからない場合
     *
     * 第1引数: 該当プロパティ
     */
    const val AE00028 =
        "Column target not found. property=%s"

    /**
     * ## AE00029
     * ### 取得対象のカラムの値が見つからない場合
     *
     * 第1引数: カラム名
     */
    const val AE00029 =
        "Column value not found. column=%s"

    /*
     * AW00001 以降は、app モジュールで Warning が必要になった時点で追加する。
     * 現時点の app/src/main には Warning 用メッセージは見当たらない。
     */

    /**
     * ## CE00001
     * ### 集約関数に列引数が指定されていない場合
     */
    const val CE00001 = "Aggregate functions require a column argument."

    /**
     * ## CE00002
     * ### 引数数が不正で、1個を期待する場合
     */
    const val CE00002 = "Invalid number of arguments. Expected 1 argument."

    /**
     * ## CE00003
     * ### 引数数が不正で、1個または2個を期待する場合
     */
    const val CE00003 = "Invalid number of arguments. Expected 1 or 2 arguments."

    /**
     * ## CE00004
     * ### 引数数が不正で、2個を期待する場合
     */
    const val CE00004 = "Invalid number of arguments. Expected 2 arguments."

    /**
     * ## CE00005
     * ### 引数数が不正で、2個以上を期待する場合
     */
    const val CE00005 = "Invalid number of arguments. Expected 2 or more arguments."

    /**
     * ## CE00006
     * ### 引数数が不正で、3個を期待する場合
     */
    const val CE00006 = "Invalid number of arguments. Expected 3 arguments."

    /**
     * ## CE00007
     * ### CUSTOM 関数に SQL 文字列が指定されていない場合
     */
    const val CE00007 = "CUSTOM requires at least one argument."

    /**
     * ## CE00008
     * ### @Table アノテーションが付与されていない場合
     * ### 第1引数: 対象クラス名
     */
    const val CE00008 = "Class `%s` does not have the `@Table` annotation."

    /**
     * ## CE00009
     * ### クラス名を解決できない場合
     * ### 第1引数: 対象クラス完全修飾名
     */
    const val CE00009 = "Could not determine class name for %s."

    /**
     * ## CE00010
     * ### LogScope 名から LogScope を解決できない場合
     * ### 第1引数: LogScope 名
     */
    const val CE00010 = "No LogScope with name: %s"

    /**
     * ## CE00011
     * ### 同一プロパティに @Column と @Function が併用されている場合
     * ### 第1引数: プロパティ名
     * ### 第2引数: Entity 名
     * ### 第3引数: 定義 Entity 完全修飾名
     */
    const val CE00011 =
        "Property '%s' in entity '%s' cannot have both @Column and @Function. Source entity: '%s'."

    /**
     * ## CE00012
     * ### SELECT 対象プロパティが存在しない場合
     * ### 第1引数: Entity 名
     * ### 第2引数: 定義 Entity 完全修飾名
     */
    const val CE00012 =
        "Entity '%s' derived from '%s' has no selectable properties. All properties are hidden from SELECT."

    /**
     * ## CE00013
     * ### SELECT 対象プロパティ同士で alias が重複している場合
     * ### 第1引数: alias
     * ### 第2引数: Entity 名
     * ### 第3引数: 定義 Entity 完全修飾名
     */
    const val CE00013 =
        "Duplicate alias '%s' in entity '%s' defined in '%s'."

    /**
     * ## CE00014
     * ### ColumnFunction の完全修飾名を取得できない場合
     */
    const val CE00014 =
        "Failed to obtain the fully qualified name of ColumnFunction."

    /**
     * ## CE00015
     * ### ColumnFunction のシンプルネームを取得できない場合
     */
    const val CE00015 =
        "Failed to obtain the simple name of ColumnFunction."

    /**
     * ## CE00016
     * ### Null 値が許可されていない項目に Null が設定された場合
     */
    const val CE00016 = "Null value is not allowed for this field."

    /**
     * ## CE00017
     * ### データベース接続に失敗した場合
     */
    const val CE00017 = "Failed to connect to the database."

    /**
     * ## CE00018
     * ### クエリ実行に失敗した場合
     */
    const val CE00018 = "Failed to execute the query."

    /**
     * ## CE00019
     * ### データ挿入に失敗した場合
     */
    const val CE00019 = "Failed to insert data into the database."

    /**
     * ## CE00020
     * ### データ更新に失敗した場合
     */
    const val CE00020 = "Failed to update data in the database."

    /**
     * ## CE00021
     * ### データ削除に失敗した場合
     */
    const val CE00021 = "Failed to delete data from the database."

    /**
     * ## CW00001
     * ### hidden 列を含む alias 重複など、クエリ構築には影響しない alias 重複がある場合
     * ### 第1引数: alias
     * ### 第2引数: Entity 名
     * ### 第3引数: 定義 Entity 完全修飾名
     */
    const val CW00001 =
        "Query construction is not affected, but alias '%s' appears multiple times in entity '%s' defined in '%s'."
}