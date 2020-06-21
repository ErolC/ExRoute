package com.erolc.compiler

import com.erolc.exroute.Extra
import com.sun.xml.internal.fastinfoset.util.StringArray
import org.yanex.takenoko.*
import java.io.Serializable
import javax.annotation.processing.Messager
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeKind
import kotlin.reflect.KClass

class StartMethodCreate(
    private val messager: Messager,
    private val koFile: KoFile,
    private val elements: Map<Element, Array<out Extra>>
) {

    init {
        elements.forEach { it ->
            val typeElement = it.key.toTypeElementOrNull()
            val arrayOfExtras = elements[it.key]
            koFile.import(typeElement?.qualifiedName.toString())

            val receiveNames = arrayOf("android.app.Activity", "androidx.fragment.app.Fragment")
            receiveNames.forEach {
                //构造方法
                koFile.function("to" + typeElement?.simpleName) {
                    receiverType(it)
                    arrayOfExtras?.forEach {
                        val type = it.getType()
                        param(it.value, type.koType)//构造形参
                        if (type.name.isNotEmpty()) {
                            koFile.import(type.name)
                        }
                    }
                    param("requestCode", "Int", -1)
//                this.comment("注释")//注释
//                    this.typeParam("newIntent","Intent")//方法的泛型
                    body {//构建方法体
                        val context = if (it.contains("Fragment")) "requireContext()" else "this"
                        appendln("val intent = Intent($context,${typeElement?.simpleName}::class.java)")
                        arrayOfExtras?.forEach {
                            appendln("intent.putExtra(\"${it.value}\",${it.value})")
                        }
                        appendln("startActivityForResult(intent,requestCode)")
                    }
                }

            }
        }

    }
}

class Test : Serializable

fun Element.toTypeElementOrNull(): TypeElement? {
    if (this !is TypeElement) {
        error("Invalid element type, class expected")
        return null
    }

    return this
}

