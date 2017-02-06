package com.deepoove.swagger.test;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.ChangedEndpoint;
import com.deepoove.swagger.diff.model.Endpoint;
import com.deepoove.swagger.diff.output.HtmlRender;
import com.deepoove.swagger.diff.output.MarkdownRender;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class SwaggerDiffTest {

    final String SWAGGER_V1_DOC = "http://petstore.swagger.io/v2/swagger.json";
    // String swagger_v1_doc = "petstore_v1.json";
    final String SWAGGER_V2_DOC = "petstore_v2.json";

    final String SWAGGER_EMPTY_DOC = "petstore_empty.json";

    @Test
    public void testEqual() {
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V1_DOC, SWAGGER_V1_DOC);
        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        Assert.assertTrue(newEndpoints.isEmpty());
        Assert.assertTrue(missingEndpoints.isEmpty());
        Assert.assertTrue(changedEndPoints.isEmpty());
    }

    @Test
    public void testNewApi() {
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_EMPTY_DOC, SWAGGER_V1_DOC);
        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        String html = new HtmlRender("Changelog",
                "http://deepoove.com/swagger-diff/stylesheets/demo.css")
                .render(diff);

        try {
            FileWriter fw = new FileWriter(
                    "src/test/resources/testNewApi.html");
            fw.write(html);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(newEndpoints.size() > 0);
        Assert.assertTrue(missingEndpoints.isEmpty());
        Assert.assertTrue(changedEndPoints.isEmpty());
    }

    @Test
    public void testDeprecatedApi() {
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V1_DOC, SWAGGER_EMPTY_DOC);
        List<Endpoint> newEndpoints = diff.getNewEndpoints();
        List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        String html = new HtmlRender("Changelog",
                "http://deepoove.com/swagger-diff/stylesheets/demo.css")
                .render(diff);

        try {
            FileWriter fw = new FileWriter(
                    "src/test/resources/testDeprecatedApi.html");
            fw.write(html);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(newEndpoints.isEmpty());
        Assert.assertTrue(missingEndpoints.size() > 0);
        Assert.assertTrue(changedEndPoints.isEmpty());
    }

    @Test
    public void testDiff() {
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V1_DOC, SWAGGER_V2_DOC);
        List<ChangedEndpoint> changedEndPoints = diff.getChangedEndpoints();
        String html = new HtmlRender("Changelog",
                "http://deepoove.com/swagger-diff/stylesheets/demo.css")
                .render(diff);

        try {
            FileWriter fw = new FileWriter(
                    "src/test/resources/testDiff.html");
            fw.write(html);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Assert.assertFalse(changedEndPoints.isEmpty());
    }

    @Test
    public void testDiffAndMarkdown() {
        SwaggerDiff diff = SwaggerDiff.compareV2(SWAGGER_V1_DOC, SWAGGER_V2_DOC);
        String render = new MarkdownRender().render(diff);
        try {
            FileWriter fw = new FileWriter(
                    "src/test/resources/testDiff.md");
            fw.write(render);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
