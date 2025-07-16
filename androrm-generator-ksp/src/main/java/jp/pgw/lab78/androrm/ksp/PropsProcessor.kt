package jp.pgw.lab78.androrm.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class PropsProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation("jp.pgw.lab78.androrm.ksp.annotation.GenerateProps")
        symbols.filterIsInstance<KSClassDeclaration>().forEach { classDecl ->
            // Props 生成
            generateProps(classDecl)

            // Projection 生成 ← ここを追加！
            generateProjections(classDecl)
        }
        return emptyList()
    }

    /**
     * ## プロパティ生成メソッド
     * ### Entity クラスからプロパティ文字列を生成する
     * ### 生成された文字列は、定数クラスとして自動生成される
     * @param classDecl クラス情報を管理するインスタンス：生成クラスの元ネタ
     */
    private fun generateProps(classDecl: KSClassDeclaration) {
        val className = classDecl.simpleName.asString()
        val pkg = classDecl.packageName.asString()
        val propsName = "${className.removeSuffix("Entity")}Props"

        logger.info(">>> Processing class: $className in $pkg")
        val props = classDecl.getAllProperties().map {
            it.simpleName.asString()
        }

        val fileSpec = FileSpec.builder(pkg, propsName)
            .addType(
                TypeSpec.objectBuilder(propsName).addModifiers(KModifier.PUBLIC).apply {
                    props.forEach {
                        addProperty(
                            PropertySpec.builder(it.uppercase(), String::class, KModifier.CONST)
                                .initializer("%S", it)
                                .build()
                        )
                    }
                }.build()
            ).build()
        fileSpec.writeTo(codeGenerator, Dependencies(false))
    }

    /**
     * ## データクラス生成メソッド
     * ### Entity クラスから Projection アノテーションに
     * ### 指定されたプロパティを抽出し 別の データクラスとして自動生成される
     * @param classDecl クラス情報を管理するインスタンス：生成クラスの元ネタ
     */
    private fun generateProjections(classDecl: KSClassDeclaration) {
        val projections = mutableListOf<Pair<String, List<KSPropertyDeclaration>>>()

        classDecl.annotations.forEach { ann ->
            val annotationName = ann.annotationType.resolve().declaration.qualifiedName?.asString()
            if (annotationName == "jp.pgw.lab78.androrm.ksp.annotation.Projection") {
                val name = ann.arguments.find { it.name?.asString() == "name" }?.value as? String ?: return@forEach
                val fields = (ann.arguments.find { it.name?.asString() == "fields" }?.value as? List<*>)
                    ?.mapNotNull { it as? String } ?: return@forEach

                val selectedProps = classDecl.getAllProperties()
                    .filter { fields.contains(it.simpleName.asString()) }
                    .toList()

                projections.add(name to selectedProps)
            }
            if (annotationName == "jp.pgw.lab78.androrm.ksp.annotation.Projections") {
                val value = ann.arguments.find { it.name?.asString() == "value" }?.value as? List<KSAnnotation> ?: return@forEach
                value.forEach {
                    val name = it.arguments.find { it.name?.asString() == "name" }?.value as? String ?: return@forEach
                    val fields = (it.arguments.find { it.name?.asString() == "fields" }?.value as? List<*>)
                        ?.mapNotNull { it as? String } ?: return@forEach

                    val selectedProps = classDecl.getAllProperties()
                        .filter { fields.contains(it.simpleName.asString()) }
                        .toList()

                    projections.add(name to selectedProps)
                }
            }
        }

        val pkg = classDecl.packageName.asString()
        val originalName = classDecl.simpleName.asString()

        for ((name, props) in projections) {
            val projectionClassName = "${originalName}${name}"

            val typeSpec = TypeSpec.classBuilder(projectionClassName)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .apply {
                            props.forEach {
                                val type = it.type.resolve().toTypeName()
                                addParameter(it.simpleName.asString(), type)
                            }
                        }
                        .build()
                )
                .apply {
                    props.forEach {
                        val type = it.type.resolve().toTypeName()
                        addProperty(
                            PropertySpec.builder(it.simpleName.asString(), type)
                                .initializer(it.simpleName.asString())
                                .build()
                        )
                    }
                }
                .build()

            val fileSpec = FileSpec.builder(pkg, projectionClassName)
                .addType(typeSpec)
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(false))
            logger.warn(">>> Generated Projection Class: $projectionClassName")
        }
    }

}

class PropsProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return PropsProcessor(environment.codeGenerator, environment.logger)
    }
}
