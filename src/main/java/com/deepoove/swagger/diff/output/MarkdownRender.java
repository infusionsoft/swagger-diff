package com.deepoove.swagger.diff.output;

import com.deepoove.swagger.diff.SwaggerDiff;
import com.deepoove.swagger.diff.model.*;
import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.Property;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MarkdownRender implements OutputRender {
	final String H3 = "### ";
	final String BLOCKQUOTE = "> ";
	final String CODE = "`";
	final String PRE_CODE = "    ";
	final String PRE_LI = "    ";
	final String LI = "* ";
	final String HR = "---\n";

	public String render(SwaggerDiff diff) {
		List<Endpoint> newEndpoints = diff.getNewEndpoints();
		String ol_newEndpoint = ol_newEndpoint(newEndpoints);

		List<Endpoint> missingEndpoints = diff.getMissingEndpoints();
		String ol_missingEndpoint = ol_missingEndpoint(missingEndpoints);

		List<ChangedEndpoint> changedEndpoints = diff.getChangedEndpoints();
		String ol_changed = ol_changed(changedEndpoints);

		return renderMarkdown(ol_newEndpoint, ol_missingEndpoint, ol_changed);
	}

	private String renderMarkdown(String ol_new, String ol_miss, String ol_changed) {
		StringBuffer sb = new StringBuffer();
		sb.append(H3).append("What's New").append("\n").append(HR)
				.append(ol_new).append("\n").append(H3)
				.append("What's Deprecated").append("\n").append(HR)
				.append(ol_miss).append("\n").append(H3)
				.append("What's Changed").append("\n").append(HR)
				.append(ol_changed);
		return sb.toString();
	}

	private String ol_newEndpoint(List<Endpoint> endpoints) {
		if (null == endpoints) return "";
		StringBuffer sb = new StringBuffer();
		for (Endpoint endpoint : endpoints) {
			sb.append(li_newEndpoint(endpoint.getMethod().toString(),
					endpoint.getPathUrl(), endpoint.getSummary()));
		}
		return sb.toString();
	}

	private String li_newEndpoint(String method, String path, String desc) {
		StringBuffer sb = new StringBuffer();
		sb.append(LI).append(CODE).append(method).append(CODE)
				.append(" " + path).append(" " + desc + "\n");
		return sb.toString();
	}

	private String ol_missingEndpoint(List<Endpoint> endpoints) {
		if (null == endpoints) return "";
		StringBuffer sb = new StringBuffer();
		for (Endpoint endpoint : endpoints) {
			sb.append(li_newEndpoint(endpoint.getMethod().toString(),
					endpoint.getPathUrl(), endpoint.getSummary()));
		}
		return sb.toString();
	}

	private String ol_changed(List<ChangedEndpoint> changedEndpoints) {
		if (null == changedEndpoints) return "";
		StringBuffer sb = new StringBuffer();
		for (ChangedEndpoint changedEndpoint : changedEndpoints) {
			String pathUrl = changedEndpoint.getPathUrl();
			Map<HttpMethod, ChangedOperation> changedOperations = changedEndpoint
					.getChangedOperations();
			for (Entry<HttpMethod, ChangedOperation> entry : changedOperations
					.entrySet()) {
				String method = entry.getKey().toString();
				ChangedOperation changedOperation = entry.getValue();
				String desc = changedOperation.getSummary();

				StringBuffer ul_detail = new StringBuffer();
				if (changedOperation.isDiffParam()) {
					ul_detail.append(PRE_LI).append("参数")
							.append(ul_param(changedOperation));
				}
				if (changedOperation.isDiffProp()) {
					ul_detail.append(PRE_LI).append("返回类型")
							.append(ul_response(changedOperation));
				}
				sb.append(LI).append(CODE).append(method).append(CODE)
						.append(" " + pathUrl).append(" " + desc + "  \n")
						.append(ul_detail);
			}
		}
		return sb.toString();
	}

	private String ul_response(ChangedOperation changedOperation) {
		List<ElProperty> addProps = changedOperation.getAddProps();
		List<ElProperty> delProps = changedOperation.getMissingProps();
		StringBuffer sb = new StringBuffer("\n\n");
		for (ElProperty prop : addProps) {
			sb.append(PRE_LI).append(PRE_CODE).append(li_addProp(prop) + "\n");
		}
		for (ElProperty prop : delProps) {
			sb.append(PRE_LI).append(PRE_CODE)
					.append(li_missingProp(prop) + "\n");
		}
		return sb.toString();
	}

	private String li_missingProp(ElProperty prop) {
		Property property = prop.getProperty();
		StringBuffer sb = new StringBuffer("");
		sb.append("Delete ").append(prop.getEl())
				.append(null == property.getDescription() ? ""
						: (" //" + property.getDescription()));
		return sb.toString();
	}

	private String li_addProp(ElProperty prop) {
		Property property = prop.getProperty();
		StringBuffer sb = new StringBuffer("");
		sb.append("Add ").append(prop.getEl())
				.append(null == property.getDescription() ? ""
						: (" //" + property.getDescription()));
		return sb.toString();
	}

	private String ul_param(ChangedOperation changedOperation) {
		List<Parameter> addParameters = changedOperation.getAddParameters();
		List<Parameter> delParameters = changedOperation.getMissingParameters();
		List<ChangedParameter> changedParameters = changedOperation
				.getChangedParameter();
		StringBuffer sb = new StringBuffer("\n\n");
		for (Parameter param : addParameters) {
			sb.append(PRE_LI).append(PRE_CODE)
					.append(li_addParam(param) + "\n");
		}
		for (ChangedParameter param : changedParameters) {
			List<ElProperty> increased = param.getIncreased();
			for (ElProperty prop : increased) {
				sb.append(PRE_LI).append(PRE_CODE)
						.append(li_addProp(prop) + "\n");
			}
		}
		for (ChangedParameter param : changedParameters) {
			boolean changeRequired = param.isChangeRequired();
			boolean changeDescription = param.isChangeDescription();
			if (changeRequired || changeDescription) sb.append(PRE_LI)
					.append(PRE_CODE).append(li_changedParam(param) + "\n");
		}
		for (ChangedParameter param : changedParameters) {
			List<ElProperty> missing = param.getMissing();
			for (ElProperty prop : missing) {
				sb.append(PRE_LI).append(PRE_CODE)
						.append(li_missingProp(prop) + "\n");
			}
		}
		for (Parameter param : delParameters) {
			sb.append(PRE_LI).append(PRE_CODE)
					.append(li_missingParam(param) + "\n");
		}
		return sb.toString();
	}

	private String li_addParam(Parameter param) {
		StringBuffer sb = new StringBuffer("");
		sb.append("Add ").append(param.getName())
				.append(null == param.getDescription() ? ""
						: (" //" + param.getDescription()));
		return sb.toString();
	}

	private String li_missingParam(Parameter param) {
		StringBuffer sb = new StringBuffer("");
		sb.append("Delete ").append(param.getName())
				.append(null == param.getDescription() ? ""
						: (" //" + param.getDescription()));
		return sb.toString();
	}

	private String li_changedParam(ChangedParameter changeParam) {
		boolean changeRequired = changeParam.isChangeRequired();
		boolean changeDescription = changeParam.isChangeDescription();
		Parameter rightParam = changeParam.getRightParameter();
		Parameter leftParam = changeParam.getLeftParameter();
		StringBuffer sb = new StringBuffer("");
		sb.append(rightParam.getName());
		if (changeRequired) {
			sb.append(" 修改为" + (rightParam.getRequired() ? "必填" : "非必填"));
		}
		if (changeDescription) {
			sb.append(" 注释 ").append(leftParam.getDescription()).append(" 改为 ")
					.append(rightParam.getDescription());
		}
		return sb.toString();
	}
}
