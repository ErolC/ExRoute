package com.erolc.compiler

import com.erolc.exroute.Extra
import com.erolc.exroute.Route
import org.yanex.takenoko.*
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.*
import javax.lang.model.util.Elements
import javax.lang.model.util.Types
import javax.tools.Diagnostic
import kotlin.reflect.KClass


class Compiler : AbstractProcessor() {
    private lateinit var typeUtils: Types
    private lateinit var elementUtils: Elements
    lateinit var messager: Messager
    private val elements = mutableMapOf<Element, Array<out Extra>>()


    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"

    }

    override fun init(environment: ProcessingEnvironment) {
        typeUtils = environment.typeUtils
        elementUtils = environment.elementUtils
        messager = environment.messager
        super.init(environment)
    }


    override fun process(
        types: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val routeList = roundEnv.getElementsAnnotatedWith(Route::class.java)
        if (routeList.isEmpty()) return false

        //输出的目录
        val kaptKotlinGeneratedDir =
            processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME] ?: run {
                error("Can't find the target directory for generated Kotlin files.")
                return false
            }

        //得到注解中的数据
        routeList.forEach { it ->
            val annotation = it.getAnnotation(Route::class.java)
            elements[it] = annotation.value
        }


        //生成类
        val kotlinFile = kotlinFile("com.erolc.route") {
            import()//导入需要用到的类
            StartMethodCreate(messager, this, elements)//生成打开activity的方法
            DataHandleMethodCreate(messager, this, elements)//生成各个activity处理intent数据的方法
        }

        //生成文件
        File(kaptKotlinGeneratedDir, "Route.kt").apply {
            parentFile.mkdirs()
            writeText(kotlinFile.accept(PrettyPrinter(PrettyPrinterConfiguration())))
        }
        return true
    }

    //该处理器支持的注解
    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(Route::class.java.canonicalName)
    }
    //该处理器支持的版扽
    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.RELEASE_8
    }
    //该处理器支持的选项
    override fun getSupportedOptions(): MutableSet<String> {
        return mutableSetOf(KAPT_KOTLIN_GENERATED_OPTION_NAME)
    }
}

//一些有可能用到的类的导入
fun KoFile.import() {
    import("android.content.Intent")

}

//解析Route中的数据Extra中的数据的类型，也就是type
//由于type的类型是KClass，而class或者KClass都是编译之后才能得到的，对于当前项目中没有经过编译的一些类来说，在处理器处理阶段（尚未编译），是无法得到其class对象的
//所以只能通过别的方式获得相关信息
fun Extra.getType(): TypeModel {
    var result: TypeModel
    try {
        val clazz: KClass<*> = type
        result = TypeModel(KoType.parseType(clazz::class.java), kclass = clazz)//正常的获取途径
    } catch (mte: MirroredTypeException) {
        val typeMirror = mte.typeMirror//别的方法，typeMirror对象中存在有type的一些信息。
        result = when (typeMirror) {
            is PrimitiveType -> TypeModel(//如果该类型是基本数据类型
                KoType.parseType(typeMirror.toString().firstUpperCase()),
                typeMirror
            )
            is ArrayType -> {//如果该类型是数组类型
                val oldType = typeMirror.componentType
                var typeName = oldType.toString()
                if (oldType is PrimitiveType) {
                    typeName = typeName.firstUpperCase()
                }
                return TypeModel(
                    KoClassType("kotlin.Array", listOf(KoType.parseType(typeName))),
                    typeMirror
                )
            }
            is DeclaredType -> {//如果该类型是普通的类型（不是八大基本数据类型，也不是数组类型）
                val typeElement = typeMirror.asElement() as TypeElement
                TypeModel(KoType.parseType(typeElement.simpleName.toString()), typeMirror)
            }
            else -> {//对于其他的未知情况，待以后考虑
                TypeModel(KoType.parseType(typeMirror.toString()), typeMirror)
            }
        }
    }
    return result
}

//错误的输出方法
fun Messager.error(message: String) = printMessage(Diagnostic.Kind.ERROR, "RouteError: $message\n\r")

//将字符串的首位变大写
fun String.firstUpperCase(): String {
    if (isEmpty()) {
        return this
    }
    val toUpperCase = substring(0, 1).toUpperCase()
    return replace(get(0).toString(), toUpperCase)
}

//普通消息的输出方法
fun Messager.message(message: String) =
    printMessage(Diagnostic.Kind.NOTE, message + "\n\r")

