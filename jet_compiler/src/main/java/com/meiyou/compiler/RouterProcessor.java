package com.meiyou.compiler;

import com.google.auto.service.AutoService;
import com.meiyou.annotation.JUri;
import com.meiyou.router.RouterConstant;
import com.meiyou.router.model.RouteBean;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.TypeSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * 直接Build，由于Gradle的配置，代码不变，APT可能不执行，要执行clean + build;
 *
 * @author zhengxiaobin@xiaoyouzi.com
 * @since 17/7/13
 */
@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedAnnotationTypes({"com.meiyou.annotation.JUri"})
public class RouterProcessor extends AbstractProcessor {
    /**
     * APT 默认目录
     */
    Filer filer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        Filer filer = processingEnvironment.getFiler();
    }

    /**
     * @param annotations      所有支持的Annotation
     * @param roundEnvironment 当前环境
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnvironment) {
        try {
            HashMap<String, String> map = new HashMap<>();
            for (TypeElement annotation : annotations) {
//                System.out.println("anonotation: "+ annotation.toString());
                
                Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(annotation);
                for (Element element : elements) {
                    JUri uri = element.getAnnotation(JUri.class);
                    TypeElement typeElement = (TypeElement) element;

                    String value = uri.value();
                    String clazzName = typeElement.getQualifiedName().toString();
                    map.put(value, clazzName);
                }
            }
            //生成Java代码
            createJava(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * javapoet 🔚介绍
     * <p>
     * http://www.jianshu.com/p/95f12f72f69a
     * http://www.jianshu.com/p/76e9e3a8ec0f
     * http://blog.csdn.net/crazy1235/article/details/51876192
     * http://blog.csdn.net/qq_26376637/article/details/52374063
     *
     * @param map
     * @throws Exception
     */
    private void createJava(HashMap<String, String> map) throws Exception {

        CodeBlock.Builder builder = CodeBlock.builder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String clazz = entry.getValue();

            builder.add("$T.createBean(map,$S, $S);", RouteBean.class, key, clazz);
        }
        CodeBlock codeBlock = builder.build();

        FieldSpec field = FieldSpec.builder(HashMap.class, "map", Modifier.PUBLIC)
                                   .initializer(CodeBlock.of("new HashMap()")).build();

        TypeSpec typeSpec = TypeSpec.classBuilder(RouterConstant.ClassName)
                                    .addModifiers(Modifier.PUBLIC)
                                    .addField(field)
                                    .addStaticBlock(codeBlock)
                                    .build();
        JavaFile javaFile = JavaFile.builder(RouterConstant.PkgName, typeSpec).build();

        javaFile.writeTo(System.out);
//        javaFile.writeTo(filer);
    }
    // TODO: 17/7/14  APT 会执行多次， WriteTO 不成功； APT  调试

}