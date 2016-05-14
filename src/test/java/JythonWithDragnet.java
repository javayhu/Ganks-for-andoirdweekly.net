import org.python.core.Py;
import org.python.core.PyInteger;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Jython User Guide: https://wiki.python.org/jython/UserGuide
 * <p>
 * As of Jython 2.5.1 an implementation of JSR 223 is bundled in jython.jar.
 * Simply add jython to your CLASSPATH and ask for the python script engine.
 * <p>
 * <p>
 * Dragnet: https://github.com/seomoz/dragnet
 * <p>
 * Dragnet isn't interested in the shiny chrome or boilerplate dressing of a web page.
 * It's interested in... 'just the facts.'
 * The machine learning models in Dragnet extract the main article content and optionally user generated comments from a web page.
 * They provide state of the art performance on variety of test benchmarks.
 * <p>
 * import requests
 * from dragnet import content_extractor, content_comments_extractor
 * <p>
 * # fetch HTML
 * url = 'https://moz.com/devblog/dragnet-content-extraction-from-diverse-feature-sets/'
 * r = requests.get(url)
 * <p>
 * # get main article without comments
 * content = content_extractor.analyze(r.content)
 * <p>
 * 两种方式都不行，module导入出错
 * http://www.cnblogs.com/lmyhao/p/3363385.html
 */
public class JythonWithDragnet {

    public static void main(String[] args) throws ScriptException {
        //jsr223();

        pythonInterpreter();

    }

    private static void pythonInterpreter() {
        //问题1：ImportError: No module named requests
        //添加sys.path
        //问题2：ImportError: No module named dragnet.blocks
        //是因为blocks是so文件？ from .blocks import Blockifier 导入出错
        PySystemState engineSys = new PySystemState();
        System.out.println(engineSys.path);
        engineSys.path.append(Py.newString("/Library/Python/2.7/site-packages"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7.zip"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/plat-darwin"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/plat-mac"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/plat-mac/lib-scriptpackages"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/lib-tk"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/lib-old"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/lib-dyload"));
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/site-packages"));
        Py.setSystemState(engineSys);
        System.out.println(engineSys.path);

        PythonInterpreter interp = new PythonInterpreter();
        interp.exec("import requests");
        interp.exec("from dragnet import content_extractor");
        interp.set("a", new PyInteger(42));
        interp.exec("url = 'https://moz.com/devblog/dragnet-content-extraction-from-diverse-feature-sets/'");
        interp.exec("r = requests.get(url)");
        interp.exec("content = content_extractor.analyze(r.content)");
        PyObject content = interp.get("content");
        System.out.println("content: " + content);
    }

    private static void jsr223() throws ScriptException {
        //问题1：NullPointer，需要设置sys.path
        //python模块路径 /Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/site-packages

        //问题2：SyntaxError: mismatched input 'as' expecting COLON
        //使用Jython2.7版本，不要使用Jython 2.5版本
        //http://stackoverflow.com/questions/12991057/jython-syntaxerror-mismatched-input-expecting-colon?rq=1

        //问题3：ImportError: No module named dragnet.blocks
        PySystemState engineSys = new PySystemState();
        engineSys.path.append(Py.newString("/Library/Frameworks/Python.framework/Versions/2.7/lib/python2.7/site-packages"));
        Py.setSystemState(engineSys);

        ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
        engine.eval("import requests");
        engine.eval("from dragnet import content_extractor");
        engine.eval("url = 'https://moz.com/devblog/dragnet-content-extraction-from-diverse-feature-sets/'");
        engine.eval("r = requests.get(url)");
        engine.eval("content = content_extractor.analyze(r.content)");
        Object content = engine.get("content");
        System.out.println("content: " + content);
    }
}