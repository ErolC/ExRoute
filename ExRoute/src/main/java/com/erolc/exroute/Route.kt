package com.erolc.exroute

import java.io.Serializable
import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
annotation class Route(vararg val value: Extra)

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class Extra(val value: String, val type: KClass<*>)


