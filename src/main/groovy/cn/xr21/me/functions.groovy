import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

import java.awt.*
import java.lang.annotation.*

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface Parameter {

    String value() default "";

    String name() default "";

    boolean required() default false;

}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@interface GPTFunction {

    String value() default "";

    String name() default "";

}

class BingSearchQuery {
    @Parameter(name = "query", value = "搜索关键字", required = true)
    public String query

    @Parameter(name = "count", value = "返回结果数量", required = true)
    public int count
}

@GPTFunction(name = "searchBing", value = "使用Bing搜索API进行Web搜索")
static searchBing(BingSearchQuery query) {
    try {
        String endpoint = "https://bing.search.api.xr21.me"
        String url = "${endpoint}?query=${URLEncoder.encode(query.query, "UTF-8")}&count=5&answerCount=5&safeSearch=Off"
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
        return EntityUtils.toString(response.getEntity())
    } catch (Exception e) {
        throw new RuntimeException("执行失败：" + e.getMessage())
    }
}

class NewsQuery {
    @Parameter(name = "index", value = "页码最大3", required = false)
    public int index
}
@GPTFunction(name = "news", value = "今日新闻头条、每天60秒读懂世界")
static news(NewsQuery query) {
    try {
        String endpoint = "https://hub.onmicrosoft.cn/public/news?index=1"
        HttpGet httpGet = new HttpGet(endpoint);
        HttpResponse response = HttpClientBuilder.create().build().execute(httpGet)
        return EntityUtils.toString(response.getEntity())
    } catch (Exception e) {
        throw new RuntimeException("执行失败：" + e.getMessage())
    }
}


class Command {

    @Parameter(name = "command", value = "一个完整的powershell命令脚本、必须可以在客户端本地主机直接执行", required = true)
    String command
}

/**
 * 执行powershell脚本，参数必须可以在客户端本地主机直接执行
 *
 * @param command 命令对象，包含要执行的powershell命令脚本
 * @return 返回cmd命令执行结果字符串
 */
@GPTFunction(name = "executeCommand", value = "执行powershell脚本，你可以通过这个回调函数执行powershell命令、以实现读取文件、访问网络、启动软件 以控制用户本地计算机")
/**
 * 执行命令并返回执行结果
 * @param command 命令对象
 * @return 执行结果字符串
 */
String executeCommand(Command command) {
    StringBuilder output = new StringBuilder()
    output.append("执行成功：")
    try {
        ProcessBuilder processBuilder = new ProcessBuilder()
        processBuilder.directory(new File(getActiveProject().basePath))
        processBuilder.command("powershell.exe", "/c", command.command)
        processBuilder.redirectErrorStream(true)
        Process process = processBuilder.start()
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))
        String line
        // 逐行读取命令输出并添加到结果字符串中
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n")
        }
        process.waitFor();
    } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        println("IOException: ${e.getMessage()}");
    }

    return output;
}


class Keyword {
    @Parameter(name = "keyword", value = "搜索github仓库时用的q关键词", required = true)
    String keyword

    @Parameter(name = "page", value = "搜索github仓库时指定第几页", required = true)
    Integer page = 1

    @Parameter(name = "per_page", value = "搜索github仓库时指定每页返回条数", required = true)
    Integer per_page

    @Parameter(name = "order", value = "排序顺序 asc/desc", required = false)
    String order

    @Parameter(name = "sort", value = "排序方式 stars/forks/updated/help-wanted-issues/good-first-issues/best-match", required = false)
    String sort

    @Parameter(name = "language", value = "指定编程语言", required = false)
    String language

    @Parameter(name = "user", value = "指定搜索仓库的所有者", required = false)
    String user
}


@GPTFunction(name = "githubSearch", value = "通过此方法可以搜索git仓库")
static String githubSearch(Keyword search) {
    StringBuilder output = new StringBuilder();
    def searchUrl = "https://api.github.com/search/repositories?" + "language=${search.language}" + "&user=${search.user}" + "&sort=${search.sort}" + "&order=${search.order}" + "&page=${search.page}" + "&per_page=${search.per_page}" + "&q=${search.keyword}"
    def connection = new URL(searchUrl).openConnection()
    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
    connection.connect()
    BufferedReader stdinReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line;
    while ((line = stdinReader.readLine()) != null) {
        output.append(line).append("\n");
    }
    def JSON = new JsonSlurper()
    def data = JSON.parseText(output.toString())
    def result = data["items"].collect { item ->
        def projectName = item.name
        def projectUrl = item.html_url
        return [name            : projectName,
                url             : projectUrl,
                language        : item.language,
                stargazers_count: item.stargazers_count,
                forks           : item.forks,
                description     : item.description]

    }
    return result
}

/*******************************TEST**************************************/
command = new Command()
command.command = "ipconfig"
println(executeCommand(command))
/*******************************TEST**************************************/
key = new Keyword()
key.keyword = "gpt"
key.page = 1
key.per_page = 10
println(githubSearch(key))
/*******************************TEST**************************************/
println(searchBing(new BingSearchQuery(query: "隔壁老王", count: 10)))

static Project getActiveProject() {
    return WriteAction.computeAndWait(() -> {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (Arrays.isNullOrEmpty(projects)) {
            return ProjectManager.getInstance().getDefaultProject();
        }
        if (projects.length == 1) {
            return projects[0];
        }
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                return project;
            }
        }
        return projects[0];
    });
}


