package ro.simavi.sphinx.dtm;

import org.junit.Test;
import org.python.core.*;
import org.python.util.PythonInterpreter;
import ro.simavi.sphinx.dtm.util.PythonHelper;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class PythonTest {

    //@Test
    public void test1() throws Exception {

        List<String> list = new ArrayList<>();
        list.add("eth0");
        list.add("wifi");
      //  engine.put(ScriptEngine.ARGV,list);

//        StringWriter writer = new StringWriter();
//        ScriptContext context = new SimpleScriptContext();
//        context.setAttribute("interfaces",list, ScriptContext.ENGINE_SCOPE);
//        context.setWriter(writer);

        ScriptEngineManager manager = new ScriptEngineManager();
        Options.importSite = false;
        ScriptEngine engine = manager.getEngineByName("python");
        assertNotNull(engine);
//        if (engine!=null) {
//            String filePath = resolvePythonScriptPath("hello.py");
//            FileReader fileReader = new FileReader(filePath);
//            engine.eval(fileReader, context);
//
//            System.out.println(writer.toString().trim());
//            //assertEquals("Should contain script output: ", "Hello World!!", writer.toString().trim());
//        }

        // 2. call a method
         PythonInterpreter interpreter = new PythonInterpreter();
//        try {
//            String filePath = resolvePythonScriptPath("hello2.py");
//            interpreter.execfile(filePath);
//            PyFunction pyFunction = interpreter.get("hello", PyFunction.class);
//            PyObject pyObject = pyFunction.__call__();
//            System.out.println(pyObject);
//        } catch (Exception e) {
//            e.printStackTrace();
//            e.getMessage();
//            e.toString();
//        }

        // 3. call a method class
        try {

            String filePath = resolvePythonScriptPath("feature-test.py");
            interpreter.execfile(filePath);
            PyClass pyClass = interpreter.get("Feature", PyClass.class);
            PyObject pyObject = pyClass.__call__(); // obtinut instanta clasei

            PyFunction pyFunction = interpreter.get("start", PyFunction.class);
            System.out.println(pyFunction.__call__(new PyString("xxxx.pcap")));

            PyObject pcapPyMethod = pyObject.__getattr__("collectFromPcap");
            System.out.println(pcapPyMethod.__call__(new PyString("aaa")));

            PyObject interfacePyMethod = pyObject.__getattr__("collectFromInterfaces");
            System.out.println(interfacePyMethod.__call__(new PyList(list)));


        } catch (Exception e) {
            e.printStackTrace();
            e.getMessage();
            e.toString();
        }

        PythonHelper pythonHelper = new PythonHelper();
        pythonHelper.start("python",resolvePythonScriptPath("feature-test.py"), "xxxx.pcap", "/home/sphinx/nfstream/flowNf", "collectTest");
    }


    //@Test
    public void test2() throws Exception {
        PythonHelper pythonHelper = new PythonHelper();
        pythonHelper.start("python",resolvePythonScriptPath("feature-test.py"), "xxxx.pcap", "/home/sphinx/nfstream/flowNf", "collectTest");
    }

    private String resolvePythonScriptPath(String filename) {
        File file = new File("src/test/resources/" + filename);
        return file.getAbsolutePath();
    }

}
