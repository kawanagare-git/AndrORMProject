package jp.pgw.lab78.androrm.database.validation

import jp.pgw.lab78.androrm.common.MessageConstants.AE00010

/**
 * ## QueryBuilder メソッド呼び出し識別子
 * ### where / order / limit など、1回だけ指定可能なメソッドを識別する
 * @author Masahiro Inoue
 * @since 2026-05-14
 */
interface QueryMethodCall {
    val methodName: String
}

/**
 * ## 重複メソッド呼び出し検証
 * ### 同一 QueryBuilder インスタンス内で、同じメソッドが複数回指定されないことを検証する
 *
 * ### 仕様
 * #### 呼び出し済み識別子を保持し、同じ識別子の2回目以降の指定を `IllegalStateException` として拒否する。
 * @param ownerName 検証対象クラス名
 * @author Masahiro Inoue
 * @since 2026-05-14
 */
class DuplicateMethodCallValidator<M : QueryMethodCall>(
    private val ownerName: String,
) {
    private val calledMethodSet = mutableSetOf<M>()

    /**
     * ## 重複メソッド呼び出し検証
     * ### 既に同一メソッドが呼び出されている場合は例外を発生させる
     * @param methodCall 検証対象メソッド
     * @throws IllegalStateException 同一メソッドが複数回指定された場合にスローされる例外
     * @author Masahiro Inoue
     * @since 2026-05-14
     */
    fun validateNoDuplicateMethodCall(methodCall: M) {
        check(calledMethodSet.add(methodCall)) { AE00010.format(ownerName, methodCall.methodName) }
    }
}