package org.tdr.webresource.test;

import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.core.ThreadUnsafeLessCompiler;

import java.io.*;

/**
 * @author nic
 */
public class TestLessCompile {

    static public void main(String[] args) throws Throwable {
        //LessCompiler lessCompiler = new LessCompiler();
 //       lessCompiler.compile(new File("web-resource-runtime/src/test/resources/test/style.less"),
 //                             new File("web-resource-runtime/src/test/resources/test/style-compile.css"));

      //  lessCompiler.compile(new File("web-resource-runtime/src/test/resources/common/icon.less"),
      //          new File("web-resource-runtime/src/test/resources/common/icon.css"));


        /*
        Reader reader = new FileReader("web-resource-runtime/src/test/resources/common/test.less");
        Writer writer = new FileWriter("web-resource-runtime/src/test/resources/common/test.css");

        new RhinoLessCssProcessor().process(null, reader, writer);
        */

        File lessFile = new File("web-resource-runtime/src/test/resources/common/test.less");
        System.out.println("Compiling less file: " + lessFile);
        LessCompiler compiler = new ThreadUnsafeLessCompiler();
        LessCompiler.CompilationResult result = compiler.compile(lessFile);
        System.out.println("Warnings: " +  result.getWarnings());
        System.out.println("CSS: \n" + result.getCss());
    }
}
