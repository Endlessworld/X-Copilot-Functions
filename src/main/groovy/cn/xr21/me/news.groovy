import groovy.json.JsonSlurper
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

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

class NewsQuery {
    @Parameter(name = "index", value = "The difference between the occurrence time of the news and today is 0 for today, 1 for yesterday, 2 for the day before yesterday, and so on.\n", required = false)
    public int index
}
@GPTFunction(name = "news", value = "Headlines, understand the world in 60 seconds a day. You can get news for a specific date by specifying the difference in days between the specified date and the\n")
static news(NewsQuery query) {
    try {
        String endpoint = "https://hub.onmicrosoft.cn/public/news?index=${query.index}"
        HttpGet httpGet = new HttpGet(endpoint);
        HttpResponse response = HttpClientBuilder.create().build().execute(httpGet);
        def data = new JsonSlurper().parseText(EntityUtils.toString(response.getEntity()))
        return data["data"]
    } catch (Exception e) {
        throw new RuntimeException("Execution failed:：" + e.getMessage())
    }
}
