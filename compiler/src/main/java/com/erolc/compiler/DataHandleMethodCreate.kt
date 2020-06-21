package com.erolc.compiler

import com.erolc.exroute.Extra
import org.yanex.takenoko.*
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.ArrayType
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.PrimitiveType
import kotlin.reflect.KClass

class DataHandleMethodCreate(
    private val messager: Messager,
    private val koFile: KoFile,
    private val elements: Map<Element, Array<out Extra>>
) {
    init {

        elements.forEach { it ->

            val typeElement = it.key.toTypeElementOrNull()

            val arrayOfExtras = elements[it.key]
            arrayOfExtras?.forEach {
                koFile.function("get${it.value.firstUpperCase()}") {
                    receiverType(typeElement?.simpleName.toString())
                    val type = it.getType()

                    //make function body
                    body {

                        appendln("val intent = getIntent()")
                        var returnIntentMethod = ""
                        type.typeMirror?.apply {
                            when (this) {
                                is PrimitiveType -> {
                                    val firstUpperCase = toString().firstUpperCase()
                                    returnIntentMethod =
                                        "get${firstUpperCase}Extra(\"${it.value}\",${firstUpperCase.default})"
                                }
                                is ArrayType -> {
                                    if (componentType is PrimitiveType) {
                                        val firstUpperCase =
                                            componentType.toString().firstUpperCase()
                                        returnIntentMethod =
                                            "get${firstUpperCase}ArrayExtra(\"${it.value}\")"
                                    } else if (componentType is DeclaredType) {
                                        var asType = componentType.toString()
                                        var supertypeName =
                                            (componentType as DeclaredType).getSupertypeName()
                                        if (componentType.toString() == "java.lang.CharSequence") {
                                            supertypeName = "CharSequence"
                                            asType = supertypeName
                                        }
                                        if (supertypeName.isEmpty()) {
                                            messager.error("Array&$componentType is not implement Parcelable")
                                        }

                                        returnIntentMethod =
                                            "get${supertypeName}ArrayExtra(\"${it.value}\")?.map{ it as $asType}"
                                    }
                                }
                                is DeclaredType -> {
                                    val typeEle = asElement() as TypeElement
                                    returnIntentMethod =
                                        if (typeEle.simpleName.toString() == CharSequence::class.simpleName ||typeEle.simpleName.toString() == "String"|| typeEle.simpleName.toString() == "Bundle") {
                                            "get${typeEle.simpleName}Extra(\"${it.value}\")"
                                        } else {
                                            val superType = getSupertypeName()
                                            if (superType.isEmpty()) {
                                                messager.error("$this is not implement Serializable or Parcelable")
                                            }
                                            "get${superType}Extra(\"${it.value}\") as $this"
                                        }
                                }
                            }

                            returnType(
                                if (this is PrimitiveType) type.koType else KoNullableType(
                                    when (this) {
                                        is ArrayType -> getRealType()
                                        else -> type.koType
                                    }
                                )
                            )
                        }


                        type.kclass?.apply {

                            returnIntentMethod = when {
                                java.isArray -> {
                                    val componentType = java.componentType
                                    if (componentType.isPrimitive) {
                                        "get${componentType.simpleName.firstUpperCase()}ArrayExtra(\"${it.value}\")"
                                    } else {
                                        var asType = componentType.toString()
                                        var filter = ""
                                        componentType.interfaces.forEach {
                                            if (it.simpleName == "Parcelable") {
                                                filter = it.simpleName
                                            }
                                        }
                                        if (filter.isEmpty()) {
                                            messager.error("Array&$componentType is not implement Parcelable")
                                        }
                                        if (componentType.toString() == "java.lang.CharSequence") {
                                            filter = "CharSequence"
                                            asType = filter
                                        }

                                        "get${filter}ArrayExtra(\"${it.value}\")?.map{ it as $asType}"
                                    }
                                }
                                else -> {
                                    val interfaces = java.interfaces
                                    if (interfaces.isEmpty()) {//如果没有任何接口，那么就是基本数据类型
                                        "get${simpleName}Extra(\"${it.value}\",${simpleName.toString().default})"
                                    } else {//那么就是其他的数据类型，但也要区分是否是String,CharSequence或者是Bundle
                                        if (simpleName == String::class.simpleName || simpleName == CharSequence::class.simpleName || simpleName == "Bundle") {
                                            "get${simpleName}Extra(\"${it.value}\")"
                                        } else {
                                            var filter = ""
                                            interfaces.forEach {
                                                if (it.simpleName == "Parcelable" || it.simpleName == "Serializable") {
                                                    filter = it.simpleName
                                                }
                                            }
                                            if (filter.isEmpty()) {
                                                messager.error("$this is not implement Serializable or Parcelable")
                                            }
                                            "get${filter}Extra(\"${it.value}\") as ${this.simpleName}"
                                        }
                                    }
                                }
                            }
                            returnType(
                                if (this.java.isPrimitive) type.koType else KoNullableType(
                                    if (this.java.isArray) {
                                        getRealType()
                                    } else {
                                        type.koType
                                    }

                                )
                            )
                        }
                        appendln("return intent.$returnIntentMethod")
                    }

                }
            }
        }
    }
}

fun KClass<*>.getRealType(): KoType {
    val java = this.java
    val componentType = java.componentType
    return if (componentType.interfaces.isEmpty()) {
        KoClassType(this.simpleName.toString())
    } else {
        KoClassType(
            "kotlin.List",
            listOf(KoClassType(componentType.toString()))
        )
    }
}

fun ArrayType.getRealType(): KoType {
    return if ((componentType is PrimitiveType))//IntArray
        KoClassType(
            "kotlin.${componentType.toString().firstUpperCase()}Array"
        )
    else {
        var componentTypeName =
            if (componentType is DeclaredType) {
                (componentType as DeclaredType).asElement().simpleName
            } else
                componentType.toString()
        KoClassType(
            "kotlin.List",
            listOf(KoClassType(componentTypeName.toString()))
        )
    }

}

//获取类实现的接口中是否存在序列化接口
fun DeclaredType.getSupertypeName(): String {
    val typeEle = this.asElement() as TypeElement
    var superType = ""
    typeEle.interfaces.forEach {
        if (it is DeclaredType) {
            val simpleName = it.asElement().simpleName.toString()
            if (simpleName == "Serializable" || simpleName == "Parcelable") {
                superType = simpleName
            }
        }
    }
    return superType
}

private val String.default: String
    get() {
        return when (this) {
            "Boolean" -> "false"
            "Float" -> "0f"
            "Double" -> "0.0"
            "Char" -> "'0'"
            else -> "0"
        }
    }