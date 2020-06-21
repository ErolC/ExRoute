package com.erolc.compiler

import org.yanex.takenoko.KoType
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import kotlin.reflect.KClass

class TypeModel(
    val koType: KoType,
    val typeMirror: TypeMirror? = null,
    val kclass: KClass<*>? = null
) {
    var name:String = ""
    init {
        typeMirror?.apply {
             if(this is DeclaredType){
                 name = (asElement() as TypeElement).qualifiedName.toString()
             }
        }
        kclass?.apply {
            name = qualifiedName.toString()
        }
    }
}