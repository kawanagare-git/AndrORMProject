package jp.pgw.lab78.androrm.database.entities.define

import jp.pgw.lab78.androrm.common.database.annotation.*
import jp.pgw.lab78.androrm.common.dml.interfaces.TableDefinitionEntity
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ## 大項目テスト用 Entity
 * ### CREATE TABLE / INDEX / UNIQUE 生成確認用
 */
@Table(name = "TEST_LARGE_ENTITY", alias = "TLE")
@Unique(
    name = "UQ_TEST_CODE",
    properties = ["code"],
)
@Unique(
    name = "UQ_TEST_PERSONAL_INFO",
    properties = ["name", "kanaName", "gender", "birthday", "placeOfBirth"],
)
@Index(
    name = "IDX_TEST_LARGE_ENTITY_ACTIVE_CREATED",
    properties = ["active", "createdAt"],
)
data class TestLargeEntity(
    @PrimaryKey
    @Column(name = "ID")
    val id: Long,

    @Column(name = "CODE")
    val code: String,

    @Column(name = "NAME")
    val name: String,

    @Column(name = "FURIGANA")
    val kanaName: String,

    @Column(name = "GENDER")
    val gender: String,

    @Column(name = "BIRTHDAY")
    val birthday: LocalDate,

    @Column(name = "PLACE_OF_BIRTH")
    val placeOfBirth: String,

    @Column(name = "EMAIL")
    val email: String,

    @Column(name = "PHONE_NUMBER")
    val phoneNumber: String,

    @Column(name = "POSTAL_CODE")
    val postalCode: String,

    @Column(name = "PREFECTURE")
    val prefecture: String,

    @Column(name = "CITY")
    val city: String,

    @Column(name = "ADDRESS_LINE")
    val addressLine: String,

    @Column(name = "SCORE")
    val score: Double,

    @Column(name = "BALANCE")
    val balance: Long,

    @Column(name = "ACTIVE")
    val active: Boolean,

    @Column(name = "REGISTERED_AT")
    val registeredAt: LocalDateTime,

    @Column(name = "LAST_LOGIN_AT")
    val lastLoginAt: LocalDateTime,

    @Column(name = "MEMO")
    val memo: String,

    @Column(name = "CREATED_AT")
    val createdAt: LocalDateTime,

    @Column(name = "UPDATED_AT")
    val updatedAt: LocalDateTime,
) : TableDefinitionEntity