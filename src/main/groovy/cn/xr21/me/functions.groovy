import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.util.ArrayUtil
import groovy.json.JsonSlurper

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


//class BingSearchQuery {
//    @Parameter(name = "query", value = "Search keyword", required = true)
//    public String query
//
//    @Parameter(name = "count", value = "Return result count", required = true)
//    public int count
//}
//
//@GPTFunction(name = "searchBing", value = "Use Bing search API for web search\n")
//static searchBing(BingSearchQuery query) {
//    try {
//        String endpoint = "https://bing.search.api.xr21.me"
//        String url = "${endpoint}?query=${URLEncoder.encode(query.query, "UTF-8")}&count=5&answerCount=5&safeSearch=Off"
//        HttpGet httpGet = new HttpGet(url);
//        HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
//        return EntityUtils.toString(response.getEntity())
//    } catch (Exception e) {
//        throw new RuntimeException("Execution failed：" + e.getMessage())
//    }
//}


class Command {

    @Parameter(name = "command", value = "A commands on Windows, Linux, or macOS.", required = true)
    String command;
}

/**
 * 执行命令，支持 Windows、Linux 和 macOS
 *
 * @param command 命令对象，包含要执行的命令
 * @return 返回命令执行结果字符串
 */
@GPTFunction(name = "executeCommand", value = "Execute commands on Windows, Linux, and macOS.")
String executeCommand(Command command) {
    StringBuilder output = new StringBuilder();
    output.append("Execution successful.\n：");
    try {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(getActiveProject().basePath));
        // 根据操作系统选择命令
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            processBuilder.command("cmd.exe", "/c", command.command);
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            processBuilder.command("bash", "-c", command.command);
        } else {
            return "Unsupported operating system: " + os;
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        String line;
        // 逐行读取命令输出并添加到结果字符串中
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }
        process.waitFor();
    } catch (Throwable e) {
        e.printStackTrace();
        println("IOException: ${e.getMessage()}");
    }

    return output.toString();
}



class Keyword {
    @Parameter(name = "keyword", value = "The q keyword used when searching for GitHub repositories.", required = true)
    String keyword

    @Parameter(name = "page", value = "Specify the page number when searching for GitHub repositories.", required = true)
    Integer page = 1

    @Parameter(name = "per_page", value = "The q keyword used when searching for GitHub repositories.", required = true)
    Integer per_page

    @Parameter(name = "order", value = "asc/desc", required = false)
    String order

    @Parameter(name = "sort", value = "stars/forks/updated/help-wanted-issues/good-first-issues/best-match", required = false)
    String sort

    @Parameter(name = "language", value = "Specify the programming language.", required = false)
    String language

    @Parameter(name = "user", value = "Specify the owner of the repository.", required = false)
    String user
}


@GPTFunction(name = "githubSearch", value = "This method can be used to search for Git repositories.\n")
static String githubSearch(Keyword search) {
    StringBuilder output = new StringBuilder();
    def searchUrl = "https://api.github.com/search/repositories?" +
            "language=${search.language}" +
            "&user=${search.user}" +
            "&sort=${search.sort}" +
            "&order=${search.order}" +
            "&page=${search.page}" +
            "&per_page=${search.per_page}" +
            "&q=${URLEncoder.encode(search.keyword,'utf-8')}"
    def connection = new URL(searchUrl).openConnection()
    connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
    connection.connect()
    BufferedReader stdinReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
    String line;
    while ((line = stdinReader.readLine()) != null) {
        output.append(line).append("\n");
    }
    def slurper = new JsonSlurper()
    def data = slurper.parseText(output.toString())
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
//println(searchBing(new BingSearchQuery(query: "隔壁老王", count: 10)))

static Project getActiveProject() {
    return WriteAction.computeAndWait(() -> {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (ArrayUtil.isEmpty(projects)) {
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


