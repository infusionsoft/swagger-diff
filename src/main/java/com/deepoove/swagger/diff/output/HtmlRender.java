package com.deepoove.swagger.diff.output;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.*;
import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;
import j2html.tags.ContainerTag;
import j2html.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static j2html.TagCreator.*;

public class HtmlRender implements OutputRender {
      private final String title;
      private final String linkCss;

      public HtmlRender() {
            this.title = "API change log";
            this.linkCss = "demo.css";
      }

      public HtmlRender(String title, String linkCss) {
            this.title = title;
            this.linkCss = linkCss;
      }

      public String render(SwaggerDiff diff) {
            final List<Endpoint> newEndpoints = diff.getNewEndpoints();
            final ContainerTag ol_newEndpoint = ol_newEndpoint(newEndpoints);

            final List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
            final ContainerTag ol_missingEndpoint = ol_missingEndpoint(missingEndpoints);

            final List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
            final Tag ol_changed = ol_changed(changedEndpoints);

            return renderHtml(ol_newEndpoint, ol_missingEndpoint, ol_changed);
      }

      private String renderHtml(Tag ol_new, Tag ol_miss, Tag ol_changed) {
            ContainerTag html = html().attr("lang", "en").with(
                    head().with(
                            meta().withCharset("utf-8"),
                            title(title),
                            link().withRel("stylesheet").withHref(linkCss)
                    ),
                    body().with(
                            header().with(h1(title)),
                            div().withClass("article").with(
                                    div().with(h2("What's New"), hr(), ol_new),
                                    div().with(h2("What's Deprecated"), hr(), ol_miss),
                                    div().with(h2("What's Changed"), hr(), ol_changed)
                            )
                    )
            );

            return document().render() + html.render();
      }

      private ContainerTag ol_newEndpoint(List<Endpoint> endpoints) {
            final ContainerTag ol = ol();
            if (endpoints != null) {
                  for (Endpoint endpoint : endpoints) {
                        ol.with(li_newEndpoint(endpoint.getMethod().toString(),
                                endpoint.getPathUrl(), endpoint.getSummary()));
                  }
            }

            return ol;
      }

      private ContainerTag li_newEndpoint(String method, String path,
                                          String desc) {
            return li().with(span(method).withClass(method)).withText(path + " ")
                    .with(span(desc));
      }

      private ContainerTag ol_missingEndpoint(List<Endpoint> endpoints) {
            final ContainerTag ol = ol();
            if (endpoints != null) {
                  for (Endpoint endpoint : endpoints) {
                        ol.with(li_missingEndpoint(endpoint.getMethod().toString(),
                                endpoint.getPathUrl(), endpoint.getSummary()));
                  }
            }

            return ol;
      }

      private ContainerTag li_missingEndpoint(String method, String path,
                                              String desc) {
            return li().with(span(method).withClass(method),
                    del().withText(path)).with(span(" " + desc));
      }

      private Tag ol_changed(List<ChangedEndpoint> changedEndpoints) {
            final ContainerTag ol = ol();
            if (null != changedEndpoints) {
                  for (ChangedEndpoint changedEndpoint : changedEndpoints) {
                        final String pathUrl = changedEndpoint.getPathUrl();
                        final Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint.getChangedOperations();
                        for (Entry<HttpMethod, ChangedOperation> entry : changedOperations.entrySet()) {
                              final String method = entry.getKey().toString();
                              final ChangedOperation changedOperation = entry.getValue();
                              final String desc = changedOperation.getSummary();

                              final ContainerTag ul_detail = ul().withClass("detail");
                              if (changedOperation.isDiffParam()) {
                                    ul_detail.with(li().with(h3("参数")).with(ul_param(changedOperation)));
                              }
                              if (changedOperation.isDiffProp()) {
                                    ul_detail.with(li().with(h3("返回类型")).with(ul_response(changedOperation)));
                              }
                              ol.with(li().with(span(method).withClass(method)).withText(pathUrl + " ").with(span(desc))
                                      .with(ul_detail));
                        }
                  }
            }

            return ol;
      }

      private Tag ul_response(ChangedOperation changedOperation) {
            final List<ElProperty> addProps = changedOperation.getAddProps();
            final List<ElProperty> delProps = changedOperation.getMissingProps();
            final ContainerTag ul = ul().withClass("change response");
            for (ElProperty prop : addProps) {
                  ul.with(li_addProp(prop));
            }
            for (ElProperty prop : delProps) {
                  ul.with(li_missingProp(prop));
            }

            return ul;
      }

      private Tag li_missingProp(ElProperty prop) {
            final Property property = prop.getProperty();

            return li().withClass("missing").withText("Delete").with(del(prop.getEl())).with(span(null == property.getDescription() ? "" : ("//" + property.getDescription())).withClass("comment"));
      }

      private Tag li_addProp(ElProperty prop) {
            final Property property = prop.getProperty();

            return li().withText("Add " + prop.getEl()).with(span(null == property.getDescription() ? "" : ("//" + property.getDescription())).withClass("comment"));
      }

      private Tag ul_param(ChangedOperation changedOperation) {
            final List<Parameter> addParameters = changedOperation.getAddParameters();
            final List<Parameter> delParameters = changedOperation.getMissingParameters();
            final List<ChangedParameter> changedParameters = changedOperation.getChangedParameter();
            final ContainerTag ul = ul().withClass("change param");
            for (Parameter param : addParameters) {
                  ul.with(li_addParam(param));
            }
            for (ChangedParameter param : changedParameters) {
                  final List<ElProperty> increased = param.getIncreased();
                  for (ElProperty prop : increased) {
                        ul.with(li_addProp(prop));
                  }
            }
            for (ChangedParameter param : changedParameters) {
                  final boolean changeRequired = param.isChangeRequired();
                  final boolean changeDescription = param.isChangeDescription();
                  if (changeRequired || changeDescription) {
                        ul.with(li_changedParam(param));
                  }
            }
            for (ChangedParameter param : changedParameters) {
                  final List<ElProperty> missing = param.getMissing();
                  for (ElProperty prop : missing) {
                        ul.with(li_missingProp(prop));
                  }
            }
            for (Parameter param : delParameters) {
                  ul.with(li_missingParam(param));
            }

            return ul;
      }

      private Tag li_addParam(Parameter param) {
            return li().withText("Add " + param.getName()).with(span(null == param.getDescription() ? "" : ("//" + param.getDescription())).withClass("comment"));
      }

      private Tag li_missingParam(Parameter param) {
            return li().withClass("missing").with(span("Delete")).with(del(param.getName())).with(span(null == param.getDescription() ? "" : ("//" + param.getDescription())).withClass("comment"));
      }

      private Tag li_changedParam(ChangedParameter changeParam) {
            final boolean changeRequired = changeParam.isChangeRequired();
            final boolean changeDescription = changeParam.isChangeDescription();
            final Parameter rightParam = changeParam.getRightParameter();
            final Parameter leftParam = changeParam.getLeftParameter();
            final ContainerTag li = li().withText(rightParam.getName());
            if (changeRequired) {
                  li.withText(" 修改为" + (rightParam.getRequired() ? "必填" : "非必填"));
            }
            if (changeDescription) {
                  li.withText(" 注释 ").with(del(leftParam.getDescription()).withClass("comment")).withText(" 改为 ").with(span(rightParam.getDescription()).withClass("comment"));
            }

            return li;
      }
}
