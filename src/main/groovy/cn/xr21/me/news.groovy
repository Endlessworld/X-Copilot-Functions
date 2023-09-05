import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

class NewsQuery {
    @Parameter(name = "index", value = "新闻发生时间与今天的差值 0 今天 1 昨天 2前天 依此类推", required = false)
    public int index
}
@GPTFunction(name = "news", value = "新闻头条、每天60秒读懂世界 可根据指定日期与当前时间的天数差获取指定日期的新闻")
static news(NewsQuery query) {
    try {
        String endpoint = "https://hub.onmicrosoft.cn/public/news?index=${query.index}"
        HttpGet httpGet = new HttpGet(endpoint);
        HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
        def data = new JsonSlurper().parseText(EntityUtils.toString(response.getEntity()))
        return data["data"]
    } catch (Exception e) {
        throw new RuntimeException("执行失败：" + e.getMessage())
    }
}
